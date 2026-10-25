package dev.auramate.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

/**
 * One key swaps your chestplate and elytra instantly: all three inventory clicks are sent
 * in the same tick, so there is no opening the inventory and no waiting between clicks.
 */
public class ElytraSwapFeature extends Feature {
	private static final int CHEST_SLOT = 6; // chest armor slot in the player screen handler

	public ElytraSwapFeature() {
		super("elytraswap", "Elytra Swap", "Key (H): instantly swap chestplate and elytra.", false, true, "Optimizers");
	}

	public void tick(MinecraftClient mc, KeyBinding key) {
		if (mc.player == null || mc.interactionManager == null) return;
		if (!isEnabled()) {
			while (key.wasPressed()) { /* ignore */ }
			return;
		}
		while (key.wasPressed()) swap(mc);
	}

	private static void swap(MinecraftClient mc) {
		ClientPlayerEntity p = mc.player;
		boolean wearingElytra = p.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
		int src = -1;
		// screen handler slots: 9-35 main inventory, 36-44 hotbar
		for (int slot = 9; slot <= 44; slot++) {
			ItemStack s = p.playerScreenHandler.getSlot(slot).getStack();
			if (s.isEmpty()) continue;
			if (wearingElytra ? isChestplate(s) : s.isOf(Items.ELYTRA)) {
				src = slot;
				break;
			}
		}
		if (src < 0) {
			p.sendMessage(Text.literal(wearingElytra
					? "Elytra Swap: no chestplate in your inventory"
					: "Elytra Swap: no elytra in your inventory"), true);
			return;
		}
		int sync = p.playerScreenHandler.syncId;
		mc.interactionManager.clickSlot(sync, src, 0, SlotActionType.PICKUP, p);
		mc.interactionManager.clickSlot(sync, CHEST_SLOT, 0, SlotActionType.PICKUP, p);
		mc.interactionManager.clickSlot(sync, src, 0, SlotActionType.PICKUP, p);
	}

	private static boolean isChestplate(ItemStack s) {
		if (s.isOf(Items.ELYTRA)) return false;
		EquippableComponent eq = s.get(DataComponentTypes.EQUIPPABLE);
		return eq != null && eq.slot() == EquipmentSlot.CHEST;
	}
}
