package dev.auramate.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

/** Wind charge, swap to Lunge spear, jab. Logic is the original Lunge Swap Optimizer. */
public class LungeSwapFeature extends Feature {
	private int delayTicks = Math.max(0, Config.getInt("lunge.delay_ticks", 3));
	// 0 = idle, 1 = wind charge thrown (waiting), 2 = spear selected (waiting), 3 = jab release
	private int stage = 0;
	private int wait = 0;

	public LungeSwapFeature() {
		super("lungeswap", "Lunge Swap", "One key: wind charge, swap to Lunge spear, jab.", false, true, "Optimizers");
	}

	@Override
	public Setting setting() {
		return new Setting("delay", 0, 20, () -> delayTicks, v -> {
			delayTicks = v;
			Config.set("lunge.delay_ticks", v);
			Config.save();
		});
	}

	@Override
	protected void onToggle(boolean value) {
		stage = 0;
	}

	public void tick(MinecraftClient mc, KeyBinding comboKey) {
		if (mc.player == null || mc.interactionManager == null) {
			stage = 0;
			return;
		}
		// Drain presses so they don't pile up while the feature is off.
		if (!isEnabled()) {
			stage = 0;
			while (comboKey.wasPressed()) { /* ignore */ }
			return;
		}
		PlayerInventory inv = mc.player.getInventory();

		if (stage == 0) {
			while (comboKey.wasPressed()) {
				int charge = findHotbar(inv, false);
				int spear = findHotbar(inv, true);
				if (charge < 0 || spear < 0) {
					mc.player.sendMessage(Text.literal(
							"Lunge Swap: need a wind charge and a Lunge spear in your hotbar"), true);
					continue;
				}
				inv.setSelectedSlot(charge);
				mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
				stage = 1;
				wait = delayTicks;
				return;
			}
			return;
		}

		if (wait > 0) {
			wait--;
			return;
		}

		if (stage == 1) {
			int spear = findHotbar(inv, true);
			if (spear >= 0) inv.setSelectedSlot(spear);
			stage = 2;
			wait = delayTicks;
		} else if (stage == 2) {
			// jab with the spear (left click)
			KeyBinding.onKeyPressed(mc.options.attackKey.getDefaultKey());
			stage = 3;
			wait = 1;
		} else if (stage == 3) {
			stage = 0;
		}
	}

	private static int findHotbar(PlayerInventory inv, boolean spearWithLunge) {
		for (int i = 0; i < 9; i++) {
			ItemStack s = inv.getStack(i);
			if (s.isEmpty()) continue;
			if (!spearWithLunge) {
				if (s.isOf(Items.WIND_CHARGE)) return i;
			} else {
				String path = Registries.ITEM.getId(s.getItem()).getPath();
				if (path.endsWith("_spear") && hasLunge(s)) return i;
			}
		}
		return -1;
	}

	private static boolean hasLunge(ItemStack stack) {
		for (var entry : stack.getEnchantments().getEnchantments()) {
			if (entry.getKey().map(k -> k.getValue().getPath().equals("lunge")).orElse(false)) return true;
		}
		return false;
	}
}
