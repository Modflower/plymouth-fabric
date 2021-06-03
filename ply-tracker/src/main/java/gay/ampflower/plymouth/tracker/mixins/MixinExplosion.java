package gay.ampflower.plymouth.tracker.mixins;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import gay.ampflower.plymouth.database.DatabaseHelper;
import gay.ampflower.plymouth.database.Target;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;

/**
 * @author Ampflower
 * @since ${version}
 **/
@Mixin(Explosion.class)
public abstract class MixinExplosion {
    @Shadow
    @Final
    private World world;

    @Shadow
    @Nullable
    public abstract LivingEntity getCausingEntity();

    @Shadow
    @Final
    @Nullable
    private Entity entity;

    @SuppressWarnings("ConstantConditions") // the block entity should be not null if it has one.
    @Inject(method = "affectWorld",
            locals = LocalCapture.CAPTURE_FAILEXCEPTION,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;shouldDropItemsOnExplosion(Lnet/minecraft/world/explosion/Explosion;)Z"))
    private void plymouth$affectWorld$onSetBlock(boolean flag, CallbackInfo cbi, boolean $2, ObjectArrayList<?> $0, Iterator<?> $1, BlockPos pos, BlockState old) {
        if (!(world instanceof ServerWorld)) return;
        Entity e = getCausingEntity();
        if (e == null) e = entity;
        DatabaseHelper.database.breakBlock((ServerWorld) world, pos, old, old.hasBlockEntity() ? world.getBlockEntity(pos).writeNbt(new NbtCompound()) : null, (Target) e);
    }
}
