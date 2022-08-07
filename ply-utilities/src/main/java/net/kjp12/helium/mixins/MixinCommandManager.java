package net.kjp12.helium.mixins;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import net.kjp12.helium.Helium;
import net.kjp12.helium.commands.HotspotCommand;
import net.kjp12.helium.commands.InventoryLookupCommand;
import net.kjp12.helium.commands.MappingCommand;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// TODO: Use Fabric API
@Mixin(CommandManager.class)
public abstract class MixinCommandManager {
    @Shadow
    @Final
    private CommandDispatcher<ServerCommandSource> dispatcher;

    @Inject(method = "<init>", at = @At("TAIL"))
    public void helium$registerCommands(CommandManager.RegistrationEnvironment environment, CommandRegistryAccess commandRegistryAccess, CallbackInfo ci) {
        InventoryLookupCommand.register(dispatcher);
        HotspotCommand.register(dispatcher);
        MappingCommand.register(dispatcher);
    }

    @Inject(method = "execute(Lcom/mojang/brigadier/ParseResults;Ljava/lang/String;)I", at = @At("HEAD"))
    public void helium$execute$logCommandExecution(ParseResults<ServerCommandSource> parseResults, String command, CallbackInfoReturnable<Integer> cir) {
        Helium.logger.info("{} has executed the following command: {}", parseResults.getContext().getSource().getName(), command);
    }
}
