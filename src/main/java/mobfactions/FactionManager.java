package mobfactions;

import net.minecraft.world.entity.EntityType;

import java.util.UUID;
import java.util.Set;
import java.util.Optional;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Date;

import com.google.gson.JsonElement;
import com.google.gson.JsonArray;

public class FactionManager {
	//public static final ResourceLocation defaultIcon = ResourceLocation.fromNamespaceAndPath("mobfactions", "default_icon");
	private static final Map<UUID, Faction> factions = new HashMap();
	private static Date lastUpdatedStamp = new Date();

	static {
		Faction testing = new Faction("Testing");
		testing.addMember(EntityType.ZOMBIE);
		testing.addMember(EntityType.IRON_GOLEM);
		add(testing);
	}

	public static void update(UUID uuid) {
		if(factions.containsKey(uuid))
			lastUpdatedStamp = new Date();
	}

	public static Date getStamp() {
		return lastUpdatedStamp;
	}

	public static Optional<Faction> get(UUID uuid) {
		return Optional.ofNullable(factions.get(uuid));
	}

	public static void add(Faction faction) {
		factions.putIfAbsent(faction.getUUID(), faction);
		update(faction.getUUID());
	}

	public static void remove(Faction faction) {
		factions.remove(faction.getUUID());
		lastUpdatedStamp = new Date();
	}

	public static Set<Faction> getAllegiance(EntityType type) {
		Set<Faction> allegiances = new HashSet();
		for (Faction faction : factions.values()) {
			if (faction.getMembers().contains(type))
				allegiances.add(faction);
		}

		return allegiances;
	}

	public static JsonArray save() {
		JsonArray json = new JsonArray();
		for (Faction faction : factions.values()) {
			json.add(faction.toJson());
		}
		return json;
	}

	public static void load(JsonArray json, boolean clearOld) {
		if (clearOld)
			factions.clear();
		if (json == null)
			return;

		for (JsonElement element : json.asList()) {
			if (!element.isJsonNull())
				add(new Faction(element.getAsJsonObject()));
		}
	}
}