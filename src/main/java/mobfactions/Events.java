package mobfactions;

import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.CompoundTag;

import java.nio.file.Path;
import java.nio.file.Files;

import java.io.IOException;

public class Events {
	public static void stopServer(MinecraftServer server) {
		CompoundTag compound = FactionManager.save();
		if (compound == null || compound.isEmpty())
			return;
		try {
			Path root = server.getWorldPath(LevelResource.ROOT);
			Files.createDirectories(root);
			NbtIo.write(compound, root.resolve("mob-factions.dat"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void startServer(MinecraftServer server) {
		FactionManager.resetData();
		try {
			Path root = server.getWorldPath(LevelResource.ROOT);
			Path factionDataPath = root.resolve("mob-factions.dat");
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