package mmvc;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class VsTrackCache {
    private static final int UPDATE_INTERVAL_TICKS = 5;
    private static final Map<ResourceKey<Level>, Map<Long, TrackInfo>> TRACKS = new ConcurrentHashMap<>();

    private VsTrackCache() {
    }

    public static void update(ServerLevel level) {
        long tick = level.getGameTime();
        if (tick % UPDATE_INTERVAL_TICKS != 0) {
            return;
        }

        Map<Long, TrackInfo> byId = new ConcurrentHashMap<>();
        ServerShipWorldCore shipWorld = VSGameUtilsKt.getShipObjectWorld(level);
        if (shipWorld != null) {
            for (ServerShip ship : shipWorld.getAllShips()) {
                String claimedDimension = ship.getChunkClaimDimension();
                if (!matchesDimension(level, claimedDimension)) {
                    continue;
                }
                Vector3dc pos = ship.getTransform().getPositionInWorld();
                Vector3dc vel = ship.getVelocity();
                double mass = ship.getInertiaData().getMass();
                String name = safeShipName(ship);
                byId.put(ship.getId(), new TrackInfo(
                        ship.getId(),
                        level.dimension(),
                        new Vec3(pos.x(), pos.y(), pos.z()),
                        new Vec3(vel.x(), vel.y(), vel.z()),
                        mass,
                        name,
                        tick
                ));
            }
        }
        TRACKS.put(level.dimension(), byId);
    }

    public static Optional<TrackInfo> get(ResourceKey<Level> dimension, long shipId) {
        Map<Long, TrackInfo> tracks = TRACKS.get(dimension);
        if (tracks == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(tracks.get(shipId));
    }

    public static ArrayList<TrackInfo> query(ServerLevel level, BlockPos radarPos, double range, int page, int pageSize) {
        Vec3 origin = radarWorldCenter(level, radarPos);
        long ownShipId = shipIdAt(level, radarPos);
        Map<Long, TrackInfo> tracks = TRACKS.get(level.dimension());
        ArrayList<TrackInfo> result = new ArrayList<>();
        if (tracks == null || tracks.isEmpty()) {
            return result;
        }

        double maxRange = range <= 0 ? 1024.0 : Mth.clamp(range, 16.0, 8192.0);
        double maxRangeSqr = maxRange * maxRange;
        long gameTime = level.getGameTime();
        tracks.values().stream()
                .filter(track -> track.shipId() != ownShipId)
                .filter(track -> track.predictedPosition(gameTime).distanceToSqr(origin) <= maxRangeSqr)
                .sorted(Comparator.comparingDouble(track -> track.predictedPosition(gameTime).distanceToSqr(origin)))
                .skip((long) Math.max(0, page) * pageSize)
                .limit(pageSize)
                .forEach(result::add);
        return result;
    }

    public static Vec3 radarWorldCenter(ServerLevel level, BlockPos pos) {
        ServerShip ship = VSGameUtilsKt.getShipManagingPos(level, pos);
        Vector3d center = new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (ship != null) {
            ship.getShipToWorld().transformPosition(center, center);
        }
        return new Vec3(center.x, center.y, center.z);
    }

    public static long shipIdAt(ServerLevel level, BlockPos pos) {
        ServerShip ship = VSGameUtilsKt.getShipManagingPos(level, pos);
        return ship == null ? Long.MIN_VALUE : ship.getId();
    }

    private static String safeShipName(ServerShip ship) {
        try {
            String slug = ship.getSlug();
            if (slug != null && !slug.isBlank()) {
                return slug;
            }
        } catch (Throwable ignored) {
        }
        return "VS2 #" + ship.getId();
    }

    private static boolean matchesDimension(ServerLevel level, String claimedDimension) {
        if (claimedDimension == null || claimedDimension.isBlank()) {
            return true;
        }
        String full = level.dimension().location().toString();
        String path = level.dimension().location().getPath();
        return claimedDimension.equals(full)
                || claimedDimension.equals(path)
                || claimedDimension.endsWith(":" + path);
    }
}
