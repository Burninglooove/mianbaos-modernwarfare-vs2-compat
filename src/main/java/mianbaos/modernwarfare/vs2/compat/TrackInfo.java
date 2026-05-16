package mianbaos.modernwarfare.vs2.compat;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public record TrackInfo(
        long shipId,
        ResourceKey<Level> dimension,
        Vec3 position,
        Vec3 velocity,
        double mass,
        String name,
        long updateTick
) {
    public Vec3 predictedPosition(long gameTime) {
        double dt = Math.max(0, gameTime - updateTick) / 20.0;
        return position.add(velocity.scale(dt));
    }
}
