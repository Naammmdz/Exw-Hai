create table if not exists public.device_tokens (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  token text not null,
  provider text not null default 'fcm',
  platform text not null default 'android',
  app_version text,
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  last_seen_at timestamptz not null default now(),
  unique (provider, token)
);

create table if not exists public.notification_deliveries (
  id uuid primary key default gen_random_uuid(),
  notification_id uuid references public.notifications(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  recipient_user_id uuid references auth.users(id) on delete set null,
  recipient_contact text,
  channel text not null check (channel in ('in_app', 'push', 'sms', 'email', 'call')),
  status text not null default 'pending' check (status in ('pending', 'sent', 'failed', 'read')),
  error_message text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.alert_incidents (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  status text not null default 'active' check (status in ('active', 'escalated', 'resolved', 'cancelled')),
  reason text not null,
  last_safe_at timestamptz,
  escalation_due_at timestamptz not null,
  resolved_at timestamptz,
  created_at timestamptz not null default now()
);

create unique index if not exists alert_incidents_missed_unique
  on public.alert_incidents (user_id, reason, last_safe_at)
  where reason = 'missed_check_in' and status in ('active', 'escalated');

create table if not exists public.alert_jobs (
  id uuid primary key default gen_random_uuid(),
  incident_id uuid not null references public.alert_incidents(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  run_at timestamptz not null,
  status text not null default 'scheduled' check (status in ('scheduled', 'sent', 'cancelled', 'failed')),
  created_at timestamptz not null default now()
);

create table if not exists public.location_shares (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  incident_id uuid references public.alert_incidents(id) on delete set null,
  latitude double precision not null,
  longitude double precision not null,
  accuracy_meters double precision,
  status text not null default 'active' check (status in ('active', 'expired', 'revoked')),
  created_at timestamptz not null default now(),
  expires_at timestamptz not null
);

create table if not exists public.payment_orders (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  provider text not null check (provider in ('google_play', 'sepay')),
  plan text not null check (plan in ('basic', 'monthly', 'yearly')),
  amount_vnd integer not null default 0,
  status text not null default 'pending' check (status in ('pending', 'paid', 'expired', 'cancelled', 'failed')),
  checkout_url text,
  qr_url text,
  reference_code text not null unique,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.entitlements (
  user_id uuid primary key references auth.users(id) on delete cascade,
  plan text not null default 'basic' check (plan in ('basic', 'monthly', 'yearly')),
  is_premium boolean not null default false,
  source text not null default 'basic' check (source in ('basic', 'google_play', 'sepay', 'manual')),
  valid_until timestamptz,
  updated_at timestamptz not null default now()
);

create table if not exists public.audit_logs (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  actor_user_id uuid references auth.users(id) on delete set null,
  action text not null,
  metadata text,
  created_at timestamptz not null default now()
);

alter table public.device_tokens enable row level security;
alter table public.notification_deliveries enable row level security;
alter table public.alert_incidents enable row level security;
alter table public.alert_jobs enable row level security;
alter table public.location_shares enable row level security;
alter table public.payment_orders enable row level security;
alter table public.entitlements enable row level security;
alter table public.audit_logs enable row level security;

drop policy if exists "device-tokens-own" on public.device_tokens;
drop policy if exists "notification-deliveries-own" on public.notification_deliveries;
drop policy if exists "notification-deliveries-circle-insert" on public.notification_deliveries;
drop policy if exists "alert-incidents-own" on public.alert_incidents;
drop policy if exists "alert-jobs-own" on public.alert_jobs;
drop policy if exists "location-shares-own" on public.location_shares;
drop policy if exists "payment-orders-own" on public.payment_orders;
drop policy if exists "entitlements-own" on public.entitlements;
drop policy if exists "audit-logs-own" on public.audit_logs;

create policy "device-tokens-own" on public.device_tokens
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

create policy "notification-deliveries-own" on public.notification_deliveries
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

create policy "notification-deliveries-circle-insert" on public.notification_deliveries
  for insert with check (public.esmery_can_deliver_to(coalesce(recipient_user_id, user_id)));

create policy "alert-incidents-own" on public.alert_incidents
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

create policy "alert-jobs-own" on public.alert_jobs
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

create policy "location-shares-own" on public.location_shares
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

create policy "payment-orders-own" on public.payment_orders
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

create policy "entitlements-own" on public.entitlements
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

create policy "audit-logs-own" on public.audit_logs
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
