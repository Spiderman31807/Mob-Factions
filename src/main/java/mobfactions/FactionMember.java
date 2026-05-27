package mobfactions;

import net.minecraft.world.entity.EntityType;

import java.util.Set;

public interface FactionMember {
	abstract FactionCache cachedFactions();

	abstract EntityType getType();

	default Set<Faction> getFactions() {
		FactionCache cached = this.cachedFactions();
		if (cached.getStamp().equals(FactionManager.getStamp()))
			return cached.get();

		Set<Faction> factions = FactionManager.getAllegiance(this.getType());
		cached.updateFactions(factions);
		return factions;
	}

	default boolean getDostileTowards(EntityType type) {
		FactionCache cached = this.cachedFactions();
		if (cached.hasDostileCached(type) && cached.getStamp().equals(FactionManager.getStamp()))
			return cached.isDostileTowards(type);
		boolean isDostile = this.shouldBehaveDostileTowards(type);
		cached.updateDostile(type, isDostile);
		return isDostile;
	}

	default boolean getHostileTowards(EntityType type) {
		FactionCache cached = this.cachedFactions();
		if (cached.hasHostileCached(type) && cached.getStamp().equals(FactionManager.getStamp()))
			return cached.isHostileTowards(type);
		boolean isHostile = this.shouldBehaveHostileTowards(type);
		cached.updateHostile(type, isHostile);
		return isHostile;
	}

	default boolean shouldBehaveDostileTowards(EntityType type) {
		Set<Faction> targetFactions = FactionManager.getAllegiance(type);
		for (Faction faction : this.getFactions()) {
			if (faction.getMembers().contains(type))
				return true;
			if (faction.getDostile().contains(type))
				return true;
		}
		for (Faction faction : this.getFactions()) {
			for (Faction targetFaction : targetFactions) {
				if (faction.getAllies().contains(targetFaction.getUUID()))
					return true;
			}
		}

		return false;
	}

	default boolean shouldBehaveHostileTowards(EntityType type) {
		Set<Faction> targetFactions = FactionManager.getAllegiance(type);
		for (Faction faction : this.getFactions()) {
			if (faction.getMembers().contains(type))
				return false;
			if (faction.getDostile().contains(type))
				return false;
			if (faction.getHostile().contains(type))
				return true;
		}
		for (Faction faction : this.getFactions()) {
			for (Faction targetFaction : targetFactions) {
				if (faction.getEnemies().contains(targetFaction.getUUID()))
					return true;
			}
		}

		return false;
	}
}