package mobfactions;

import net.minecraft.world.entity.EntityType;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;
import java.util.Set;
import java.util.HashSet;

import com.mojang.realmsclient.util.JsonUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;

public class Faction {
	private final String id;
	private final UUID uuid;
	private final Set<UUID> allies = new HashSet();
	private final Set<UUID> enemies = new HashSet();
	private final Set<EntityType> members = new HashSet();
	private final Set<EntityType> dostile = new HashSet();
	private final Set<EntityType> hostile = new HashSet();
	//private ResourceLocation iconPath = FactionManager.defaultIcon;

	public Faction(String id) {
		this.id = id;
		this.uuid = Mth.createInsecureUUID();
	}

	public Faction(String id, UUID uuid) {
		this.id = id;
		this.uuid = uuid;
	}

	public Faction(JsonObject json) {
		this.id = JsonUtils.getStringOr("id", json, "MISSING_NAME");
		this.uuid = JsonUtils.getUuidOr("uuid", json, Mth.createInsecureUUID());
		loadUUIDArray(this.allies, json.getAsJsonArray("allies"));
		loadUUIDArray(this.enemies, json.getAsJsonArray("enemies"));
		loadTypeArray(this.members, json.getAsJsonArray("members"));
		loadTypeArray(this.dostile, json.getAsJsonArray("dostile"));
		loadTypeArray(this.hostile, json.getAsJsonArray("hostile"));
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		json.addProperty("id", this.id);
		json.addProperty("uuid", this.uuid.toString());
		json.add("allies", convertSet(this.allies));
		json.add("enemies", convertSet(this.enemies));
		json.add("members", convertSet(this.members));
		json.add("dostile", convertSet(this.dostile));
		json.add("hostile", convertSet(this.hostile));
		return json;
	}

	public String getId() {
		return this.id;
	}

	public UUID getUUID() {
		return this.uuid;
	}

	public Set<UUID> getAllies() {
		return this.allies;
	}

	public Set<UUID> getEnemies() {
		return this.enemies;
	}

	public Set<EntityType> getMembers() {
		return this.members;
	}

	public Set<EntityType> getDostile() {
		return this.dostile;
	}

	public Set<EntityType> getHostile() {
		return this.hostile;
	}

	public void addMember(EntityType type) {
		if(this.members.contains(type))
			return;

		this.members.add(type);
		FactionManager.update(this.uuid);
	}

	public void removeMember(EntityType type) {
		if(!this.members.contains(type))
			return;

		this.members.remove(type);
		FactionManager.update(this.uuid);
	}

	private static <H> JsonArray convertSet(Set<H> values) {
		JsonArray array = new JsonArray();
		for (H value : values) {
			array.add(value.toString());
		}
		return array;
	}

	private static void loadUUIDArray(Set<UUID> storedSet, JsonArray uuidArray) {
		if (uuidArray == null)
			return;
		for (JsonElement element : uuidArray.asList()) {
			if (!element.isJsonNull())
				storedSet.add(UUID.fromString(element.getAsString()));
		}
	}

	private static void loadTypeArray(Set<EntityType> storedSet, JsonArray typeArray) {
		if (typeArray == null)
			return;
		for (JsonElement element : typeArray.asList()) {
			if (!element.isJsonNull()) {
				EntityType.byString(element.getAsString()).ifPresent((type) -> {
					storedSet.add(type);
				});
			}
		}
	}
}