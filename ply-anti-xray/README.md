<img width="128" src="src/main/resources/pack.png"  alt="Plymouth Anti-Xray" align="right"/>
<div align="left">

# Plymouth: Anti-Xray

A cache-based anti-xray engine designed for FabricMC that by default hides precious vanilla materials such as ores and
buried chests.

This mod is not intended to go on the client. The option is however there if you like doing LAN parties with people you
don't trust with your materials, or if you don't trust yourself to not hack the game, even though Plymouth cannot
entirely prevent hacks whilst running alongside with said hacks on the same instance of Minecraft.

## Downloads

You may download Anti-Xray from [Modrinth](https://modrinth.com/mod/plymouth-anti-xray) or from
[GitHub Releases](https://github.com/Modflower/plymouth-fabric/releases).

## Usage

Drop the mod into the mods folder of your server then boot it up. No configuring nor commands required.

Do note, unusually flat bedrock from x-ray view is to be expected, as bedrock is considered to be a hidden block
via `#operator_blocks`.

## Configuration

Anti-Xray can be configured by datapacks. There are two resource tags you'll need to cover, which are
`plymouth-anti-xray:hidden` and `plymouth-anti-xray:no_smear`.

All tags that will work includes...

- `hidden` - All blocks that the anti-xray engine will attempt to hide.
- `no_smear` - All blocks that the anti-xray engine will not 'smear' across the map to hide hidden blocks. Typically
  non-full blocks or multiblocks that would be obviously out of place, such as plants or doors.
- `common_structure_blocks` - Various building blocks.
- `containers` - Various container types.
- `furnaces` - Various furnace types.
- `infested` - Note, infested blocks are hardcoded within the engine to always be replaced with their regular
  counterpart. There is no override available.
- `operator_blocks` - Command blocks and structure blocks.
- `ores` - Stores the ores themselves, including the reference to `#gold_ores`.
- `pistons` - Intended to store both the regular piston and sticky piston.
- `precious_blocks` - Stores the ores, and the 10 crafted blocks.
- `redstone` - Various redstone components end up here.
- `redstone_containers` - Redstone components that happen to be containers, such as hoppers and droppers.
- `redstone_non_full` - Redstone components that don't take up a full block of space.
- `workstations` - Places villagers work at.

If you wish to replace `hidden` or `no_smear`, you can make a datapack as you normally would, and at
`data/plymouth-anti-xray/tags/blocks/hidden.json`, (or `no_smear.json`), you can add in the following:

```json
{
  "$comment": "'replace' when set to true will replace every other instance of this tag. Use sparingly.",
  "replace": true,
  "values": [
    "#plymouth-anti-xray:precious_blocks",
    "#plymouth-anti-xray:redstone",
    "#plymouth-anti-xray:operator_blocks",
    "#plymouth-anti-xray:common_structure_blocks",
    "dragon_egg"
  ]
}
```

For mod makers, always set `replace` to `false` to avoid overriding any configurations that maybe added in later.

</div>