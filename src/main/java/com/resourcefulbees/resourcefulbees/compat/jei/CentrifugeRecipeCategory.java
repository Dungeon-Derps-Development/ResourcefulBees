package com.resourcefulbees.resourcefulbees.compat.jei;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.resourcefulbees.resourcefulbees.ResourcefulBees;
import com.resourcefulbees.resourcefulbees.recipe.CentrifugeRecipe;
import com.resourcefulbees.resourcefulbees.registry.ModItems;
import com.resourcefulbees.resourcefulbees.tileentity.CentrifugeTileEntity;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IGuiFluidStackGroup;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fluids.FluidStack;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CentrifugeRecipeCategory implements IRecipeCategory<CentrifugeRecipe> {

    public static final ResourceLocation ID = new ResourceLocation(ResourcefulBees.MOD_ID, "centrifuge");
    protected final IDrawableAnimated arrow;
    private final IDrawable icon;
    private final IDrawable background;
    private final IDrawable multiblock;
    private final String localizedName;

    public CentrifugeRecipeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(new ResourceLocation(ResourcefulBees.MOD_ID, "textures/gui/jei/centrifuge.png"), 0, 0, 133, 65);
        this.icon = guiHelper.createDrawableIngredient(new ItemStack(ModItems.CENTRIFUGE_ITEM.get()));
        this.localizedName = I18n.format("gui.resourcefulbees.jei.category.centrifuge");
        this.arrow = guiHelper.drawableBuilder(new ResourceLocation(ResourcefulBees.MOD_ID, "textures/gui/jei/centrifuge.png"), 0, 66, 73, 30)
                .buildAnimated(200, IDrawableAnimated.StartDirection.LEFT, false);
        this.multiblock = guiHelper.createDrawable(new ResourceLocation(ResourcefulBees.MOD_ID, "textures/gui/jei/icons.png"), 25, 0, 16, 16);
    }

    @Override
    public @NotNull ResourceLocation getUid() {
        return ID;
    }

    @Override
    public @NotNull Class<? extends CentrifugeRecipe> getRecipeClass() {
        return CentrifugeRecipe.class;
    }

    @Override
    public @NotNull String getTitle() {
        return localizedName;
    }

    @Override
    public @NotNull IDrawable getBackground() {
        return background;
    }

    @Override
    public @NotNull IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setIngredients(CentrifugeRecipe recipe, IIngredients iIngredients) {
        iIngredients.setInputIngredients(Lists.newArrayList(recipe.ingredient, Ingredient.fromItems(Items.GLASS_BOTTLE)));
        List<Pair<ItemStack, Float>> outputs = recipe.itemOutputs;
        List<Pair<FluidStack, Float>> fluidOutput = recipe.fluidOutput;

        List<ItemStack> stacks = new ArrayList<>();
        if (recipe.hasFluidOutput) {
            stacks.add(outputs.get(1).getLeft().copy());
            stacks.add(outputs.get(2).getLeft().copy());
            iIngredients.setOutputs(VanillaTypes.ITEM, stacks);
            List<FluidStack> fluids = new ArrayList<>();
            fluids.add(fluidOutput.get(0).getLeft().copy());
            iIngredients.setOutputs(VanillaTypes.FLUID, fluids);
        } else {
            stacks.add(outputs.get(0).getLeft().copy());
            stacks.add(outputs.get(1).getLeft().copy());
            stacks.add(outputs.get(2).getLeft().copy());
            iIngredients.setOutputs(VanillaTypes.ITEM, stacks);
        }
    }

    @Override
    public void setRecipe(IRecipeLayout iRecipeLayout, CentrifugeRecipe centrifugeRecipe, IIngredients iIngredients) {
        IGuiItemStackGroup guiItemStacks = iRecipeLayout.getItemStacks();
        IGuiFluidStackGroup guiFluidStacks = iRecipeLayout.getFluidStacks();

        guiItemStacks.init(CentrifugeTileEntity.HONEYCOMB_SLOT, true, 9, 5);
        guiItemStacks.init(CentrifugeTileEntity.BOTTLE_SLOT, true, 9, 23);

        guiItemStacks.set(CentrifugeTileEntity.HONEYCOMB_SLOT, iIngredients.getInputs(VanillaTypes.ITEM).get(0));
        guiItemStacks.set(CentrifugeTileEntity.BOTTLE_SLOT, iIngredients.getInputs(VanillaTypes.ITEM).get(1));

        if (centrifugeRecipe.hasFluidOutput) {
            guiFluidStacks.init(CentrifugeTileEntity.OUTPUT1, false, 109, 6, 16, 16, iIngredients.getOutputs(VanillaTypes.FLUID).get(0).get(0).getAmount(), true, null);
            guiFluidStacks.set(CentrifugeTileEntity.OUTPUT1, iIngredients.getOutputs(VanillaTypes.FLUID).get(0));

            guiItemStacks.init(CentrifugeTileEntity.OUTPUT2, false, 108, 23);
            guiItemStacks.init(CentrifugeTileEntity.HONEY_BOTTLE, false, 59, 44);

            guiItemStacks.set(CentrifugeTileEntity.OUTPUT2, iIngredients.getOutputs(VanillaTypes.ITEM).get(0));
            guiItemStacks.set(CentrifugeTileEntity.HONEY_BOTTLE, iIngredients.getOutputs(VanillaTypes.ITEM).get(1));
        } else {
            guiItemStacks.init(CentrifugeTileEntity.OUTPUT1, false, 108, 5);
            guiItemStacks.init(CentrifugeTileEntity.OUTPUT2, false, 108, 23);
            guiItemStacks.init(CentrifugeTileEntity.HONEY_BOTTLE, false, 59, 44);

            guiItemStacks.set(CentrifugeTileEntity.OUTPUT1, iIngredients.getOutputs(VanillaTypes.ITEM).get(0));
            guiItemStacks.set(CentrifugeTileEntity.OUTPUT2, iIngredients.getOutputs(VanillaTypes.ITEM).get(1));
            guiItemStacks.set(CentrifugeTileEntity.HONEY_BOTTLE, iIngredients.getOutputs(VanillaTypes.ITEM).get(2));
        }
    }

    @Override
    public void draw(CentrifugeRecipe recipe, @NotNull MatrixStack matrix, double mouseX, double mouseY) {
        this.arrow.draw(matrix, 31, 14);

        final float beeOutput = recipe.itemOutputs.get(0).getRight();
        final float beeswax = recipe.itemOutputs.get(1).getRight();
        final float honeyBottle = recipe.itemOutputs.get(2).getRight();

        DecimalFormat decimalFormat = new DecimalFormat("##%");

        String honeyBottleString = decimalFormat.format(honeyBottle);
        String beeOutputString = decimalFormat.format(beeOutput);
        String beeswaxString = decimalFormat.format(beeswax);

        Minecraft minecraft = Minecraft.getInstance();
        FontRenderer fontRenderer = minecraft.fontRenderer;
        if (beeOutput < 1.0) fontRenderer.draw(matrix, beeOutputString, 80, 10, 0xff808080);
        if (honeyBottle < 1.0) fontRenderer.draw(matrix, honeyBottleString, 80, 50, 0xff808080);
        if (beeswax < 1.0) fontRenderer.draw(matrix, beeswaxString, 80, 30, 0xff808080);
        if (recipe.multiblock) {
            multiblock.draw(matrix, 10, 45);
        }


    }

    @Override
    public @NotNull List<ITextComponent> getTooltipStrings(@NotNull CentrifugeRecipe recipe, double mouseX, double mouseY) {
        if (mouseX >= 10 && mouseX <= 26 && mouseY >= 45 && mouseY <= 61) {
            return Collections.singletonList(new StringTextComponent("Multiblock only recipe."));
        }
        return IRecipeCategory.super.getTooltipStrings(recipe, mouseX, mouseY);
    }
}
