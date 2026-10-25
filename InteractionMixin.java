package dev.auramate.client.mixin;

import dev.auramate.client.Features;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Crystal Optimizer and Anchor Optimizer: remove the target client-side as soon as the click is sent. */
@Mixin(ClientPlayerInteractionManager.class)
public abstract class InteractionMixin {
	@Unique
	private BlockPos auramate$anchor;

	/** After the hit packet is sent, drop the crystal locally instead of waiting for the server. */
	@Inject(method = "attackEntity", at = @At("TAIL"))
	private void auramate$crystal(PlayerEntity player, Entity target, CallbackInfo ci) {
		if (!Features.CRYSTAL_OPT.isEnabled() || !(target instanceof EndCrystalEntity)) return;
		// a bare hand with weakness does no damage, so the crystal would not break
		if (player.hasStatusEffect(StatusEffects.WEAKNESS) && !player.hasStatusEffect(StatusEffects.STRENGTH)
				&& player.getMainHandStack().isEmpty()) return;
		target.setRemoved(Entity.RemovalReason.KILLED);
	}

	/** Before the click: remember the anchor only if this click will make it explode. */
	@Inject(method = "interactBlock", at = @At("HEAD"))
	private void auramate$anchorBefore(ClientPlayerEntity player, Hand hand, BlockHitResult hit,
									   CallbackInfoReturnable<ActionResult> cir) {
		auramate$anchor = null;
		if (!Features.ANCHOR_OPT.isEnabled()) return;
		ClientWorld world = MinecraftClient.getInstance().world;
		if (world == null || world.getRegistryKey() == World.NETHER) return; // anchors only explode outside the Nether
		BlockPos pos = hit.getBlockPos();
		BlockState state = world.getBlockState(pos);
		if (!state.isOf(Blocks.RESPAWN_ANCHOR)) return;
		int charges = state.get(RespawnAnchorBlock.CHARGES);
		if (charges <= 0) return;
		ItemStack held = player.getStackInHand(hand);
		boolean glowstone = held.isOf(Items.GLOWSTONE);
		if (glowstone && charges < 4) return; // this click only adds a charge
		if (hand == Hand.MAIN_HAND && !glowstone && player.getStackInHand(Hand.OFF_HAND).isOf(Items.GLOWSTONE)) return;
		auramate$anchor = pos.toImmutable();
	}

	/** After the click was accepted: remove the anchor locally so the spot is free right away. */
	@Inject(method = "interactBlock", at = @At("RETURN"))
	private void auramate$anchorAfter(ClientPlayerEntity player, Hand hand, BlockHitResult hit,
									  CallbackInfoReturnable<ActionResult> cir) {
		BlockPos pos = auramate$anchor;
		auramate$anchor = null;
		if (pos == null) return;
		ClientWorld world = MinecraftClient.getInstance().world;
		if (world == null || !cir.getReturnValue().isAccepted()) return;
		world.setBlockState(pos, Blocks.AIR.getDefaultState(), 11);
	}
}
