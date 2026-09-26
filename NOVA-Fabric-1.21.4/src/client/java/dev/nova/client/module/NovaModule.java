package dev.nova.client.module;

import net.minecraft.text.Text;

public interface NovaModule {
    String id();

    Text name();

    Text description();

    Category category();
}
