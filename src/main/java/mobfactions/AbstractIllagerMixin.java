package mobfactions.mixin;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.Entity;

import mobfactions.FactionHolder;

@Mixin(AbstractIllager.class)
public abstract class AbstractIllagerMixin {
	@Inject(method = "considersEntityAsAlly", at = @At("RETURN"), cancellable = true)
	public void considersEntityAsAlly(Entity entity, CallbackInfoReturnable<Boolean> callback) {
		if (this instanceof FactionHolder holder) {
			if (callback.getReturnValue() == true && holder.isFactionEnemy(entity)) {
				callback.setReturnValue(false);
			} else if (callback.getReturnValue() == false && holder.isFactionAlly(entity)) {
				callback.setReturnValue(true);
			}
		}
	}
}
