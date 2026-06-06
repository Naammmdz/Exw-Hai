import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
const fcmProjectId = Deno.env.get("FCM_PROJECT_ID") ?? "";
const fcmClientEmail = Deno.env.get("FCM_CLIENT_EMAIL") ?? "";
const fcmPrivateKey = (Deno.env.get("FCM_PRIVATE_KEY") ?? "").replace(/\\n/g, "\n");

const supabase = createClient(supabaseUrl, serviceRoleKey);

Deno.serve(async () => {
  const now = new Date();
  const { data: settings, error } = await supabase
    .from("safety_settings")
    .select("user_id,inactivity_hours,escalation_delay_minutes");

  if (error) return json({ error: error.message }, 500);

  const results: unknown[] = [];
  for (const setting of settings ?? []) {
    const { data: profile } = await supabase
      .from("profiles")
      .select("id,display_name,last_safe_at")
      .eq("id", setting.user_id)
      .single();
    if (!profile?.last_safe_at) continue;

    const lastSafe = new Date(profile.last_safe_at);
    const missed = now.getTime() - lastSafe.getTime() >= setting.inactivity_hours * 60 * 60 * 1000;
    if (!missed) continue;

    const escalationDue = new Date(now.getTime() + setting.escalation_delay_minutes * 60 * 1000).toISOString();
    const { data: existingIncident } = await supabase
      .from("alert_incidents")
      .select("*")
      .eq("user_id", setting.user_id)
      .eq("reason", "missed_check_in")
      .eq("last_safe_at", profile.last_safe_at)
      .in("status", ["active", "escalated"])
      .maybeSingle();

    const { data: incident } = existingIncident
      ? { data: existingIncident }
      : await supabase
        .from("alert_incidents")
        .insert({
          user_id: setting.user_id,
          status: "active",
          reason: "missed_check_in",
          last_safe_at: profile.last_safe_at,
          escalation_due_at: escalationDue,
        })
        .select()
        .single();

    if (!incident) continue;

    const notification = {
      user_id: setting.user_id,
      type: "missed_check_in",
      title: "Missed check-in detected",
      body: "Your safety rhythm needs attention.",
      related_entity_id: incident.id,
    };
    const { data: insertedNotification } = await supabase
      .from("notifications")
      .insert(notification)
      .select()
      .single();

    await supabase.from("alert_jobs").upsert(
      {
        incident_id: incident.id,
        user_id: setting.user_id,
        run_at: escalationDue,
        status: "scheduled",
      },
      { onConflict: "incident_id,user_id,run_at" },
    );

    if (insertedNotification) {
      await sendPush(setting.user_id, insertedNotification.title, insertedNotification.body);
    }
    results.push({ user_id: setting.user_id, incident_id: incident.id });
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
          data: { title, body, source: "safety-automation" },
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
