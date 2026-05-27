package mobfactions.mixin;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;

import mobfactions.MobFactionsMod;
import mobfactions.FactionHolder;

@Mixin(Mob.class)
public class MobMixin {
	@Shadow
	@Final
	protected GoalSelector targetSelector;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void construct(EntityType<? extends Mob> type, Level world, CallbackInfo callback) {
		if (!DefaultAttributes.hasSupplier(type))
			return;
		if (!DefaultAttributes.getSupplier(type).hasAttribute(Attributes.ATTACK_DAMAGE))
			return;
		MobFactionsMod.queueServerWork(1, () -> addGoals());
	}

	@Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
	private void setTarget(LivingEntity newTarget, CallbackInfo callback) {
		if (newTarget != null && this.getMob().isAlliedTo(newTarget))
			callback.cancel();
	}

	private Mob getMob() {
		return (Mob) (Object) this;
	}

	private void addGoals() {
		int priority = 1;
		for (WrappedGoal wrapped : this.targetSelector.getAvailableGoals()) {
			priority = Math.max(wrapped.getPriority() + 1, priority);
		}
		TargetingConditions.Selector filter = (entity, server) -> this instanceof FactionHolder holder && holder.isFactionEnemy(entity);
		this.targetSelector.addGoal(priority, new NearestAttackableTargetGoal(this.getMob(), LivingEntity.class, 10, true, true, filter));
	}
}