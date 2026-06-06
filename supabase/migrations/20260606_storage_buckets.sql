insert into storage.buckets (id, name, public)
values
  ('moments', 'moments', true),
  ('avatars', 'avatars', true)
on conflict (id) do nothing;

drop policy if exists "moments-read" on storage.objects;
drop policy if exists "moments-write" on storage.objects;
drop policy if exists "avatars-read" on storage.objects;
drop policy if exists "avatars-write" on storage.objects;

create policy "moments-read" on storage.objects
  for select using (bucket_id = 'moments');

create policy "moments-write" on storage.objects
  for insert with check (bucket_id = 'moments' and auth.role() = 'authenticated');

create policy "avatars-read" on storage.objects
  for select using (bucket_id = 'avatars');

create policy "avatars-write" on storage.objects
  for insert with check (bucket_id = 'avatars' and auth.role() = 'authenticated');
