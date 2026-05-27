package mobfactions;

import net.minecraft.world.entity.EntityType;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.commands.CommandSourceStack;

import java.util.concurrent.CompletableFuture;
import java.util.UUID;
import java.util.Set;
import java.util.Optional;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Collection;

import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContext;

public class FactionManager implements SuggestionProvider<CommandSourceStack> {
	public static final Map<UUID, Faction> factions = new HashMap<>();
	public static final Map<EntityType<?>, Set<UUID>> entityFactions = new HashMap<>();

	public FactionManager() {
	}

	public CompletableFuture<Suggestions> getSuggestions(final CommandContext<CommandSourceStack> context, final SuggestionsBuilder builder) throws CommandSyntaxException {
		String base = builder.getRemainingLowerCase();
		for (Faction faction : factions.values()) {
			if (base.isBlank() || base.startsWith(faction.id.toLowerCase()))
				builder.suggest(faction.id);
		}
		
		return builder.buildFuture();
	}

	public static void resetData() {
		factions.clear();
		entityFactions.clear();
	}

	public static void load(CompoundTag compound) {
		if (compound == null || !compound.contains("Factions"))
			return;
		resetData();
		ListTag factionData = compound.getList("Factions", 10);
		for (int idx = 0; idx < factionData.size(); idx++) {
			addFaction(Faction.load(factionData.getCompound(idx)));
		}
	}

	public static CompoundTag save() {
		CompoundTag compound = new CompoundTag();
		ListTag factionList = new ListTag();
		factions.values().forEach((faction) -> factionList.addTag(factionList.size(), faction.save()));
		compound.put("Factions", factionList);
		return compound;
	}

	public static Set<Faction> getFactionsByType(EntityType type) {
		final Set<Faction> factions = new HashSet<>();
		for (UUID uuid : entityFactions.getOrDefault(type, new HashSet<>())) {
			getFaction(uuid).ifPresent((faction) -> factions.add(faction));
		}
		return factions;
	}

	public static String getFactionList() {
		StringBuilder builder = new StringBuilder();
		for (Faction faction : factions.values()) {
			builder.append("\n").append("  - ").append(faction.id);
		}
		return builder.toString();
	}

	public static void addFactionsToEntity(EntityType type, UUID... factionsUUID) {
		Set<UUID> current = entityFactions.getOrDefault(type, new HashSet<>());
		for (UUID uuid : factionsUUID) {
			current.add(uuid);
		}
		entityFactions.put(type, current);
	}

	public static void removeFactionsFromEntity(EntityType type, UUID... factionsUUID) {
		Set<UUID> current = entityFactions.getOrDefault(type, new HashSet<>());
		for (UUID uuid : factionsUUID) {
			current.remove(uuid);
		}
		entityFactions.put(type, current);
	}

	public static void addFactionEntities(Faction faction, EntityType... types) {
		for (EntityType type : types) {
			faction.entities.add(type);
			addFactionsToEntity(type, faction.getUUID());
		}
	}

	public static void removeFactionEntities(Faction faction, EntityType... types) {
		for (EntityType type : types) {
			faction.entities.remove(type);
			removeFactionsFromEntity(type, faction.getUUID());
		}
	}

	public static void addFactionEntities(Faction faction) {
		faction.entities.forEach((type) -> addFactionsToEntity(type, faction.getUUID()));
	}

	public static void removeFactionEntities(Faction faction) {
		faction.entities.forEach((type) -> removeFactionsFromEntity(type, faction.getUUID()));
	}

	public static Collection<String> getAllNames() {
		Set<String> names = new HashSet<>();
		factions.values().forEach((faction) -> names.add(faction.id));
		return names;
	}

	public static Optional<Faction> byId(String id) {
		for (Faction faction : factions.values()) {
			if (faction.id.equals(id))
				return Optional.of(faction);
		}
		return Optional.empty();
	}

	public static Optional<Faction> getFaction(UUID uuid) {
		if (!factions.containsKey(uuid))
			return Optional.empty();
		return Optional.of(factions.get(uuid));
	}

	public static boolean addFaction(Faction faction) {
		if (factions.containsKey(faction.getUUID()))
			return false;
		factions.put(faction.getUUID(), faction);
		addFactionEntities(faction);
		return true;
	}

	public static boolean removeFaction(Faction faction) {
		if (!removeFaction(faction.getUUID()))
			return false;
		removeFactionEntities(faction);
		return true;
	}

	public static boolean removeFaction(UUID uuid) {
		return factions.remove(uuid) != null;
	}

	public static boolean canCreateAllianceBetween(Faction faction1, Faction faction2) {
		return faction1 != faction2 && !faction1.hasWarOrAlliance(faction2);
	}

	public static boolean canStartWarBetween(Faction faction1, Faction faction2) {
		return faction1 != faction2 && !faction1.hasWarOrAlliance(faction2);
	}

	public static void createAllianceBetweenFactions(Faction... factions) {
		for (Faction faction : factions) {
			faction.createAllianceWith(factions);
		}
	}

	public static boolean createAlliance(Faction faction1, Faction faction2) {
		if (faction1 == null || faction2 == null)
			return false;
		if (!canCreateAllianceBetween(faction1, faction2))
			return false;
		faction1.allied.add(faction2.getUUID());
		faction2.allied.add(faction1.getUUID());
		return true;
	}

	public static boolean endAlliance(Faction faction1, Faction faction2) {
		if (faction1 == null || faction2 == null)
			return false;
		if (!faction1.isAlliedTo(faction2))
			return false;
		faction1.allied.remove(faction2.getUUID());
		faction2.allied.remove(faction1.getUUID());
		return true;
	}

	public static void startWarBetweenFactions(Faction... factions) {
		for (Faction faction : factions) {
			faction.startWarWith(factions);
		}
	}

	public static boolean startWar(Faction faction1, Faction faction2) {
		if (faction1 == null || faction2 == null)
			return false;
		if (!canStartWarBetween(faction1, faction2))
			return false;
		faction1.enemies.add(faction2.getUUID());
		faction2.enemies.add(faction1.getUUID());
		return true;
	}

	public static boolean endWar(Faction faction1, Faction faction2) {
		if (faction1 == null || faction2 == null)
			return false;
		if (!faction1.isAtWar(faction2))
			return false;
		faction1.enemies.remove(faction2.getUUID());
		faction2.enemies.remove(faction1.getUUID());
		return true;
	}
}