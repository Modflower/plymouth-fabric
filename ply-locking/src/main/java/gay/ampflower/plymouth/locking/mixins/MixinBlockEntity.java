package gay.ampflower.plymouth.locking.mixins;

import gay.ampflower.plymouth.locking.ILockable;
import gay.ampflower.plymouth.locking.Locking;
import gay.ampflower.plymouth.locking.handler.AdvancedPermissionHandler;
import gay.ampflower.plymouth.locking.handler.BasicPermissionHandler;
import gay.ampflower.plymouth.locking.handler.IPermissionHandler;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntity.class)
public abstract class MixinBlockEntity implements ILockable {
    // 1 - Read
    // 2 - Write
    // 4 - Destroy
    // 8 - Modify Permissions
    //--------pdwr

    @Unique
    private IPermissionHandler permissionHandler;

    @Override
    public @Nullable IPermissionHandler plymouth$getPermissionHandler() {
        return permissionHandler;
    }

    @Override
    public void plymouth$setPermissionHandler(@Nullable IPermissionHandler handler) {
        this.permissionHandler = handler;
    }

    @Inject(method = "readNbt(Lnet/minecraft/nbt/NbtCompound;)V", at = @At("RETURN"))
    private void helium$fromTag(NbtCompound nbt, CallbackInfo cbi) {
        // LEGACY NOTE: "helium" and "sodium" namespaces are already used in production.
        //  In order to keep production tile entities from losing their lock, presence of Sodium will be checked if Helium doesn't exist.
        var helium = nbt.contains("helium", 10) ? nbt.getCompound("helium") : nbt.contains("sodium", 10) ? nbt.getCompound("sodium") : null;
        if (helium != null) {
            if (!helium.containsUuid("owner")) {
                Locking.logger.warn(":concern: (https://cdn.discordapp.com/emojis/798290111656886283.png?v=1) Helium or sodium tag exists but there is no owner bound?!");
                return;
            }
            // LEGACY NOTE: "access" is a carry over from helium. DFU is a pain to set up, so a primitive JIT conversion is done instead.
            if (helium.contains("access", 9) || helium.contains("players", 9) || helium.contains("groups", 9)) {
                permissionHandler = new AdvancedPermissionHandler();
            } else {
                permissionHandler = new BasicPermissionHandler();
            }
            permissionHandler.fromTag(helium);
        }
    }

    @Inject(method = "writeNbt(Lnet/minecraft/nbt/NbtCompound;)Lnet/minecraft/nbt/NbtCompound;", at = @At("RETURN"))
    private void helium$toTag(NbtCompound nbt, CallbackInfoReturnable<NbtCompound> cbi) {
        if (permissionHandler != null) {
            var helium = new NbtCompound();
            permissionHandler.toTag(helium);
            // Legacy naming
            nbt.put("helium", helium);
        }
    }
}
