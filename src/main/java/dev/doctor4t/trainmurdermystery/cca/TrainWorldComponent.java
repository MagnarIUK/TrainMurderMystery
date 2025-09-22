package dev.doctor4t.trainmurdermystery.cca;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

public class TrainWorldComponent implements AutoSyncedComponent, ServerTickingComponent, ClientTickingComponent {
    private final World world;
    private float trainSpeed = 0; // im km/h
    private int time = 0;

    public TrainWorldComponent(World world) {
        this.world = world;
    }

    private void sync() {
        TMMComponents.TRAIN.sync(this.world);
    }

    public void setTrainSpeed(float trainSpeed) {
        this.trainSpeed = trainSpeed;
        this.sync();
    }

    public float getTrainSpeed() {
        return trainSpeed;
    }

    public float getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
        this.sync();
    }

    @Override
    public void readFromNbt(NbtCompound nbtCompound, RegistryWrapper.WrapperLookup wrapperLookup) {
        this.trainSpeed = nbtCompound.getFloat("Speed");
        this.setTime(nbtCompound.getInt("Time"));
    }

    @Override
    public void writeToNbt(NbtCompound nbtCompound, RegistryWrapper.WrapperLookup wrapperLookup) {
        nbtCompound.putFloat("Speed", trainSpeed);
        nbtCompound.putInt("Time", time);
    }

    @Override
    public void clientTick() {
        tickTime();
    }

    private void tickTime() {
        if (trainSpeed > 0) {
            time++;
        } else {
            time = 0;
        }
    }

    @Override
    public void serverTick() {
        tickTime();
    }
}
