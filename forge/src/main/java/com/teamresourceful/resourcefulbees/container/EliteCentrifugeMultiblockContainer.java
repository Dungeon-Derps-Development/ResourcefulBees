package com.teamresourceful.resourcefulbees.container;

import com.teamresourceful.resourcefulbees.registry.ModContainers;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.Level;

public class EliteCentrifugeMultiblockContainer extends CentrifugeContainer {

    public EliteCentrifugeMultiblockContainer(int id, Level world, BlockPos pos, Inventory inv) {
        this(ModContainers.ELITE_CENTRIFUGE_MULTIBLOCK_CONTAINER.get(), id, world, pos, inv, new SimpleContainerData(6));
    }

    public EliteCentrifugeMultiblockContainer(MenuType<?> containerType, int id, Level world, BlockPos pos, Inventory inv, ContainerData times) {
        super(containerType, id, world, pos, inv, times);
    }

    @Override
    protected void initialize() {
        inputXPos = 53;
        outputXPos = 44;
    }
}

