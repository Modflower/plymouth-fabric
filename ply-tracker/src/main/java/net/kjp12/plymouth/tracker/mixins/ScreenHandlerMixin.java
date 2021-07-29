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
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.IdentityHashMap;

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
    public DefaultedList<Slot> slots;

    private static final String TAKE_SLOT_DEFINITION = "Lnet/minecraft/screen/slot/Slot;onTakeItem(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;)V";

    /**
     * @reason To create an event bus for taking items.
     * @author KJP12
     */
    // [RAW ASM - MUST CHECK]
    @SuppressWarnings("UnresolvedMixinReference")
    @Redirect(method = {"internalOnSlotClick", "method_34249"}, require = 4, at = @At(value = "INVOKE", target = TAKE_SLOT_DEFINITION))
    private void plymouth$30010$onTakeItem(Slot slot, PlayerEntity player, ItemStack stack) {
        plymouth$bridge$onTakeItem(slot, player, stack);
    }

    // [RAW ASM - MUST CHECK]
    @SuppressWarnings("UnresolvedMixinReference")
    @Redirect(method = "method_34251", require = 1, at = @At(value = "INVOKE", target = TAKE_SLOT_DEFINITION))
    private static void plymouth$34251$onTakeItem(Slot slot, PlayerEntity player, ItemStack stack) {
        plymouth$bridge$onTakeItem(slot, player, stack);
    }

    @Unique
    private static void plymouth$bridge$onTakeItem(Slot slot, PlayerEntity player, ItemStack stack) {
        var inv = slot.inventory instanceof AccessorDoubleInventory doubleInventory ? getInventory(doubleInventory, slot.id) : slot.inventory;
        if (player instanceof ServerPlayerEntity && inv instanceof Target targetInv) {
            Tracker.logger.info("[TAKE] onTakeItem: Stack is {}, slot is {}", stack, slot);
            DatabaseHelper.database.takeItems(targetInv, stack, stack.getCount(), (Target) player);
        }
        slot.onTakeItem(player, stack);
    }

    @Redirect(method = "internalOnSlotClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/slot/Slot;insertStack(Lnet/minecraft/item/ItemStack;I)Lnet/minecraft/item/ItemStack;"))
    private ItemStack plymouth$30010$onInsertStack(Slot slot, ItemStack stack, int c, int i, int j, SlotActionType actionType, PlayerEntity player) {
        int cu = stack.getCount();
        var ret = slot.insertStack(stack, c);
        var di = cu - ret.getCount();
        if (di != 0 && player instanceof ServerPlayerEntity) {
            var inv = slot.inventory instanceof AccessorDoubleInventory doubleInventory ? getInventory(doubleInventory, slot.id) : slot.inventory;
            if (inv instanceof Target targetInv) {
                Tracker.logger.info("[PUT] onInsertStack: Add is {}, Return is {}, slot is {}", di, stack, slot);
                DatabaseHelper.database.putItems(targetInv, slot.getStack(), di, (Target) player);
            }
        }
        return ret;
    }

    @Redirect(method = "internalOnSlotClick", require = 7, at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/slot/Slot;setStack(Lnet/minecraft/item/ItemStack;)V"))
    private void plymouth$30010$onSetStack(Slot slot, ItemStack stack, int i, int j, SlotActionType slotActionType, PlayerEntity player) {
        var inv = slot.inventory instanceof DoubleInventory ? getInventory((AccessorDoubleInventory) slot.inventory, slot.id) : slot.inventory;
        if (player instanceof ServerPlayerEntity && inv instanceof Target targetInv) {
            if (stack.isEmpty()) {
                if (slot.hasStack()) {
                    Tracker.logger.info("[TAKE] onSetStack: Return is {}, slot is {}.", stack, slot);
                }
                //var st = slot.getStack();
                //DatabaseHelper.database.takeItems((ServerWorld) be.getWorld(), be.getPos(), st, st.getCount(), player);
            } else {
                Tracker.logger.info("[PUT] onSetStack: Return is {}, slot is {}", stack, slot);
                DatabaseHelper.database.putItems(targetInv, stack, stack.getCount(), (Target) player);
            }
        }
        slot.setStack(stack);
    }

    @Redirect(method = "internalOnSlotClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/ScreenHandler;transferSlot(Lnet/minecraft/entity/player/PlayerEntity;I)Lnet/minecraft/item/ItemStack;"))
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
                    if (in instanceof Target targetIn) {
                        Tracker.logger.info("[TAKE] Return is {}, slot is now {}, delta of -{}", ret, slot, c);
                        DatabaseHelper.database.takeItems(targetIn, ret, c, (Target) player);
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
                    if (da != 0 && ia instanceof Target targetInv) {
                        Tracker.logger.info("[PUT-IA] Return is {}, slot is now {}, delta of {}", ret, slot, da);
                        DatabaseHelper.database.putItems(targetInv, ret, da, (Target) player);
                    }
                    if (db != 0 && ib instanceof Target targetInv) {
                        Tracker.logger.info("[PUT-IB] Return is {}, slot is now {}, delta of {}", ret, slot, db);
                        DatabaseHelper.database.putItems(targetInv, ret, db, (Target) player);
                    }
                } else if (inv instanceof Target targetInv) {
                    Tracker.logger.info("[PUT] Return is {}, slot is now {}, delta of {}", ret, slot, c);
                    DatabaseHelper.database.putItems(targetInv, ret, c, (Target) player);
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
