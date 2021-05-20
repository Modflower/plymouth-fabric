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

- `8`: Read from block
- `4`: Write into block
- `2`: Delete/destroy block
- `1`: Permissions management

You may join these together by adding to get the desired effect. They will be replaced with `rwdp` in a future release
for better user friendliness.

- `/lock add <position?> <players> <permissions>`: Adds players to a block.
- `/lock remove <position?> <players>`: Removes players from a block.
- `/lock set <permissions>`: Sets permissions on a block.
- `/lock get <position?>`: Gets the information of the block.

### Permissions

- `plymouth.locking.lock`: Be able to lock block entities. Default is true.
- `plymouth.locking.bypass.read`: Bypass read permission. Default is OP level 2.
- `plymouth.locking.bypass.write`: Bypass write permission. Default is OP level 2.
- `plymouth.locking.bypass.delete`: Bypass delete permission. Default is OP level 2.
- `plymouth.locking.bypass.permissions`: Bypass permissions. Default is OP level 2.

</div>