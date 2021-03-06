package com.teamresourceful.resourcefulbees.mixin;

import com.teamresourceful.resourcefulbees.tileentity.TieredBeehiveTileEntity;
import com.teamresourceful.resourcefulbees.tileentity.multiblocks.apiary.ApiaryTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Bee.class)
public abstract class MixinBeeEntity extends Animal {

    protected MixinBeeEntity(EntityType<? extends Animal> entityType, Level world) {
        super(entityType, world);
    }

    @Shadow
    public BlockPos hivePos;

    @Shadow
    public boolean hasHive() {
        return this.hivePos != null;
    }

    @Inject(at = @At("HEAD"), method = "doesHiveHaveSpace(Lnet/minecraft/core/BlockPos;)Z", cancellable = true)
    private void doesHiveHaveSpace(BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
        BlockEntity blockEntity = this.level.getBlockEntity(pos);
        if ((blockEntity instanceof TieredBeehiveTileEntity && !((TieredBeehiveTileEntity) blockEntity).isFull())
                || (blockEntity instanceof ApiaryTileEntity && ((ApiaryTileEntity) blockEntity).hasSpace())
                || (blockEntity instanceof BeehiveBlockEntity && !((BeehiveBlockEntity) blockEntity).isFull())) {
            callback.setReturnValue(true);
        }
    }

    @Inject(at = @At("HEAD"), method = "isHiveValid()Z", cancellable = true)
    public void isHiveValid(CallbackInfoReturnable<Boolean> callback) {
        if (this.hasHive()) {
            BlockPos pos = this.hivePos;
            if (pos != null) {
                BlockEntity blockEntity = this.level.getBlockEntity(this.hivePos);
                if ((blockEntity instanceof TieredBeehiveTileEntity && ((TieredBeehiveTileEntity) blockEntity).isAllowedBee())
                        || (blockEntity instanceof ApiaryTileEntity && ((ApiaryTileEntity) blockEntity).isAllowedBee())) {
                    callback.setReturnValue(true);
                }
            }
        }
    }
}
