package gay.ampflower.plymouth.tracker.mixins;

import gay.ampflower.plymouth.database.DatabaseHelper;
import gay.ampflower.plymouth.database.Target;
import net.minecraft.block.Blocks;
import net.minecraft.item.FireChargeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(FireChargeItem.class)
public class MixinFireCharge extends Item {
    public MixinFireCharge(Settings settings) {
        super(settings);
    }

    @Inject(method = "useOnBlock(Lnet/minecraft/item/ItemUsageContext;)Lnet/minecraft/util/ActionResult;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z", ordinal = 1),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private void helium$useOnBlock$logUsage(ItemUsageContext iuc, CallbackInfoReturnable<ActionResult> cbir, World world, BlockPos pos) {
        if (world instanceof ServerWorld)
            DatabaseHelper.database.placeBlock((ServerWorld) world, pos, Blocks.FIRE, (Target) iuc.getPlayer());
    }
}
