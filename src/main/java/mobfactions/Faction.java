package mobfactions;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.Mth;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;
import java.util.Set;
import java.util.HashSet;

public class Faction {
	public final UUID uuid;
	public final String id;
	public final Set<EntityType<?>> entities = new HashSet<>();
	public final Set<EntityType<?>> alliedEntities = new HashSet<>();
	public final Set<EntityType<?>> hostileEntities = new HashSet<>();
	public final Set<UUID> allied = new HashSet<>();
	public final Set<UUID> enemies = new HashSet<>();

	public Faction(String id) {
		this(Mth.createInsecureUUID(), id);
	}

	public Faction(UUID uuid, String id) {
		this.uuid = uuid;
		this.id = id;
	}

	public static Faction load(CompoundTag compound) {
		UUID factionUUID = compound.contains("UUID") ? UUID.fromString(compound.getStringOr("UUID", "")) : Mth.createInsecureUUID();
		String id = compound.getStringOr("Id", "MISSING-ID");
		final Faction faction =  new Faction(factionUUID, id);
		
		if (compound.contains("Entities")) {
			ListTag list = compound.getListOrEmpty("Entities");
			for (int idx = 0; idx < list.size(); idx++) {
				EntityType.byString(list.getStringOr(idx, "")).ifPresent((type) -> faction.entities.add(type));
			}
		}
		if (compound.contains("AlliedEntities")) {
			ListTag list = compound.getListOrEmpty("AlliedEntities");
			for (int idx = 0; idx < list.size(); idx++) {
				EntityType.byString(list.getStringOr(idx, "")).ifPresent((type) -> faction.alliedEntities.add(type));
			}
		}
		if (compound.contains("HostileEntities")) {
			ListTag list = compound.getListOrEmpty("HostileEntities");
			for (int idx = 0; idx < list.size(); idx++) {
				EntityType.byString(list.getStringOr(idx, "")).ifPresent((type) -> faction.hostileEntities.add(type));
			}
		}
		if (compound.contains("Allies")) {
			ListTag list = compound.getListOrEmpty("Allies");
			for (int idx = 0; idx < list.size(); idx++) {
				try {
					UUID uuid = UUID.fromString(list.getStringOr(idx, ""));
					faction.allied.add(uuid);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				}
			}
		}
		if (compound.contains("Enemies")) {
			ListTag list = compound.getListOrEmpty("Enemies");
			for (int idx = 0; idx < list.size(); idx++) {
				try {
					UUID uuid = UUID.fromString(list.getStringOr(idx, ""));
					faction.enemies.add(uuid);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				}
			}
		}
		return faction;
	}

	public CompoundTag save() {
		CompoundTag compound = new CompoundTag();
		compound.putString("UUID", this.uuid.toString());
		compound.putString("Id", this.id);
		
		final ListTag entityList = new ListTag();
		this.entities.forEach((type) -> entityList.addTag(entityList.size(), StringTag.valueOf(EntityType.getKey(type).toString())));
		compound.put("Entities", entityList);
		
		final ListTag alliedEntityList = new ListTag();
		this.alliedEntities.forEach((type) -> alliedEntityList.addTag(alliedEntityList.size(), StringTag.valueOf(EntityType.getKey(type).toString())));
		compound.put("AlliedEntities", alliedEntityList);
		
		final ListTag hostileEntityList = new ListTag();
		this.hostileEntities.forEach((type) -> hostileEntityList.addTag(hostileEntityList.size(), StringTag.valueOf(EntityType.getKey(type).toString())));
		compound.put("HostileEntities", hostileEntityList);
		
		final ListTag alliesList = new ListTag();
		this.allied.forEach((uuid) -> alliesList.addTag(alliesList.size(), StringTag.valueOf(uuid.toString())));
		compound.put("Allies", alliesList);
		
		final ListTag enemyList = new ListTag();
		this.enemies.forEach((uuid) -> enemyList.addTag(enemyList.size(), StringTag.valueOf(uuid.toString())));
		compound.put("Enemies", enemyList);
		
		return compound;
	}

	public UUID getUUID() {
		return this.uuid;
	}

	public boolean isWithinFaction(EntityType type) {
		return this.entities.contains(type);
	}

	public boolean isFriendlyTowards(EntityType type) {
		return this.alliedEntities.contains(type);
	}

	public boolean isHostileTowards(EntityType type) {
		return this.hostileEntities.contains(type);
	}

	public boolean canAttackType(EntityType type, boolean checkAlliedFactions) {
		if (this.isWithinFaction(type))
			return false;
		if (this.isFriendlyTowards(type))
			return false;
		if (checkAlliedFactions) {
			for (UUID factionUUID : this.allied) {
				if (FactionManager.getFaction(factionUUID).orElse(null) instanceof Faction faction && faction.isWithinFaction(type))
					return false;
			}
		}
		return true;
	}

	public boolean canAttackEntity(Entity entity) {
		if (!this.canAttackType(entity.getType(), false))
			return false;
		if (entity instanceof FactionHolder holder) {
			for (Faction faction : holder.getFactions()) {
				if (this.isAlliedTo(faction))
					return false;
			}
		}
		return true;
	}

	public boolean shouldAttackType(EntityType type, boolean checkAlliedFactions, boolean checkEnemyFactions) {
		if (!this.canAttackType(type, checkAlliedFactions))
			return false;
		if (this.isHostileTowards(type))
			return true;
		if (checkEnemyFactions) {
			for (UUID factionUUID : this.enemies) {
				if (FactionManager.getFaction(factionUUID).orElse(null) instanceof Faction faction && faction.isWithinFaction(type))
					return true;
			}
		}
		return false;
	}

	public boolean shouldAttackEntity(Entity entity) {
		if (!this.canAttackEntity(entity))
			return false;
		if (this.shouldAttackType(entity.getType(), false, false))
			return true;
		if (entity instanceof FactionHolder holder) {
			for (Faction faction : holder.getFactions()) {
				if (this.isAtWar(faction))
					return true;
			}
		}
		return false;
	}

	public boolean hasWarOrAlliance(Faction faction) {
		return this.isAtWar(faction) || this.isAlliedTo(faction);
	}

	public boolean isAtWar(Faction faction) {
		return this.isAtWar(faction, true);
	}

	public boolean isAtWar(Faction faction, boolean checkRef) {
		return this.enemies.contains(faction.getUUID()) || (checkRef && faction.isAtWar(this, false));
	}

	public boolean isAlliedTo(Faction faction) {
		return this.isAlliedTo(faction, true);
	}

	public boolean isAlliedTo(Faction faction, boolean checkRef) {
		return this.allied.contains(faction.getUUID()) || (checkRef && faction.isAlliedTo(this, false));
	}

	public boolean isAlliedToAny(Set<Faction> factions) {
		for(Faction faction : factions) {
			if(isAlliedTo(faction))
				return true;
		}

		return false;
	}

	public void addEntity(EntityType type) {
		FactionManager.addFactionEntities(this, type);
		this.removeAllyEntity(type);
		this.removeHostileEntity(type);
	}

	public void removeEntity(EntityType type) {
		FactionManager.removeFactionEntities(this, type);
	}

	public boolean addAllyEntity(EntityType type) {
		if(this.isWithinFaction(type))
			return false;
		if(this.isHostileTowards(type))
			return false;
		return this.alliedEntities.add(type);
	}

	public boolean removeAllyEntity(EntityType type) {
		return this.alliedEntities.remove(type);
	}

	public boolean addHostileEntity(EntityType type) {
		if(this.isWithinFaction(type))
			return false;
		if(this.isFriendlyTowards(type))
			return false;
		return this.hostileEntities.add(type);
	}

	public boolean removeHostileEntity(EntityType type) {
		return this.hostileEntities.remove(type);
	}

	public void createAllianceWith(Faction... factions) {
		for(Faction faction : factions) {
			this.createAlliance(faction);
		}
	}

	public boolean createAlliance(Faction faction) {
		return FactionManager.createAlliance(this, faction);
	}

	public void startWarWith(Faction... factions) {
		for(Faction faction : factions) {
			this.startWar(faction);
		}
	}

	public boolean startWar(Faction faction) {
		return FactionManager.startWar(this, faction);
	}

	public String getList(int type) {
		return switch(type) {
			default -> this.getEntityTypeList(this.entities);
			case 1 -> this.getEntityTypeList(this.alliedEntities);
			case 2 -> this.getEntityTypeList(this.hostileEntities);
			case 3 -> this.getFactionsList(this.allied);
			case 4 -> this.getFactionsList(this.enemies);
		};
	}

	public String getEntityTypeList(Set<EntityType<?>> entities) {
	    StringBuilder builder = new StringBuilder();
	    for (EntityType type : entities) {
	        builder.append("\n").append("  - ").append(type.getDescription().getString());
	    }
	    
	    return builder.toString();
	}

	public String getFactionsList(Set<UUID> uuids) {
	    final StringBuilder builder = new StringBuilder();
	    for (UUID uuid : uuids) {
	    	FactionManager.getFaction(uuid).ifPresent((faction) -> {
		        builder.append("\n").append("  - ").append(faction.id);
	    	});
	    }
	    
	    return builder.toString();
	}
}
