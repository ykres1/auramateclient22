package dev.auramate.client;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class AuraMateClient implements ClientModInitializer {
	private static KeyBinding guiKey;
	private static KeyBinding lungeKey;
	private static KeyBinding elytraKey;

	@Override
	public void onInitializeClient() {
		KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("auramateclient", "main"));

		guiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.auramateclient.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));
		lungeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.auramateclient.lunge", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, category));

		elytraKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.auramateclient.elytra", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, category));

		HudElementRegistry.addLast(Identifier.of("auramateclient", "hud"), new Hud());
		HudElementRegistry.addLast(Identifier.of("auramateclient", "damage_tint"), new DamageTint());
		HudElementRegistry.addLast(Identifier.of("auramateclient", "now_playing"), new NowPlaying());
		NowPlaying.start();

		ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
			if (screen instanceof TitleScreen) {
				ScreenEvents.afterRender(screen).register((s, ctx, mx, my, delta) -> TitleLogo.draw(ctx, s.width));
			}
		});

		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			while (guiKey.wasPressed()) {
				if (mc.currentScreen == null) mc.setScreen(new PanelScreen());
			}
			Features.LUNGE_SWAP.tick(mc, lungeKey);
			Features.ELYTRA_SWAP.tick(mc, elytraKey);
			Tweaks.cobweb(mc);
			Tweaks.anchorFast(mc);
			Tweaks.fullbright(mc);
		});

		// Kept from the original mod: /iseeyourhearts [on|off]
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				dispatcher.register(ClientCommandManager.literal("iseeyourhearts")
						.executes(ctx -> setHearts(ctx, !Features.HEARTS.isEnabled()))
						.then(ClientCommandManager.literal("on").executes(ctx -> setHearts(ctx, true)))
						.then(ClientCommandManager.literal("off").executes(ctx -> setHearts(ctx, false)))));
	}

	private static int setHearts(CommandContext<FabricClientCommandSource> ctx, boolean on) {
		Features.HEARTS.setEnabled(on);
		ctx.getSource().sendFeedback(Text.literal("iseeyourhearts: " + (on ? "ON" : "OFF"))
				.formatted(on ? Formatting.GREEN : Formatting.RED));
		return 1;
	}
}
