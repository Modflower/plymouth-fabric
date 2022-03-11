package net.kjp12.plymouth.debug.mixins.client;// Created 2022-11-03T05:38:36

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.gui.screen.Screen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author KJP12
 * @since ${version}
 **/
@Mixin(MinecraftClient.class)
public class MixinBustLoadingMinecraftClient {
    @Shadow
    @Nullable
    public Screen currentScreen;

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void plymouth$debug$bustLoadingScreen(Screen screen, CallbackInfo ci) {
        if (screen instanceof DownloadingTerrainScreen) {
            var current = this.currentScreen;

            if (current != null) {
                current.removed();
                this.currentScreen = null;
            }
            screen.removed();
            ci.cancel();
        }
    }
}
