package dev.nova.client;

import dev.nova.client.ui.NovaScreen;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;

public final class NovaCommands {
    private NovaCommands() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("123").executes(context -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    // Deferred: the chat screen closes itself right after running the command.
                    client.send(() -> client.setScreen(new NovaScreen()));
                    return 1;
                }))
        );
    }
}
