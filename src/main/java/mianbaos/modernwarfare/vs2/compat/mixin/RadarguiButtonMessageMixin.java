package mianbaos.modernwarfare.vs2.compat.mixin;

import mianbaos.modernwarfare.vs2.compat.RadarAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.mcreator.myfirstmod.network.RadarguiButtonMessage", remap = false)
public abstract class RadarguiButtonMessageMixin {
    @Inject(method = "handleButtonAction", at = @At("HEAD"), cancellable = true, remap = false)
    private static void mwvsbridge$handleVsButtons(Player entity, int buttonID, int x, int y, int z, CallbackInfo ci) {
        if (entity == null) {
            return;
        }
        Level world = entity.level();
        if (!(world instanceof ServerLevel level)) {
            return;
        }

        BlockPos pos = new BlockPos(x, y, z);
        if (!level.isLoaded(pos)) {
            return;
        }

        if (buttonID == 6) {
            RadarAccess.cycleMode(level, pos);
            ci.cancel();
            return;
        }

        if (!RadarAccess.isVsMode(level, pos)) {
            return;
        }

        if (buttonID == 0) {
            RadarAccess.cycleTarget(level, pos, 1, entity);
            ci.cancel();
        } else if (buttonID == 1) {
            RadarAccess.cycleTarget(level, pos, -1, entity);
            ci.cancel();
        } else if (buttonID == 2) {
            RadarAccess.lockPreview(level, pos, entity);
            ci.cancel();
        } else if (buttonID == 3) {
            RadarAccess.clear(level, pos);
            ci.cancel();
        }
    }
}
