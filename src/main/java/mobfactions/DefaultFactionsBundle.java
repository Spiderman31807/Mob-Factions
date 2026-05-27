package mobfactions;

import net.minecraft.world.entity.EntityType;

import java.util.UUID;
import java.util.Set;
import java.util.Optional;
import java.util.HashSet;

public class DefaultFactionsBundle {
	private static final UUID zombieUUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
	private static final UUID skeletonUUID = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
	private static final UUID illagerUUID = UUID.fromString("7d6c1f98-a8bb-4f23-9cb6-df4b1de5c7e7");
	private static final UUID piglinUUID = UUID.fromString("9a0b1a29-5d5d-4857-b2d7-35b88f2db987");
	private static final Set<Faction> data = new HashSet<>();
	
	static {
		data.add(zombieFaction());
		data.add(skeletonFaction());
		data.add(illagerFaction());
		data.add(piglinFaction());
	}

	public static Set<Faction> getDefaultFactions() {
		return data;
	}

	public static boolean isCurrentConfig() {
		if (FactionManager.factions.size() != data.size())
			return false;
		for (Faction defaultFaction : getDefaultFactions()) {
			Optional<Faction> faction = FactionManager.getFaction(defaultFaction.getUUID());
			if (faction.isEmpty())
				return false;
			if (!faction.get().equals(defaultFaction))
				return false;
		}
		return true;
	}

	public static Faction zombieFaction() {
		Faction faction = new Faction(zombieUUID, "Zombie");
		faction.entities.add(EntityType.ZOMBIE);
		faction.entities.add(EntityType.HUSK);
		faction.entities.add(EntityType.DROWNED);
		faction.entities.add(EntityType.ZOMBIE_VILLAGER);
		faction.entities.add(EntityType.ZOMBIFIED_PIGLIN);
		faction.entities.add(EntityType.ZOMBIE_HORSE);
		faction.entities.add(EntityType.ZOGLIN);
		faction.entities.add(EntityType.GIANT);
		faction.allied.add(skeletonUUID);
		faction.enemies.add(illagerUUID);
		faction.enemies.add(piglinUUID);
		return faction;
	}

	public static Faction skeletonFaction() {
		Faction faction = new Faction(skeletonUUID, "Skeleton");
		faction.entities.add(EntityType.SKELETON);
		faction.entities.add(EntityType.STRAY);
		faction.entities.add(EntityType.WITHER_SKELETON);
		faction.entities.add(EntityType.SKELETON_HORSE);
		faction.allied.add(zombieUUID);
		faction.enemies.add(illagerUUID);
		faction.enemies.add(piglinUUID);
		return faction;
	}

	public static Faction illagerFaction() {
		Faction faction = new Faction(illagerUUID, "Illager");
		faction.entities.add(EntityType.PILLAGER);
		faction.entities.add(EntityType.VINDICATOR);
		faction.entities.add(EntityType.WITCH);
		faction.entities.add(EntityType.ILLUSIONER);
		faction.entities.add(EntityType.EVOKER);
		faction.entities.add(EntityType.VEX);
		faction.entities.add(EntityType.RAVAGER);
		faction.enemies.add(zombieUUID);
		faction.enemies.add(skeletonUUID);
		faction.enemies.add(piglinUUID);
		return faction;
	}

	public static Faction piglinFaction() {
		Faction faction = new Faction(piglinUUID, "Piglin");
		faction.entities.add(EntityType.PIGLIN);
		faction.entities.add(EntityType.PIGLIN_BRUTE);
		faction.entities.add(EntityType.ZOMBIFIED_PIGLIN);
		faction.entities.add(EntityType.HOGLIN);
		faction.enemies.add(zombieUUID);
		faction.enemies.add(skeletonUUID);
		faction.enemies.add(illagerUUID);
		return faction;
	}
}