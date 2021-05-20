<img width="128" src="src/main/resources/pack.png" alt="Plymouth Utilities" align="right"/>
<div align="left">

# Plymouth

An anti-xray engine, claiming system and world tracker.

## [Plymouth: Anti-Xray](ply-anti-xray/README.md)

A cache-based anti-xray engine for FabricMC that replaces ores, buried chests and other materials with surrounding
materials.

## [Plymouth: Common](ply-common/README.md)

Internal utilities for deriving UUIDs from various objects and temporarily injecting into the player interaction
manager.

## [Plymouth: Database](ply-database/README.md)

Database API for Tracker. Requires extra setup due to the use of PostgreSQL. Please
see [the setup guide](ply-database/README.md#setup-postgresql--linux) for how to setup the database.

## [Plymouth: Debug](ply-debug/README.md)

Debugging mod for the anti-xray engine. Not intended for production use. If installed on the server, any client with the
debug client can see any recently hidden blocks otherwise hidden by the anti-xray engine.

## [Plymouth: Locking](ply-locking/README.md)

Chest... well, any block entity claiming and locking mod that allows players to lock various block entities and have
them only be mutable by them unless they choose to open it up for others.

## [Plymouth: Tracker](ply-tracker/README.md)

World, inventory and death tracking system. Can do lookups, but not rollbacks at the moment.

</div>