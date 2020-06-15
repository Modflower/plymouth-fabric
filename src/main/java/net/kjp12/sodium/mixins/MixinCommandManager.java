package net.kjp12.sodium.mixins;

import com.mojang.brigadier.CommandDispatcher;
import net.kjp12.sodium.SodiumHelper;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CommandManager.class)
public abstract class MixinCommandManager {
    @Shadow
    @Final
    private CommandDispatcher<ServerCommandSource> dispatcher;
/*
    @Inject(method = "<init>", at = @At("TAIL"))
    public void sodium$registerCommands(boolean isDedicatedServer, CallbackInfo cbi) {
        {
            var lockCommand = dispatcher.register(literal("lock"));
            var players = argument("players", players());
            var add = literal("add").
                    then(argument("permission", integer(0, 1 | 2 | 4)).then(players).executes(cmd -> {

                        var p = cmd.getArgument("permission", int.class);
                        var l = cmd.getArgument("players", List.class);
                        for(var i : l) {
                            ((PlayerEntity) i).getUuid();
                        }
                    })).
                    then(players).executes(cmd -> {
                var l = cmd.getArgument("players", List.class);
            });
        }

    }*/

    @Inject(method = "execute(Lnet/minecraft/server/command/ServerCommandSource;Ljava/lang/String;)I", at = @At("HEAD"))
    public void sodium$execute$logCommandExecution(ServerCommandSource source, String str, CallbackInfoReturnable<Integer> cbir) {
        SodiumHelper.LOGGER.info("{} has executed the following command: {}", source.getName(), str);
    }
}
