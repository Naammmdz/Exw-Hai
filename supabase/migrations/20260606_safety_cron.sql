-- Requires pg_cron and pg_net extensions (Supabase Pro or enabled in dashboard).
-- safety-automation and alert-escalation have verify_jwt = false, so no auth header needed.

create extension if not exists pg_cron with schema extensions;
create extension if not exists pg_net with schema extensions;

select cron.unschedule(jobid)
from cron.job
where jobname in ('safety-check', 'alert-escalation');

select cron.schedule(
  'safety-check',
  '*/15 * * * *',
  $$
  select net.http_post(
    url := 'https://trmkreamtdfzdnkyktwg.supabase.co/functions/v1/safety-automation',
    headers := '{"Content-Type": "application/json"}'::jsonb,
    body := '{}'::jsonb
  );
  $$
);

select cron.schedule(
  'alert-escalation',
  '*/5 * * * *',
  $$
  select net.http_post(
    url := 'https://trmkreamtdfzdnkyktwg.supabase.co/functions/v1/alert-escalation',
    headers := '{"Content-Type": "application/json"}'::jsonb,
    body := '{}'::jsonb
  );
  $$
);
