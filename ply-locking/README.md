<img width="128" src="src/main/resources/pack.png" alt="Plymouth Locking" align="right"/>
<div>

# Plymouth: Locking

A claims system that allows one to lock their chests with a sneak+use.

## Downloads

You may download Locking from [Modrinth](https://modrinth.com/mod/plymouth-locking) or
from [GitHub Releases](https://github.com/the-glitch-network/plymouth-fabric/releases).

## Usage

Drop the mod into the mods folder of your server then boot it up. No configuring nor commands required.

### In-world

#### (Un)Locking a block entity

You can lock a chest by shift+right-clicking it with an empty hand. This will lock the chest to be only accessible to
you. You can also unlock a chest using the same action.

### Commands

The position argument is optional for these commands. If you choose to omit the position, you must interact with the
block after executing. If you do define a position, you don't own the block and you are near the block, you will
automatically own the block as if you shift+right-clicked it.

Permissions within the following commands mean the following:

- `r`: Read from block
- `w`: Write into block
- `d`: Destroy block
- `p`: Permissions management

The expected syntax for permissions is similar to chmod, where it accepts either `rwdx`, or `+r-w` to allow setting all
permissions, or to allow read and deny write respectively.

The following commands is what to use for permission management:

- `/lock (at <position>|interact) add <players> <permissions ?: rw>`: Adds players to a block.
- `/lock (at <position>|interact) remove <players>`: Removes players from a block.
- `/lock (at <position>|interact) modify <permissions>`: Sets permissions on a block.
- `/lock (at <position>|interact) get`: Gets the information of the block.

Convenience commands:

- `/trust <players> <permissions ?: rw>`: Same as `/lock interact add`
- `/distrust <players>`: Same as `/lock interact remove`

### Permissions

- `plymouth.locking.lock`: Be able to lock block entities. Default is true.
- `plymouth.locking.bypass.read`: Bypass read permission. Default is OP level 2.
- `plymouth.locking.bypass.write`: Bypass write permission. Default is OP level 2.
- `plymouth.locking.bypass.delete`: Bypass delete permission. Default is OP level 2.
- `plymouth.locking.bypass.permissions`: Bypass permissions. Default is OP level 2.

</div>