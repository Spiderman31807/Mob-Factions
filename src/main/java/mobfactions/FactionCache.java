package mobfactions;

import net.minecraft.world.entity.EntityType;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Date;

public class FactionCache {
	private Date lastUpdatedStamp;
	private Set<Faction> factions;
	private Map<EntityType, Boolean> isDostile = new HashMap();
	private Map<EntityType, Boolean> isHostile = new HashMap();

	public FactionCache(Set<Faction> factions) {
		this.factions = factions;
		this.lastUpdatedStamp = FactionManager.getStamp();
	}

	public Date getStamp() {
		return this.lastUpdatedStamp;
	}

	public Set<Faction> get() {
		return this.factions;
	}

	public void updateFactions(Set<Faction> factions) {
		this.factions = factions;
	}

	public boolean hasDostileCached(EntityType type) {
		return this.isDostile.containsKey(type);
	}

	public boolean isDostileTowards(EntityType type) {
		return this.isDostile.getOrDefault(type, false);
	}

	public void updateDostile(EntityType type, boolean isDostile) {
		this.isDostile.put(type, isDostile);
	}

	public boolean hasHostileCached(EntityType type) {
		return this.isHostile.containsKey(type);
	}

	public boolean isHostileTowards(EntityType type) {
		return this.isHostile.getOrDefault(type, false);
	}

	public void updateHostile(EntityType type, boolean isHostile) {
		this.isHostile.put(type, isHostile);
	}

}