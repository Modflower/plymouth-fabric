package net.kjp12.helium.mixins.items;

import net.kjp12.helium.Helium;
import net.minecraft.item.FlintAndSteelItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlintAndSteelItem.class)
public class MixinFlintAndSteel extends Item {
    public MixinFlintAndSteel(Settings settings) {
        super(settings);
    }

    @Inject(method = "useOnBlock(Lnet/minecraft/item/ItemUsageContext;)Lnet/minecraft/util/ActionResult;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/advancement/criterion/PlacedBlockCriterion;trigger(Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/item/ItemStack;)V"))
    private void helium$useOnBlock$logUsage(ItemUsageContext iuc, CallbackInfoReturnable<ActionResult> cbir) {
        Helium.LOGGER.info("{} used Flint & Steel at {}.", iuc.getPlayer(), iuc.getBlockPos());
    }
}
