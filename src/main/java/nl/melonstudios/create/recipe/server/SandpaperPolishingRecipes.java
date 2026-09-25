package nl.melonstudios.create.recipe.server;

import com.melonstudios.melonlib.misc.StackUtil;
import com.melonstudios.melonlib.recipe.ISyncedRecipeType;
import com.melonstudios.melonlib.recipe.Ingredient;
import com.melonstudios.melonlib.recipe.RecipeException;
import com.melonstudios.melonlib.recipe.UniversalRecipe;
import io.netty.buffer.ByteBuf;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import nl.melonstudios.create.init.ItemInit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Sandpaper polishing recipes (Create {@code create:sandpaper_polishing}, translated
 * schema: one item ingredient -&gt; one item result).
 * <p>
 * Shape mirrors {@link PressingRecipes}/{@code PressingRecipe} exactly (single
 * input, single result), because polishing is likewise a one-in/one-out
 * transformation. Deliberately separate from the deprecated
 * {@code SandingRecipes} NBT map so polishing participates in the synced
 * recipe registry like every other machine type.
 * <p>
 * Integration (NEEDS-LEAD, do not wire here): add a polishing branch to the
 * deployer {@code recipes:} block in {@code TileEntityDeployer.java:113-130}
 * (next to {@code DeployerRecipes.instance.getRecipeForInput(in, this.heldItem)}
 * at line 116) that fires when the held item is sandpaper
 * ({@code ItemInit.SANDPAPER}/{@code ItemInit.RED_SANDPAPER}) and
 * {@code SandpaperPolishingRecipes.getRecipeForInput(in, ...)} hits, damaging
 * the sandpaper instead of consuming it.
 * Registration (NEEDS-LEAD): {@code RecipeRegistry.registerServer("create:sandpaper_polishing", ...)}
 * in {@code CommonProxy.java:213-223}, client side in {@code ClientProxy.java:272-283},
 * accessor in {@code RecipeInit.java:1104-1118}, defaults call in {@code RecipeInit.init()}
 * ({@code RecipeInit.java:77-85}).
 */
public class SandpaperPolishingRecipes implements ISyncedRecipeType<SandpaperPolishingRecipes.Recipe> {
    public static final SandpaperPolishingRecipes instance = new SandpaperPolishingRecipes();

    private SandpaperPolishingRecipes() {

    }

    public final HashMap<String, Recipe> recipes = new HashMap<>();

    /** Starter set from the reference polishing table (1.12 items only). */
    public static void initDefaults() {
        SandpaperPolishingRecipes recipes = SandpaperPolishingRecipes.instance;
        recipes.addRecipe("create:polishing/rose_quartz",
                Ingredient.of(new ItemStack(ItemInit.INGREDIENT, 1, 3), false),
                new ItemStack(ItemInit.INGREDIENT, 1, 4));
        recipes.addRecipe("create:polishing/granite",
                Ingredient.of(new ItemStack(Blocks.STONE, 1, 1), false),
                new ItemStack(Blocks.STONE, 1, 2));
        recipes.addRecipe("create:polishing/diorite",
                Ingredient.of(new ItemStack(Blocks.STONE, 1, 3), false),
                new ItemStack(Blocks.STONE, 1, 4));
        recipes.addRecipe("create:polishing/andesite",
                Ingredient.of(new ItemStack(Blocks.STONE, 1, 5), false),
                new ItemStack(Blocks.STONE, 1, 6));
        recipes.addRecipe("create:polishing/prismarine_crystals",
                Ingredient.of(new ItemStack(Items.PRISMARINE_SHARD), false),
                new ItemStack(Items.PRISMARINE_CRYSTALS));
    }

    public final void addRecipe(String recipeID, Ingredient input, ItemStack result) {
        this.addRecipe(recipeID, new Recipe(input, result));
    }

    /**
     * Polishing lookup: single input match.
     * Iterates the server map directly until the lead registers the synced type
     * (then switch to a {@code RecipeInit.getPolishingRecipes(client)} accessor
     * like {@code PressingRecipes.getRecipeForInput} does).
     */
    @Nullable
    public static Recipe getRecipeForInput(ItemStack input) {
        if (input.isEmpty()) return null;
        for (Recipe recipe : instance.recipes.values()) {
            if (recipe.input.matches(input)) {
                return recipe;
            }
        }
        return null;
    }

    @Nonnull
    @Override
    public Collection<String> getAllRecipeIDs() {
        return this.recipes.keySet();
    }

    @Nonnull
    @Override
    public Collection<Recipe> getAllRecipes() {
        return this.recipes.values();
    }

    @Nonnull
    @Override
    public Map<String, Recipe> getRecipeMap() {
        return this.recipes;
    }

    @Nonnull
    @Override
    public Recipe convert(@Nonnull UniversalRecipe universal) throws RecipeException {
        try {
            return new Recipe(universal.itemInputs.get(0).get(0), universal.itemOutputs.get(0).get(0));
        } catch (Throwable e) {
            throw new RecipeException(e);
        }
    }

    @Override
    public void addRecipe(@Nonnull String recipeID, @Nonnull Recipe recipe) {
        this.recipes.put(recipeID, recipe);
    }

    @Override
    public Recipe getRecipe(@Nonnull String recipeID) {
        return this.recipes.get(recipeID);
    }

    @Override
    public boolean hasRecipe(@Nonnull String recipeID) {
        return this.recipes.containsKey(recipeID);
    }

    @Override
    public void removeRecipe(@Nonnull String recipeID) {
        this.recipes.remove(recipeID);
    }

    @Override
    public void write(Recipe recipe, ByteBuf buf) throws IOException {
        recipe.input.serialize(buf);
        StackUtil.writeItemStack(recipe.result, buf, true, true);
    }

    @Override
    public Recipe read(String recipeID, ByteBuf buf) throws IOException {
        Ingredient input = Ingredient.read(buf);
        ItemStack result = StackUtil.readItemStack(buf, true, true);
        return new Recipe(input, result);
    }

    /** Polishing data: one item in, one item out. */
    public static class Recipe {
        public final Ingredient input;
        public final ItemStack result;

        public Recipe(Ingredient input, ItemStack result) {
            this.input = input;
            this.result = result;
        }
    }
}
