---
-- #%L
-- JSQLParser library
-- %%
-- Copyright (C) 2004 - 2026 JSQLParser
-- %%
-- Dual licensed under GNU LGPL 2.1 or Apache License 2.0
-- #L%
---
CREATE PUBLICATION empty_publication
CREATE PUBLICATION all_tables FOR ALL TABLES
CREATE PUBLICATION dbz_publication FOR TABLE users, orders WITH (publish = 'insert, update, delete')
CREATE PUBLICATION filtered FOR TABLE ONLY app.users (id, email) WHERE (active = true) WITH (publish_via_partition_root = true)
CREATE PUBLICATION inherited FOR TABLE app.users *
CREATE PUBLICATION schemas FOR TABLES IN SCHEMA public, app
CREATE PUBLICATION current_schema_pub FOR TABLES IN SCHEMA CURRENT_SCHEMA
CREATE PUBLICATION mixed FOR TABLE users, orders, TABLES IN SCHEMA app, audit, TABLE events
CREATE PUBLICATION explicit_groups FOR TABLE users, TABLE orders
CREATE PUBLICATION stored_columns FOR ALL TABLES WITH (publish_generated_columns = stored, publish_via_partition_root)
CREATE PUBLICATION flags FOR ALL TABLES WITH (publish_via_partition_root = off)
ALTER PUBLICATION dbz_publication ADD TABLE events
ALTER PUBLICATION dbz_publication ADD TABLE users (id, email), orders WHERE (amount > 10)
ALTER PUBLICATION dbz_publication SET TABLE ONLY users, TABLES IN SCHEMA app
ALTER PUBLICATION dbz_publication DROP TABLE users *, orders
ALTER PUBLICATION dbz_publication DROP TABLES IN SCHEMA app, audit
ALTER PUBLICATION dbz_publication SET (publish = 'update, delete, truncate')
ALTER PUBLICATION dbz_publication SET (publish_via_partition_root = on, publish_generated_columns = 'stored')
ALTER PUBLICATION dbz_publication OWNER TO CURRENT_USER
ALTER PUBLICATION dbz_publication RENAME TO live_publication
CREATE SUBSCRIPTION app_sub CONNECTION 'host=localhost dbname=appdb' PUBLICATION dbz_publication WITH (connect = false)
CREATE SUBSCRIPTION app_sub CONNECTION 'dbname=app' PUBLICATION one, two
CREATE SUBSCRIPTION app_sub CONNECTION 'dbname=app' PUBLICATION one WITH (enabled = false, create_slot = false, slot_name = NONE)
CREATE SUBSCRIPTION app_sub CONNECTION 'dbname=app' PUBLICATION one WITH (slot_name = 'NONE', enabled = false, create_slot = false)
CREATE SUBSCRIPTION app_sub CONNECTION 'dbname=app' PUBLICATION one WITH (binary, copy_data = false, streaming = parallel, synchronous_commit = remote_apply)
CREATE SUBSCRIPTION app_sub CONNECTION 'dbname=app' PUBLICATION one WITH (two_phase = true, disable_on_error = true, password_required = true, run_as_owner = false, origin = none, failover = true)
CREATE SUBSCRIPTION app_sub CONNECTION 'dbname=app' PUBLICATION one WITH (streaming = on, synchronous_commit = off)
CREATE SUBSCRIPTION app_sub CONNECTION 'dbname=app' PUBLICATION one WITH (binary = 'yes', enabled = 0)
ALTER SUBSCRIPTION app_sub CONNECTION 'host=localhost dbname=app2'
ALTER SUBSCRIPTION app_sub SET PUBLICATION one, two WITH (refresh = false)
ALTER SUBSCRIPTION app_sub ADD PUBLICATION three WITH (copy_data = false)
ALTER SUBSCRIPTION app_sub DROP PUBLICATION two WITH (refresh = true, copy_data = false)
ALTER SUBSCRIPTION app_sub REFRESH PUBLICATION WITH (copy_data = false)
ALTER SUBSCRIPTION app_sub REFRESH PUBLICATION
ALTER SUBSCRIPTION app_sub ENABLE
ALTER SUBSCRIPTION app_sub DISABLE
ALTER SUBSCRIPTION app_sub SET (slot_name = NONE)
ALTER SUBSCRIPTION app_sub SET (streaming = off, synchronous_commit = local, origin = any)
ALTER SUBSCRIPTION app_sub SET (binary = true, two_phase = false, failover = false)
ALTER SUBSCRIPTION app_sub SKIP (lsn = '0/14C0378')
ALTER SUBSCRIPTION app_sub SKIP (lsn = NONE)
ALTER SUBSCRIPTION app_sub OWNER TO CURRENT_ROLE
ALTER SUBSCRIPTION app_sub RENAME TO next_sub
CREATE PUBLICATION "Pub" FOR TABLE "App"."Orders" ("Id") WHERE ("Id" > 0)
