package dev.doctor4t.trainmurdermystery.block_entity;

import dev.doctor4t.trainmurdermystery.index.TMMBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PlateBlockEntity extends BlockEntity {
    private final List<ItemStack> storedItems = new ArrayList<>();
    public PlateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static PlateBlockEntity create(BlockPos pos, BlockState state) {
        return new PlateBlockEntity(TMMBlockEntities.PLATE, pos, state);
    }
    public List<ItemStack> getStoredItems() {
        return storedItems;
    }

    public void addItem(ItemStack stack) {
        if (stack.isEmpty()) return;

        storedItems.add(stack.copy());
        markDirty();
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);

        NbtCompound itemsNbt = new NbtCompound();
        for (int i = 0; i < storedItems.size(); i++) {
            itemsNbt.put("Item" + i, storedItems.get(i).encode(registryLookup));
        }
        nbt.put("Items", itemsNbt);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);

        storedItems.clear();
        if (nbt.contains("Items")) {
            NbtCompound itemsNbt = nbt.getCompound("Items");
            for (String key : itemsNbt.getKeys()) {
                Optional<ItemStack> itemStack = ItemStack.fromNbt(registryLookup, itemsNbt.get(key));
                itemStack.ifPresent(storedItems::add);
            }
        }
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        return createNbt(registryLookup);
    }

    @Override
    public @Nullable Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }
}
