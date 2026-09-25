package nl.melonstudios.create.recipe.server;

import com.melonstudios.melonlib.misc.StackUtil;
import com.melonstudios.melonlib.recipe.FluidIngredient;
import com.melonstudios.melonlib.recipe.ISyncedRecipeType;
import com.melonstudios.melonlib.recipe.Ingredient;
import com.melonstudios.melonlib.recipe.RecipeException;
import com.melonstudios.melonlib.recipe.UniversalRecipe;
import io.netty.buffer.ByteBuf;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import nl.melonstudios.create.init.ItemInit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Spout filling recipes (Create {@code create:filling}, translated schema:
 * empty item + fluid ingredient -&gt; filled item).
 * <p>
 * Shape mirrors {@link PressingRecipes}/{@code PressingRecipe} (single input,
 * single result) with a fluid ingredient added, because the spout pairs one
 * presented item with its tank contents.
 * <p>
 * Integration (NEEDS-LEAD, do not wire here): {@code TileEntitySpout#canFill}
 * in {@code TileEntitySpout.java:104} currently accepts any
 * {@code FluidUtil.getFluidHandler} fill; gate it on
 * {@code SpoutFillingRecipes.getRecipeForInput(presented, tankFluid) != null},
 * and resolve the output in {@code TileEntitySpout#finishFill}
 * ({@code TileEntitySpout.java:113-134}) instead of the generic handler fill.
 * Registration (NEEDS-LEAD): {@code RecipeRegistry.registerServer("create:filling", ...)}
 * in {@code CommonProxy.java:213-223}, client side in {@code ClientProxy.java:272-283},
 * accessor in {@code RecipeInit.java:1104-1118}, defaults call in {@code RecipeInit.init()}
 * ({@code RecipeInit.java:77-85}).
 */
public class SpoutFillingRecipes implements ISyncedRecipeType<SpoutFillingRecipes.Recipe> {
    public static final SpoutFillingRecipes instance = new SpoutFillingRecipes();

    private SpoutFillingRecipes() {

    }

    public final HashMap<String, Recipe> recipes = new HashMap<>();

    /** Starter set translated from the reference filling table (1.12 items only). */
    public static void initDefaults() {
        SpoutFillingRecipes recipes = SpoutFillingRecipes.instance;
        recipes.addRecipe("create:filling/water_bucket",
                Ingredient.of(new ItemStack(Items.BUCKET), false),
                new FluidStack(FluidRegistry.WATER, 1000),
                new ItemStack(Items.WATER_BUCKET));
        recipes.addRecipe("create:filling/lava_bucket",
                Ingredient.of(new ItemStack(Items.BUCKET), false),
                new FluidStack(FluidRegistry.LAVA, 1000),
                new ItemStack(Items.LAVA_BUCKET));
        recipes.addRecipe("create:filling/milk_bucket",
                Ingredient.of(new ItemStack(Items.BUCKET), false),
                new FluidStack(FluidRegistry.getFluid("milk"), 1000),
                new ItemStack(Items.MILK_BUCKET));
        recipes.addRecipe("create:filling/builders_tea",
                Ingredient.of(new ItemStack(Items.GLASS_BOTTLE), false),
                new FluidStack(FluidRegistry.getFluid("builders_tea"), 250),
                new ItemStack(ItemInit.BUILDERS_TEA));
        recipes.addRecipe("create:filling/blaze_cake",
                Ingredient.of(new ItemStack(ItemInit.INGREDIENT, 1, 30), false),
                new FluidStack(FluidRegistry.LAVA, 250),
                new ItemStack(ItemInit.INGREDIENT, 1, 31));
        recipes.addRecipe("create:filling/grass_block",
                Ingredient.of(new ItemStack(Blocks.DIRT, 1, 0), false),
                new FluidStack(FluidRegistry.WATER, 250),
                new ItemStack(Blocks.GRASS));
    }

    public void addRecipe(String recipeID, Ingredient input, FluidStack fluid, ItemStack result) {
        this.addRecipe(recipeID, new Recipe(input, FluidIngredient.of(fluid), fluid.amount, result));
    }

    public void addRecipe(String recipeID, Ingredient input, FluidIngredient fluid, ItemStack result) {
        this.addRecipe(recipeID, new Recipe(input, fluid, result));
    }

    /**
     * Spout lookup: presented depot item plus the tank fluid must both match.
     * Iterates the server map directly until the lead registers the synced type
     * (then switch to a {@code RecipeInit.getFillingRecipes(client)} accessor).
     */
    @Nullable
    public static Recipe getRecipeForInput(ItemStack presented, FluidStack tankFluid) {
        if (presented.isEmpty() || tankFluid == null) return null;
        for (Recipe recipe : instance.recipes.values()) {
            if (recipe.input.matches(presented) && recipe.fluid.matches(tankFluid)) {
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
            Ingredient input = universal.itemInputs.get(0).get(0);
            FluidIngredient fluid = universal.fluidInputs.get(0).get(0);
            ItemStack result = universal.itemOutputs.get(0).get(0);
            return new Recipe(input, fluid, result);
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
        recipe.fluid.serialize(buf);
        StackUtil.writeItemStack(recipe.result, buf, true, true);
    }

    @Override
    public Recipe read(String recipeID, ByteBuf buf) throws IOException {
        Ingredient input = Ingredient.read(buf);
        FluidIngredient fluid = FluidIngredient.read(buf);
        ItemStack result = StackUtil.readItemStack(buf, true, true);
        return new Recipe(input, fluid, result);
    }

    /** Spout filling data: empty item + fluid ingredient -&gt; filled item. */
    public static class Recipe {
        public final Ingredient input;
        public final FluidIngredient fluid;
        public final int fluidAmount;
        public final ItemStack result;

        public Recipe(Ingredient input, FluidIngredient fluid, ItemStack result) {
            this(input, fluid, -1, result);
        }

        public Recipe(Ingredient input, FluidIngredient fluid, int fluidAmount, ItemStack result) {
            this.input = input;
            this.fluid = fluid;
            this.fluidAmount = fluidAmount;
            this.result = result;
        }
    }
}
