package net.kjp12.helium.mixins;

import it.unimi.dsi.fastutil.objects.Object2ByteMap;
import it.unimi.dsi.fastutil.objects.Object2ByteOpenHashMap;
import net.kjp12.helium.helpers.IShadowBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.UUID;

@Mixin(BlockEntity.class)
public abstract class MixinBlockEntity implements IShadowBlockEntity {
    protected UUID helium$owner;
    // Bits:
    // 1 - Read
    // 2 - Write
    // 4 - Destroy
    protected Object2ByteMap<UUID> helium$access;

    @Inject(method = "fromTag(Lnet/minecraft/block/BlockState;Lnet/minecraft/nbt/CompoundTag;)V", at = @At("RETURN"))
    public void helium$fromTag(BlockState state, CompoundTag nbt, CallbackInfo cbi) {
        // LEGACY NOTE: "sodium" namespace is already used in production.
        //  In order to keep production tile entities from losing their lock, presence of Sodium will be checked if Helium doesn't exist.
        var helium = nbt.contains("helium", 10) ? nbt.getCompound("helium") : nbt.getCompound("sodium");
        if (helium != null) {
            helium$owner = helium.containsUuid("owner") ? helium.getUuid("owner") : null;
            if (helium$owner != null) {
                var publicAccess = helium.getByte("public");
                helium$access = new Object2ByteOpenHashMap<>();
                helium$access.defaultReturnValue(publicAccess);
                var al = helium.getList("access", helium.getType());
                if (al != null) {
                    for (var a : al) {
                        var access = (CompoundTag) a;
                        if (access.containsUuid("t") && access.contains("p"))
                            helium$access.put(access.getUuid("t"), access.getByte("p"));
                    }
                }
            }
        }
    }

    @Inject(method = "toTag(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/nbt/CompoundTag;", at = @At("RETURN"))
    public void helium$toTag(CompoundTag nbt, CallbackInfoReturnable<CompoundTag> cbi) {
        if (helium$owner != null) {
            var helium = new CompoundTag();
            helium.putUuid("owner", helium$owner);
            if (helium$access != null) {
                helium.putByte("public", helium$access.defaultReturnValue());
                var access = new ListTag();
                for (var e : helium$access.object2ByteEntrySet()) {
                    var tmp = new CompoundTag();
                    tmp.putUuid("t", e.getKey());
                    tmp.putByte("p", e.getByteValue());
                    access.add(tmp);
                }
                helium.put("access", access);
            }
            nbt.put("helium", helium);
        }
    }

    public UUID helium$getOwner() {
        return helium$owner;
    }

    public void helium$setOwner(UUID uuid) {
        helium$owner = uuid;
    }

    public boolean helium$canOpenBlock(UUID uuid) {
        return helium$owner == null || helium$owner.equals(uuid) || (helium$access != null && (helium$access.getByte(uuid) & 1) == 1);
    }

    public boolean helium$canModifyBlock(UUID uuid) {
        return helium$owner == null || helium$owner.equals(uuid) || (helium$access != null && (helium$access.getByte(uuid) & 2) == 2);
    }

    public boolean helium$canBreakBlock(UUID uuid) {
        return helium$owner == null || helium$owner.equals(uuid) || (helium$access != null && (helium$access.getByte(uuid) & 4) == 4);
    }
}
