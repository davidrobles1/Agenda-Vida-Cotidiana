-- Runs once, on first init of the postgres container's data volume
-- (docker-entrypoint-initdb.d convention) — creates a second database in the
-- same Postgres instance for GlitchTip, reusing the container instead of
-- running a duplicate one just for error tracking.
CREATE DATABASE glitchtip;
