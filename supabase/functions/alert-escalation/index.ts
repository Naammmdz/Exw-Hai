import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
const fcmProjectId = Deno.env.get("FCM_PROJECT_ID") ?? "";
const fcmClientEmail = Deno.env.get("FCM_CLIENT_EMAIL") ?? "";
const fcmPrivateKey = (Deno.env.get("FCM_PRIVATE_KEY") ?? "").replace(/\\n/g, "\n");

const supabase = createClient(supabaseUrl, serviceRoleKey);

Deno.serve(async () => {
  const now = new Date().toISOString();
  const { data: jobs, error } = await supabase
    .from("alert_jobs")
    .select("id,incident_id,user_id,run_at,status")
    .eq("status", "scheduled")
    .lte("run_at", now);

  if (error) return json({ error: error.message }, 500);

  const results: unknown[] = [];
  for (const job of jobs ?? []) {
    const { data: incident } = await supabase
      .from("alert_incidents")
      .select("*")
      .eq("id", job.incident_id)
      .maybeSingle();
    if (!incident) continue;

    const { data: profile } = await supabase
      .from("profiles")
      .select("display_name")
      .eq("id", job.user_id)
      .single();

    const displayName = profile?.display_name ?? "A trusted contact";
    const title = "Safety alert escalated";
    const body = `${displayName} may need attention. Last check-in was missed.`;

    const { data: circleMembers } = await supabase
      .from("circle_members")
      .select("member_user_id,invited_contact")
      .eq("owner_user_id", job.user_id)
      .eq("status", "accepted");

    const recipientIds = new Set<string>();
    for (const member of circleMembers ?? []) {
      if (member.member_user_id) recipientIds.add(member.member_user_id);
    }

    const { data: emergencyContacts } = await supabase
      .from("emergency_contacts")
      .select("name,contact,auto_notify")
      .eq("user_id", job.user_id)
      .eq("auto_notify", true);

    for (const recipientId of recipientIds) {
      const { data: notification } = await supabase
        .from("notifications")
        .insert({
          user_id: recipientId,
          type: "missed_check_in",
          title,
          body,
          related_entity_id: incident.id,
        })
        .select()
        .single();

      if (notification) {
        await supabase.from("notification_deliveries").insert({
          notification_id: notification.id,
          user_id: job.user_id,
          recipient_user_id: recipientId,
          channel: "push",
          status: "pending",
        });
        await sendPush(recipientId, title, body);
      }
    }

    for (const contact of emergencyContacts ?? []) {
      await supabase.from("notification_deliveries").insert({
        user_id: job.user_id,
        recipient_contact: contact.contact,
        channel: "sms",
        status: "pending",
        error_message: "SMS provider not configured",
      });
    }

    await supabase
      .from("alert_incidents")
      .update({ status: "escalated" })
      .eq("id", incident.id);

    await supabase
      .from("alert_jobs")
      .update({ status: "sent" })
      .eq("id", job.id);

    await supabase.from("audit_logs").insert({
      user_id: job.user_id,
      actor_user_id: job.user_id,
      action: "alert_escalated",
      metadata: incident.id,
    });

    results.push({ job_id: job.id, incident_id: incident.id, recipients: recipientIds.size });
  }

  return json({ processed: results.length, results });
});

async function sendPush(userId: string, title: string, body: string) {
  if (!fcmProjectId || !fcmClientEmail || !fcmPrivateKey) return;
  const accessToken = await getFcmAccessToken();
  if (!accessToken) return;

  const { data: tokens } = await supabase
    .from("device_tokens")
    .select("token")
    .eq("user_id", userId)
    .eq("is_active", true);

  for (const row of tokens ?? []) {
    await fetch(`https://fcm.googleapis.com/v1/projects/${fcmProjectId}/messages:send`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${accessToken}`,
      },
      body: JSON.stringify({
        message: {
          token: row.token,
          notification: { title, body },
          data: { title, body, source: "alert-escalation" },
        },
      }),
    });
  }
}

async function getFcmAccessToken(): Promise<string | null> {
  const now = Math.floor(Date.now() / 1000);
  const header = btoa(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claim = btoa(JSON.stringify({
    iss: fcmClientEmail,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
  }));
  const unsigned = `${header}.${claim}`;
  const key = await crypto.subtle.importKey(
    "pkcs8",
    pemToArrayBuffer(fcmPrivateKey),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    key,
    new TextEncoder().encode(unsigned),
  );
  const jwt = `${unsigned}.${base64UrlEncode(new Uint8Array(signature))}`;
  const response = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=${jwt}`,
  });
  const payload = await response.json();
  return payload.access_token ?? null;
}

function pemToArrayBuffer(pem: string): ArrayBuffer {
  const cleaned = pem
    .replace("-----BEGIN PRIVATE KEY-----", "")
    .replace("-----END PRIVATE KEY-----", "")
    .replace(/\s+/g, "");
  const binary = atob(cleaned);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
  return bytes.buffer;
}

function base64UrlEncode(bytes: Uint8Array): string {
  let binary = "";
  bytes.forEach((byte) => { binary += String.fromCharCode(byte); });
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
