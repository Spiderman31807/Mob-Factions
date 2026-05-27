package mobfactions;

import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.CompoundTag;

import java.nio.file.Path;
import java.nio.file.Files;

import java.io.IOException;

@EventBusSubscriber
public class Events {
	@SubscribeEvent
	public static void targeting(EntityEvent.EntityConstructing event) {
		if (event.getEntity() instanceof Mob mob) {
			EntityType<? extends Mob> type = (EntityType<? extends Mob>) mob.getType();
			if (!DefaultAttributes.hasSupplier(type))
				return;
			if (!DefaultAttributes.getSupplier(type).hasAttribute(Attributes.ATTACK_DAMAGE))
				return;
			MobFactionsMod.queueServerWork(1, () -> addGoals(mob));
		}
	}

	public static void addGoals(Mob mob) {
		int priority = 1;
		for (WrappedGoal wrapped : mob.targetSelector.getAvailableGoals()) {
			priority = Math.max(wrapped.getPriority() + 1, priority);
		}
		TargetingConditions.Selector filter = (entity, server) -> mob instanceof FactionHolder holder && holder.isFactionEnemy(entity);
		mob.targetSelector.addGoal(priority, new NearestAttackableTargetGoal(mob, LivingEntity.class, 10, true, true, filter));
	}

	@SubscribeEvent
	public static void targeting(LivingChangeTargetEvent event) {
		if (event.getEntity().isAlliedTo(event.getNewAboutToBeSetTarget()))
			event.setCanceled(true);
	}

	@SubscribeEvent
	public static void stopServer(ServerStoppingEvent event) {
		CompoundTag compound = FactionManager.save();
		if (compound == null || compound.isEmpty())
			return;
		try {
			Path server = event.getServer().getWorldPath(LevelResource.ROOT);
			Files.createDirectories(server);
			NbtIo.write(compound, server.resolve("mob-factions.dat"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SubscribeEvent
	public static void startServer(ServerStartingEvent event) {
		FactionManager.resetData();
		try {
			Path server = event.getServer().getWorldPath(LevelResource.ROOT);
			Path factionDataPath = server.resolve("mob-factions.dat");
			if (factionDataPath.toFile().exists()) {
				FactionManager.load(NbtIo.read(factionDataPath));
			} else {
				DefaultFactionsBundle.getDefaultFactions().forEach((faction) -> FactionManager.addFaction(faction));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
