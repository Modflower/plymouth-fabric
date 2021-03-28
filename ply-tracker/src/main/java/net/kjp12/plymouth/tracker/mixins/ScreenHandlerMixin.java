package net.kjp12.plymouth.tracker.mixins;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author KJP12
 * @since Jan. 02, 2021 @ 00:57
 **/
@Mixin(ScreenHandler.class)
public class ScreenHandlerMixin {

    /**
     * @reason To create an event bus for taking items.
     * @author KJP12
     */
    @Redirect(method = "method_30010", at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/slot/Slot;onTakeItem(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;"))
    private net.minecraft.item.ItemStack helium$onTakeItem(Slot slot, PlayerEntity player, ItemStack stack) {
        // TODO: Plymouth impl req.
        return slot.onTakeItem(player, stack);
    }
}
