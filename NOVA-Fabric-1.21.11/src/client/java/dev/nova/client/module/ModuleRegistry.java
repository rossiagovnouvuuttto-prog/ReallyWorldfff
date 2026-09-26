package dev.nova.client.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class ModuleRegistry {
    private static final List<NovaModule> MODULES = new ArrayList<>();

    private ModuleRegistry() {
    }

    public static void register(NovaModule module) {
        Objects.requireNonNull(module, "module");
        if (MODULES.stream().anyMatch(existing -> existing.id().equalsIgnoreCase(module.id()))) {
            throw new IllegalArgumentException("Duplicate NOVA module id: " + module.id());
        }
        MODULES.add(module);
    }

    public static List<NovaModule> all() {
        return Collections.unmodifiableList(MODULES);
    }

    public static NovaModule byId(String id) {
        return MODULES.stream()
                .filter(module -> module.id().equalsIgnoreCase(id))
                .findFirst()
                .orElse(null);
    }

    public static List<NovaModule> find(Category category, String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return MODULES.stream()
                .filter(module -> module.category() == category)
                .filter(module -> normalized.isEmpty()
                        || module.id().toLowerCase(Locale.ROOT).contains(normalized)
                        || module.name().getString().toLowerCase(Locale.ROOT).contains(normalized))
                .toList();
    }
}
