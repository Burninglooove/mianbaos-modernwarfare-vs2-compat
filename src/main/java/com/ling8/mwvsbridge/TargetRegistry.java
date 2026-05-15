package com.ling8.mwvsbridge;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;

public final class TargetRegistry {
    private static final Map<String, Long> CHANNEL_TARGETS = new ConcurrentHashMap<>();

    private TargetRegistry() {
    }

    public static void set(ServerLevel level, String channel, long shipId) {
        if (channel == null || channel.isBlank()) {
            return;
        }
        CHANNEL_TARGETS.put(key(level.dimension(), channel), shipId);
    }

    public static void clear(ServerLevel level, String channel) {
        if (channel == null || channel.isBlank()) {
            return;
        }
        CHANNEL_TARGETS.remove(key(level.dimension(), channel));
    }

    public static OptionalLong get(ResourceKey<Level> dimension, String channel) {
        if (channel == null || channel.isBlank()) {
            return OptionalLong.empty();
        }
        Long id = CHANNEL_TARGETS.get(key(dimension, channel));
        return id == null ? OptionalLong.empty() : OptionalLong.of(id);
    }

    private static String key(ResourceKey<Level> dimension, String channel) {
        return dimension.location() + "|" + channel;
    }
}
