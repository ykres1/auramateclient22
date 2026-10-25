package dev.auramate.client;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BlocksAttacksComponent;
import net.minecraft.component.type.UseCooldownComponent;
import net.minecraft.item.Items;

import java.util.Optional;

/** Common entrypoint: Fast Shield changes the shield item itself, so it lives here. */
public class AuraMate implements ModInitializer {
	private static final float BLOCK_DELAY_SECONDS = 0.15f;

	@Override
	public void onInitialize() {
		// Wind Charge Opt: shorten the wind charge use cooldown (vanilla 10 ticks) to the panel setting.
		DefaultItemComponentEvents.MODIFY.register(context ->
				context.modify(item -> item == Items.WIND_CHARGE, (builder, item) -> {
					if (!Features.WIND_CHARGE_OPT.isEnabled()) return;
					int ticks = Features.WIND_CHARGE_OPT.value();
					if (ticks <= 0) {
						builder.add(DataComponentTypes.USE_COOLDOWN, null);
					} else {
						builder.add(DataComponentTypes.USE_COOLDOWN, new UseCooldownComponent(ticks / 20f, Optional.empty()));
					}
				}));

		DefaultItemComponentEvents.MODIFY.register(context ->
				context.modify(item -> item == Items.SHIELD, (builder, item) -> {
					if (!Features.FAST_SHIELD.isEnabled()) return;
					BlocksAttacksComponent old = item.getComponents().get(DataComponentTypes.BLOCKS_ATTACKS);
					if (old == null) return;
					builder.add(DataComponentTypes.BLOCKS_ATTACKS, new BlocksAttacksComponent(
							BLOCK_DELAY_SECONDS,
							old.disableCooldownScale(),
							old.damageReductions(),
							old.itemDamage(),
							old.bypassedBy(),
							old.blockSound(),
							old.disableSound()));
				}));
	}
}
