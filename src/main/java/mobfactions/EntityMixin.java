package mobfactions.mixin;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.entity.Entity;

import mobfactions.FactionHolder;

@Mixin(Entity.class)
public abstract class EntityMixin implements FactionHolder {
	@Inject(method = "considersEntityAsAlly", at = @At("RETURN"), cancellable = true)
	public void considersEntityAsAlly(Entity entity, CallbackInfoReturnable<Boolean> callback) {
		if (callback.getReturnValue() == true && this.isFactionEnemy(entity))
			callback.setReturnValue(false);
		else if (callback.getReturnValue() == false && this.isFactionAlly(entity))
			callback.setReturnValue(true);
	}
}
