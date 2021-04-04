CREATE FUNCTION now_utc() RETURNS timestamp AS
$$
SELECT now() AT TIME ZONE 'utc'
$$ LANGUAGE SQL;

-- Get the database version. Planning to migrate structures over to be less shenanigans-based.
-- This will be used to fall back to APIvX, or to migrate the database to a new structure if feasible.
-- If the structure migration is not feasible live, the old format will be supported for two Minecraft versions before it becomes read-only.

-- Types for ease of storage.
CREATE TYPE inventory_action AS ENUM ('TAKE', 'PUT');
CREATE TYPE block_action AS ENUM ('BREAK', 'PLACE', 'USE');
CREATE TYPE ipos AS
(
    x int,
    y int,
    z int,
    d int
);
CREATE TYPE dpos AS
(
    x double precision,
    y double precision,
    z double precision,
    d int
);

-- Block, User and Item Indices
-- Note: Helium UUIDs will be used under uid. *This is intended.*
-- Legacy note: Helium UUIDs are used instead of Plymouth UUIDs due to already being used in production.
-- Name of the entity, world, block. Will be the identifier if not a player.
-- UUID of the player. If any other entity or block, Helium UUIDs will be set instead.
CREATE TABLE IF NOT EXISTS users_table
(
    index SERIAL PRIMARY KEY,
    name  TEXT NOT NULL,
    uid   uuid NOT NULL UNIQUE
);
CREATE TABLE IF NOT EXISTS worlds_table
(
    index     SERIAL PRIMARY KEY,
    name      TEXT NOT NULL,
    dimension TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS blocks_table
(
    index      SERIAL PRIMARY KEY,
    name       TEXT  NOT NULL,
    properties jsonb NULL
);
CREATE TABLE IF NOT EXISTS entities_table
(
    index SERIAL PRIMARY KEY,
    uid   uuid NOT NULL UNIQUE
);

-- Indexes for fast lookup.
CREATE INDEX IF NOT EXISTS users_index ON users_table (name);
CREATE INDEX IF NOT EXISTS blocks_index ON blocks_table (name);

-- Index functions.
CREATE FUNCTION get_else_insert_user(bname text, buid uuid) RETURNS int AS
$$
-- noinspection SqlUnusedCte

WITH s AS (SELECT index FROM users_table WHERE uid = buid),
     i AS (INSERT INTO users_table (name, uid) SELECT bname, buid WHERE NOT EXISTS(SELECT 1 FROM s) RETURNING index),
     u AS (UPDATE users_table SET name = bname WHERE NOT EXISTS(SELECT 1 FROM i) AND uid = buid AND name != bname)
SELECT index
FROM i
UNION ALL
SELECT index
FROM s
$$ LANGUAGE SQL;
CREATE FUNCTION get_else_insert_world(bname text, bdim text) RETURNS int AS
$$
WITH s AS (SELECT index FROM worlds_table WHERE name = bname AND dimension = bdim),
     i
         AS (INSERT INTO worlds_table (name, dimension) SELECT bname, bdim WHERE NOT EXISTS(SELECT 1 FROM s) RETURNING index)
SELECT index
FROM i
UNION ALL
select index
FROM s
$$ LANGUAGE SQL;
CREATE FUNCTION get_else_insert_block(bname text, bprops jsonb) RETURNS int AS
$$
WITH s AS (SELECT index FROM blocks_table WHERE name = bname AND (properties = bprops OR properties IS NULL)),
     i
         AS (INSERT INTO blocks_table (name, properties) SELECT bname, bprops WHERE NOT EXISTS(SELECT 1 FROM s) RETURNING index)
SELECT index
FROM i
UNION ALL
select index
FROM s
$$ LANGUAGE SQL;
CREATE FUNCTION get_else_insert_entity(buid uuid) RETURNS int AS
$$
WITH s AS (SELECT index FROM entities_table WHERE uid = buid),
     i AS (INSERT INTO entities_table (uid) SELECT buid WHERE NOT EXISTS(SELECT 1 FROM s) RETURNING index)
SELECT index
FROM i
UNION ALL
select index
FROM s
$$ LANGUAGE SQL;

-- Mutation Tables
CREATE TABLE IF NOT EXISTS mutation
(
    cause_id  int REFERENCES users_table (index) NOT NULL,
    cause_raw int REFERENCES entities_table (index),
    cause_pos ipos,
    time      timestamp                          NOT NULL DEFAULT now_utc(),
    undone    boolean                            NOT NULL DEFAULT false
);
CREATE TABLE IF NOT EXISTS blocks
(
    pos    ipos NOT NULL,
    block  int REFERENCES blocks_table (index),
    action block_action
) INHERITS (mutation);
CREATE TABLE IF NOT EXISTS deaths
(
    target_id  int REFERENCES users_table (index) NOT NULL,
    target_raw int REFERENCES entities_table (index),
    death_pos  dpos                               NOT NULL
) INHERITS (mutation);
CREATE TABLE IF NOT EXISTS items
(
    inventory_id  int REFERENCES users_table (index),
    inventory_raw int REFERENCES entities_table (index),
    inventory_pos ipos,
    data          jsonb,
    action        inventory_action
) INHERITS (mutation);