package dev.doctor4t.trainmurdermystery.cca;

import dev.doctor4t.trainmurdermystery.TMM;
import dev.doctor4t.trainmurdermystery.index.TMMItems;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.*;

public class ScoreboardRoleSelectorComponent implements AutoSyncedComponent {
    public static final ComponentKey<ScoreboardRoleSelectorComponent> KEY = ComponentRegistry.getOrCreate(TMM.id("rolecounter"), ScoreboardRoleSelectorComponent.class);
    public final Scoreboard scoreboard;
    public final MinecraftServer server;
    public final Map<UUID, Integer> killerRounds = new HashMap<>();
    public final Map<UUID, Integer> vigilanteRounds = new HashMap<>();
    public final List<UUID> forcedKillers = new ArrayList<>();
    public final List<UUID> forcedVigilantes = new ArrayList<>();

    public ScoreboardRoleSelectorComponent(Scoreboard scoreboard, @Nullable MinecraftServer server) {
        this.scoreboard = scoreboard;
        this.server = server;
    }

    public int reset() {
        var count = this.killerRounds.size() + this.vigilanteRounds.size();
        this.killerRounds.clear();
        this.vigilanteRounds.clear();
        return count;
    }

    public int assignKillers(ServerWorld world, GameWorldComponent gameComponent, @NotNull List<ServerPlayerEntity> players, int killerCount) {
        this.reduceKillers();
        var killers = new ArrayList<UUID>();
        for (var uuid : this.forcedKillers) {
            killers.add(uuid);
            killerCount--;
            this.killerRounds.put(uuid, this.killerRounds.getOrDefault(uuid, 1) + 1);
        }
        this.forcedKillers.clear();
        var map = new HashMap<ServerPlayerEntity, Float>();
        var total = 0f;
        for (var player : players) {
            var weight = (float) Math.exp(-this.killerRounds.getOrDefault(player.getUuid(), 0) * 4);
            map.put(player, weight);
            total += weight;
        }
        for (var i = 0; i < killerCount; i++) {
            var random = world.getRandom().nextFloat() * total;
            for (var entry : map.entrySet()) {
                random -= entry.getValue();
                if (random <= 0) {
                    killers.add(entry.getKey().getUuid());
                    total -= entry.getValue();
                    map.remove(entry.getKey());
                    this.killerRounds.put(entry.getKey().getUuid(), this.killerRounds.getOrDefault(entry.getKey().getUuid(), 1) + 1);
                    break;
                }
            }
        }
        for (var player : killers) gameComponent.addKiller(player);
        return killers.size();
    }

    private void reduceKillers() {
        var minimum = Integer.MAX_VALUE;
        for (var times : this.killerRounds.values()) minimum = Math.min(minimum, times);
        for (var times : this.killerRounds.keySet()) this.killerRounds.put(times, this.killerRounds.get(times) - minimum);
    }

    public void assignVigilantes(ServerWorld world, GameWorldComponent gameComponent, @NotNull List<ServerPlayerEntity> players, int vigilanteCount) {
        this.reduceVigilantes();
        var vigilantes = new ArrayList<ServerPlayerEntity>();
        for (var uuid : this.forcedVigilantes) {
            var player = world.getPlayerByUuid(uuid);
            if (player instanceof ServerPlayerEntity serverPlayer && players.contains(serverPlayer) && !gameComponent.isKiller(serverPlayer)) {
                player.giveItemStack(new ItemStack(TMMItems.REVOLVER));
                gameComponent.addVigilante(player);
                vigilanteCount--;
                this.vigilanteRounds.put(player.getUuid(), this.vigilanteRounds.getOrDefault(player.getUuid(), 1) + 1);
            }
        }
        this.forcedVigilantes.clear();
        var map = new HashMap<ServerPlayerEntity, Float>();
        var total = 0f;
        for (var player : players) {
            if (gameComponent.isKiller(player)) continue;
            var weight = (float) Math.exp(-this.vigilanteRounds.getOrDefault(player.getUuid(), 0) * 4);
            map.put(player, weight);
            total += weight;
        }
        for (var i = 0; i < vigilanteCount; i++) {
            var random = world.getRandom().nextFloat() * total;
            for (var entry : map.entrySet()) {
                random -= entry.getValue();
                if (random <= 0) {
                    vigilantes.add(entry.getKey());
                    total -= entry.getValue();
                    map.remove(entry.getKey());
                    this.vigilanteRounds.put(entry.getKey().getUuid(), this.vigilanteRounds.getOrDefault(entry.getKey().getUuid(), 1) + 1);
                    break;
                }
            }
        }
        for (var player : vigilantes) {
            player.giveItemStack(new ItemStack(TMMItems.REVOLVER));
            gameComponent.addVigilante(player);
        }
    }

    private void reduceVigilantes() {
        var minimum = Integer.MAX_VALUE;
        for (var times : this.vigilanteRounds.values()) minimum = Math.min(minimum, times);
        for (var times : this.vigilanteRounds.keySet()) this.vigilanteRounds.put(times, this.vigilanteRounds.get(times) - minimum);
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        var killerRounds = new NbtList();
        for (var detail : this.killerRounds.entrySet()) {
            var compound = new NbtCompound();
            compound.putUuid("uuid", detail.getKey());
            compound.putInt("times", detail.getValue());
            killerRounds.add(compound);
        }
        tag.put("killerRounds", killerRounds);
        var vigilanteRounds = new NbtList();
        for (var detail : this.vigilanteRounds.entrySet()) {
            var compound = new NbtCompound();
            compound.putUuid("uuid", detail.getKey());
            compound.putInt("times", detail.getValue());
            vigilanteRounds.add(compound);
        }
        tag.put("vigilanteRounds", vigilanteRounds);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.killerRounds.clear();
        for (var element : tag.getList("killerRounds", 10)) {
            var compound = (NbtCompound) element;
            if (!compound.contains("uuid") || !compound.contains("times")) continue;
            this.killerRounds.put(compound.getUuid("uuid"), compound.getInt("times"));
        }
        this.vigilanteRounds.clear();
        for (var element : tag.getList("vigilanteRounds", 10)) {
            var compound = (NbtCompound) element;
            if (!compound.contains("uuid") || !compound.contains("times")) continue;
            this.vigilanteRounds.put(compound.getUuid("uuid"), compound.getInt("times"));
        }
    }
}