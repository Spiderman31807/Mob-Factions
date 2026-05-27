package mobfactions.mixin;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.entity.Entity;

import mobfactions.FactionMember;
import mobfactions.FactionCache;

import java.util.Set;

@Mixin(Entity.class)
public abstract class EntityMixin implements FactionMember {
	@Unique
	private FactionCache cachedFactionData = new FactionCache(Set.of());

	@Override
	public FactionCache cachedFactions() {
		return this.cachedFactionData;
	}

	@Inject(method = "isAlliedTo", at = @At("RETURN"), cancellable = true)
	private void isAlliedTo(Entity entity, CallbackInfoReturnable<Boolean> callback) {
		if (this.getDostileTowards(entity.getType()))
			callback.setReturnValue(true);
	}
}