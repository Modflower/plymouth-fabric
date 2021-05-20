<img width="128" src="src/main/resources/pack.png" alt="Plymouth Debug" align="right"/>
<div align="left">

# Plymouth: Debug

A debugging mod for the anti-xray engine, complete with defeating the purpose of the anti-xray engine.

This mod is not intended for production use. This will expose any and all hidden blocks should both the client and
server have this mod present.

## Usage

If the anti-xray engine is not outputting the expected response, you can boot up the debug client by setting the
classpath to `ply-debug`.

As you start up a world, you'll immediately see multitudes of boxes of three coloured boxes as the world begins to
update.

- `Red`: The anti-xray engine has been updated at that position.
- `Green`: The anti-xray engine has set the block at that position.
- `Blue`: The anti-xray engine has tested the block at that position.
- `Yellow`: A world update has occurred to the client at that position.

If you wish to see the active mask for a given chunk, you can the command `/mdump`, which will send the entire mask to
the client, which will promptly render it.

If you wish to stop seeing the active mask, run the command `/plymouth debug anti-xray clear`

</div>