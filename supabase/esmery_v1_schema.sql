create extension if not exists "pgcrypto";

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  display_name text not null,
  email text,
  phone text,
  avatar_url text,
  is_premium boolean not null default false,
  last_safe_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.circle_members (
  id uuid primary key default gen_random_uuid(),
  owner_user_id uuid not null references auth.users(id) on delete cascade,
  member_user_id uuid references auth.users(id) on delete set null,
  invited_contact text not null,
  name text not null,
  relationship text not null,
  status text not null default 'pending' check (status in ('pending', 'accepted', 'declined')),
  last_safe_at timestamptz,
  created_at timestamptz not null default now()
);

create table if not exists public.friend_requests (
  id uuid primary key default gen_random_uuid(),
  sender_user_id uuid not null references auth.users(id) on delete cascade,
  receiver_contact text not null,
  status text not null default 'pending' check (status in ('pending', 'accepted', 'declined')),
  created_at timestamptz not null default now()
);

create table if not exists public.check_ins (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  status text not null default 'safe',
  note text,
  created_at timestamptz not null default now()
);

create table if not exists public.timeline_events (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  type text not null check (type in ('check_in', 'moment', 'nudge', 'friend_request', 'safety_rhythm', 'emergency')),
  title text not null,
  body text not null,
  related_entity_id uuid,
  created_at timestamptz not null default now()
);

create table if not exists public.moments (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  caption text not null,
  image_url text not null,
  visibility text not null default 'circle',
  created_at timestamptz not null default now()
);

create table if not exists public.emergency_contacts (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  contact text not null,
  is_verified boolean not null default false,
  auto_notify boolean not null default true,
  created_at timestamptz not null default now()
);

create table if not exists public.safety_rhythms (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  label text not null,
  check_time time not null,
  is_enabled boolean not null default true,
  created_at timestamptz not null default now()
);

create table if not exists public.subscription_status (
  user_id uuid primary key references auth.users(id) on delete cascade,
  plan text not null default 'basic' check (plan in ('basic', 'monthly', 'yearly')),
  is_active boolean not null default true,
  updated_at timestamptz not null default now()
);

alter table public.profiles enable row level security;
alter table public.circle_members enable row level security;
alter table public.friend_requests enable row level security;
alter table public.check_ins enable row level security;
alter table public.timeline_events enable row level security;
alter table public.moments enable row level security;
alter table public.emergency_contacts enable row level security;
alter table public.safety_rhythms enable row level security;
alter table public.subscription_status enable row level security;

create policy "profiles-own" on public.profiles for all using (auth.uid() = id) with check (auth.uid() = id);
create policy "circle-owner" on public.circle_members for all using (auth.uid() = owner_user_id) with check (auth.uid() = owner_user_id);
create policy "circle-member-read" on public.circle_members for select using (auth.uid() = member_user_id and status = 'accepted');
create policy "friend-requests-own" on public.friend_requests for all using (auth.uid() = sender_user_id) with check (auth.uid() = sender_user_id);
create policy "check-ins-own" on public.check_ins for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "timeline-own" on public.timeline_events for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "moments-own" on public.moments for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "emergency-own" on public.emergency_contacts for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "rhythm-own" on public.safety_rhythms for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "subscription-own" on public.subscription_status for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
