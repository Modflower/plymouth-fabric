/* Copyright (c) 2021 Ampflower
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package gay.ampflower.helium.mixins;

import com.mojang.brigadier.CommandDispatcher;
import gay.ampflower.helium.Helium;
import gay.ampflower.helium.commands.InventoryLookupCommand;
import gay.ampflower.helium.commands.MappingCommand;
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
    public void helium$registerCommands(CommandManager.RegistrationEnvironment env, CallbackInfo cbi) {
        InventoryLookupCommand.register(dispatcher);
        MappingCommand.register(dispatcher);
    }

    @Inject(method = "execute(Lnet/minecraft/server/command/ServerCommandSource;Ljava/lang/String;)I", at = @At("HEAD"))
    public void helium$execute$logCommandExecution(ServerCommandSource source, String str,
            CallbackInfoReturnable<Integer> cbir) {
        Helium.logger.info("{} has executed the following command: {}", source.getName(), str);
    }
}
