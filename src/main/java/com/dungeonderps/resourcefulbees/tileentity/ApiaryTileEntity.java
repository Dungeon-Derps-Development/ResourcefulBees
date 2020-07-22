package com.dungeonderps.resourcefulbees.tileentity;


import com.dungeonderps.resourcefulbees.block.ApiaryBlock;
import com.dungeonderps.resourcefulbees.container.AutomationSensitiveItemStackHandler;
import com.dungeonderps.resourcefulbees.container.UnvalidatedApiaryContainer;
import com.dungeonderps.resourcefulbees.container.ValidatedApiaryContainer;
import com.dungeonderps.resourcefulbees.entity.passive.CustomBeeEntity;
import com.dungeonderps.resourcefulbees.item.BeeJar;
import com.dungeonderps.resourcefulbees.lib.BeeConstants;
import com.dungeonderps.resourcefulbees.network.NetPacketHandler;
import com.dungeonderps.resourcefulbees.network.packets.UpdateClientApiaryMessage;
import com.dungeonderps.resourcefulbees.registry.RegistryHandler;
import com.dungeonderps.resourcefulbees.utils.BeeInfoUtils;
import net.minecraft.block.BeehiveBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tags.Tag;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class ApiaryTileEntity extends TileEntity implements ITickableTileEntity, INamedContainerProvider {
    public static final int IMPORT = 0;
    public static final int EXPORT = 2;
    public static final int EMPTY_JAR = 1;
    public final LinkedHashMap<String, ApiaryBee> BEES = new LinkedHashMap<>();
    public final List<BlockPos> STRUCTURE_BLOCKS = new ArrayList<>();
    protected final int TIER = 4;
    protected final float TIER_MODIFIER = 5;
    private final Tag<Block> validApiaryTag;
    public Stack<String> honeycombs = new Stack<>();
    public ApiaryStorageTileEntity apiaryStorage;
    public boolean isValidApiary;
    public boolean previewed;
    public AutomationSensitiveItemStackHandler h = new ApiaryTileEntity.TileStackHandler(4);
    public LazyOptional<IItemHandler> lazyOptional = LazyOptional.of(() -> h);
    public int horizontalOffset = 0;
    public int verticalOffset = 0;
    public int numPlayersUsing;
    private int ticksSinceValidation = 290;
    private int ticksSinceSync;
    public BlockPos apiaryStoragePos;


    public ApiaryTileEntity() {
        super(RegistryHandler.APIARY_TILE_ENTITY.get());
        validApiaryTag = BeeInfoUtils.getBlockTag("resourcefulbees:valid_apiary");
    }

    public static int calculatePlayersUsingSync(World world, ApiaryTileEntity apiaryTileEntity, int ticksSinceSync, int posX, int posY, int posZ, int numPlayersUsing) {
        if (!world.isRemote && numPlayersUsing != 0 && (ticksSinceSync + posX + posY + posZ) % 200 == 0) {
            numPlayersUsing = calculatePlayersUsing(world, apiaryTileEntity, posX, posY, posZ);
        }

        return numPlayersUsing;
    }

    public static int calculatePlayersUsing(World world, ApiaryTileEntity apiaryTileEntity, int posX, int posY, int posZ) {
        int i = 0;
        float f = 5.0F;

        for (PlayerEntity playerentity : world.getEntitiesWithinAABB(PlayerEntity.class, new AxisAlignedBB(posX - f, posY - f, posZ - f, (posX + 1) + f, (posY + 1) + f, (posZ + 1) + f))) {
            if (playerentity.openContainer instanceof ValidatedApiaryContainer) {
                ApiaryTileEntity apiaryTileEntity1 = ((ValidatedApiaryContainer) playerentity.openContainer).apiaryTileEntity;
                if (apiaryTileEntity1 == apiaryTileEntity) {
                    ++i;
                }
            }
        }

        return i;
    }

    public static void syncApiaryToPlayersUsing(World world, BlockPos pos, CompoundNBT data) {
        NetPacketHandler.sendToAllLoaded(new UpdateClientApiaryMessage(pos, data), world, pos);
    }

    public int getTier() {
        return TIER;
    }

    public float getTierModifier() {
        return TIER_MODIFIER;
    }

    public int getMaxBees() {
        return 9;
    }

    public int getBeeCount() {
        return this.BEES.size();
    }

    public boolean releaseBee(@Nonnull BlockState state, @Nonnull CompoundNBT nbt, @Nonnull State beehiveState, @Nullable BlockPos flowerPos, boolean exportBee) {
        BlockPos blockpos = this.getPos();
        if (exportBee || !shouldStayInHive(beehiveState)) {
            nbt.remove("Passengers");
            nbt.remove("Leash");
            nbt.removeUniqueId("UUID");
            Direction direction = state.get(BeehiveBlock.FACING);
            BlockPos blockpos1 = blockpos.offset(direction);
            if (world == null || this.world.getBlockState(blockpos1).getCollisionShape(this.world, blockpos1).isEmpty()) {
                Entity entity = EntityType.loadEntityAndExecute(nbt, this.world, entity1 -> entity1);
                if (entity != null) {
                    float f = entity.getWidth();
                    double d0 = 0.55D + f / 2.0F;
                    double d1 = blockpos.getX() + 0.5D + d0 * direction.getXOffset();
                    double d2 = blockpos.getY() + 0.5D - (entity.getHeight() / 2.0F);
                    double d3 = blockpos.getZ() + 0.5D + d0 * direction.getZOffset();
                    entity.setLocationAndAngles(d1, d2, d3, entity.rotationYaw, entity.rotationPitch);
                    if (entity instanceof CustomBeeEntity) {
                        CustomBeeEntity beeEntity = (CustomBeeEntity) entity;
                        if (flowerPos != null && !beeEntity.hasFlower() && this.world.rand.nextFloat() < 0.9F) {
                            beeEntity.setFlowerPos(flowerPos);
                        }

                        if (beehiveState == State.HONEY_DELIVERED) {
                            beeEntity.onHoneyDelivered();
                            if (!exportBee && !beeEntity.getBeeInfo().getMainOutput().isEmpty() && isValidApiary) {
                                apiaryStorage.deliverHoneycomb(beeEntity.getBeeType(), getTier());
                            }
                        }

                        beeEntity.resetTicksWithoutNectar();

                        if (exportBee) {
                            export(beeEntity);
                        } else {
                            BlockPos hivePos = this.getPos();
                            this.world.playSound(null, hivePos.getX(), hivePos.getY(), hivePos.getZ(), SoundEvents.BLOCK_BEEHIVE_EXIT, SoundCategory.BLOCKS, 1.0F, 1.0F);
                            this.world.addEntity(entity);
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean tryEnterHive(Entity bee, boolean hasNectar, boolean imported) {
        if (isValidApiary)
            return this.tryEnterHive(bee, hasNectar, 0, imported);
        return false;
    }

    public boolean tryEnterHive(@Nonnull Entity bee, boolean hasNectar, int ticksInHive, boolean imported) {
        if (this.world != null) {
            if (bee instanceof CustomBeeEntity) {
                CustomBeeEntity bee1 = (CustomBeeEntity) bee;
                String type = bee1.getBeeType();

                if (!this.BEES.containsKey(type) && this.BEES.size() < getMaxBees()) {
                    bee.removePassengers();
                    CompoundNBT nbt = new CompoundNBT();
                    bee.writeUnlessPassenger(nbt);

                    int maxTimeInHive = bee1.getBeeInfo().getMaxTimeInHive();
                    maxTimeInHive = (int) (maxTimeInHive * (1 - (0.30F + this.getTier() * .05)));
                    int finalMaxTimeInHive = 200; //maxTimeInHive; TODO revert back before publishing!

                    this.BEES.computeIfAbsent(bee1.getBeeType(), k -> new ApiaryBee(nbt, ticksInHive, hasNectar ? finalMaxTimeInHive : BeeConstants.MIN_HIVE_TIME, bee1.getFlowerPos(), bee1.getBeeType()));
                    BlockPos pos = this.getPos();
                    this.world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BLOCK_BEEHIVE_ENTER, SoundCategory.BLOCKS, 1.0F, 1.0F);

                    if (imported) {
                        this.BEES.get(bee1.getBeeType()).isLocked = true;
                    }

                    if (this.numPlayersUsing > 0)
                        syncApiaryToPlayersUsing(this.world, this.getPos(), this.saveToNBT(new CompoundNBT()));

                    bee.remove();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void tick() {
        if (world != null) {
            BlockPos blockpos = this.getPos();
            int x = blockpos.getX();
            int y = blockpos.getY();
            int z = blockpos.getZ();
            ++ticksSinceSync;
            this.numPlayersUsing = calculatePlayersUsingSync(this.world, this, this.ticksSinceSync, x, y, z, this.numPlayersUsing);

            this.tickBees();

            if (!world.isRemote && isValidApiary) {
                if (ticksSinceValidation >= 300)
                    runStructureValidation(null);
                else
                    ticksSinceValidation++;
                if (this.BEES.size() > 0 && this.world.getRandom().nextDouble() < 0.005D) {
                    double d0 = blockpos.getX() + 0.5D;
                    double d1 = blockpos.getY();
                    double d2 = blockpos.getZ() + 0.5D;
                    this.world.playSound(null, d0, d1, d2, SoundEvents.BLOCK_BEEHIVE_WORK, SoundCategory.BLOCKS, 1.0F, 1.0F);
                }
            }
        }
    }

    private void tickBees() {
        if (this.world != null) {
            Iterator<Map.Entry<String, ApiaryBee>> iterator = this.BEES.entrySet().iterator();
            BlockState blockstate = this.getBlockState();

            while (iterator.hasNext()) {
                Map.Entry<String, ApiaryBee> element = iterator.next();
                ApiaryBee apiaryBee = element.getValue();
                if (!apiaryBee.isLocked && apiaryBee.ticksInHive > apiaryBee.minOccupationTicks && !world.isRemote) {

                    CompoundNBT compoundnbt = apiaryBee.entityData;
                    State state = compoundnbt.getBoolean("HasNectar") ? State.HONEY_DELIVERED : State.BEE_RELEASED;
                    if (this.releaseBee(blockstate, compoundnbt, state, apiaryBee.savedFlowerPos, false)) {
                        iterator.remove();
                        if (this.numPlayersUsing > 0 && !this.world.isRemote)
                            syncApiaryToPlayersUsing(this.world, this.getPos(), this.saveToNBT(new CompoundNBT()));
                    }
                } else {
                    apiaryBee.ticksInHive++;
                }
            }
        }
    }

    @Nonnull
    public ListNBT writeBees() {
        ListNBT listnbt = new ListNBT();

        this.BEES.forEach((key, apiaryBee) -> {
            apiaryBee.entityData.removeUniqueId("UUID");
            CompoundNBT compoundnbt = new CompoundNBT();
            compoundnbt.put("EntityData", apiaryBee.entityData);
            compoundnbt.putInt("TicksInHive", apiaryBee.ticksInHive);
            compoundnbt.putInt("MinOccupationTicks", apiaryBee.minOccupationTicks);
            compoundnbt.putBoolean("Locked", apiaryBee.isLocked);
            compoundnbt.putString(BeeConstants.NBT_BEE_TYPE, apiaryBee.beeType);
            if (apiaryBee.savedFlowerPos != null) {
                compoundnbt.put("FlowerPos", NBTUtil.writeBlockPos(apiaryBee.savedFlowerPos));
            }
            listnbt.add(compoundnbt);
        });

        return listnbt;
    }

    public boolean shouldStayInHive(State beehiveState) {
        if (world != null)
            return (this.world.isNightTime() || this.world.isRaining()) && beehiveState != State.EMERGENCY;
        return false;
    }

    public boolean isFullOfBees() {
        return this.BEES.size() >= getMaxBees();
    }

    public boolean isAllowedBee() {
        Block hive = getBlockState().getBlock();
        return isValidApiary && hive instanceof ApiaryBlock;
    }

    @Override
    public void read(@Nonnull CompoundNBT nbt) {
        super.read(nbt);
        this.loadFromNBT(nbt);
    }

    @Nonnull
    @Override
    public CompoundNBT write(@Nonnull CompoundNBT nbt) {
        super.write(nbt);
        return this.saveToNBT(nbt);
    }

    public void loadFromNBT(CompoundNBT nbt) {
        ListNBT listnbt = nbt.getList("Bees", 10);

        for (int i = 0; i < listnbt.size(); ++i) {
            CompoundNBT data = listnbt.getCompound(i);

            BlockPos savedFlowerPos = data.contains("FlowerPos") ? NBTUtil.readBlockPos(data.getCompound("FlowerPos")) : null;

            String beeType = data.getString(BeeConstants.NBT_BEE_TYPE);

            this.BEES.computeIfAbsent(data.getString(BeeConstants.NBT_BEE_TYPE), k -> new ApiaryBee(
                    data.getCompound("EntityData"),
                    data.getInt("TicksInHive"),
                    data.getInt("MinOccupationTicks"),
                    savedFlowerPos,
                    beeType));

            this.BEES.get(beeType).isLocked = data.getBoolean("Locked");
        }

        if (nbt.contains("isValid"))
            this.isValidApiary = nbt.getBoolean("isValid");
        if (nbt.contains("verticalOffset"))
            this.verticalOffset = nbt.getInt("verticalOffset");
        if (nbt.contains("horizontalOffset"))
            this.horizontalOffset = nbt.getInt("horizontalOffset");
        CompoundNBT invTag = nbt.getCompound("inv");
        h.deserializeNBT(invTag);

        apiaryStoragePos = NBTUtil.readBlockPos(nbt.getCompound("StoragePos"));
    }

    public CompoundNBT saveToNBT(CompoundNBT nbt) {
        CompoundNBT inv = this.h.serializeNBT();
        nbt.put("inv", inv);
        nbt.put("Bees", this.writeBees());
        nbt.putBoolean("isValid", isValidApiary);
        nbt.putInt("verticalOffset", verticalOffset);
        nbt.putInt("horizontalOffset", horizontalOffset);
        nbt.put("StoragePos", NBTUtil.writeBlockPos(apiaryStoragePos));
        return nbt;
    }

    @Nonnull
    @Override
    public CompoundNBT getUpdateTag() {
        CompoundNBT nbtTagCompound = new CompoundNBT();
        write(nbtTagCompound);
        return nbtTagCompound;
    }

    @Override
    public void handleUpdateTag(CompoundNBT tag) {
        this.read(tag);
    }

    public void lockOrUnlockBee(String beeType) {
        this.BEES.get(beeType).isLocked = !this.BEES.get(beeType).isLocked;
        syncApiaryToPlayersUsing(this.world, this.getPos(), this.saveToNBT(new CompoundNBT()));
    }

    public void importBee(ServerPlayerEntity player) {
        World world = this.world;
        boolean imported = false;

        if (world != null) {
            if (!this.h.getStackInSlot(IMPORT).isEmpty() && this.h.getStackInSlot(EMPTY_JAR).getCount() < 16) {
                ItemStack filledJar = this.h.getStackInSlot(IMPORT);
                ItemStack emptyJar = this.h.getStackInSlot(EMPTY_JAR);

                if (filledJar.getItem() instanceof BeeJar) {
                    BeeJar jarItem = (BeeJar) filledJar.getItem();
                    Entity entity = jarItem.getEntityFromStack(filledJar, world, true);

                    if (entity instanceof CustomBeeEntity) {
                        CustomBeeEntity beeEntity = (CustomBeeEntity) entity;
                        imported = tryEnterHive(beeEntity, beeEntity.hasNectar(), true);

                        if (imported) {
                            filledJar.shrink(1);
                            if (emptyJar.isEmpty()) {
                                this.h.setStackInSlot(EMPTY_JAR, new ItemStack(jarItem));
                            } else {
                                emptyJar.grow(1);
                            }
                        }
                    }
                }
            }
        }

        player.sendStatusMessage(new TranslationTextComponent("gui.resourcefulbees.apiary.import." + imported), true);
    }

    public void exportBee(ServerPlayerEntity player, String beeType) {
        boolean exported = false;
        ApiaryBee bee = this.BEES.get(beeType);
        CompoundNBT data = bee.entityData;
        State state = data.getBoolean("HasNectar") ? State.HONEY_DELIVERED : State.BEE_RELEASED;

        if (bee.isLocked && h.getStackInSlot(EXPORT).isEmpty() && !h.getStackInSlot(EMPTY_JAR).isEmpty()) {
            exported = releaseBee(this.getBlockState(), data, state, bee.savedFlowerPos, true);
        }
        if (exported) {
            this.BEES.remove(beeType);
            this.h.getStackInSlot(EMPTY_JAR).shrink(1);
            if (this.numPlayersUsing > 0 && this.world != null && !this.world.isRemote)
                syncApiaryToPlayersUsing(this.world, this.getPos(), this.saveToNBT(new CompoundNBT()));
        }

        player.sendStatusMessage(new TranslationTextComponent("gui.resourcefulbees.apiary.export." + exported), true);
    }

    public void export(CustomBeeEntity beeEntity) {
        ItemStack beeJar = new ItemStack(RegistryHandler.BEE_JAR.get());
        CompoundNBT data = new CompoundNBT();
        data.putString(BeeConstants.NBT_ENTITY, "resourcefulbees:bee");
        beeEntity.writeWithoutTypeId(data);
        data.putString(BeeConstants.NBT_COLOR, beeEntity.getBeeInfo().getPrimaryColor());
        beeJar.setTag(data);
        this.h.setStackInSlot(EXPORT, beeJar);
    }

    public void runStructureValidation(@Nullable ServerPlayerEntity validatingPlayer) {
        if (this.world != null && !this.world.isRemote()) {
            if (!this.isValidApiary || STRUCTURE_BLOCKS.isEmpty())
                buildStructureBlockList();
            this.isValidApiary = validateStructure(this.world, validatingPlayer);
            this.world.setBlockState(this.getPos(), getBlockState().with(ApiaryBlock.VALIDATED, this.isValidApiary));
            if (validatingPlayer != null && this.isValidApiary) {
                NetworkHooks.openGui(validatingPlayer, this, this.getPos());
            }
            this.ticksSinceValidation = 0;
        }
    }

    public boolean validateStructure(World worldIn, @Nullable ServerPlayerEntity validatingPlayer) {
        boolean isStructureValid = true;
        apiaryStorage = null;
        for (BlockPos pos : STRUCTURE_BLOCKS) {
            Block block = worldIn.getBlockState(pos).getBlock();
            if (block.isIn(validApiaryTag)) {
                TileEntity tile = worldIn.getTileEntity(pos);
                if (tile instanceof ApiaryStorageTileEntity) {
                    if (apiaryStorage == null) {
                        apiaryStorage = (ApiaryStorageTileEntity) tile;
                        apiaryStoragePos = apiaryStorage.getPos();
                    }
                }
            } else {
                isStructureValid = false;
                if (validatingPlayer != null) {
                    validatingPlayer.sendStatusMessage(new StringTextComponent(String.format("Block at position (X: %1$s Y: %2$s Z: %3$s) is invalid!", pos.getX(), pos.getY(), pos.getZ())), false);
                }
            }
        }
        if (apiaryStorage == null){
            isStructureValid = false;
        }

        if (validatingPlayer != null) {
            validatingPlayer.sendMessage(new TranslationTextComponent("gui.resourcefulbees.apiary.validated." + isStructureValid));
        }

        return isStructureValid;
    }

    public MutableBoundingBox buildStructureBounds(int horizontalOffset, int verticalOffset) {
        MutableBoundingBox box;
        int posX = this.getPos().getX();
        int posY = this.getPos().getY();
        int posZ = this.getPos().getZ();

        switch (this.getBlockState().get(ApiaryBlock.FACING)) {
            case NORTH:
                posX -= 3 + horizontalOffset;
                posY -= 2 + verticalOffset;
                box = new MutableBoundingBox(posX, posY, posZ, posX + 6, posY + 5, posZ - 6);
                break;
            case EAST:
                posZ -= 3 + horizontalOffset;
                posY -= 2 + verticalOffset;
                box = new MutableBoundingBox(posX, posY, posZ, posX + 6, posY + 5, posZ + 6);
                break;
            case SOUTH:
                posX -= 3 - horizontalOffset;
                posY -= 2 + verticalOffset;
                box = new MutableBoundingBox(posX, posY, posZ, posX + 6, posY + 5, posZ + 6);
                break;
            default:
                posZ -= 3 - horizontalOffset;
                posY -= 2 + verticalOffset;
                box = new MutableBoundingBox(posX, posY, posZ, posX - 6, posY + 5, posZ + 6);
        }
        return box;
    }

    private void buildStructureBlockList() {
        if (this.world != null) {
            MutableBoundingBox box = buildStructureBounds(this.horizontalOffset, this.verticalOffset);
            STRUCTURE_BLOCKS.clear();
            BlockPos.getAllInBox(box).forEach((blockPos -> {
                if (blockPos.getX() == box.minX || blockPos.getX() == box.maxX ||
                        blockPos.getY() == box.minY || blockPos.getY() == box.maxY ||
                        blockPos.getZ() == box.minZ || blockPos.getZ() == box.maxZ) {
                    BlockPos savedPos = new BlockPos(blockPos.getX(), blockPos.getY(), blockPos.getZ());
                    STRUCTURE_BLOCKS.add(savedPos);
                    TileEntity tile = this.world.getTileEntity(blockPos);
                    if (tile instanceof ApiaryStorageTileEntity) {
                        apiaryStorage = (ApiaryStorageTileEntity) tile;
                    }
                }
            }));
        }
    }

    @Nullable
    @Override
    public Container createMenu(int i, @Nonnull PlayerInventory playerInventory, @Nonnull PlayerEntity playerEntity) {
        if (world != null)
            if (this.isValidApiary) {
                numPlayersUsing++;
                return new ValidatedApiaryContainer(i, world, pos, playerInventory);
            } else {
                numPlayersUsing++;
                return new UnvalidatedApiaryContainer(i, world, pos, playerInventory);
            }
        return null;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ? lazyOptional.cast() :
                super.getCapability(cap, side);
    }

    public AutomationSensitiveItemStackHandler.IAcceptor getAcceptor() {
        return (slot, stack, automation) -> !automation || slot == 0 || slot == 1;
    }

    public AutomationSensitiveItemStackHandler.IRemover getRemover() {
        return (slot, automation) -> !automation || slot == 2 || slot == 3 || slot == 4;
    }

    @Nonnull
    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("gui.resourcefulbees.apiary");
    }

    public void runCreativeBuild(ServerPlayerEntity player) {
        if (this.world != null) {
            buildStructureBlockList();
            for (BlockPos pos : STRUCTURE_BLOCKS) {
                Block block = this.world.getBlockState(pos).getBlock();
                if (!(block instanceof ApiaryBlock)) {
                    this.world.setBlockState(pos, Blocks.GLASS.getDefaultState());
                }
            }
            runStructureValidation(player);
        }
    }

    public enum State {
        HONEY_DELIVERED,
        BEE_RELEASED,
        EMERGENCY
    }

    public static class ApiaryBee {
        public final CompoundNBT entityData;
        public final int minOccupationTicks;
        public final BlockPos savedFlowerPos;
        public final String beeType;
        public int ticksInHive;
        public boolean isLocked = false;

        public ApiaryBee(CompoundNBT nbt, int ticksinhive, int minoccupationticks, @Nullable BlockPos flowerPos, String beeType) {
            nbt.removeUniqueId("UUID");
            this.entityData = nbt;
            this.ticksInHive = ticksinhive;
            this.minOccupationTicks = minoccupationticks;
            this.savedFlowerPos = flowerPos;
            this.beeType = beeType;
        }
    }

    protected class TileStackHandler extends AutomationSensitiveItemStackHandler {

        protected TileStackHandler(int slots) {
            super(slots);
        }

        @Override
        public AutomationSensitiveItemStackHandler.IAcceptor getAcceptor() {
            return ApiaryTileEntity.this.getAcceptor();
        }

        @Override
        public AutomationSensitiveItemStackHandler.IRemover getRemover() {
            return ApiaryTileEntity.this.getRemover();
        }

        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            markDirty();
        }
    }
}
