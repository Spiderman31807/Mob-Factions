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

import java.nio.file.Path;
import java.nio.file.Files;

import java.io.IOException;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

@EventBusSubscriber
public class FactionCommand {
	public static final SuggestionProvider<String> factions = new FactionManager();

	public static boolean canAccessCommand(ServerPlayer player) {
		MinecraftServer server = player.getServer();
		if (server.isSingleplayer())
			return true;
		if (player.getPermissionLevel() >= 2)
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
				arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_list"));
				return 0;
			}
			arguments.getSource().sendSuccess(() -> Component.translatable("command.mob_faction.success_list", FactionManager.getFactionList()), false);
			return 15;
		}));
		
		base.requires(source -> source.hasPermission(2)).then(Commands.literal("save").executes(arguments -> {
			if (arguments.getSource().isPlayer() && !canAccessCommand(arguments.getSource().getPlayer())) {
				arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_access"));
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
			arguments.getSource().sendSuccess(() -> Component.translatable("command.mob_faction.success_save"), true);
			return 15;
		})));
		
		base.requires(source -> source.hasPermission(2)).then(Commands.literal("clear").executes(arguments -> {
			if (arguments.getSource().isPlayer() && !canAccessCommand(arguments.getSource().getPlayer())) {
				arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_access"));
				return 0;
			}
			if (FactionManager.factions.isEmpty()) {
				arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_clear"));
				return 0;
			}
			int count = FactionManager.factions.size();
			FactionManager.resetData();
			arguments.getSource().sendSuccess(() -> Component.translatable("command.mob_faction.success_clear", count), true);
			return 15;
		})));
		
		base.requires(source -> source.hasPermission(2)).then(Commands.literal("reset").executes(arguments -> {
			if (arguments.getSource().isPlayer() && !canAccessCommand(arguments.getSource().getPlayer())) {
				arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_access"));
				return 0;
			}
			if (DefaultFactionsBundle.isCurrentConfig()) {
				arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_reset"));
				return 0;
			}
			FactionManager.resetData();
			DefaultFactionsBundle.getDefaultFactions().forEach((faction) -> FactionManager.addFaction(faction));
			arguments.getSource().sendSuccess(() -> Component.translatable("command.mob_faction.success_reset"), true);
			return 15;
		})));
		
		base.requires(source -> source.hasPermission(2)).then(Commands.literal("add").then(Commands.argument("id", StringArgumentType.string()).executes(arguments -> {
			if (arguments.getSource().isPlayer() && !canAccessCommand(arguments.getSource().getPlayer())) {
				arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_access"));
				return 0;
			}
			String id = StringArgumentType.getString(arguments, "id");
			FactionManager.addFaction(new Faction(id));
			arguments.getSource().sendSuccess(() -> Component.translatable("command.mob_faction.success_create", id), true);
			return 15;
		}))));
		
		base.requires(source -> source.hasPermission(2)).then(Commands.literal("remove").then(Commands.argument("faction", StringArgumentType.string()).suggests(factions)).executes(arguments -> {
			if (arguments.getSource().isPlayer() && !canAccessCommand(arguments.getSource().getPlayer())) {
				arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_access"));
				return 0;
			}
			Faction faction = FactionArgument.getFaction(arguments, "faction");
			FactionManager.removeFaction(faction);
			arguments.getSource().sendSuccess(() -> Component.translatable("command.mob_faction.success_remove", faction.id), true);
			return 15;
		}))));
		
		base.then(Commands.literal("query").then(Commands.argument("faction", StringArgumentType.string()).suggests(factions)).then(Commands.literal("entities").executes(arguments -> {
			Faction faction = FactionArgument.getFaction(arguments, "faction");
			if (faction.entities.isEmpty()) {
				arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_query.entity_type"));
				return 0;
			}
			arguments.getSource().sendSuccess(() -> Component.translatable("command.mob_faction.success_query.entity_type", faction.getList(0)), false);
			return 15;
		})).then(Commands.literal("allies").executes(arguments -> {
			Faction faction = FactionArgument.getFaction(arguments, "faction");
			if (faction.alliedEntities.isEmpty()) {
				arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_query.entity_type"));
				return 0;
			}
			arguments.getSource().sendSuccess(() -> Component.translatable("command.mob_faction.success_query.entity_type", faction.getList(1)), false);
			return 15;
		})).then(Commands.literal("hostiles").executes(arguments -> {
			Faction faction = FactionArgument.getFaction(arguments, "faction");
			if (faction.hostileEntities.isEmpty()) {
				arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_query.entity_type"));
				return 0;
			}
			arguments.getSource().sendSuccess(() -> Component.translatable("command.mob_faction.success_query.entity_type", faction.getList(2)), false);
			return 15;
		})).then(Commands.literal("alliances").executes(arguments -> {
			Faction faction = FactionArgument.getFaction(arguments, "faction");
			if (faction.allied.isEmpty()) {
				arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_query.faction"));
				return 0;
			}
			arguments.getSource().sendSuccess(() -> Component.translatable("command.mob_faction.success_query.faction", faction.getList(3)), false);
			return 15;
		})).then(Commands.literal("enemies").executes(arguments -> {
			Faction faction = FactionArgument.getFaction(arguments, "faction");
			if (faction.enemies.isEmpty()) {
				arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_query.faction"));
				return 0;
			}
			arguments.getSource().sendSuccess(() -> Component.translatable("command.mob_faction.success_query.faction", faction.getList(4)), false);
			return 15;
		}))));
		
		base.requires(source -> source.hasPermission(2)).then(Commands.literal("modify").then(Commands.argument("faction", StringArgumentType.string()).suggests(factions))
			.then(Commands.literal("entities").then(Commands.literal("add").then(Commands.argument("entity", ResourceArgument.resource(context, Registries.ENTITY_TYPE)).executes(arguments -> {
				if (arguments.getSource().isPlayer() && !canAccessCommand(arguments.getSource().getPlayer())) {
					arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_access"));
					return 0;
				}
				Faction faction = FactionArgument.getFaction(arguments, "faction");
				EntityType type = ResourceArgument.getEntityType(arguments, "entity").value();
				if (faction.entities.contains(type)) {
					arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_modify.entity_add", type.getDescription(), faction.id));
					return 0;
				}
				FactionManager.addFactionEntities(faction, type);
				arguments.getSource().sendSuccess(() -> Component.translatable("command.mob_faction.success_modify.entity_add", type.getDescription(), faction.id), true);
				return 15;
			}))).then(Commands.literal("remove").then(Commands.argument("entity", ResourceArgument.resource(context, Registries.ENTITY_TYPE)).executes(arguments -> {
				if (arguments.getSource().isPlayer() && !canAccessCommand(arguments.getSource().getPlayer())) {
					arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_access"));
					return 0;
				}
				Faction faction = FactionArgument.getFaction(arguments, "faction");
				EntityType type = ResourceArgument.getEntityType(arguments, "entity").value();
				if (!faction.entities.contains(type)) {
					arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_modify.entity_remove", type.getDescription(), faction.id));
					return 0;
				}
				FactionManager.removeFactionEntities(faction, type);
				arguments.getSource().sendSuccess(() -> Component.translatable("command.mob_faction.success_modify.entity_remove", type.getDescription(), faction.id), true);
				return 15;
			}))).then(Commands.literal("clear").executes(arguments -> {
				if (arguments.getSource().isPlayer() && !canAccessCommand(arguments.getSource().getPlayer())) {
					arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_access"));
					return 0;
				}
				Faction faction = FactionArgument.getFaction(arguments, "faction");
				if (faction.entities.isEmpty()) {
					arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_modify.entity_clear", faction.id));
					return 0;
				}
				FactionManager.removeFactionEntities(faction);
				faction.entities.clear();
				arguments.getSource().sendSuccess(() -> Component.translatable("command.mob_faction.success_modify.entity_clear", faction.id), true);
				return 15;
			}))))));
				
		base.requires(source -> source.hasPermission(2)).then(Commands.literal("modify").then(
			Commands.argument("faction", StringArgumentType.string()).suggests(factions)).then(Commands.literal("allies").then(Commands.literal("add").then(Commands.argument("entity", ResourceArgument.resource(context, Registries.ENTITY_TYPE)).executes(arguments -> {
				if (arguments.getSource().isPlayer() && !canAccessCommand(arguments.getSource().getPlayer())) {
					arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_access"));
					return 0;
				}
				Faction faction = FactionArgument.getFaction(arguments, "faction");
				EntityType type = ResourceArgument.getEntityType(arguments, "entity").value();
				if (!faction.addAllyEntity(type)) {
					arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_modify.entity_allies_add", type.getDescription(), faction.id));
					return 0;
				}
				arguments.getSource().sendSuccess(() -> Component.translatable("command.mob_faction.success_modify.entity_allies_add", type.getDescription(), faction.id), true);
				return 15;
			}))).then(Commands.literal("remove").then(Commands.argument("entity", ResourceArgument.resource(context, Registries.ENTITY_TYPE)).executes(arguments -> {
				if (arguments.getSource().isPlayer() && !canAccessCommand(arguments.getSource().getPlayer())) {
					arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_access"));
					return 0;
				}
				Faction faction = FactionArgument.getFaction(arguments, "faction");
				EntityType type = ResourceArgument.getEntityType(arguments, "entity").value();
				if (!faction.removeAllyEntity(type)) {
					arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_modify.entity_allies_remove", type.getDescription(), faction.id));
					return 0;
				}
				FactionManager.removeFactionEntities(faction, type);
				arguments.getSource().sendSuccess(() -> Component.translatable("command.mob_faction.success_modify.entity_allies_remove", type.getDescription(), faction.id), true);
				return 15;
			}))).then(Commands.literal("clear").executes(arguments -> {
				if (arguments.getSource().isPlayer() && !canAccessCommand(arguments.getSource().getPlayer())) {
					arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_access"));
					return 0;
				}
				Faction faction = FactionArgument.getFaction(arguments, "faction");
				if (faction.alliedEntities.isEmpty()) {
					arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_modify.entity_allies_clear", faction.id));
					return 0;
				}
				faction.alliedEntities.clear();
				arguments.getSource().sendSuccess(() -> Component.translatable("command.mob_faction.success_modify.entity_allies_clear", faction.id), true);
				return 15;
			}))))));
			
		base.requires(source -> source.hasPermission(2)).then(Commands.literal("modify").then(Commands.argument("faction", StringArgumentType.string()).suggests(factions))
			.then(Commands.literal("hostiles").then(Commands.literal("add").then(Commands.argument("entity", ResourceArgument.resource(context, Registries.ENTITY_TYPE)).executes(arguments -> {
				if (arguments.getSource().isPlayer() && !canAccessCommand(arguments.getSource().getPlayer())) {
					arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_access"));
					return 0;
				}
				Faction faction = FactionArgument.getFaction(arguments, "faction");
				EntityType type = ResourceArgument.getEntityType(arguments, "entity").value();
				if (!faction.addHostileEntity(type)) {
					arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_modify.entity_hostile_add", type.getDescription(), faction.id));
					return 0;
				}
				arguments.getSource().sendSuccess(() -> Component.translatable("command.mob_faction.success_modify.entity_hostile_add", type.getDescription(), faction.id), true);
				return 15;
			}))).then(Commands.literal("remove").then(Commands.argument("entity", ResourceArgument.resource(context, Registries.ENTITY_TYPE)).executes(arguments -> {
				if (arguments.getSource().isPlayer() && !canAccessCommand(arguments.getSource().getPlayer())) {
					arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_access"));
					return 0;
				}
				Faction faction = FactionArgument.getFaction(arguments, "faction");
				EntityType type = ResourceArgument.getEntityType(arguments, "entity").value();
				if (!faction.removeHostileEntity(type)) {
					arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_modify.entity_hostile_remove", type.getDescription(), faction.id));
					return 0;
				}
				FactionManager.removeFactionEntities(faction, type);
				arguments.getSource().sendSuccess(() -> Component.translatable("command.mob_faction.success_modify.entity_hostile_remove", type.getDescription(), faction.id), true);
				return 15;
			}))).then(Commands.literal("clear").executes(arguments -> {
				if (arguments.getSource().isPlayer() && !canAccessCommand(arguments.getSource().getPlayer())) {
					arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_access"));
					return 0;
				}
				Faction faction = FactionArgument.getFaction(arguments, "faction");
				if (faction.hostileEntities.isEmpty()) {
					arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_modify.entity_hostile_clear", faction.id));
					return 0;
				}
				faction.hostileEntities.clear();
				arguments.getSource().sendSuccess(() -> Component.translatable("command.mob_faction.success_modify.entity_hostile_clear", faction.id), true);
				return 15;
			}))))));
			
		base.requires(source -> source.hasPermission(2)).then(Commands.literal("modify")
			.then(Commands.argument("faction", StringArgumentType.string()).suggests(factions)).then(Commands.literal("alliances").then(Commands.literal("start").then(Commands.argument("ally", StringArgumentType.string()).suggests(factions)).executes(arguments -> {
				if (arguments.getSource().isPlayer() && !canAccessCommand(arguments.getSource().getPlayer())) {
					arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_access"));
					return 0;
				}
				Faction faction = FactionArgument.getFaction(arguments, "faction");
				Faction allyFaction = FactionArgument.getFaction(arguments, "ally");
				if (faction.isAlliedTo(allyFaction)) {
					arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_modify.start_alliance_allied", faction.id, allyFaction.id));
					return 0;
				}
				if (!FactionManager.createAlliance(faction, allyFaction)) {
					arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_modify.start_alliance_conflict", faction.id, allyFaction.id));
					return 0;
				}
				arguments.getSource().sendSuccess(() -> Component.translatable("command.mob_faction.success_modify.start_alliance", faction.id, allyFaction.id), true);
				return 15;
			}))).then(Commands.literal("end").then(Commands.argument("ally", StringArgumentType.string()).suggests(factions)).executes(arguments -> {
				if (arguments.getSource().isPlayer() && !canAccessCommand(arguments.getSource().getPlayer())) {
					arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_access"));
					return 0;
				}
				Faction faction = FactionArgument.getFaction(arguments, "faction");
				Faction allyFaction = FactionArgument.getFaction(arguments, "ally");
				if (!FactionManager.endAlliance(faction, allyFaction)) {
					arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_modify.end_alliance", faction.id, allyFaction.id));
					return 0;
				}
				arguments.getSource().sendSuccess(() -> Component.translatable("command.mob_faction.success_modify.end_alliance", faction.id, allyFaction.id), true);
				return 15;
			}))).then(Commands.literal("clear").executes(arguments -> {
				if (arguments.getSource().isPlayer() && !canAccessCommand(arguments.getSource().getPlayer())) {
					arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_access"));
					return 0;
				}
				Faction faction = FactionArgument.getFaction(arguments, "faction");
				if (faction.allied.isEmpty()) {
					arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_modify.alliance_clear", faction.id));
					return 0;
				}
				faction.allied.forEach((factionUUID) -> FactionManager.endAlliance(faction, FactionManager.getFaction(factionUUID).orElse(null)));
				arguments.getSource().sendSuccess(() -> Component.translatable("command.mob_faction.success_modify.alliance_clear", faction.id), true);
				return 15;
			}))))));
			
		base.requires(source -> source.hasPermission(2)).then(Commands.literal("modify").then(Commands.argument("faction", StringArgumentType.string()).suggests(factions)).then(Commands.literal("enemies").then(Commands.literal("add").then(Commands.argument("ally", StringArgumentType.string()).suggests(factions)).executes(arguments -> {
			if (arguments.getSource().isPlayer() && !canAccessCommand(arguments.getSource().getPlayer())) {
				arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_access"));
				return 0;
			}
			Faction faction = FactionArgument.getFaction(arguments, "faction");
			Faction enemyFaction = FactionArgument.getFaction(arguments, "ally");
			if (faction.isAtWar(enemyFaction)) {
				arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_modify.start_war", faction.id, enemyFaction.id));
				return 0;
			}
			if (!FactionManager.startWar(faction, enemyFaction)) {
				arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_modify.start_war_conflict", faction.id, enemyFaction.id));
				return 0;
			}
			arguments.getSource().sendSuccess(() -> Component.translatable("command.mob_faction.success_modify.start_war", faction.id, enemyFaction.id), true);
			return 15;
		}))).then(Commands.literal("remove").then(Commands.argument("ally", StringArgumentType.string()).suggests(factions)).executes(arguments -> {
			if (arguments.getSource().isPlayer() && !canAccessCommand(arguments.getSource().getPlayer())) {
				arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_access"));
				return 0;
			}
			Faction faction = FactionArgument.getFaction(arguments, "faction");
			Faction enemyFaction = FactionArgument.getFaction(arguments, "ally");
			if (!FactionManager.endWar(faction, enemyFaction)) {
				arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_modify.end_war", faction.id, enemyFaction.id));
				return 0;
			}
			arguments.getSource().sendSuccess(() -> Component.translatable("command.mob_faction.success_modify.end_war", faction.id, enemyFaction.id), true);
			return 15;
		}))).then(Commands.literal("clear").executes(arguments -> {
			if (arguments.getSource().isPlayer() && !canAccessCommand(arguments.getSource().getPlayer())) {
				arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_access"));
				return 0;
			}
			Faction faction = FactionArgument.getFaction(arguments, "faction");
			if (faction.enemies.isEmpty()) {
				arguments.getSource().sendFailure(Component.translatable("command.mob_faction.failure_modify.war_clear", faction.id));
				return 0;
			}
			faction.enemies.forEach((factionUUID) -> FactionManager.endWar(faction, FactionManager.getFaction(factionUUID).orElse(null)));
			arguments.getSource().sendSuccess(() -> Component.translatable("command.mob_faction.success_modify.war_clear", faction.id), true);
			return 15;
		}))))));
		
		event.getDispatcher().register(base);
	}
}
