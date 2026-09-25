package nl.melonstudios.create.recipe.server;

import com.melonstudios.melonlib.misc.StackUtil;
import com.melonstudios.melonlib.recipe.ISyncedRecipeType;
import com.melonstudios.melonlib.recipe.Ingredient;
import com.melonstudios.melonlib.recipe.RecipeException;
import com.melonstudios.melonlib.recipe.UniversalRecipe;
import io.netty.buffer.ByteBuf;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import nl.melonstudios.create.init.ItemInit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Item-drain emptying recipes (Create {@code create:emptying}, translated schema:
 * filled item -&gt; empty container + fluid result).
 * <p>
 * Shape mirrors {@link SpoutFillingRecipes} with inputs and outputs swapped,
 * because draining is the exact inverse of spout filling.
 * <p>
 * Integration (NEEDS-LEAD, do not wire here): {@code TileEntityItemDrain#tick}
 * in {@code TileEntityItemDrain.java:42-64} currently drains any
 * {@code FluidUtil.getFluidHandler} container generically; look up
 * {@code DrainEmptyingRecipes.getRecipeForInput(this.draining)} first and, on a
 * hit, fill the tank with {@code recipe.fluid} and replace
 * {@code this.draining} with {@code recipe.container} instead of the generic
 * handler path.
 * Registration (NEEDS-LEAD): {@code RecipeRegistry.registerServer("create:emptying", ...)}
 * in {@code CommonProxy.java:213-223}, client side in {@code ClientProxy.java:272-283},
 * accessor in {@code RecipeInit.java:1104-1118}, defaults call in {@code RecipeInit.init()}
 * ({@code RecipeInit.java:77-85}).
 */
public class DrainEmptyingRecipes implements ISyncedRecipeType<DrainEmptyingRecipes.Recipe> {
    public static final DrainEmptyingRecipes instance = new DrainEmptyingRecipes();

    private DrainEmptyingRecipes() {

    }

    public final HashMap<String, Recipe> recipes = new HashMap<>();

    /** Starter set: inverse of the spout filling starters (1.12 items only). */
    public static void initDefaults() {
        DrainEmptyingRecipes recipes = DrainEmptyingRecipes.instance;
        recipes.addRecipe("create:emptying/water_bucket",
                Ingredient.of(new ItemStack(Items.WATER_BUCKET), false),
                new ItemStack(Items.BUCKET),
                new FluidStack(FluidRegistry.WATER, 1000));
        recipes.addRecipe("create:emptying/lava_bucket",
                Ingredient.of(new ItemStack(Items.LAVA_BUCKET), false),
                new ItemStack(Items.BUCKET),
                new FluidStack(FluidRegistry.LAVA, 1000));
        recipes.addRecipe("create:emptying/milk_bucket",
                Ingredient.of(new ItemStack(Items.MILK_BUCKET), false),
                new ItemStack(Items.BUCKET),
                new FluidStack(FluidRegistry.getFluid("milk"), 1000));
        recipes.addRecipe("create:emptying/builders_tea",
                Ingredient.of(new ItemStack(ItemInit.BUILDERS_TEA), false),
                new ItemStack(Items.GLASS_BOTTLE),
                new FluidStack(FluidRegistry.getFluid("builders_tea"), 250));
        recipes.addRecipe("create:emptying/blaze_cake",
                Ingredient.of(new ItemStack(ItemInit.INGREDIENT, 1, 31), false),
                new ItemStack(ItemInit.INGREDIENT, 1, 30),
                new FluidStack(FluidRegistry.LAVA, 250));
    }

    public void addRecipe(String recipeID, Ingredient input, ItemStack container, FluidStack fluid) {
        this.addRecipe(recipeID, new Recipe(input, container, fluid));
    }

    /**
     * Drain lookup: draining item must match.
     * Iterates the server map directly until the lead registers the synced type
     * (then switch to a {@code RecipeInit.getEmptyingRecipes(client)} accessor).
     */
    @Nullable
    public static Recipe getRecipeForInput(ItemStack draining) {
        if (draining.isEmpty()) return null;
        for (Recipe recipe : instance.recipes.values()) {
            if (recipe.input.matches(draining)) {
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
            ItemStack container = universal.itemOutputs.get(0).get(0);
            FluidStack fluid = universal.fluidOutputs.get(0).get(0);
            return new Recipe(input, container, fluid);
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
        StackUtil.writeItemStack(recipe.container, buf, true, true);
        writeFluid(recipe.fluid, buf);
    }

    @Override
    public Recipe read(String recipeID, ByteBuf buf) throws IOException {
        Ingredient input = Ingredient.read(buf);
        ItemStack container = StackUtil.readItemStack(buf, true, true);
        FluidStack fluid = readFluid(buf);
        return new Recipe(input, container, fluid);
    }

    private static void writeFluid(FluidStack stack, ByteBuf buf) {
        String id = FluidRegistry.getFluidName(stack);
        buf.writeInt(id.length());
        buf.writeCharSequence(id, StandardCharsets.UTF_8);
        buf.writeInt(stack.amount);
        NBTTagCompound tag = stack.tag;
        buf.writeBoolean(tag != null);
        if (tag != null) {
            new PacketBuffer(buf).writeCompoundTag(tag);
        }
    }

    private static FluidStack readFluid(ByteBuf buf) throws IOException {
        int len = buf.readInt();
        String id = buf.readCharSequence(len, StandardCharsets.UTF_8).toString();
        int amount = buf.readInt();
        NBTTagCompound tag = buf.readBoolean() ? new PacketBuffer(buf).readCompoundTag() : null;
        return new FluidStack(FluidRegistry.getFluid(id), amount, tag);
    }

    /** Drain emptying data: filled item -&gt; container item + fluid. */
    public static class Recipe {
        public final Ingredient input;
        public final ItemStack container;
        public final FluidStack fluid;

        public Recipe(Ingredient input, ItemStack container, FluidStack fluid) {
            this.input = input;
            this.container = container;
            this.fluid = fluid;
        }
    }
}
