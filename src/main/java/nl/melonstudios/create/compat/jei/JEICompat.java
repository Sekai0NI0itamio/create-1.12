package nl.melonstudios.create.compat.jei;

import mcp.MethodsReturnNonnullByDefault;
import mezz.jei.api.*;
import mezz.jei.api.ingredients.IIngredientRegistry;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import mezz.jei.api.recipe.transfer.IRecipeTransferRegistry;
import net.minecraft.item.ItemStack;
import nl.melonstudios.create.init.BlockInit;
import nl.melonstudios.create.init.ItemInit;

import javax.annotation.ParametersAreNonnullByDefault;

@JEIPlugin
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class JEICompat implements IModPlugin {
    @Override
    public void registerItemSubtypes(ISubtypeRegistry registry) {
        registry.registerSubtypeInterpreter(BlockInit.SAIL_ITEM, (stack) -> ISubtypeRegistry.ISubtypeInterpreter.NONE);
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        final IJeiHelpers helpers = registry.getJeiHelpers();
        final IGuiHelper gui = helpers.getGuiHelper();

        registry.addRecipeCategories(new PressingRecipeCategory(gui));
        registry.addRecipeCategories(new CrushingRecipeCategory(gui));
        registry.addRecipeCategories(new SandingRecipeCategory(gui));
        registry.addRecipeCategories(new CuttingRecipeCategory(gui));
        registry.addRecipeCategories(new DeployerRecipeCategory(gui));
    }

    @Override
    public void register(IModRegistry registry) {
        final IIngredientRegistry ingredientRegistry = registry.getIngredientRegistry();
        final IJeiHelpers jeiHelpers = registry.getJeiHelpers();

        IRecipeTransferRegistry recipeTransfer = registry.getRecipeTransferRegistry();

        registry.addRecipes(RecipeMaker.getPressingRecipes(jeiHelpers), "create.pressing");
        registry.addRecipeCatalyst(new ItemStack(BlockInit.PRESS), "create.pressing");

        registry.addRecipes(RecipeMaker.getCrushingRecipes(jeiHelpers), "create.crushing");
        registry.addRecipeCatalyst(new ItemStack(BlockInit.CRUSHING_WHEEL), "create.crushing");

        registry.addRecipes(RecipeMaker.getSandingRecipes(jeiHelpers), "create.sanding");
        registry.addRecipeCatalyst(new ItemStack(ItemInit.SANDPAPER), "create.sanding");

        registry.addRecipes(RecipeMaker.getCuttingRecipes(jeiHelpers), "create.cutting");
        registry.addRecipeCatalyst(new ItemStack(BlockInit.SAW), "create.cutting");

        //registry.addRecipes(RecipeMaker.getDeployingRecipes(jeiHelpers), "create.deploying");
        //registry.addRecipeCatalyst(new ItemStack(BlockInit.DEPLOYER), "create.deploying");
    }
}
