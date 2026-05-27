package mobfactions;

import net.minecraft.world.entity.Entity;

import java.util.Set;

public interface FactionHolder {
	default Set<Faction> getFactions() {
		Entity entity = (Entity) (Object) this;
		if(entity.level().isClientSide)
			throw new IllegalArgumentException("Attempted to get Mob Factions from Clientside for " + entity.getDisplayName());
		return FactionManager.getFactionsByType(entity.getType());
	}

	default boolean isFactionEnemy(Entity entity) {
		if (entity instanceof FactionHolder holder) {
			for (Faction faction : this.getFactions()) {
				if (faction.shouldAttackEntity(entity))
					return true;
			}
		}
		
		return false;
	}

	default boolean isFactionAlly(Entity entity) {
		if (entity instanceof FactionHolder holder) {
			for (Faction faction : this.getFactions()) {
				if (!faction.canAttackEntity(entity))
					return true;
			}
		}
		
		return false;
	}
}