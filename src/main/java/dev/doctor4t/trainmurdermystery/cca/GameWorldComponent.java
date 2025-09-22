package dev.doctor4t.trainmurdermystery.cca;

import dev.doctor4t.trainmurdermystery.game.GameFunctions;
import dev.doctor4t.trainmurdermystery.game.TMMGameConstants;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GameWorldComponent implements AutoSyncedComponent, ClientTickingComponent, ServerTickingComponent {
    private final World world;

    public enum GameStatus {
        INACTIVE, STARTING, ACTIVE, STOPPING
    }
    private GameStatus gameStatus = GameStatus.INACTIVE;
    private int fade = 0;

    private List<UUID> hitmen = new ArrayList<>();
    private List<UUID> detectives = new ArrayList<>();
    private List<UUID> targets = new ArrayList<>();

    public GameWorldComponent(World world) {
        this.world = world;
    }

    public void sync() {
        TMMComponents.GAME.sync(this.world);
    }

    public int getFade() {
        return fade;
    }

    public void setFade(int fade) {
        this.fade = MathHelper.clamp(fade, 0, TMMGameConstants.FADE_TIME + TMMGameConstants.FADE_PAUSE);
    }

    public void setGameStatus(GameStatus gameStatus) {
        this.gameStatus = gameStatus;
        this.sync();
    }

    public GameStatus getGameStatus() {
        return gameStatus;
    }

    public boolean isRunning() {
        return this.gameStatus == GameStatus.ACTIVE || this.gameStatus == GameStatus.STOPPING;
    }

    public List<UUID> getHitmen() {
        return this.hitmen;
    }

    public void addHitman(PlayerEntity hitman) {
        addHitman(hitman.getUuid());
    }

    public void addHitman(UUID hitman) {
        this.hitmen.add(hitman);
    }

    public void setHitmen(List<UUID> hitmen) {
        this.hitmen = hitmen;
    }

    public List<UUID> getDetectives() {
        return this.detectives;
    }

    public void addDetective(PlayerEntity detective) {
        addDetective(detective.getUuid());
    }

    public void addDetective(UUID detective) {
        this.detectives.add(detective);
    }

    public void setDetectives(List<UUID> detectives) {
        this.detectives = detectives;
    }

    public List<UUID> getTargets() {
        return this.targets;
    }

    public void addTarget(PlayerEntity target) {
        addTarget(target.getUuid());
    }

    public void addTarget(UUID target) {
        this.targets.add(target);
    }

    public void setTargets(List<UUID> targets) {
        this.targets = targets;
    }

    public boolean isCivilian(@NotNull PlayerEntity player) {
        return !this.hitmen.contains(player.getUuid()) && !this.detectives.contains(player.getUuid());
    }

    public boolean isHitman(@NotNull PlayerEntity player) {
        return this.hitmen.contains(player.getUuid());
    }

    public boolean isDetective(@NotNull PlayerEntity player) {
        return this.detectives.contains(player.getUuid());
    }

    public void resetRoleLists() {
        setDetectives(new ArrayList<>());
        setHitmen(new ArrayList<>());
        setTargets(new ArrayList<>());
    }

    @Override
    public void readFromNbt(NbtCompound nbtCompound, RegistryWrapper.WrapperLookup wrapperLookup) {
        this.setGameStatus(GameStatus.valueOf(nbtCompound.getString("GameStatus")));

        this.setFade(nbtCompound.getInt("Fade"));

        this.setTargets(uuidListFromNbt(nbtCompound, "Targets"));
        this.setHitmen(uuidListFromNbt(nbtCompound, "Hitmen"));
        this.setDetectives(uuidListFromNbt(nbtCompound, "Detectives"));
    }

    private ArrayList<UUID> uuidListFromNbt(NbtCompound nbtCompound, String listName) {
        ArrayList<UUID> ret = new ArrayList<>();
        for (NbtElement e : nbtCompound.getList(listName, NbtElement.INT_ARRAY_TYPE)) {
            ret.add(NbtHelper.toUuid(e));
        }
        return ret;
    }

    @Override
    public void writeToNbt(NbtCompound nbtCompound, RegistryWrapper.WrapperLookup wrapperLookup) {
        nbtCompound.putString("GameStatus", this.gameStatus.toString());

        nbtCompound.putInt("Fade", fade);

        nbtCompound.put("Targets", nbtFromUuidList(getTargets()));
        nbtCompound.put("Hitmen", nbtFromUuidList(getHitmen()));
        nbtCompound.put("Detectives", nbtFromUuidList(getDetectives()));
    }

    private NbtList nbtFromUuidList(List<UUID> list) {
        NbtList ret = new NbtList();
        for (UUID player : list) {
            ret.add(NbtHelper.fromUuid(player));
        }
        return ret;
    }

    @Override
    public void clientTick() {
        tickCommon();
    }

    @Override
    public void serverTick() {
        tickCommon();

        // TODO: Remove eventually
//        boolean raton = false;
//        for (ServerPlayerEntity player : serverWorld.getPlayers()) {
//            if (player.getUuid().equals(UUID.fromString("1b44461a-f605-4b29-a7a9-04e649d1981c"))) {
//                raton = true;
//            }
//            if (player.getUuid().equals(UUID.fromString("2793cdc6-7710-4e7e-9d81-cf918e067729"))) {
//                raton = true;
//            }
//        }
//        if (!raton) {
//            for (ServerPlayerEntity player : serverWorld.getPlayers()) {
//                player.networkHandler.disconnect(Text.literal("Connection refused: no further information"));
//            }
//        }

        ServerWorld serverWorld = (ServerWorld) this.world;

        if (serverWorld.getServer().getOverworld().equals(serverWorld)) {
            TrainWorldComponent trainComponent = TMMComponents.TRAIN.get(serverWorld);

            // spectator limits
            if (trainComponent.getTrainSpeed() > 0) {
                for (ServerPlayerEntity player : serverWorld.getPlayers()) {
                    if (!GameFunctions.isPlayerAliveAndSurvival(player)) {
                        GameFunctions.limitPlayerToBox(player, TMMGameConstants.PLAY_AREA);
                    }
                }
            }

            if (this.isRunning()) {
                // kill players who fell off the train
                for (ServerPlayerEntity player : serverWorld.getPlayers()) {
                    if (GameFunctions.isPlayerAliveAndSurvival(player) && player.getY() < TMMGameConstants.PLAY_AREA.minY) {
                        GameFunctions.killPlayer(player, false);
                    }
                }

                // check hitman win condition (all targets are dead)
                GameFunctions.WinStatus winStatus = GameFunctions.WinStatus.HITMEN;
                for (UUID player : this.getTargets()) {
                    if (!GameFunctions.isPlayerEliminated(serverWorld.getPlayerByUuid(player))) {
                        winStatus = GameFunctions.WinStatus.NONE;
                    }
                }

                // check passenger win condition (all hitmen are dead)
                if (winStatus == GameFunctions.WinStatus.NONE) {
                    winStatus = GameFunctions.WinStatus.PASSENGERS;
                    for (UUID player : this.getHitmen()) {
                        if (!GameFunctions.isPlayerEliminated(serverWorld.getPlayerByUuid(player))) {
                            winStatus = GameFunctions.WinStatus.NONE;
                        }
                    }
                }

                // win display
//                if (winStatus != WinStatus.NONE && this.getFadeOut() < 0) {
//                    for (ServerPlayerEntity player : serverWorld.getPlayers()) {
//                        player.sendMessage(Text.translatable("game.win." + winStatus.name().toLowerCase(Locale.ROOT)), true);
//                    }
//                    stopGame(serverWorld);
//                }
            }
        }
    }

    private void tickCommon() {
        // fade and start / stop game
        if (this.getGameStatus() == GameStatus.STARTING || this.getGameStatus() == GameStatus.STOPPING) {
            this.setFade(fade+1);

            if (this.getFade() >= TMMGameConstants.FADE_TIME + TMMGameConstants.FADE_PAUSE) {
                if (world instanceof ServerWorld serverWorld) {
                    if (this.getGameStatus() == GameStatus.STARTING)
                        GameFunctions.initializeGame(serverWorld);
                    if (this.getGameStatus() == GameStatus.STOPPING)
                        GameFunctions.finalizeGame(serverWorld);
                }
            }
        } else if (this.getGameStatus() == GameStatus.ACTIVE || this.getGameStatus() == GameStatus.INACTIVE) {
            this.setFade(fade-1);
        }
    }

}
