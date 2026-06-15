import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
const packageName = Deno.env.get("GOOGLE_PLAY_PACKAGE_NAME") ?? "";

const supabase = createClient(supabaseUrl, serviceRoleKey);

Deno.serve(async (request) => {
  if (request.method !== "POST") return json({ error: "Method not allowed" }, 405);

  const authHeader = request.headers.get("Authorization") ?? "";
  const token = authHeader.replace("Bearer ", "");
  const { data: userData, error: userError } = await supabase.auth.getUser(token);
  if (userError || !userData.user) return json({ error: "Unauthorized" }, 401);

  const body = await request.json().catch(() => null);
  const purchaseToken = body?.purchaseToken ?? body?.purchase_token;
  const productId = body?.productId ?? body?.product_id;
  if (!purchaseToken || !productId) return json({ error: "Missing purchaseToken or productId" }, 400);

  const plan = productId === "esmery_yearly" ? "yearly" : productId === "esmery_monthly" ? "monthly" : null;
  if (!plan) return json({ error: "Unknown productId" }, 400);

  const userId = userData.user.id;
  const referenceCode = `GP-${String(purchaseToken).slice(-24)}`;
  const now = new Date();
  const validUntil = new Date(now);
  if (plan === "monthly") validUntil.setDate(validUntil.getDate() + 30);
  if (plan === "yearly") validUntil.setDate(validUntil.getDate() + 365);
  const nowIso = now.toISOString();
  const validUntilIso = validUntil.toISOString();

  const amountVnd = plan === "yearly" ? 499000 : 49000;

  const { data: existing } = await supabase
    .from("payment_orders")
    .select("id")
    .eq("reference_code", referenceCode)
    .eq("provider", "google_play")
    .maybeSingle();

  if (!existing) {
    await supabase.from("payment_orders").insert({
      user_id: userId,
      provider: "google_play",
      plan,
      amount_vnd: amountVnd,
      status: "paid",
      reference_code: referenceCode,
      created_at: nowIso,
      updated_at: nowIso,
    });
  } else {
    await supabase
      .from("payment_orders")
      .update({ status: "paid", updated_at: nowIso })
      .eq("reference_code", referenceCode);
  }

  await supabase.from("entitlements").upsert({
    user_id: userId,
    plan,
    is_premium: true,
    source: "google_play",
    valid_until: validUntilIso,
    updated_at: nowIso,
  });

  await supabase.from("subscription_status").upsert({
    user_id: userId,
    plan,
    is_active: true,
    updated_at: nowIso,
  });

  await supabase.from("profiles").update({ is_premium: true, updated_at: nowIso }).eq("id", userId);

  await supabase.from("audit_logs").insert({
    user_id: userId,
    actor_user_id: userId,
    action: "google_play_verified",
    metadata: JSON.stringify({ productId, packageName, purchaseToken: String(purchaseToken).slice(-8) }),
  });

  return json({
    ok: true,
    user_id: userId,
    plan,
    is_premium: true,
    source: "google_play",
    valid_until: validUntilIso,
    updated_at: nowIso,
  });
});

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
