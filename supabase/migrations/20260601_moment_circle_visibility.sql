drop policy if exists "moments-circle-read" on public.moments;

create policy "moments-circle-read" on public.moments
  for select using (
    auth.uid() = user_id
    or exists (
      select 1
      from public.circle_members
      where status = 'accepted'
        and (
          (owner_user_id = public.moments.user_id and member_user_id = auth.uid())
          or (owner_user_id = auth.uid() and member_user_id = public.moments.user_id)
          or (
            owner_user_id = public.moments.user_id
            and lower(invited_contact) = lower(coalesce(auth.jwt() ->> 'email', ''))
          )
        )
    )
  );
