package dev.doctor4t.trainmurdermystery.cca;

import dev.doctor4t.trainmurdermystery.TMM;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy;
import org.ladysnake.cca.api.v3.world.WorldComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.world.WorldComponentInitializer;

public class TMMComponents implements WorldComponentInitializer, EntityComponentInitializer {
    public static final ComponentKey<TrainWorldComponent> TRAIN = ComponentRegistry.getOrCreate(TMM.id("train"), TrainWorldComponent.class);
    public static final ComponentKey<GameWorldComponent> GAME = ComponentRegistry.getOrCreate(TMM.id("game"), GameWorldComponent.class);

    @Override
    public void registerWorldComponentFactories(@NotNull WorldComponentFactoryRegistry registry) {
        registry.register(TRAIN, TrainWorldComponent::new);
        registry.register(GAME, GameWorldComponent::new);
    }

    @Override
    public void registerEntityComponentFactories(@NotNull EntityComponentFactoryRegistry registry) {
        registry.beginRegistration(PlayerEntity.class, PlayerMoodComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(PlayerMoodComponent::new);
        registry.beginRegistration(PlayerEntity.class, PlayerStoreComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(PlayerStoreComponent::new);
    }
}