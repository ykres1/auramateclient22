package dev.auramate.client.mixin;

import dev.auramate.client.Hearts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerMixin {
	@Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
	private void auramate$addStats(CallbackInfoReturnable<Text> cir) {
		if (!Hearts.isEnabled()) return;
		PlayerEntity self = (PlayerEntity) (Object) this;
		if (!(self instanceof AbstractClientPlayerEntity)) return;
		if (self == MinecraftClient.getInstance().player) return;
		cir.setReturnValue(Hearts.decorate(self, cir.getReturnValue()));
	}
}
