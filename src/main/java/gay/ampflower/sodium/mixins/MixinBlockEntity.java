package gay.ampflower.sodium.mixins;

import it.unimi.dsi.fastutil.objects.Object2ByteMap;
import it.unimi.dsi.fastutil.objects.Object2ByteOpenHashMap;
import gay.ampflower.sodium.helpers.IProtectBlock;
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
public abstract class MixinBlockEntity implements IProtectBlock {
    protected UUID sodium$owner;
    // Bits:
    // 1 - Read
    // 2 - Write
    // 4 - Destroy
    protected Object2ByteMap<UUID> sodium$access;

    @Inject(method="fromTag(Lnet/minecraft/block/BlockState;Lnet/minecraft/nbt/CompoundTag;)V", at = @At("RETURN"))
    public void sodium$fromTag(BlockState state, CompoundTag nbt, CallbackInfo cbi) {
        var sodium = nbt.getCompound("sodium");
        if(sodium != null) {
            sodium$owner = sodium.containsUuidNew("owner") ? sodium.getUuidNew("owner") : null;
            if (sodium$owner != null) {
                var publicAccess = sodium.getByte("public");
                sodium$access = new Object2ByteOpenHashMap<>();
                sodium$access.defaultReturnValue(publicAccess);
                var al = sodium.getList("access", sodium.getType());
                if (al != null) {
                    for (var a : al) {
                        var access = (CompoundTag) a;
                        if(access.containsUuidNew("t") && access.contains("p"))
                        sodium$access.put(access.getUuidNew("t"), access.getByte("p"));
                    }
                }
            }
        }
    }

    @Inject(method="toTag(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/nbt/CompoundTag;", at = @At("RETURN"))
    public void sodium$toTag(CompoundTag nbt, CallbackInfoReturnable<CompoundTag> cbi) {
        if(sodium$owner != null) {
            var sodium = new CompoundTag();
            sodium.putUuidNew("owner", sodium$owner);
            if (sodium$access != null) {
                sodium.putByte("public", sodium$access.defaultReturnValue());
                var access = new ListTag();
                for (var e : sodium$access.object2ByteEntrySet()) {
                    var tmp = new CompoundTag();
                    tmp.putUuidNew("t", e.getKey());
                    tmp.putByte("p", e.getByteValue());
                    access.add(tmp);
                }
                sodium.put("access", access);
            }
            nbt.put("sodium", sodium);
        }
    }

    public UUID sodium$getOwner() {
        return sodium$owner;
    }

    public void sodium$setOwner(UUID uuid) {
        sodium$owner = uuid;
    }

    public boolean sodium$canOpenBlock(UUID uuid) {
        return sodium$owner == null || sodium$owner.equals(uuid) || (sodium$access != null && (sodium$access.getByte(uuid) & 1) == 1);
    }

    public boolean sodium$canModifyBlock(UUID uuid) {
        return sodium$owner == null || sodium$owner.equals(uuid) || (sodium$access != null && (sodium$access.getByte(uuid) & 2) == 2);
    }

    public boolean sodium$canBreakBlock(UUID uuid) {
        return sodium$owner == null || sodium$owner.equals(uuid) || (sodium$access != null && (sodium$access.getByte(uuid) & 4) == 4);
    }
}
