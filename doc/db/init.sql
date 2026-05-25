-- 启用pgvector扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- public.ai_helpers definition
CREATE TABLE public.ai_helpers (
                                   id varchar(64) NOT NULL,
                                   instruction text NOT NULL,
                                   context jsonb NULL,
                                   generated_nodes jsonb NOT NULL,
                                   pipeline_id varchar(64) NULL,
                                   user_feedback int2 NULL,
                                   embedding public.vector(1536) NULL,
                                   created_by varchar(64) NULL,
                                   created_at timestamptz DEFAULT now() NULL,
                                   CONSTRAINT ai_helpers_pkey PRIMARY KEY (id),
                                   CONSTRAINT ai_helpers_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(id),
                                   CONSTRAINT ai_helpers_pipeline_id_fkey FOREIGN KEY (pipeline_id) REFERENCES public.pipelines(id) ON DELETE SET NULL
);
CREATE INDEX idx_ai_helpers_embedding ON public.ai_helpers USING hnsw (embedding vector_cosine_ops) WITH (m='16', ef_construction='64');

-- public.audit_logs definition
CREATE TABLE public.audit_logs (
                                   id bigserial NOT NULL,
                                   user_id varchar(64) NOT NULL,
                                   "action" varchar(100) NOT NULL,
                                   resource_type varchar(50) NOT NULL,
                                   resource_id varchar(64) NULL,
                                   details jsonb NULL,
                                   ip_address inet NULL,
                                   user_agent text NULL,
                                   created_at timestamptz DEFAULT now() NULL,
                                   CONSTRAINT audit_logs_pkey PRIMARY KEY (id),
                                   CONSTRAINT audit_logs_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id)
);

-- public.data_column_permissions definition
CREATE TABLE public.data_column_permissions (
                                                id varchar(64) NOT NULL,
                                                data_source_id varchar(64) NOT NULL,
                                                column_name varchar(255) NOT NULL,
                                                target_role varchar(50) NULL,
                                                target_department varchar(100) NULL,
                                                target_user varchar(64) NULL,
                                                access_type varchar(20) NOT NULL,
                                                mask_rule varchar(255) NULL,
                                                created_at timestamptz DEFAULT now() NULL,
                                                CONSTRAINT data_column_permissions_pkey PRIMARY KEY (id),
                                                CONSTRAINT data_column_permissions_data_source_id_fkey FOREIGN KEY (data_source_id) REFERENCES public.data_sources(id) ON DELETE CASCADE,
                                                CONSTRAINT data_column_permissions_target_user_fkey FOREIGN KEY (target_user) REFERENCES public.users(id)
);

-- public.data_row_permissions definition
CREATE TABLE public.data_row_permissions (
                                             id varchar(64) NOT NULL,
                                             data_source_id varchar(64) NOT NULL,
                                             target_role varchar(50) NULL,
                                             target_department varchar(100) NULL,
                                             target_user varchar(64) NULL,
                                             filter_condition text NOT NULL,
                                             priority int4 DEFAULT 0 NULL,
                                             created_at timestamptz DEFAULT now() NULL,
                                             CONSTRAINT data_row_permissions_pkey PRIMARY KEY (id),
                                             CONSTRAINT data_row_permissions_data_source_id_fkey FOREIGN KEY (data_source_id) REFERENCES public.data_sources(id) ON DELETE CASCADE,
                                             CONSTRAINT data_row_permissions_target_user_fkey FOREIGN KEY (target_user) REFERENCES public.users(id)
);

-- public.data_sources definition
CREATE TABLE public.data_sources (
                                     id varchar(64) NOT NULL,
                                     "name" varchar(255) NOT NULL,
                                     "type" varchar(50) NOT NULL,
                                     connection_config jsonb NOT NULL,
                                     created_by varchar(64) NOT NULL,
                                     created_at timestamptz DEFAULT now() NULL,
                                     updated_at timestamptz DEFAULT now() NULL,
                                     CONSTRAINT data_sources_pkey PRIMARY KEY (id),
                                     CONSTRAINT data_sources_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(id)
);

-- public.execution_runs definition
CREATE TABLE public.execution_runs (
                                       id varchar(64) NOT NULL,
                                       pipeline_id varchar(64) NOT NULL,
                                       status varchar(20) NOT NULL,
                                       start_time timestamptz NULL,
                                       end_time timestamptz NULL,
                                       error_message text NULL,
                                       execution_log jsonb NULL,
                                       metrics jsonb NULL,
                                       triggered_by varchar(64) NULL,
                                       created_at timestamptz DEFAULT now() NULL,
                                       cancel_requested bool DEFAULT false NULL,
                                       CONSTRAINT execution_runs_pkey PRIMARY KEY (id),
                                       CONSTRAINT execution_runs_pipeline_id_fkey FOREIGN KEY (pipeline_id) REFERENCES public.pipelines(id) ON DELETE CASCADE,
                                       CONSTRAINT execution_runs_triggered_by_fkey FOREIGN KEY (triggered_by) REFERENCES public.users(id)
);

-- public.flyway_schema_history definition
CREATE TABLE public.flyway_schema_history (
                                              installed_rank int4 NOT NULL,
                                              "version" varchar(50) NULL,
                                              description varchar(200) NOT NULL,
                                              "type" varchar(20) NOT NULL,
                                              script varchar(1000) NOT NULL,
                                              checksum int4 NULL,
                                              installed_by varchar(100) NOT NULL,
                                              installed_on timestamp DEFAULT now() NOT NULL,
                                              execution_time int4 NOT NULL,
                                              success bool NOT NULL,
                                              CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank)
);
CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);

-- public.instruction_patterns definition
CREATE TABLE public.instruction_patterns (
                                             id bigserial NOT NULL,
                                             instruction_hash varchar(64) NOT NULL,
                                             instruction_text text NOT NULL,
                                             transform_template jsonb NOT NULL,
                                             use_count int4 DEFAULT 1 NULL,
                                             avg_embedding public.vector(1536) NULL,
                                             acceptance_rate numeric(3, 2) DEFAULT 0 NULL,
                                             last_used_at timestamptz DEFAULT now() NULL,
                                             created_at timestamptz DEFAULT now() NULL,
                                             CONSTRAINT instruction_patterns_instruction_hash_key UNIQUE (instruction_hash),
                                             CONSTRAINT instruction_patterns_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_patterns_embedding ON public.instruction_patterns USING hnsw (avg_embedding vector_cosine_ops) WITH (m='16', ef_construction='64');

-- public.pipelines definition
CREATE TABLE public.pipelines (
                                  id varchar(64) NOT NULL,
                                  "name" varchar(255) NOT NULL,
                                  description text NULL,
                                  "source" jsonb NULL,
                                  "transforms" jsonb NULL,
                                  sink jsonb NULL,
                                  schedule jsonb NULL,
                                  owner_id varchar(64) NOT NULL,
                                  permission_level varchar(20) DEFAULT 'private'::character varying NULL,
                                  status varchar(20) DEFAULT 'active'::character varying NULL,
                                  created_at timestamptz DEFAULT now() NULL,
                                  updated_at timestamptz DEFAULT now() NULL,
                                  allowed_roles jsonb NULL,
                                  allowed_users jsonb NULL,
                                  allowed_departments jsonb NULL,
                                  CONSTRAINT pipelines_pkey PRIMARY KEY (id),
                                  CONSTRAINT pipelines_owner_id_fkey FOREIGN KEY (owner_id) REFERENCES public.users(id)
);

-- public.users definition
CREATE TABLE public.users (
                              id varchar(64) NOT NULL,
                              username varchar(100) NOT NULL,
                              email varchar(255) NOT NULL,
                              password_hash varchar(255) NOT NULL,
                              "role" varchar(50) NOT NULL,
                              department varchar(100) NULL,
                              status varchar(20) DEFAULT 'active'::character varying NULL,
                              created_at timestamptz DEFAULT now() NULL,
                              last_login_at timestamptz NULL,
                              CONSTRAINT users_email_key UNIQUE (email),
                              CONSTRAINT users_pkey PRIMARY KEY (id),
                              CONSTRAINT users_username_key UNIQUE (username)
);
