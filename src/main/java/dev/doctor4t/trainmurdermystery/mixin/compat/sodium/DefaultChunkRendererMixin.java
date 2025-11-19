package dev.doctor4t.trainmurdermystery.mixin.compat.sodium;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.doctor4t.trainmurdermystery.cca.TrainWorldComponent;
import dev.doctor4t.trainmurdermystery.client.TMMClient;
import net.caffeinemc.mods.sodium.client.gui.SodiumGameOptions;
import net.caffeinemc.mods.sodium.client.render.chunk.DefaultChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = DefaultChunkRenderer.class)
public abstract class DefaultChunkRendererMixin {

    @Shadow(remap = false)
    private static float getCameraTranslation(int chunkBlockPos, int cameraBlockPos, float cameraPos) {
        return 0;
    }

    @ModifyExpressionValue(
            method = "render",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/caffeinemc/mods/sodium/client/gui/SodiumGameOptions$PerformanceSettings;useBlockFaceCulling:Z"
            ),
            remap = false
    )
    private boolean tmm$disable_culling(boolean original) {
        if (TMMClient.isTrainMoving()) {
            return false;
        }
        return original;
    }

    @WrapOperation(method = "setModelMatrixUniforms",
            at = @At(value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/shader/ChunkShaderInterface;setRegionOffset(FFF)V"),
            remap = false)
    private static void tmm$offsetScenery(
            ChunkShaderInterface shader,
            float x,
            float y,
            float z,
            Operation<Void> original,
            @Local(argsOnly = true) RenderRegion region,
            @Local(argsOnly = true) CameraTransform camera
    ) {
        if (TMMClient.isTrainMoving()) {
            float trainSpeed = TMMClient.getTrainSpeed();
            int chunkSize = 16;
            int tileWidth = 15 * chunkSize;
            int height = 116;
            int tileLength = 32 * chunkSize;
            int tileSize = tileLength * 3;
            float time = TMMClient.trainComponent.getTime() + MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(true);

            BlockPos blockPos = new BlockPos(
                    region.getOriginX(),
                    region.getOriginY(),
                    region.getOriginZ()
            );

            boolean trainSection = ChunkSectionPos.getSectionCoord(blockPos.getY()) >= 4;
            float v1 = camera.fracX;
            float v2 = camera.fracY;
            float v3 = camera.fracZ;
            int zSection = (region.getOriginZ() / chunkSize - camera.intZ / chunkSize);

            float finalX = blockPos.getX() - v1;
            float finalY = v2;
            float finalZ = v3;

            float worldPosV1 = blockPos.getX() - v1;
            if (zSection <= -8) {
                finalX = (worldPosV1 + tileLength + time / 73.8F * trainSpeed) % tileSize - tileSize / 2.0F;
                finalY = v2 - height;
                finalZ = v3 - tileWidth;
            } else if (zSection >= 8) {
                finalX = (worldPosV1 - tileLength + time / 73.8F * trainSpeed) % tileSize - tileSize / 2.0F;
                finalY = v2 - height;
                finalZ = v3 + tileWidth;
            } else if (!trainSection) {
                finalX = (worldPosV1 + time / 73.8F * trainSpeed) % tileSize - tileSize / 2.0F;
                finalY = v2 - height;
                finalZ = v3;
            }

            finalX = blockPos.getX() - finalX;

//            boolean tooFar = !(Math.abs(finalX) < (TMMClient.trainComponent.getTimeOfDay() == TrainWorldComponent.TimeOfDay.SUNDOWN ? 320 : 160));
//            if (!tooFar) {
//
//                x = getCameraTranslation(region.getOriginX(), camera.intX, finalX);
//                y = getCameraTranslation(region.getOriginY(), camera.intY, finalY);
//                z = getCameraTranslation(region.getOriginZ(), camera.intZ, finalZ);
//            }
            x = getCameraTranslation(region.getOriginX(), camera.intX, finalX);
            y = getCameraTranslation(region.getOriginY(), camera.intY, finalY);
            z = getCameraTranslation(region.getOriginZ(), camera.intZ, finalZ);
        }
        original.call(shader, x, y, z);
    }
}
