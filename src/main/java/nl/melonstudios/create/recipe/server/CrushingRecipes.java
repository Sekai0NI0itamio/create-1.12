package nl.melonstudios.create.recipe.server;

import com.google.common.collect.ImmutableList;
import com.melonstudios.melonlib.recipe.ISyncedRecipeType;
import com.melonstudios.melonlib.recipe.Ingredient;
import com.melonstudios.melonlib.recipe.RecipeException;
import com.melonstudios.melonlib.recipe.UniversalRecipe;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Tuple;
import nl.melonstudios.create.recipe.PulverizationRecipe;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Crushing-wheel recipes. Same data shape as milling (PulverizationRecipe:
 * input + chanced outputs + processing time) but a separate registry, since
 * official crushing has asterisk recipes the millstone cannot do.
 */
public class CrushingRecipes implements ISyncedRecipeType<PulverizationRecipe> {
    public static final CrushingRecipes instance = new CrushingRecipes();

    private CrushingRecipes() {

    }

    public final HashMap<String, PulverizationRecipe> recipes = new HashMap<>();

    @SafeVarargs
    public final void addRecipe(String recipeID, Ingredient input, Tuple<ItemStack, Float>... results) {
        this.addRecipe(recipeID, input, 100, results);
    }
    @SafeVarargs
    public final void addRecipe(String recipeID, Ingredient input, int processingTime, Tuple<ItemStack, Float>... results) {
        if (results.length == 0) throw new IllegalArgumentException("Cannot have recipes with no results!");
        this.recipes.put(recipeID, new PulverizationRecipe(input, ImmutableList.copyOf(results), processingTime));
    }

    @Override
    public final void removeRecipe(@Nonnull String recipeID) {
        this.recipes.remove(recipeID);
    }

    @Nonnull
    @Override
    public Collection<String> getAllRecipeIDs() {
        return this.recipes.keySet();
    }

    @Nonnull
    @Override
    public Collection<PulverizationRecipe> getAllRecipes() {
        return this.recipes.values();
    }

    @Nonnull
    @Override
    public Map<String, PulverizationRecipe> getRecipeMap() {
        return this.recipes;
    }

    @Nonnull
    @Override
    public PulverizationRecipe convert(@Nonnull UniversalRecipe universal) throws RecipeException {
        return null;
    }

    @Override
    public void addRecipe(@Nonnull String recipeID, @Nonnull PulverizationRecipe recipe) {
        this.recipes.put(recipeID, recipe);
    }

    @Nonnull
    @Override
    public PulverizationRecipe getRecipe(@Nonnull String recipeID) {
        return this.recipes.get(recipeID);
    }

    @Override
    public boolean hasRecipe(@Nonnull String recipeID) {
        return this.recipes.containsKey(recipeID);
    }

    public PulverizationRecipe getRecipeForInput(ItemStack input) {
        for (PulverizationRecipe recipe : this.recipes.values()) {
            if (recipe.input.test(input)) return recipe;
        }
        // Reference falls back to milling recipes when nothing crushes.
        return MillingRecipes.instance.getRecipeForInput(input);
    }

    @Override
    public void write(PulverizationRecipe pulverizationRecipe, ByteBuf byteBuf) throws IOException {

    }

    @Override
    public PulverizationRecipe read(String s, ByteBuf byteBuf) throws IOException {
        return null;
    }
}
