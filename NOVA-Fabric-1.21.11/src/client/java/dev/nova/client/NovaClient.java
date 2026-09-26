package dev.nova.client;

import dev.nova.client.config.ConfigManager;
import dev.nova.client.module.BuiltinModules;
import net.fabricmc.api.ClientModInitializer;

public final class NovaClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        BuiltinModules.register();
        NovaCommands.register();
        NovaKeybinds.register();
    }
}
