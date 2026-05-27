package mobfactions.mixin;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityType;

@Mixin(Mob.class)
public abstract class MobMixin extends EntityMixin {
	@Inject(method = "canAttackType", at = @At("RETURN"), cancellable = true)
	private void canAttackType(EntityType<?> type, CallbackInfoReturnable<Boolean> callback) {
		if (this.getDostileTowards(type))
			callback.setReturnValue(false);
	}
}