package gay.ampflower.plymouth.tracker.mixins;

import gay.ampflower.plymouth.database.DatabaseHelper;
import gay.ampflower.plymouth.database.Target;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FlintAndSteelItem;
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

@Mixin(FlintAndSteelItem.class)
public class MixinFlintAndSteel extends Item {
    public MixinFlintAndSteel(Settings settings) {
        super(settings);
    }

    @Inject(method = "useOnBlock(Lnet/minecraft/item/ItemUsageContext;)Lnet/minecraft/util/ActionResult;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z", ordinal = 1),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private void helium$useOnBlock$logUsage(ItemUsageContext iuc, CallbackInfoReturnable<ActionResult> cbir, PlayerEntity player, World world, BlockPos $2, BlockPos pos) {
        if (world instanceof ServerWorld serverWorld)
            DatabaseHelper.database.placeBlock(serverWorld, pos, Blocks.FIRE, (Target) player);
    }
}
