package mianbaos.modernwarfare.vs2.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ServerEvents {
    private static final Map<ResourceKey<Level>, Set<UUID>> TRACKED_PROJECTILES = new ConcurrentHashMap<>();

    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Entity entity = event.getEntity();
        if (isModernWarfareProjectile(entity)) {
            TRACKED_PROJECTILES
                    .computeIfAbsent(level.dimension(), ignored -> ConcurrentHashMap.newKeySet())
                    .add(entity.getUUID());
        }
    }

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }
        VsTrackCache.update(level);
        guideProjectiles(level);
    }

    private static void guideProjectiles(ServerLevel level) {
        Set<UUID> projectiles = TRACKED_PROJECTILES.getOrDefault(level.dimension(), new HashSet<>());
        Iterator<UUID> iterator = projectiles.iterator();
        while (iterator.hasNext()) {
            UUID uuid = iterator.next();
            Entity entity = level.getEntity(uuid);
            if (entity == null || !entity.isAlive()) {
                iterator.remove();
                continue;
            }
            updateProjectileGuidance(level, entity);
        }
    }

    private static void updateProjectileGuidance(ServerLevel level, Entity entity) {
        CompoundTag data = entity.getPersistentData();
        long shipId = data.getLong("mwvsbridge_target_ship");
        if (shipId == 0L) {
            OptionalLong fromChannel = TargetRegistry.get(level.dimension(), readChannel(data));
            if (fromChannel.isEmpty()) {
                return;
            }
            shipId = fromChannel.getAsLong();
            data.putBoolean("mwvsbridge_guided", true);
            data.putLong("mwvsbridge_target_ship", shipId);
        }

        long finalShipId = shipId;
        VsTrackCache.get(level.dimension(), finalShipId).ifPresent(track -> {
            Vec3 aim = track.predictedPosition(level.getGameTime());
            data.putBoolean("mwvsbridge_guided", true);
            data.putLong("mwvsbridge_target_ship", finalShipId);
            data.putBoolean("\u5750\u6807\u6a21\u5f0f", true);
            data.putBoolean("\u9501\u5b9a\u6a21\u5f0f", false);
            data.putBoolean("\u9891\u9053\u6a21\u5f0f", false);
            data.putDouble("target_x", aim.x);
            data.putDouble("target_y", aim.y);
            data.putDouble("target_z", aim.z);
            data.putDouble("x_target", aim.x);
            data.putDouble("y_target", aim.y);
            data.putDouble("z_target", aim.z);
        });
    }

    private static String readChannel(CompoundTag data) {
        String channel = data.getString("\u9891\u9053");
        if (!channel.isBlank()) {
            return channel;
        }
        return data.getString("target");
    }

    private static boolean isModernWarfareProjectile(Entity entity) {
        String className = entity.getClass().getName();
        if (!className.startsWith("net.mcreator.myfirstmod.entity.")) {
            return false;
        }
        return className.endsWith("tansheEntity")
                || className.endsWith("TansheEntity")
                || className.endsWith("rocketEntity")
                || className.endsWith("missileEntity");
    }
}
