package nl.melonstudios.create.recipe.server;

import com.melonstudios.melonlib.misc.StackUtil;
import com.melonstudios.melonlib.recipe.FluidIngredient;
import com.melonstudios.melonlib.recipe.ISyncedRecipeType;
import com.melonstudios.melonlib.recipe.Ingredient;
import com.melonstudios.melonlib.recipe.RecipeException;
import com.melonstudios.melonlib.recipe.UniversalRecipe;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.oredict.OreDictionary;
import nl.melonstudios.create.init.ItemInit;
import nl.melonstudios.create.tileentity.TileEntityBasin;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Basin compacting recipes (Create {@code create:compacting}, translated schema:
 * item ingredients + fluid ingredients -&gt; item results).
 * <p>
 * Shape mirrors {@link MixingRecipes}/{@link MixingRecipe}: a nested data class
 * with a flexible builder, basin matching, and input consumption. Kept separate
 * from mixing because official compacting is driven by the press, not the mixer.
 * <p>
 * Integration (NEEDS-LEAD, do not wire here): the press basin branch should try
 * this lookup next to {@code PressingRecipes.getRecipeForInput} in
 * {@code TileEntityPress.java:64}
 * ({@code PressingRecipe recipe = PressingRecipes.getRecipeForInput(stack, this.world.isRemote);}),
 * or {@code TileEntityMixer#searchRecipe} in {@code TileEntityMixer.java:95-101}
 * ({@code String found = MixingRecipes.getRecipeForInput(basin, this.world.isRemote);})
 * for mixer-driven compacting. Results can reuse
 * {@code TileEntityBasin#dumpRecipeResults} ({@code TileEntityBasin.java:371}).
 * Registration (NEEDS-LEAD): {@code RecipeRegistry.registerServer("create:compacting", ...)}
 * in {@code CommonProxy.java:213-223}, client side in {@code ClientProxy.java:272-283},
 * accessor in {@code RecipeInit.java:1104-1118}, defaults call in {@code RecipeInit.init()}
 * ({@code RecipeInit.java:77-85}).
 */
public class CompactingRecipes implements ISyncedRecipeType<CompactingRecipes.Recipe> {
    public static final CompactingRecipes instance = new CompactingRecipes();

    private CompactingRecipes() {

    }

    public final HashMap<String, Recipe> recipes = new HashMap<>();

    /** Starter set translated from the reference compacting table (1.12 items only). */
    public static void initDefaults() {
        CompactingRecipes recipes = CompactingRecipes.instance;
        recipes.addRecipe("create:compacting/andesite",
                Recipe.builder()
                        .setItemInputs(
                                new ItemStack(Items.FLINT),
                                new ItemStack(Items.FLINT),
                                new ItemStack(Blocks.GRAVEL))
                        .setFluidInputs(new FluidStack(FluidRegistry.LAVA, 100))
                        .setItemOutputs(new ItemStack(Blocks.STONE, 1, 5))
                        .build());
        recipes.addRecipe("create:compacting/blazecake_base",
                Recipe.builder()
                        .setItemInputs(
                                new ItemStack(Items.EGG),
                                new ItemStack(Items.SUGAR),
                                new ItemStack(ItemInit.INGREDIENT, 1, 2))
                        .setItemOutputs(new ItemStack(ItemInit.INGREDIENT, 1, 30))
                        .build());
        recipes.addRecipe("create:compacting/dough",
                Recipe.builder()
                        .setItemInputs(new ItemStack(ItemInit.INGREDIENT, 1, 0))
                        .setFluidInputs(new FluidStack(FluidRegistry.WATER, 250))
                        .setItemOutputs(new ItemStack(ItemInit.INGREDIENT, 1, 1))
                        .build());
        recipes.addRecipe("create:compacting/sturdy_sheet",
                Recipe.builder()
                        .setItemInputs(
                                new ItemStack(ItemInit.INGREDIENT, 1, 5),
                                new ItemStack(ItemInit.INGREDIENT, 1, 5))
                        .setItemOutputs(new ItemStack(ItemInit.INGREDIENT, 1, 6))
                        .build());
        recipes.addRecipe("create:compacting/clay",
                Recipe.builder()
                        .setItemInputs(
                                new ItemStack(Blocks.SAND),
                                new ItemStack(Blocks.GRAVEL))
                        .setFluidInputs(new FluidStack(FluidRegistry.WATER, 250))
                        .setItemOutputs(new ItemStack(Blocks.CLAY))
                        .build());
        recipes.addRecipe("create:compacting/coarse_dirt",
                Recipe.builder()
                        .setItemInputs(
                                new ItemStack(Blocks.DIRT),
                                new ItemStack(Blocks.GRAVEL))
                        .setItemOutputs(new ItemStack(Blocks.DIRT, 1, 1))
                        .build());
    }

    /**
     * Basin lookup. Iterates the server map directly until the lead registers
     * the synced type (then switch to a {@code RecipeInit.getCompactingRecipes(client)}
     * accessor like {@code MixingRecipes.getRecipeForInput} does).
     */
    public static String getRecipeForInput(TileEntityBasin basin) {
        if (basin == null) return null;
        for (Map.Entry<String, Recipe> entry : instance.recipes.entrySet()) {
            if (entry.getValue().matches(basin)) return entry.getKey();
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
            Recipe.Builder builder = Recipe.builder();
            List<Ingredient> items = new ArrayList<>();
            for (List<Ingredient> slot : universal.itemInputs) items.add(slot.get(0));
            builder.itemInputs = items;
            List<FluidIngredient> fluids = new ArrayList<>();
            for (List<FluidIngredient> slot : universal.fluidInputs) fluids.add(slot.get(0));
            builder.fluidInputs = fluids;
            List<ItemStack> outputs = new ArrayList<>();
            for (List<ItemStack> slot : universal.itemOutputs) outputs.add(slot.get(0));
            builder.itemOutputs = outputs;
            return builder.build();
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
        buf.writeInt(recipe.itemInputs.size());
        for (Ingredient input : recipe.itemInputs) input.serialize(buf);
        buf.writeInt(recipe.fluidInputs.size());
        for (FluidIngredient input : recipe.fluidInputs) input.serialize(buf);
        buf.writeInt(recipe.itemOutputs.size());
        for (ItemStack stack : recipe.itemOutputs) StackUtil.writeItemStack(stack, buf, true, true);
        buf.writeInt(recipe.processingTime);
    }

    @Override
    public Recipe read(String recipeID, ByteBuf buf) throws IOException {
        int itemInputsLen = buf.readInt();
        List<Ingredient> itemInputs = new ArrayList<>(itemInputsLen);
        for (int i = 0; i < itemInputsLen; i++) itemInputs.add(Ingredient.read(buf));
        int fluidInputsLen = buf.readInt();
        List<FluidIngredient> fluidInputs = new ArrayList<>(fluidInputsLen);
        for (int i = 0; i < fluidInputsLen; i++) fluidInputs.add(FluidIngredient.read(buf));
        int itemOutputsLen = buf.readInt();
        List<ItemStack> itemOutputs = new ArrayList<>(itemOutputsLen);
        for (int i = 0; i < itemOutputsLen; i++) itemOutputs.add(StackUtil.readItemStack(buf, true, true));
        int processingTime = buf.readInt();
        return new Recipe(itemInputs, fluidInputs, itemOutputs, processingTime);
    }

    /** Basin compacting data: item + fluid inputs, item outputs, press ticks. */
    public static class Recipe {
        public final List<Ingredient> itemInputs;
        public final List<FluidIngredient> fluidInputs;
        public final List<ItemStack> itemOutputs;
        public final int processingTime;

        public Recipe(List<Ingredient> itemInputs, List<FluidIngredient> fluidInputs,
                      List<ItemStack> itemOutputs, int processingTime) {
            this.itemInputs = itemInputs;
            this.fluidInputs = fluidInputs;
            this.itemOutputs = itemOutputs;
            this.processingTime = processingTime;
        }

        public boolean matches(TileEntityBasin basin) {
            if (!this.itemInputs.isEmpty()) {
                List<Ingredient> test = new ArrayList<>(this.itemInputs);
                loop:
                for (ItemStack stack : basin.inventory) {
                    while (!test.isEmpty()) {
                        Ingredient ingredient = test.remove(0);
                        if (ingredient.matches(stack)) {
                            continue loop;
                        }
                        test.add(ingredient);
                    }
                }
                if (!test.isEmpty()) return false;
            }
            if (!this.fluidInputs.isEmpty()) {
                List<FluidIngredient> test = new ArrayList<>(this.fluidInputs);
                loop:
                for (FluidTank tank : basin.fluid.getHandlers()) {
                    FluidStack stack = tank.getFluid();
                    if (stack == null) throw new IllegalStateException("How is the fluid stack null? Please optimize fluid pool!");
                    while (!test.isEmpty()) {
                        FluidIngredient ingredient = test.remove(0);
                        if (ingredient.matches(stack)) {
                            continue loop;
                        }
                        test.add(ingredient);
                    }
                }
                if (!test.isEmpty()) return false;
            }
            return true;
        }

        public boolean consumeInputs(TileEntityBasin basin) {
            if (!this.itemInputs.isEmpty()) {
                List<Ingredient> test = new ArrayList<>(this.itemInputs);
                loop:
                for (ItemStack stack : basin.inventory) {
                    while (!test.isEmpty()) {
                        Ingredient ingredient = test.remove(0);
                        if (ingredient.matches(stack)) {
                            stack.shrink(1);
                            continue loop;
                        }
                        test.add(ingredient);
                    }
                }
                if (!test.isEmpty()) return false;
            }
            if (!this.fluidInputs.isEmpty()) {
                List<FluidIngredient> test = new ArrayList<>(this.fluidInputs);
                loop:
                for (FluidTank tank : basin.fluid.getHandlers()) {
                    FluidStack stack = tank.getFluid();
                    if (stack == null) throw new IllegalStateException("How is the fluid stack null? Please optimize fluid pool!");
                    while (!test.isEmpty()) {
                        FluidIngredient ingredient = test.remove(0);
                        if (ingredient.matches(stack)) {
                            stack.amount -= ingredient.getDisplayFluids().get(0).amount;
                            continue loop;
                        }
                        test.add(ingredient);
                    }
                }
                if (!test.isEmpty()) return false;
            }
            basin.optimizeInventory();
            return true;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private List<Ingredient> itemInputs = Collections.emptyList();
            private List<FluidIngredient> fluidInputs = Collections.emptyList();
            private List<ItemStack> itemOutputs = Collections.emptyList();
            private int processingTime = 5120;

            private Builder() {

            }

            public Recipe build() {
                return new Recipe(this.itemInputs, this.fluidInputs, this.itemOutputs, this.processingTime);
            }

            public Builder setItemInputs(Object... inputs) {
                this.itemInputs = new ArrayList<>(inputs.length);
                for (Object param : inputs) {
                    if (param instanceof Ingredient) {
                        this.itemInputs.add((Ingredient) param);
                    } else if (param instanceof ItemStack) {
                        ItemStack input = (ItemStack) param;
                        if (input.isEmpty()) throw new IllegalArgumentException("ItemStack input cannot be empty");
                        for (int i = 0; i < input.getCount(); i++) {
                            this.itemInputs.add(Ingredient.of(input, false));
                        }
                    } else if (param instanceof Item) {
                        this.itemInputs.add(Ingredient.of(new ItemStack((Item) param, 1, OreDictionary.WILDCARD_VALUE), false));
                    } else if (param instanceof Block) {
                        this.itemInputs.add(Ingredient.of(new ItemStack((Block) param, 1, OreDictionary.WILDCARD_VALUE), false));
                    } else if (param instanceof String) {
                        this.itemInputs.add(Ingredient.of((String) param));
                    } else {
                        throw new IllegalArgumentException("Invalid item input: " + param + " (" + param.getClass().getSimpleName() + ")");
                    }
                }
                return this;
            }

            public Builder setFluidInputs(Object... inputs) {
                this.fluidInputs = new ArrayList<>(inputs.length);
                for (Object param : inputs) {
                    if (param instanceof FluidIngredient) {
                        this.fluidInputs.add((FluidIngredient) param);
                    } else if (param instanceof FluidStack) {
                        this.fluidInputs.add(FluidIngredient.of((FluidStack) param));
                    } else {
                        throw new IllegalArgumentException("Invalid fluid input: " + param + " (" + param.getClass().getSimpleName() + ")");
                    }
                }
                return this;
            }

            public Builder setItemOutputs(Object... outputs) {
                this.itemOutputs = new ArrayList<>(outputs.length);
                for (Object param : outputs) {
                    if (param instanceof ItemStack) {
                        this.itemOutputs.add((ItemStack) param);
                    } else if (param instanceof Item) {
                        this.itemOutputs.add(new ItemStack((Item) param, 1, 0));
                    } else if (param instanceof Block) {
                        this.itemOutputs.add(new ItemStack((Block) param, 1, 0));
                    } else {
                        throw new IllegalArgumentException("Invalid item output: " + param + " (" + param.getClass().getSimpleName() + ")");
                    }
                }
                return this;
            }

            public Builder setProcessingTime(int ticks) {
                this.processingTime = ticks;
                return this;
            }

            public Builder setProcessingTime64RPM(int ticks) {
                this.processingTime = ticks * 64;
                return this;
            }
        }
    }
}
