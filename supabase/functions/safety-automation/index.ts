import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
const fcmServerKey = Deno.env.get("FCM_SERVER_KEY") ?? "";

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

    await supabase.from("alert_jobs").upsert({
      incident_id: incident.id,
      user_id: setting.user_id,
      run_at: escalationDue,
      status: "scheduled",
    });

    if (insertedNotification) {
      await sendPush(setting.user_id, insertedNotification.title, insertedNotification.body);
    }
    results.push({ user_id: setting.user_id, incident_id: incident.id });
  }

  return json({ processed: results.length, results });
});

async function sendPush(userId: string, title: string, body: string) {
  if (!fcmServerKey) return;
  const { data: tokens } = await supabase
    .from("device_tokens")
    .select("token")
    .eq("user_id", userId)
    .eq("is_active", true);
  for (const row of tokens ?? []) {
    await fetch("https://fcm.googleapis.com/fcm/send", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `key=${fcmServerKey}`,
      },
      body: JSON.stringify({
        to: row.token,
        notification: { title, body },
        data: { title, body, source: "safety-automation" },
      }),
    });
  }
}

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
