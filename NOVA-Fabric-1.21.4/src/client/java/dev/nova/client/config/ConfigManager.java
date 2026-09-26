package dev.nova.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("nova_client.json");

    private static NovaConfig config = new NovaConfig();

    private ConfigManager() {
    }

    public static NovaConfig get() {
        return config;
    }

    public static void load() {
        if (!Files.isRegularFile(CONFIG_PATH)) {
            config = new NovaConfig();
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
            NovaConfig loaded = GSON.fromJson(reader, NovaConfig.class);
            if (loaded == null) {
                throw new JsonParseException("Configuration root is null");
            }
            loaded.sanitize();
            config = loaded;
        } catch (IOException | RuntimeException exception) {
            backupBrokenConfig();
            config = new NovaConfig();
            save();
        }
    }

    public static void resetToDefaults() {
        config = new NovaConfig();
        save();
    }

    public static void save() {
        config.sanitize();
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Path temp = CONFIG_PATH.resolveSibling(CONFIG_PATH.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
            try {
                Files.move(temp, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveFailed) {
                Files.move(temp, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) {
            // Keep the in-memory configuration active if writing to disk fails.
        }
    }

    private static void backupBrokenConfig() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                Path backup = CONFIG_PATH.resolveSibling("nova_client.broken.json");
                Files.move(CONFIG_PATH, backup, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) {
            // Defaults are still usable even if the unreadable file cannot be backed up.
        }
    }
}
