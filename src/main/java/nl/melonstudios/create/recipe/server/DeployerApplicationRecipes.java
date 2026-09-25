package nl.melonstudios.create.recipe.server;

import com.melonstudios.melonlib.misc.StackUtil;
import com.melonstudios.melonlib.recipe.ISyncedRecipeType;
import com.melonstudios.melonlib.recipe.Ingredient;
import com.melonstudios.melonlib.recipe.RecipeException;
import com.melonstudios.melonlib.recipe.UniversalRecipe;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import nl.melonstudios.create.init.BlockInit;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.recipe.DeployerRecipe;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Deployer item-application recipes (Create {@code create:item_application}, translated
 * schema: target ingredient + applied ingredient -&gt; result).
 * <p>
 * Shape mirrors {@link DeployerRecipes}/{@code DeployerRecipe} exactly (depot
 * input, held applied input, result, consume/damage/keep semantics), because
 * official item application runs on the deployer. Kept as its own synced type
 * so application recipes no longer share the {@code create:deploying} namespace.
 * <p>
 * Integration (NEEDS-LEAD, do not wire here): add an application branch to the
 * deployer {@code recipes:} block in {@code TileEntityDeployer.java:113-130},
 * right after {@code DeployerRecipes.instance.getRecipeForInput(in, this.heldItem)}
 * (line 116), reusing the same consume/damage/keep switch (lines 120-130).
 * Registration (NEEDS-LEAD): {@code RecipeRegistry.registerServer("create:item_application", ...)}
 * in {@code CommonProxy.java:213-223}, client side in {@code ClientProxy.java:272-283},
 * accessor in {@code RecipeInit.java:1104-1118}, defaults call in {@code RecipeInit.init()}
 * ({@code RecipeInit.java:77-85}).
 */
public class DeployerApplicationRecipes implements ISyncedRecipeType<DeployerApplicationRecipes.Recipe> {
    public static final DeployerApplicationRecipes instance = new DeployerApplicationRecipes();

    private DeployerApplicationRecipes() {

    }

    public final HashMap<String, Recipe> recipes = new HashMap<>();

    /** Starter set from the reference item-application table (1.12 items only). */
    public static void initDefaults() {
        DeployerApplicationRecipes recipes = DeployerApplicationRecipes.instance;
        recipes.addRecipe("create:item_application/casing_andesite",
                Ingredient.of("logWood"),
                Ingredient.of(new ItemStack(ItemInit.INGREDIENT, 1, 15), false),
                new ItemStack(BlockInit.CASING, 1, 0));
        recipes.addRecipe("create:item_application/casing_copper",
                Ingredient.of("logWood"),
                Ingredient.of("plateCopper"),
                new ItemStack(BlockInit.CASING, 1, 1));
        recipes.addRecipe("create:item_application/casing_brass",
                Ingredient.of("logWood"),
                Ingredient.of("ingotBrass"),
                new ItemStack(BlockInit.CASING, 1, 2));
        recipes.addRecipe("create:item_application/cogwheel",
                Ingredient.of(new ItemStack(BlockInit.SHAFT), false),
                Ingredient.of("plankWood"),
                new ItemStack(BlockInit.COG_SMALL));
        recipes.addRecipe("create:item_application/large_cogwheel",
                Ingredient.of(new ItemStack(BlockInit.COG_SMALL), false),
                Ingredient.of("plankWood"),
                new ItemStack(BlockInit.COG_LARGE));
        recipes.addRecipe("create:item_application/electron_tube",
                Ingredient.of("plateIron"),
                Ingredient.of("gemPolishedRoseQuartz"),
                new ItemStack(ItemInit.INGREDIENT, 1, 10),
                DeployerRecipe.InputType.CONSUME);
    }

    public void addRecipe(@Nonnull String recipeID, @Nonnull Ingredient input, @Nonnull Ingredient applied,
                          @Nonnull ItemStack result, DeployerRecipe.InputType inputType) {
        this.addRecipe(recipeID, new Recipe(input, applied, result, inputType));
    }

    public void addRecipe(@Nonnull String recipeID, @Nonnull Ingredient input, @Nonnull Ingredient applied,
                          @Nonnull ItemStack result) {
        this.addRecipe(recipeID, input, applied, result, DeployerRecipe.InputType.CONSUME);
    }

    /**
     * Application lookup: depot item plus held item must both match.
     * Iterates the server map directly until the lead registers the synced type
     * (then switch to a {@code RecipeInit.getApplicationRecipes(client)} accessor).
     */
    @Nullable
    public final Recipe getRecipeForInput(ItemStack depotItem, ItemStack deployerItem) {
        if (depotItem.isEmpty() || deployerItem.isEmpty()) return null; //failsafe
        for (Recipe recipe : this.recipes.values()) {
            if (recipe.input.matches(depotItem)) {
                if (recipe.applied.matches(deployerItem)) {
                    return recipe;
                }
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
            List<Ingredient> in = universal.itemInputs.get(0);
            Ingredient depot = in.get(0);
            Ingredient held = in.get(1);
            ItemStack result = universal.itemOutputs.get(0).get(0);
            return new Recipe(depot, held, result, DeployerRecipe.InputType.get(universal.extraData.getString("inputType")));
        } catch (Throwable e) {
            throw new RecipeException(e);
        }
    }

    @Override
    public void addRecipe(@Nonnull String recipeID, @Nonnull Recipe recipe) {
        this.recipes.put(recipeID, recipe);
    }

    @Nullable
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
        recipe.applied.serialize(buf);
        StackUtil.writeItemStack(recipe.result, buf, true, true);
        buf.writeByte(DeployerRecipe.InputType.lookup(recipe.inputType));
    }

    @Override
    public Recipe read(String recipeID, ByteBuf buf) throws IOException {
        Ingredient input = Ingredient.read(buf);
        Ingredient applied = Ingredient.read(buf);
        ItemStack result = StackUtil.readItemStack(buf, true, true);
        DeployerRecipe.InputType inputType = DeployerRecipe.InputType.lookup(buf.readByte());
        return new Recipe(input, applied, result, inputType);
    }

    /** Application data: depot target + held item -&gt; result. */
    public static class Recipe {
        public final Ingredient input;
        public final Ingredient applied;
        public final ItemStack result;
        public final DeployerRecipe.InputType inputType;

        public Recipe(Ingredient input, Ingredient applied, ItemStack result, DeployerRecipe.InputType inputType) {
            this.input = input;
            this.applied = applied;
            this.result = result;
            this.inputType = inputType;
        }
    }
}
