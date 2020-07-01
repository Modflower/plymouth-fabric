package net.kjp12.plymouth.mixins;

import net.kjp12.helium.Helium;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer {
    @Shadow
    private Profiler profiler;

    /**
     * Adds a batch sender at the end of the tick. With keeping compatibility with planned Argon, this injection will be OPTIONAL.
     *
     * @reason Send any pending batches to the database.
     */
    @Inject(method = "tick(Ljava/util/function/BooleanSupplier;)V",
            at = @At(value = "TAIL"))
    private void helium$tick$sendBatch(BooleanSupplier keepTicking, CallbackInfo cbi) {
        profiler.push("helium:database.batch");
        Helium.database.sendBatches();
        profiler.pop();
    }
}
