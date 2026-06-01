import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
const webhookSecret = Deno.env.get("SEPAY_WEBHOOK_SECRET") ?? "";

const supabase = createClient(supabaseUrl, serviceRoleKey);

Deno.serve(async (request) => {
  if (request.method !== "POST") return json({ error: "Method not allowed" }, 405);
  if (webhookSecret && request.headers.get("x-sepay-secret") !== webhookSecret) {
    return json({ error: "Unauthorized" }, 401);
  }

  const payload = await request.json().catch(() => null);
  const referenceCode = payload?.referenceCode ?? payload?.content ?? payload?.description;
  const amount = Number(payload?.amount ?? payload?.transferAmount ?? 0);
  if (!referenceCode || amount <= 0) return json({ error: "Invalid webhook payload" }, 400);

  const { data: order, error: orderError } = await supabase
    .from("payment_orders")
    .select("*")
    .eq("reference_code", referenceCode)
    .eq("provider", "sepay")
    .single();

  if (orderError || !order) return json({ error: "Payment order not found" }, 404);
  if (Number(order.amount_vnd) > amount) return json({ error: "Insufficient amount" }, 400);

  const now = new Date().toISOString();
  await supabase
    .from("payment_orders")
    .update({ status: "paid", updated_at: now })
    .eq("id", order.id);

  await supabase
    .from("entitlements")
    .upsert({
      user_id: order.user_id,
      plan: order.plan,
      is_premium: order.plan !== "basic",
      source: "sepay",
      updated_at: now,
    });

  await supabase
    .from("subscription_status")
    .upsert({
      user_id: order.user_id,
      plan: order.plan,
      is_active: true,
      updated_at: now,
    });

  await supabase
    .from("profiles")
    .update({ is_premium: order.plan !== "basic", updated_at: now })
    .eq("id", order.user_id);

  await supabase.from("audit_logs").insert({
    user_id: order.user_id,
    actor_user_id: order.user_id,
    action: "sepay_webhook_paid",
    metadata: referenceCode,
  });

  return json({ ok: true, referenceCode });
});

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
