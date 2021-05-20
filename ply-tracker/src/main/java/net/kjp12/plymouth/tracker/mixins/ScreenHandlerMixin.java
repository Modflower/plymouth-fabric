package net.kjp12.plymouth.tracker.mixins;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.kjp12.plymouth.database.DatabaseHelper;
import net.kjp12.plymouth.database.Target;
import net.kjp12.plymouth.tracker.Tracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.DoubleInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.IdentityHashMap;
import java.util.List;

/**
 * @author KJP12
 * @since Jan. 02, 2021 @ 00:57
 **/
@Mixin(ScreenHandler.class)
public abstract class ScreenHandlerMixin {

    @Unique
    private Object2IntMap<Slot> mutatedSlots = new Object2IntOpenHashMap<>();
    @Unique
    private IdentityHashMap<ItemStack, Slot> stackToSlot = new IdentityHashMap<>();

    @Shadow
    @Final
    public List<Slot> slots;

    /**
     * @reason To create an event bus for taking items.
     * @author KJP12
     */
    @Redirect(method = "method_30010", require = 7, at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/slot/Slot;onTakeItem(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;"))
    private ItemStack plymouth$30010$onTakeItem(Slot slot, PlayerEntity player, ItemStack stack) {
        var inv = slot.inventory instanceof DoubleInventory ? getInventory((AccessorDoubleInventory) slot.inventory, slot.id) : slot.inventory;
        if (player instanceof ServerPlayerEntity && inv instanceof Target) {
            Tracker.logger.info("[TAKE] onTakeItem: Stack is {}, slot is {}", stack, slot);
            DatabaseHelper.database.takeItems((Target) inv, stack, stack.getCount(), (Target) player);
        }
        return slot.onTakeItem(player, stack);
    }

    @Redirect(method = "method_30010", require = 12, at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/slot/Slot;setStack(Lnet/minecraft/item/ItemStack;)V"))
    private void plymouth$30010$onSetStack(Slot slot, ItemStack stack, int i, int j, SlotActionType slotActionType, PlayerEntity player) {
        var inv = slot.inventory instanceof DoubleInventory ? getInventory((AccessorDoubleInventory) slot.inventory, slot.id) : slot.inventory;
        if (player instanceof ServerPlayerEntity && inv instanceof Target) {
            if (stack.isEmpty()) {
                if (slot.hasStack()) {
                    Tracker.logger.info("[TAKE] onSetStack: Return is {}, slot is {}.", stack, slot);
                }
                //var st = slot.getStack();
                //DatabaseHelper.database.takeItems((ServerWorld) be.getWorld(), be.getPos(), st, st.getCount(), player);
            } else {
                Tracker.logger.info("[PUT] onSetStack: Return is {}, slot is {}", stack, slot);
                DatabaseHelper.database.putItems((Target) inv, stack, stack.getCount(), (Target) player);
            }
        }
        slot.setStack(stack);
    }

    @Redirect(method = "method_30010", at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/ScreenHandler;transferSlot(Lnet/minecraft/entity/player/PlayerEntity;I)Lnet/minecraft/item/ItemStack;"))
    private ItemStack plymouth$30010$onTransferSlot(ScreenHandler screenHandler, PlayerEntity player, int index) {
        mutatedSlots.clear();
        stackToSlot.clear();
        if (player instanceof ServerPlayerEntity) {
            var inv = slots.get(0).inventory;
            var slot = slots.get(index);
            var isDouble = inv instanceof DoubleInventory;
            var isTake = slot.inventory == inv;
            var oldCount = slot.getStack().getCount();
            Tracker.logger.info("Copy called in mid-transfer for {} by {} on {}. To {}", slot, player, null, inv);
            // We don't need mutations for takes. However, for puts, we do need it.
            if (isDouble && !isTake) regenMutationSlots();
            var ret = screenHandler.transferSlot(player, index);
            if (!ret.isEmpty()) {
                var c = oldCount - slot.getStack().getCount();
                if (isTake) {
                    var in = isDouble ? getInventory((AccessorDoubleInventory) inv, index) : inv;
                    if (in instanceof Target) {
                        Tracker.logger.info("[TAKE] Return is {}, slot is now {}, delta of -{}", ret, slot, c);
                        DatabaseHelper.database.takeItems((Target) in, ret, c, (Target) player);
                    }
                } else if (isDouble) {
                    var adi = (AccessorDoubleInventory) inv;
                    Inventory ia = adi.getFirst(), ib = adi.getSecond();
                    int da = 0, db = 0;
                    if (mutatedSlots.isEmpty())
                        throw new AssertionError("mutatedSlots is empty. See " + stackToSlot + " and " + inv + ", composite of " + ia + " and " + ib + ".");
                    var itr = Object2IntMaps.fastIterator(mutatedSlots);
                    while (itr.hasNext()) {
                        var e = itr.next();
                        if (e.getKey().id < ia.size()) {
                            da += e.getIntValue();
                        } else {
                            db += e.getIntValue();
                        }
                    }
                    if (da + db > c)
                        throw new AssertionError("Over counted?! Expected " + da + " + " + db + " to equal " + c + ". See " + stackToSlot + ", " + mutatedSlots + " and " + inv + ", composite of " + ia + " and " + ib + ", containing slots " + slots + ".");
                    if (da != 0 && ia instanceof Target) {
                        Tracker.logger.info("[PUT-IA] Return is {}, slot is now {}, delta of {}", ret, slot, da);
                        DatabaseHelper.database.putItems((Target) ia, ret, da, (Target) player);
                    }
                    if (db != 0 && ib instanceof Target) {
                        Tracker.logger.info("[PUT-IB] Return is {}, slot is now {}, delta of {}", ret, slot, db);
                        DatabaseHelper.database.putItems((Target) ib, ret, db, (Target) player);
                    }
                } else if (inv instanceof Target) {
                    Tracker.logger.info("[PUT] Return is {}, slot is now {}, delta of {}", ret, slot, c);
                    DatabaseHelper.database.putItems((Target) inv, ret, c, (Target) player);
                }
            }
            return ret;
        }
        return screenHandler.transferSlot(player, index);
    }

    @Redirect(method = "insertItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;setCount(I)V"))
    private void plymouth$insertItem$setCount(ItemStack stack, int count) {
        if (count != 0) {
            mutatedSlots.put(stackToSlot.get(stack), count - stack.getCount());
        }
        stack.setCount(count);
    }

    @Redirect(method = "insertItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/slot/Slot;setStack(Lnet/minecraft/item/ItemStack;)V"))
    private void plymouth$insertItem$setStack(Slot slot, ItemStack stack) {
        mutatedSlots.put(slot, stack.getCount());
        slot.setStack(stack);
    }

    @Unique
    private static Inventory getInventory(AccessorDoubleInventory inv, int index) {
        var first = inv.getFirst();
        return index < first.size() ? first : inv.getSecond();
    }

    @Unique
    private void regenMutationSlots() {
        for (var slot : slots)
            if (slot.hasStack()) {
                stackToSlot.put(slot.getStack(), slot);
            }
    }
}
