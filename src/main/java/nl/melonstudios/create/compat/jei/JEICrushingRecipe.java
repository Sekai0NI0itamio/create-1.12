package nl.melonstudios.create.compat.jei;

import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.item.ItemStack;
import nl.melonstudios.create.recipe.PulverizationRecipe;
import nl.melonstudios.create.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class JEICrushingRecipe implements IRecipeWrapper {
    public final PulverizationRecipe recipe;
    public final List<ItemStack> outputs = new ArrayList<>();

    public JEICrushingRecipe(PulverizationRecipe recipe) {
        this.recipe = recipe;
        for (net.minecraft.util.Tuple<ItemStack, Float> t : recipe.results) {
            if (!t.getFirst().isEmpty()) outputs.add(t.getFirst().copy());
        }
    }

    @Override
    public void getIngredients(IIngredients iIngredients) {
        iIngredients.setInputLists(VanillaTypes.ITEM,
                java.util.Collections.singletonList(this.recipe.input.getDisplayItems()));
        iIngredients.setOutputs(VanillaTypes.ITEM, this.outputs);
    }
}
