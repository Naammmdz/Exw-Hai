create unique index if not exists alert_jobs_incident_user_run_unique
  on public.alert_jobs (incident_id, user_id, run_at);
