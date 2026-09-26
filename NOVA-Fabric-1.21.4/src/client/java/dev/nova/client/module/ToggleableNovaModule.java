package dev.nova.client.module;

public interface ToggleableNovaModule extends NovaModule {
    boolean isEnabled();

    void setEnabled(boolean enabled);

    default void toggle() {
        setEnabled(!isEnabled());
    }
}
