package mmvc;

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
            if (isAirToAirProjectile(entity)) {
                steerAirToAirProjectile(level, entity, track);
            }
        });
    }

    private static void steerAirToAirProjectile(ServerLevel level, Entity entity, TrackInfo track) {
        CompoundTag data = entity.getPersistentData();
        double flightTicks = data.getDouble("fadongji1");
        if (flightTicks > 0.0 && flightTicks < 5.0) {
            return;
        }

        Vec3 current = entity.getDeltaMovement();
        double speed = Math.max(current.length(), baseAirToAirSpeed(entity));
        double leadSeconds = entity.position().distanceTo(track.predictedPosition(level.getGameTime())) / Math.max(speed * 20.0, 1.0);
        Vec3 aim = track.predictedPosition(level.getGameTime()).add(track.velocity().scale(Math.min(leadSeconds, 3.0)));
        Vec3 toTarget = aim.subtract(entity.position());
        if (toTarget.lengthSqr() < 1.0e-6) {
            return;
        }

        Vec3 desired = toTarget.normalize().scale(speed);
        double turn = airToAirTurnRate(entity);
        Vec3 guided = current.lengthSqr() < 1.0e-6
                ? desired
                : current.scale(1.0 - turn).add(desired.scale(turn));
        if (guided.lengthSqr() < 1.0e-6) {
            return;
        }

        guided = guided.normalize().scale(speed);
        if (shouldDetonateVsTarget(entity, aim, guided)) {
            level.explode(entity, entity.getX(), entity.getY(), entity.getZ(), airToAirExplosionPower(entity), Level.ExplosionInteraction.NONE);
            entity.discard();
            return;
        }

        data.putDouble("Vx", guided.x);
        data.putDouble("Vy", guided.y);
        data.putDouble("Vz", guided.z);
        entity.setNoGravity(true);
        entity.setDeltaMovement(guided);
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
        String simpleName = entity.getClass().getSimpleName().toLowerCase();
        if (simpleName.contains("tanshe") && (simpleName.contains("missile") || simpleName.contains("rocket"))) {
            return true;
        }
        return className.endsWith("tansheEntity")
                || className.endsWith("TansheEntity")
                || className.endsWith("rocketEntity")
                || className.endsWith("missileEntity");
    }

    private static boolean isAirToAirProjectile(Entity entity) {
        String name = entity.getClass().getSimpleName().toLowerCase();
        return name.contains("antiair") && name.contains("missile") && name.contains("tanshe");
    }

    private static double baseAirToAirSpeed(Entity entity) {
        String name = entity.getClass().getSimpleName().toLowerCase();
        return name.contains("tanshe2") || name.contains("system") ? 12.0 : 9.0;
    }

    private static double airToAirTurnRate(Entity entity) {
        String name = entity.getClass().getSimpleName().toLowerCase();
        return name.contains("tanshe2") || name.contains("system") ? 0.35 : 0.25;
    }

    private static boolean shouldDetonateVsTarget(Entity entity, Vec3 aim, Vec3 velocity) {
        Vec3 start = entity.position();
        Vec3 end = start.add(velocity);
        Vec3 travel = end.subtract(start);
        double travelSqr = travel.lengthSqr();
        double radius = airToAirDetonationRadius(entity);
        if (travelSqr < 1.0e-6) {
            return start.distanceToSqr(aim) <= radius * radius;
        }
        double t = aim.subtract(start).dot(travel) / travelSqr;
        t = Math.max(0.0, Math.min(1.0, t));
        Vec3 closest = start.add(travel.scale(t));
        return closest.distanceToSqr(aim) <= radius * radius;
    }

    private static double airToAirDetonationRadius(Entity entity) {
        String name = entity.getClass().getSimpleName().toLowerCase();
        return name.contains("tanshe2") || name.contains("system") ? 6.0 : 5.0;
    }

    private static float airToAirExplosionPower(Entity entity) {
        String name = entity.getClass().getSimpleName().toLowerCase();
        return name.contains("tanshe2") || name.contains("system") ? 4.0F : 3.0F;
    }
}
