drop policy if exists "alert-incidents-circle-read" on public.alert_incidents;

create policy "alert-incidents-circle-read" on public.alert_incidents
  for select using (
    auth.uid() = user_id
    or public.esmery_can_deliver_to(user_id)
  );
