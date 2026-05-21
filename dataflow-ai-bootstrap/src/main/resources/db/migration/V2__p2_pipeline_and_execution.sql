-- P2: Pipeline 共享白名单 + 执行取消标志
ALTER TABLE pipelines ADD COLUMN IF NOT EXISTS allowed_roles jsonb;
ALTER TABLE pipelines ADD COLUMN IF NOT EXISTS allowed_users jsonb;
ALTER TABLE pipelines ADD COLUMN IF NOT EXISTS allowed_departments jsonb;

ALTER TABLE execution_runs ADD COLUMN IF NOT EXISTS cancel_requested boolean DEFAULT false;
