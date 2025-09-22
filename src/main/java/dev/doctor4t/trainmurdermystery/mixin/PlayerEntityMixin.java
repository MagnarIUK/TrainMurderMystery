package dev.doctor4t.trainmurdermystery.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.doctor4t.trainmurdermystery.cca.PlayerMoodComponent;
import dev.doctor4t.trainmurdermystery.cca.TMMComponents;
import dev.doctor4t.trainmurdermystery.cca.GameWorldComponent;
import dev.doctor4t.trainmurdermystery.game.TMMGameConstants;
import dev.doctor4t.trainmurdermystery.game.GameFunctions;
import dev.doctor4t.trainmurdermystery.index.TMMItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {
    @Unique
    private float sprintingTicks;

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Shadow
    public abstract boolean isSpectator();

    @ModifyReturnValue(method = "getMovementSpeed", at = @At("RETURN"))
    public float tmm$overrideMovementSpeed(float original) {
        if (GameFunctions.isPlayerAliveAndSurvival((PlayerEntity) (Object) this)) {
            var speed = this.isSprinting() ? 0.1f : 0.07f;
            speed *= MathHelper.lerp(PlayerMoodComponent.KEY.get(this).mood, 0.5f, 1f);
            return speed;
        } else {
            return original;
        }
    }

    @Inject(method = "tickMovement", at = @At("HEAD"))
    public void tmm$limitSprint(CallbackInfo ci) {
        GameWorldComponent gameComponent = TMMComponents.GAME.get(this.getWorld());
        if (GameFunctions.isPlayerAliveAndSurvival((PlayerEntity) (Object) this) && !(gameComponent != null && gameComponent.getHitmen().contains(this.getUuid()))) {
            if (this.isSprinting()) {
                sprintingTicks = Math.max(sprintingTicks - 1, 0);
            } else {
                sprintingTicks = Math.min(sprintingTicks + 0.25f, TMMGameConstants.MAX_SPRINTING_TICKS);
            }

            if (sprintingTicks <= 0) {
                this.setSprinting(false);
            }
        }
    }

    @WrapMethod(method = "attack")
    public void attack(Entity target, Operation<Void> original) {
        if (!GameFunctions.isPlayerAliveAndSurvival((PlayerEntity) (Object)this) || this.getMainHandStack().isOf(TMMItems.KNIFE)) {
            original.call(target);
        }
    }
}
