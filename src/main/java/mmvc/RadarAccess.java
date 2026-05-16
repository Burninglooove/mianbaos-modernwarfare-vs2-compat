package mmvc;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;

public final class RadarAccess {
    public static final String TAG_SELECTED_SHIP = "mwvsbridge_selected_ship";
    public static final String TAG_VS_MODE = "mwvsbridge_vs_mode";
    public static final String TAG_VS_INDEX = "mwvsbridge_vs_index";
    public static final String MW_TAG_MODE_INDEX = "\u9501\u5b9a\u6a21\u5f0f";
    public static final String MW_TAG_ENTITY_MODE = "\u5b9e\u4f53\u6a21\u5f0f";
    public static final String MW_TAG_PLAYER_MODE = "\u73a9\u5bb6\u6a21\u5f0f";
    public static final String MW_TAG_MODE_TEXT = "\u6a21\u5f0f";
    public static final String MW_TAG_LOCKED = "\u96f7\u8fbe\u9501\u5b9a";
    public static final String MW_TAG_LOCK_X = "\u96f7\u8fbe\u9501\u5b9ax";
    public static final String MW_TAG_LOCK_Y = "\u96f7\u8fbe\u9501\u5b9ay";
    public static final String MW_TAG_LOCK_Z = "\u96f7\u8fbe\u9501\u5b9az";
    public static final String MW_TAG_TARGET_NAME = "\u9501\u5b9a\u76ee\u6807\u540d\u79f0";
    public static final String MW_TAG_CHANNEL = "\u9891\u9053";

    private RadarAccess() {
    }

    public static double radarRange(ServerLevel level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return 1024.0;
        }
        CompoundTag data = blockEntity.getPersistentData();
        double range = data.getDouble("DISTANCE");
        return range <= 0 ? 1024.0 : range;
    }

    public static String channel(ServerLevel level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return "";
        }
        String channel = blockEntity.getPersistentData().getString(MW_TAG_CHANNEL);
        return channel == null ? "" : channel;
    }

    public static boolean select(ServerLevel level, BlockPos pos, TrackInfo track) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return false;
        }
        CompoundTag data = blockEntity.getPersistentData();
        data.putBoolean(TAG_VS_MODE, true);
        data.putLong(TAG_SELECTED_SHIP, track.shipId());
        data.putBoolean(MW_TAG_LOCKED, true);
        data.putString(MW_TAG_TARGET_NAME, track.name());
        data.putString("target", Long.toString(track.shipId()));
        writeLockPosition(data, track.predictedPosition(level.getGameTime()));
        blockEntity.setChanged();
        level.sendBlockUpdated(pos, blockEntity.getBlockState(), blockEntity.getBlockState(), 3);

        String channel = channel(level, pos);
        TargetRegistry.set(level, channel, track);
        return true;
    }

    public static boolean isVsMode(ServerLevel level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return false;
        }
        CompoundTag data = blockEntity.getPersistentData();
        return data.getBoolean(TAG_VS_MODE) || "VS2".equals(data.getString(MW_TAG_MODE_TEXT));
    }

    public static void cycleMode(ServerLevel level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return;
        }
        CompoundTag data = blockEntity.getPersistentData();
        if (isVsMode(level, pos)) {
            setMode(level, pos, 0);
            return;
        }
        double current = data.getDouble(MW_TAG_MODE_INDEX);
        if (current < 1.0) {
            setMode(level, pos, 1);
        } else if (current < 2.0) {
            setMode(level, pos, 2);
        } else {
            setMode(level, pos, 3);
        }
    }

    public static void setMode(ServerLevel level, BlockPos pos, int mode) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return;
        }
        CompoundTag data = blockEntity.getPersistentData();
        data.putDouble(MW_TAG_MODE_INDEX, mode);
        data.putBoolean(TAG_VS_MODE, mode == 3);
        data.putBoolean(MW_TAG_ENTITY_MODE, mode == 1);
        data.putBoolean(MW_TAG_PLAYER_MODE, mode == 2);
        if (mode == 1) {
            data.putString(MW_TAG_MODE_TEXT, "ENTITY");
        } else if (mode == 2) {
            data.putString(MW_TAG_MODE_TEXT, "PLAYER");
        } else if (mode == 3) {
            data.putString(MW_TAG_MODE_TEXT, "VS2");
            updatePreview(level, pos, 0);
        } else {
            data.putString(MW_TAG_MODE_TEXT, "NONE");
            data.putBoolean(MW_TAG_LOCKED, false);
            data.putString(MW_TAG_TARGET_NAME, "");
            TargetRegistry.clear(level, data.getString(MW_TAG_CHANNEL));
        }
        blockEntity.setChanged();
        level.sendBlockUpdated(pos, blockEntity.getBlockState(), blockEntity.getBlockState(), 3);
    }

    public static void cycleTarget(ServerLevel level, BlockPos pos, int delta, Player player) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return;
        }
        CompoundTag data = blockEntity.getPersistentData();
        int index = data.getInt(TAG_VS_INDEX) + delta;
        updatePreview(level, pos, index);
    }

    public static void lockPreview(ServerLevel level, BlockPos pos, Player player) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return;
        }
        CompoundTag data = blockEntity.getPersistentData();
        TrackInfo track = previewTrack(level, pos, data.getInt(TAG_VS_INDEX));
        if (track == null) {
            data.putString(MW_TAG_TARGET_NAME, "NO TARGET");
            blockEntity.setChanged();
            level.sendBlockUpdated(pos, blockEntity.getBlockState(), blockEntity.getBlockState(), 3);
            return;
        }
        select(level, pos, track);
    }

    public static void clear(ServerLevel level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return;
        }
        CompoundTag data = blockEntity.getPersistentData();
        TargetRegistry.clear(level, data.getString(MW_TAG_CHANNEL));
        data.remove(TAG_SELECTED_SHIP);
        data.remove("target");
        data.putBoolean(TAG_VS_MODE, false);
        data.putString(MW_TAG_TARGET_NAME, "");
        data.remove(MW_TAG_LOCK_X);
        data.remove(MW_TAG_LOCK_Y);
        data.remove(MW_TAG_LOCK_Z);
        blockEntity.setChanged();
        level.sendBlockUpdated(pos, blockEntity.getBlockState(), blockEntity.getBlockState(), 3);
    }

    private static void updatePreview(ServerLevel level, BlockPos pos, int requestedIndex) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return;
        }
        CompoundTag data = blockEntity.getPersistentData();
        ArrayList<TrackInfo> tracks = VsTrackCache.query(level, pos, radarRange(level, pos), 0, 128);
        if (tracks.isEmpty()) {
            data.putInt(TAG_VS_INDEX, 0);
            data.putString(MW_TAG_TARGET_NAME, "NO TARGET");
            blockEntity.setChanged();
            level.sendBlockUpdated(pos, blockEntity.getBlockState(), blockEntity.getBlockState(), 3);
            return;
        }
        int index = Math.floorMod(requestedIndex, tracks.size());
        TrackInfo track = tracks.get(index);
        data.putInt(TAG_VS_INDEX, index);
        data.putLong(TAG_SELECTED_SHIP, track.shipId());
        data.putString(MW_TAG_TARGET_NAME, track.name() + " #" + track.shipId());
        data.putString("target", Long.toString(track.shipId()));
        writeLockPosition(data, track.predictedPosition(level.getGameTime()));
        blockEntity.setChanged();
        level.sendBlockUpdated(pos, blockEntity.getBlockState(), blockEntity.getBlockState(), 3);
    }

    private static TrackInfo previewTrack(ServerLevel level, BlockPos pos, int requestedIndex) {
        ArrayList<TrackInfo> tracks = VsTrackCache.query(level, pos, radarRange(level, pos), 0, 128);
        if (tracks.isEmpty()) {
            return null;
        }
        return tracks.get(Math.floorMod(requestedIndex, tracks.size()));
    }

    private static void writeLockPosition(CompoundTag data, net.minecraft.world.phys.Vec3 pos) {
        data.putDouble(MW_TAG_LOCK_X, pos.x);
        data.putDouble(MW_TAG_LOCK_Y, pos.y);
        data.putDouble(MW_TAG_LOCK_Z, pos.z);
    }
}
