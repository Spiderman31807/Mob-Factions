package mobfactions;

import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.Registries;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandBuildContext;

import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

import java.nio.file.Path;
import java.nio.file.Files;

import java.io.IOException;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;

@EventBusSubscriber
public class FactionCommand {
	private static final SuggestionProvider<CommandSourceStack> factions = new FactionManager();
	private static final Map<String, String> translationFallback = new HashMap<String, String>() {{
          put("success_modify.end_alliance", "Ended alliance between [%s] faction \u0026 [%s] faction");
	      put("failure_clear", "No factions to remove");
	      put("success_modify.end_war", "Ended war between [%s] faction \u0026 [%s] faction");
	      put("success_create", "Created new faction [%s]");
	      put("success_modify.entity_remove", "Removed %s from [%s] faction");
	      put("failure_modify.start_alliance_allied", "[%s] faction already has an alliance with [%s] faction");
	      put("failure_modify.entity_hostile_clear", "[%s] faction has no hostiles to remove");
	      put("failure_modify.end_alliance", "[%s] faction does not have an alliance with [%s] faction");
	      put("failure_modify.start_alliance_conflict", "Unable to start alliance between [%s] faction \u0026 [%s] faction");
	      put("failure_access", "You do not meet the requirements to use this command");
	      put("failure_modify.entity_allies_add", "%s is already an ally for [%s] faction");
	      put("failure_modify.entity_hostile_remove", "%s is not a hostile to [%s] faction");
	      put("success_modify.entity_hostile_add", "Added %s as a hostile for [%s] faction");
	      put("success_modify.alliance_clear", "Ended all alliances for [%s] faction");
	      put("success_query.entity_type", "Entities:%s");
	      put("failure_modify.end_war", "[%s] faction is not enemies with [%s] faction");
	      put("failure_reset", "Factions are already the default values");
	      put("success_modify.start_alliance", "Started an alliance between [%s] faction \u0026 [%s] faction");
	      put("failure_modify.start_war_conflict", "Unable to start war between [%s] faction \u0026 [%s] faction");
	      put("success_save", "Saved current faction config to world file");
	      put("success_query.faction", "Factions:%s");
	      put("failure_modify.war_clear", "[%s] faction has no enemies to remove");
	      put("failure_list", "No factions to list");
	      put("success_modify.entity_hostile_clear", "Removed all hostiles in [%s] faction");
	      put("failure_modify.alliance_clear", "[%s] faction has no alliances to remove");
	      put("failure_modify.entity_allies_clear", "[%s] faction has no allies to remove");
	      put("success_reset", "Reset factions to default values");
	      put("failure_modify.start_war", "[%s] faction is already enemies with [%s] faction");
	      put("failure_modify.entity_clear", "[%s] faction has no members to remove");
	      put("success_modify.entity_allies_remove", "Removed %s as an ally for [%s] faction");
	      put("failure_modify.entity_hostile_add", "%s is already a hostile for [%s] faction");
	      put("failure_modify.entity_allies_remove", "%s is not an ally for [%s] faction");
	      put("failure_query.faction", "No factions to display");
	      put("failure_modify.entity_add", "%s is already a member of [%s] faction");
	      put("success_modify.entity_allies_clear", "Removed all allies in [%s] faction");
	      put("success_modify.entity_hostile_remove", "Removed %s as a hostile for [%s] faction");
	      put("success_clear", "Removed %d faction(s)");
	      put("success_modify.entity_add", "Added %s to [%s] faction");
	      put("success_modify.entity_allies_add", "Added %s as an ally for [%s] faction");
	      put("success_modify.war_clear", "Ended all wars for [%s] faction");
	      put("success_list", "Factions:%s");
	      put("success_remove", "Removed faction [%s]");
	      put("success_modify.start_war", "Started war between [%s] faction \u0026 [%s] faction");
	      put("success_modify.entity_clear", "Removed all members in [%s] faction");
	      put("failure_query.entity_type", "No entities to display");
	      put("failure_modify.entity_remove", "%s is not a member of [%s] faction");
	      put("failure_missing", "Unable to find Faction");
    }};

	public static Component getTranslatable(String input, Object... parameters) {
		return Component.translatableWithFallback("command.mob_faction" + input, translationFallback.getOrDefault(input, ""), parameters);
	}

	public static boolean canAccessCommand(ServerPlayer player) {
		MinecraftServer server = player.getServer();
		if (server.isSingleplayer())
			return true;
		if (player.getServer().getProfilePermissions(player.getGameProfile()) >= 2)
			return true;
		if (server.getLocalIp().equals(player.getIpAddress()))
			return true;
		return false;
	}

	@SubscribeEvent
	public static void register(RegisterCommandsEvent event) {
		CommandBuildContext context = event.getBuildContext();
		LiteralArgumentBuilder<CommandSourceStack> base = Commands.literal("faction");
		base.then(Commands.literal("list").executes(arguments -> {
			if (FactionManager.factions.isEmpty()) {
				arguments.getSource().sendFailure(getTranslatable("failure_list"));
				return 0;
			}
			arguments.getSource().sendSuccess(() -> getTranslatable("success_list", FactionManager.getFactionList()), false);
			return 15;
		}));
		base.then(Commands.literal("save").executes(arguments -> {
			if (arguments.getSource().isPlayer() && !canAccessCommand(arguments.getSource().getPlayer())) {
				arguments.getSource().sendFailure(getTranslatable("failure_access"));
				return 0;
			}
			CompoundTag compound = FactionManager.save();
			if (compound != null && !compound.isEmpty()) {
				try {
					Path server = arguments.getSource().getServer().getWorldPath(LevelResource.ROOT);
					Files.createDirectories(server);
					NbtIo.write(compound, server.resolve("mob-factions.dat"));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			arguments.getSource().sendSuccess(() -> getTranslatable("success_save"), true);
			return 15;
		}));
		base.then(Commands.literal("clear").executes(arguments -> {
			if (arguments.getSource().isPlayer() && !canAccessCommand(arguments.getSource().getPlayer())) {
				arguments.getSource().sendFailure(getTranslatable("failure_access"));
				return 0;
			}
			if (FactionManager.factions.isEmpty()) {
				arguments.getSource().sendFailure(getTranslatable("failure_clear"));
				return 0;
			}
			int count = FactionManager.factions.size();
			FactionManager.resetData();
			arguments.getSource().sendSuccess(() -> getTranslatable("success_clear", count), true);
			return 15;
		}));
		base.then(Commands.literal("reset").executes(arguments -> {
			if (arguments.getSource().isPlayer() && !canAccessCommand(arguments.getSource().getPlayer())) {
				arguments.getSource().sendFailure(getTranslatable("failure_access"));
				return 0;
			}
			if (DefaultFactionsBundle.isCurrentConfig()) {
				arguments.getSource().sendFailure(getTranslatable("failure_reset"));
				return 0;
			}
			FactionManager.resetData();
			DefaultFactionsBundle.getDefaultFactions().forEach((faction) -> FactionManager.addFaction(faction));
			arguments.getSource().sendSuccess(() -> getTranslatable("success_reset"), true);
			return 15;
		}));
		base.then(Commands.literal("add").then(Commands.argument("id", StringArgumentType.string()).executes(arguments -> {
			if (arguments.getSource().isPlayer() && !canAccessCommand(arguments.getSource().getPlayer())) {
				arguments.getSource().sendFailure(getTranslatable("failure_access"));
				return 0;
			}
			String id = StringArgumentType.getString(arguments, "id");
			FactionManager.addFaction(new Faction(id));
			arguments.getSource().sendSuccess(() -> getTranslatable("success_create", id), true);
			return 15;
		})));
		base.then(Commands.literal("remove").then(Commands.argument("faction", StringArgumentType.string()).suggests(factions).executes(arguments -> {
			if (arguments.getSource().isPlayer() && !canAccessCommand(arguments.getSource().getPlayer())) {
				arguments.getSource().sendFailure(getTranslatable("failure_access"));
				return 0;
			}
			Optional<Faction> faction = FactionManager.byId(StringArgumentType.getString(arguments, "faction"));
			if (faction.isEmpty()) {
				arguments.getSource().sendFailure(getTranslatable("failure_missing"));
				return 0;
			}
			FactionManager.removeFaction(faction.get());
			arguments.getSource().sendSuccess(() -> getTranslatable("success_remove", faction.get().id), true);
			return 15;
		})));
		base.then(Commands.literal("query").then(Commands.argument("faction", StringArgumentType.string()).suggests(factions).then(Commands.literal("entities").executes(arguments -> {
			Optional<Faction> faction = FactionManager.byId(StringArgumentType.getString(arguments, "faction"));
			if (faction.isEmpty()) {
				arguments.getSource().sendFailure(getTranslatable("failure_missing"));
				return 0;
			}
			if (faction.get().entities.isEmpty()) {
				arguments.getSource().sendFailure(getTranslatable("failure_query.entity_type"));
				return 0;
			}
			arguments.getSource().sendSuccess(() -> getTranslatable("success_query.entity_type", faction.get().getList(0)), false);
			return 15;
		})).then(Commands.literal("allies").executes(arguments -> {
			Optional<Faction> faction = FactionManager.byId(StringArgumentType.getString(arguments, "faction"));
			if (faction.isEmpty()) {
				arguments.getSource().sendFailure(getTranslatable("failure_missing"));
				return 0;
			}
			if (faction.get().alliedEntities.isEmpty()) {
				arguments.getSource().sendFailure(getTranslatable("failure_query.entity_type"));
				return 0;
			}
			arguments.getSource().sendSuccess(() -> getTranslatable("success_query.entity_type", faction.get().getList(1)), false);
			return 15;
		})).then(Commands.literal("hostiles").executes(arguments -> {
			Optional<Faction> faction = FactionManager.byId(StringArgumentType.getString(arguments, "faction"));
			if (faction.isEmpty()) {
				arguments.getSource().sendFailure(getTranslatable("failure_missing"));
				return 0;
			}
			if (faction.get().hostileEntities.isEmpty()) {
				arguments.getSource().sendFailure(getTranslatable("failure_query.entity_type"));
				return 0;
			}
			arguments.getSource().sendSuccess(() -> getTranslatable("success_query.entity_type", faction.get().getList(2)), false);
			return 15;
		})).then(Commands.literal("alliances").executes(arguments -> {
			Optional<Faction> faction = FactionManager.byId(StringArgumentType.getString(arguments, "faction"));
			if (faction.isEmpty()) {
				arguments.getSource().sendFailure(getTranslatable("failure_missing"));
				return 0;
			}
			if (faction.get().allied.isEmpty()) {
				arguments.getSource().sendFailure(getTranslatable("failure_query.faction"));
				return 0;
			}
			arguments.getSource().sendSuccess(() -> getTranslatable("success_query.faction", faction.get().getList(3)), false);
			return 15;
		})).then(Commands.literal("enemies").executes(arguments -> {
			Optional<Faction> faction = FactionManager.byId(StringArgumentType.getString(arguments, "faction"));
			if (faction.isEmpty()) {
				arguments.getSource().sendFailure(getTranslatable("failure_missing"));
				return 0;
			}
			if (faction.get().enemies.isEmpty()) {
				arguments.getSource().sendFailure(getTranslatable("failure_query.faction"));
				return 0;
			}
			arguments.getSource().sendSuccess(() -> getTranslatable("success_query.faction", faction.get().getList(4)), false);
			return 15;
		}))));
		base.then(Commands.literal("modify").then(Commands.argument("faction", StringArgumentType.string()).suggests(factions)
				.then(Commands.literal("entities").then(Commands.literal("add").then(Commands.argument("entity", ResourceArgument.resource(context, Registries.ENTITY_TYPE)).executes(arguments -> {
					if (arguments.getSource().isPlayer() && !canAccessCommand(arguments.getSource().getPlayer())) {
						arguments.getSource().sendFailure(getTranslatable("failure_access"));
						return 0;
					}
					Optional<Faction> faction = FactionManager.byId(StringArgumentType.getString(arguments, "faction"));
					if (faction.isEmpty()) {
						arguments.getSource().sendFailure(getTranslatable("failure_missing"));
						return 0;
					}
					EntityType type = ResourceArgument.getEntityType(arguments, "entity").value();
					if (faction.get().entities.contains(type)) {
						arguments.getSource().sendFailure(getTranslatable("failure_modify.entity_add", type.getDescription(), faction.get().id));
						return 0;
					}
					FactionManager.addFactionEntities(faction.get(), type);
					arguments.getSource().sendSuccess(() -> getTranslatable("success_modify.entity_add", type.getDescription(), faction.get().id), true);
					return 15;
				}))).then(Commands.literal("remove").then(Commands.argument("entity", ResourceArgument.resource(context, Registries.ENTITY_TYPE)).executes(arguments -> {
					if (arguments.getSource().isPlayer() && !canAccessCommand(arguments.getSource().getPlayer())) {
						arguments.getSource().sendFailure(getTranslatable("failure_access"));
						return 0;
					}
					Optional<Faction> faction = FactionManager.byId(StringArgumentType.getString(arguments, "faction"));
					if (faction.isEmpty()) {
						arguments.getSource().sendFailure(getTranslatable("failure_missing"));
						return 0;
					}
					EntityType type = ResourceArgument.getEntityType(arguments, "entity").value();
					if (!faction.get().entities.contains(type)) {
						arguments.getSource().sendFailure(getTranslatable("failure_modify.entity_remove", type.getDescription(), faction.get().id));
						return 0;
					}
					FactionManager.removeFactionEntities(faction.get(), type);
					arguments.getSource().sendSuccess(() -> getTranslatable("success_modify.entity_remove", type.getDescription(), faction.get().id), true);
					return 15;
				}))).then(Commands.literal("clear").executes(arguments -> {
					if (arguments.getSource().isPlayer() && !canAccessCommand(arguments.getSource().getPlayer())) {
						arguments.getSource().sendFailure(getTranslatable("failure_access"));
						return 0;
					}
					Optional<Faction> faction = FactionManager.byId(StringArgumentType.getString(arguments, "faction"));
					if (faction.isEmpty()) {
						arguments.getSource().sendFailure(getTranslatable("failure_missing"));
						return 0;
					}
					if (faction.get().entities.isEmpty()) {
						arguments.getSource().sendFailure(getTranslatable("failure_modify.entity_clear", faction.get().id));
						return 0;
					}
					FactionManager.removeFactionEntities(faction.get());
					faction.get().entities.clear();
					arguments.getSource().sendSuccess(() -> getTranslatable("success_modify.entity_clear", faction.get().id), true);
					return 15;
				})))));
		base.then(Commands.literal("modify").then(Commands.argument("faction", StringArgumentType.string()).suggests(factions)
				.then(Commands.literal("allies").then(Commands.literal("add").then(Commands.argument("entity", ResourceArgument.resource(context, Registries.ENTITY_TYPE)).executes(arguments -> {
					if (arguments.getSource().isPlayer() && !canAccessCommand(arguments.getSource().getPlayer())) {
						arguments.getSource().sendFailure(getTranslatable("failure_access"));
						return 0;
					}
					Optional<Faction> faction = FactionManager.byId(StringArgumentType.getString(arguments, "faction"));
					if (faction.isEmpty()) {
						arguments.getSource().sendFailure(getTranslatable("failure_missing"));
						return 0;
					}
					EntityType type = ResourceArgument.getEntityType(arguments, "entity").value();
					if (!faction.get().addAllyEntity(type)) {
						arguments.getSource().sendFailure(getTranslatable("failure_modify.entity_allies_add", type.getDescription(), faction.get().id));
						return 0;
					}
					arguments.getSource().sendSuccess(() -> getTranslatable("success_modify.entity_allies_add", type.getDescription(), faction.get().id), true);
					return 15;
				}))).then(Commands.literal("remove").then(Commands.argument("entity", ResourceArgument.resource(context, Registries.ENTITY_TYPE)).executes(arguments -> {
					if (arguments.getSource().isPlayer() && !canAccessCommand(arguments.getSource().getPlayer())) {
						arguments.getSource().sendFailure(getTranslatable("failure_access"));
						return 0;
					}
					Optional<Faction> faction = FactionManager.byId(StringArgumentType.getString(arguments, "faction"));
					if (faction.isEmpty()) {
						arguments.getSource().sendFailure(getTranslatable("failure_missing"));
						return 0;
					}
					EntityType type = ResourceArgument.getEntityType(arguments, "entity").value();
					if (!faction.get().removeAllyEntity(type)) {
						arguments.getSource().sendFailure(getTranslatable("failure_modify.entity_allies_remove", type.getDescription(), faction.get().id));
						return 0;
					}
					FactionManager.removeFactionEntities(faction.get(), type);
					arguments.getSource().sendSuccess(() -> getTranslatable("success_modify.entity_allies_remove", type.getDescription(), faction.get().id), true);
					return 15;
				}))).then(Commands.literal("clear").executes(arguments -> {
					if (arguments.getSource().isPlayer() && !canAccessCommand(arguments.getSource().getPlayer())) {
						arguments.getSource().sendFailure(getTranslatable("failure_access"));
						return 0;
					}
					Optional<Faction> faction = FactionManager.byId(StringArgumentType.getString(arguments, "faction"));
					if (faction.isEmpty()) {
						arguments.getSource().sendFailure(getTranslatable("failure_missing"));
						return 0;
					}
					if (faction.get().alliedEntities.isEmpty()) {
						arguments.getSource().sendFailure(getTranslatable("failure_modify.entity_allies_clear", faction.get().id));
						return 0;
					}
					faction.get().alliedEntities.clear();
					arguments.getSource().sendSuccess(() -> getTranslatable("success_modify.entity_allies_clear", faction.get().id), true);
					return 15;
				})))));
		base.then(Commands.literal("modify").then(Commands.argument("faction", StringArgumentType.string()).suggests(factions)
				.then(Commands.literal("hostiles").then(Commands.literal("add").then(Commands.argument("entity", ResourceArgument.resource(context, Registries.ENTITY_TYPE)).executes(arguments -> {
					if (arguments.getSource().isPlayer() && !canAccessCommand(arguments.getSource().getPlayer())) {
						arguments.getSource().sendFailure(getTranslatable("failure_access"));
						return 0;
					}
					Optional<Faction> faction = FactionManager.byId(StringArgumentType.getString(arguments, "faction"));
					if (faction.isEmpty()) {
						arguments.getSource().sendFailure(getTranslatable("failure_missing"));
						return 0;
					}
					EntityType type = ResourceArgument.getEntityType(arguments, "entity").value();
					if (!faction.get().addHostileEntity(type)) {
						arguments.getSource().sendFailure(getTranslatable("failure_modify.entity_hostile_add", type.getDescription(), faction.get().id));
						return 0;
					}
					arguments.getSource().sendSuccess(() -> getTranslatable("success_modify.entity_hostile_add", type.getDescription(), faction.get().id), true);
					return 15;
				}))).then(Commands.literal("remove").then(Commands.argument("entity", ResourceArgument.resource(context, Registries.ENTITY_TYPE)).executes(arguments -> {
					if (arguments.getSource().isPlayer() && !canAccessCommand(arguments.getSource().getPlayer())) {
						arguments.getSource().sendFailure(getTranslatable("failure_access"));
						return 0;
					}
					Optional<Faction> faction = FactionManager.byId(StringArgumentType.getString(arguments, "faction"));
					if (faction.isEmpty()) {
						arguments.getSource().sendFailure(getTranslatable("failure_missing"));
						return 0;
					}
					EntityType type = ResourceArgument.getEntityType(arguments, "entity").value();
					if (!faction.get().removeHostileEntity(type)) {
						arguments.getSource().sendFailure(getTranslatable("failure_modify.entity_hostile_remove", type.getDescription(), faction.get().id));
						return 0;
					}
					FactionManager.removeFactionEntities(faction.get(), type);
					arguments.getSource().sendSuccess(() -> getTranslatable("success_modify.entity_hostile_remove", type.getDescription(), faction.get().id), true);
					return 15;
				}))).then(Commands.literal("clear").executes(arguments -> {
					if (arguments.getSource().isPlayer() && !canAccessCommand(arguments.getSource().getPlayer())) {
						arguments.getSource().sendFailure(getTranslatable("failure_access"));
						return 0;
					}
					Optional<Faction> faction = FactionManager.byId(StringArgumentType.getString(arguments, "faction"));
					if (faction.isEmpty()) {
						arguments.getSource().sendFailure(getTranslatable("failure_missing"));
						return 0;
					}
					if (faction.get().hostileEntities.isEmpty()) {
						arguments.getSource().sendFailure(getTranslatable("failure_modify.entity_hostile_clear", faction.get().id));
						return 0;
					}
					faction.get().hostileEntities.clear();
					arguments.getSource().sendSuccess(() -> getTranslatable("success_modify.entity_hostile_clear", faction.get().id), true);
					return 15;
				})))));
		base.then(Commands.literal("modify").then(Commands.argument("faction", StringArgumentType.string()).suggests(factions)
				.then(Commands.literal("alliances").then(Commands.literal("start").then(Commands.argument("ally", StringArgumentType.string()).suggests(factions).executes(arguments -> {
					if (arguments.getSource().isPlayer() && !canAccessCommand(arguments.getSource().getPlayer())) {
						arguments.getSource().sendFailure(getTranslatable("failure_access"));
						return 0;
					}
					Optional<Faction> faction = FactionManager.byId(StringArgumentType.getString(arguments, "faction"));
					Optional<Faction> allyFaction = FactionManager.byId(StringArgumentType.getString(arguments, "ally"));
					if (faction.isEmpty() || allyFaction.isEmpty()) {
						arguments.getSource().sendFailure(getTranslatable("failure_missing"));
						return 0;
					}
					if (faction.get().isAlliedTo(allyFaction.get())) {
						arguments.getSource().sendFailure(getTranslatable("failure_modify.start_alliance_allied", faction.get().id, allyFaction.get().id));
						return 0;
					}
					if (!FactionManager.createAlliance(faction.get(), allyFaction.get())) {
						arguments.getSource().sendFailure(getTranslatable("failure_modify.start_alliance_conflict", faction.get().id, allyFaction.get().id));
						return 0;
					}
					arguments.getSource().sendSuccess(() -> getTranslatable("success_modify.start_alliance", faction.get().id, allyFaction.get().id), true);
					return 15;
				}))).then(Commands.literal("end").then(Commands.argument("ally", StringArgumentType.string()).suggests(factions).executes(arguments -> {
					if (arguments.getSource().isPlayer() && !canAccessCommand(arguments.getSource().getPlayer())) {
						arguments.getSource().sendFailure(getTranslatable("failure_access"));
						return 0;
					}
					Optional<Faction> faction = FactionManager.byId(StringArgumentType.getString(arguments, "faction"));
					Optional<Faction> allyFaction = FactionManager.byId(StringArgumentType.getString(arguments, "ally"));
					if (faction.isEmpty() || allyFaction.isEmpty()) {
						arguments.getSource().sendFailure(getTranslatable("failure_missing"));
						return 0;
					}
					if (!FactionManager.endAlliance(faction.get(), allyFaction.get())) {
						arguments.getSource().sendFailure(getTranslatable("failure_modify.end_alliance", faction.get().id, allyFaction.get().id));
						return 0;
					}
					arguments.getSource().sendSuccess(() -> getTranslatable("success_modify.end_alliance", faction.get().id, allyFaction.get().id), true);
					return 15;
				}))).then(Commands.literal("clear").executes(arguments -> {
					if (arguments.getSource().isPlayer() && !canAccessCommand(arguments.getSource().getPlayer())) {
						arguments.getSource().sendFailure(getTranslatable("failure_access"));
						return 0;
					}
					Optional<Faction> faction = FactionManager.byId(StringArgumentType.getString(arguments, "faction"));
					if (faction.isEmpty()) {
						arguments.getSource().sendFailure(getTranslatable("failure_missing"));
						return 0;
					}
					if (faction.get().allied.isEmpty()) {
						arguments.getSource().sendFailure(getTranslatable("failure_modify.alliance_clear", faction.get().id));
						return 0;
					}
					faction.get().allied.forEach((factionUUID) -> FactionManager.endAlliance(faction.get(), FactionManager.getFaction(factionUUID).orElse(null)));
					arguments.getSource().sendSuccess(() -> getTranslatable("success_modify.alliance_clear", faction.get().id), true);
					return 15;
				})))));
		base.then(Commands.literal("modify").then(Commands.argument("faction", StringArgumentType.string()).suggests(factions)
				.then(Commands.literal("enemies").then(Commands.literal("add").then(Commands.argument("enemy", StringArgumentType.string()).suggests(factions).executes(arguments -> {
					if (arguments.getSource().isPlayer() && !canAccessCommand(arguments.getSource().getPlayer())) {
						arguments.getSource().sendFailure(getTranslatable("failure_access"));
						return 0;
					}
					Optional<Faction> faction = FactionManager.byId(StringArgumentType.getString(arguments, "faction"));
					Optional<Faction> enemyFaction = FactionManager.byId(StringArgumentType.getString(arguments, "enemy"));
					if (faction.isEmpty() || enemyFaction.isEmpty()) {
						arguments.getSource().sendFailure(getTranslatable("failure_missing"));
						return 0;
					}
					if (faction.get().isAtWar(enemyFaction.get())) {
						arguments.getSource().sendFailure(getTranslatable("failure_modify.start_war", faction.get().id, enemyFaction.get().id));
						return 0;
					}
					if (!FactionManager.startWar(faction.get(), enemyFaction.get())) {
						arguments.getSource().sendFailure(getTranslatable("failure_modify.start_war_conflict", faction.get().id, enemyFaction.get().id));
						return 0;
					}
					arguments.getSource().sendSuccess(() -> getTranslatable("success_modify.start_war", faction.get().id, enemyFaction.get().id), true);
					return 15;
				}))).then(Commands.literal("remove").then(Commands.argument("enemy", StringArgumentType.string()).suggests(factions).executes(arguments -> {
					if (arguments.getSource().isPlayer() && !canAccessCommand(arguments.getSource().getPlayer())) {
						arguments.getSource().sendFailure(getTranslatable("failure_access"));
						return 0;
					}
					Optional<Faction> faction = FactionManager.byId(StringArgumentType.getString(arguments, "faction"));
					Optional<Faction> enemyFaction = FactionManager.byId(StringArgumentType.getString(arguments, "enemy"));
					if (faction.isEmpty() || enemyFaction.isEmpty()) {
						arguments.getSource().sendFailure(getTranslatable("failure_missing"));
						return 0;
					}
					if (!FactionManager.endWar(faction.get(), enemyFaction.get())) {
						arguments.getSource().sendFailure(getTranslatable("failure_modify.end_war", faction.get().id, enemyFaction.get().id));
						return 0;
					}
					arguments.getSource().sendSuccess(() -> getTranslatable("success_modify.end_war", faction.get().id, enemyFaction.get().id), true);
					return 15;
				}))).then(Commands.literal("clear").executes(arguments -> {
					if (arguments.getSource().isPlayer() && !canAccessCommand(arguments.getSource().getPlayer())) {
						arguments.getSource().sendFailure(getTranslatable("failure_access"));
						return 0;
					}
					Optional<Faction> faction = FactionManager.byId(StringArgumentType.getString(arguments, "faction"));
					if (faction.isEmpty()) {
						arguments.getSource().sendFailure(getTranslatable("failure_missing"));
						return 0;
					}
					if (faction.get().enemies.isEmpty()) {
						arguments.getSource().sendFailure(getTranslatable("failure_modify.war_clear", faction.get().id));
						return 0;
					}
					faction.get().enemies.forEach((factionUUID) -> FactionManager.endWar(faction.get(), FactionManager.getFaction(factionUUID).orElse(null)));
					arguments.getSource().sendSuccess(() -> getTranslatable("success_modify.war_clear", faction.get().id), true);
					return 15;
				})))));
		event.getDispatcher().register(base);
	}
}