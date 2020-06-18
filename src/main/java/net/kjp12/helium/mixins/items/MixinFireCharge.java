package net.kjp12.helium.mixins.items;

import net.kjp12.helium.Helium;
import net.minecraft.item.FireChargeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FireChargeItem.class)
public class MixinFireCharge extends Item {
    public MixinFireCharge(Settings settings) {
        super(settings);
    }

    @Inject(method = "useOnBlock(Lnet/minecraft/item/ItemUsageContext;)Lnet/minecraft/util/ActionResult;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z", ordinal = 1))
    private void helium$useOnBlock$logUsage(ItemUsageContext iuc, CallbackInfoReturnable<ActionResult> cbir) {
        Helium.LOGGER.info("{} used Fire Charge at {}.", iuc.getPlayer(), iuc.getBlockPos());
    }
}
