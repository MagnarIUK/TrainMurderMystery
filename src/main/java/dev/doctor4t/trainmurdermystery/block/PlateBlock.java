package dev.doctor4t.trainmurdermystery.block;

import com.mojang.serialization.MapCodec;
import dev.doctor4t.trainmurdermystery.block_entity.PlateBlockEntity;
import dev.doctor4t.trainmurdermystery.index.TMMBlockEntities;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PlateBlock extends BlockWithEntity {
    public static final MapCodec<PlateBlock> CODEC = createCodec(PlateBlock::new);
    public PlateBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new PlateBlockEntity(TMMBlockEntities.PLATE, pos, state);
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    protected VoxelShape getRaycastShape(BlockState state, BlockView world, BlockPos pos) {
        return this.getShape(state);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return getShape(state);
    }

    protected VoxelShape getShape(BlockState state) {
        return Block.createCuboidShape(0, 0, 0, 16, 2, 16);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;

        if (!(world.getBlockEntity(pos) instanceof PlateBlockEntity blockEntity)) {
            return ActionResult.PASS;
        }

        if (player.isCreative()) {
            ItemStack heldItem = player.getStackInHand(Hand.MAIN_HAND);

            if (!heldItem.isEmpty()) {
                blockEntity.addItem(heldItem);
                return ActionResult.SUCCESS;
            }
        } else if (player.getStackInHand(Hand.MAIN_HAND).isEmpty()) {
            List<ItemStack> platter = blockEntity.getStoredItems();
            if (platter.isEmpty()) return ActionResult.SUCCESS;

            boolean hasPlatterItem = false;
            for (ItemStack item : platter) {
                if (player.getInventory().contains(item)) {
                    hasPlatterItem = true;
                    break;
                }
            }

            if (!hasPlatterItem) {
                ItemStack randomItem = platter.get(world.random.nextInt(platter.size()));
                player.playSoundToPlayer(SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 1f, 1f);
                player.setStackInHand(Hand.MAIN_HAND, randomItem);
            }
        }

        return ActionResult.PASS;
    }
}
