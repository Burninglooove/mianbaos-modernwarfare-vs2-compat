package mmvc;

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
        CHANNEL_TARGETS.put(key(level.dimension(), channel.trim()), shipId);
    }

    public static void set(ServerLevel level, String channel, TrackInfo track) {
        set(level, channel, track.shipId());
        set(level, track.name(), track.shipId());
        set(level, track.name() + " #" + track.shipId(), track.shipId());
        set(level, Long.toString(track.shipId()), track.shipId());
        set(level, "VS2 #" + track.shipId(), track.shipId());
    }

    public static void clear(ServerLevel level, String channel) {
        if (channel == null || channel.isBlank()) {
            return;
        }
        CHANNEL_TARGETS.remove(key(level.dimension(), channel.trim()));
    }

    public static void clear(ServerLevel level, TrackInfo track) {
        clear(level, track.name());
        clear(level, track.name() + " #" + track.shipId());
        clear(level, Long.toString(track.shipId()));
        clear(level, "VS2 #" + track.shipId());
    }

    public static void clear(ServerLevel level, long shipId) {
        CHANNEL_TARGETS.entrySet().removeIf(entry ->
                entry.getKey().startsWith(level.dimension().location() + "|")
                        && entry.getValue() == shipId);
    }

    public static OptionalLong get(ResourceKey<Level> dimension, String channel) {
        if (channel == null || channel.isBlank()) {
            return OptionalLong.empty();
        }
        Long id = CHANNEL_TARGETS.get(key(dimension, channel.trim()));
        return id == null ? OptionalLong.empty() : OptionalLong.of(id);
    }

    private static String key(ResourceKey<Level> dimension, String channel) {
        return dimension.location() + "|" + channel;
    }
}
