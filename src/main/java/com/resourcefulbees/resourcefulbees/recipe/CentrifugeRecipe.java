
package com.resourcefulbees.resourcefulbees.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.resourcefulbees.resourcefulbees.ResourcefulBees;
import com.resourcefulbees.resourcefulbees.registry.ModRecipeSerializers;
import com.resourcefulbees.resourcefulbees.utils.BeeInfoUtils;
import com.resourcefulbees.resourcefulbees.utils.RecipeUtils;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class CentrifugeRecipe implements IRecipe<IInventory> {

    public static final IRecipeType<CentrifugeRecipe> CENTRIFUGE_RECIPE_TYPE = IRecipeType.register(ResourcefulBees.MOD_ID + ":centrifuge");
    public final ResourceLocation id;
    public final Ingredient ingredient;
    public final List<Pair<ItemStack,Float>> outputs;
    public final int time;
    public final boolean multiblock;

    public CentrifugeRecipe(ResourceLocation id, Ingredient ingredient, List<Pair<ItemStack,Float>> outputs, int time, boolean multiblock) {
        this.id = id;
        this.ingredient = ingredient;
        this.outputs = outputs;
        this.time = time;
        this.multiblock = multiblock;
    }

    @Override
    public boolean matches(IInventory inventory, @Nonnull World world) {
        ItemStack stack = inventory.getStackInSlot(0);
        if (!stack.equals(ItemStack.EMPTY)) {
            ItemStack[] matchingStacks = ingredient.getMatchingStacks();
            if (matchingStacks.length > 0) {
                for(ItemStack itemstack : matchingStacks) {
                    if (itemstack.getItem() == stack.getItem()) {
                        if (itemstack.hasTag() && stack.hasTag()){
                            return itemstack.getTag().equals(stack.getTag());
                        } else {
                            return !itemstack.hasTag() && !stack.hasTag();
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    @Override
    @Nonnull
    @Deprecated
    public ItemStack getCraftingResult(@Nonnull IInventory inventory) {
        return ItemStack.EMPTY;
    }

    /**
     * Used to determine if this recipe can fit in a grid of the given width/height
     */
    @Override
    public boolean canFit(int width, int height) {
        return true;
    }

    /**
     * Get the result of this recipe, usually for display purposes (e.g. recipe book). If your recipe has more than one
     * possible result (e.g. it's dynamic and depends on its inputs), then return an empty stack.
     */
    @Deprecated
    @Override
    @Nonnull
    public ItemStack getRecipeOutput() {
        return ItemStack.EMPTY;
    }

    @Nonnull
    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Nonnull
    @Override
    public IRecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.CENTRIFUGE_RECIPE.get();
    }

    @Nonnull
    @Override
    public IRecipeType<?> getType() {
        return CENTRIFUGE_RECIPE_TYPE;
    }

    public static class Serializer<T extends CentrifugeRecipe> extends net.minecraftforge.registries.ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<T> {
        final IRecipeFactory<T> factory;

        public Serializer(Serializer.IRecipeFactory<T> factory) {
            this.factory = factory;
        }

        @Nonnull
        @Override
        public T read(@Nonnull ResourceLocation id, @Nonnull JsonObject json) {
            Ingredient ingredient;
            if (JSONUtils.isJsonArray(json, "ingredient")) {
                ingredient = Ingredient.deserialize(JSONUtils.getJsonArray(json, "ingredient"));
            } else {
                ingredient = Ingredient.deserialize(JSONUtils.getJsonObject(json, "ingredient"));
            }

            JsonArray jsonArray = JSONUtils.getJsonArray(json, "results");
            List<Pair<ItemStack,Float>> outputs = new ArrayList<>();
            jsonArray.forEach(jsonElement -> {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                String registryname = JSONUtils.getString(jsonObject,"item");
                int count = JSONUtils.getInt(jsonObject,"count",1);
                Float chance = JSONUtils.getFloat(jsonObject, "chance", 1);
                ItemStack stack = new ItemStack(BeeInfoUtils.getItem(registryname),count);
                outputs.add(Pair.of(stack,chance));
            });

            int time = JSONUtils.getInt(json,"time");
            boolean multiblock = JSONUtils.getBoolean(json,"multiblock", false);

            return this.factory.create(id, ingredient, outputs,time, multiblock);
        }

        public T read(@Nonnull ResourceLocation id, @Nonnull PacketBuffer buffer) {
            Ingredient ingredient = Ingredient.read(buffer);
            List<Pair<ItemStack,Float>> outputs = new ArrayList<>();
            IntStream.range(0,buffer.readInt()).forEach(i -> outputs.add(Pair.of(RecipeUtils.readItemStack(buffer),buffer.readFloat())));
            int time = buffer.readInt();
            boolean multiblock = buffer.readBoolean();
            return this.factory.create(id, ingredient, outputs,time, multiblock);
        }

        public void write(@Nonnull PacketBuffer buffer, T recipe) {
            recipe.ingredient.write(buffer);
            buffer.writeInt(recipe.outputs.size());
            recipe.outputs.forEach(itemStackFloatPair -> {
                ItemStack stack = itemStackFloatPair.getLeft();
                RecipeUtils.writeItemStack(stack, buffer);
                buffer.writeFloat(itemStackFloatPair.getRight());
            });
            buffer.writeInt(recipe.time);
            buffer.writeBoolean(recipe.multiblock);
        }

        public interface IRecipeFactory<T extends CentrifugeRecipe> {
            T create(ResourceLocation id, Ingredient input, List<Pair<ItemStack,Float>> stacks,int time, boolean multiblock);
        }
    }
}

