package com.resourcefulbees.resourcefulbees.item.dispenser;

import com.resourcefulbees.resourcefulbees.block.TieredBeehiveBlock;
import net.minecraft.block.BeehiveBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.DispenserBlock;
import net.minecraft.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.BeehiveTileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nonnull;

public class ScraperDispenserBehavior extends DefaultDispenseItemBehavior {

    @Nonnull
    @Override
    protected ItemStack dispenseStack(@Nonnull IBlockSource source, @Nonnull ItemStack stack) {
        ServerWorld world = source.getWorld();
        BlockPos blockpos = source.getBlockPos().offset(source.getBlockState().get(DispenserBlock.FACING));
        BlockState blockstate = world.getBlockState(blockpos);
        if (blockstate.getBlock() instanceof TieredBeehiveBlock) {
            int i = blockstate.get(BeehiveBlock.HONEY_LEVEL);
            if (i >= 5) {
                if (stack.attemptDamageItem(1, world.rand, null)) {
                    stack.setCount(0);
                }

                if (TieredBeehiveBlock.dropResourceHoneycomb((TieredBeehiveBlock) blockstate.getBlock(), world, blockpos, true)) {
                    ((BeehiveBlock) blockstate.getBlock()).takeHoney(world, blockstate, blockpos, null, BeehiveTileEntity.State.BEE_RELEASED);
                }
            }
        }
        return stack;
    }
}