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
import nl.melonstudios.create.tileentity.actor.TileEntityCrafter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mechanical-crafting recipes (Create {@code create:mechanical_crafting}, translated
 * schema: pattern rows + char key + result, optional mirroring).
 * <p>
 * The backport crafter chain forms an arbitrary {@code TileEntityCrafter[][]}
 * grid (see {@code TileEntityCrafter#convertToGrid}, {@code TileEntityCrafter.java:391}),
 * so matching trims empty borders off both the pattern and the live grid, then
 * compares cell by cell. Cover plates ({@code ItemInit.CRAFTER_COVER}) count as
 * empty slots for matching and are never consumed, mirroring the vanilla path.
 * <p>
 * Integration (NEEDS-LEAD, do not wire here): {@code TileEntityCrafter#tick} in
 * {@code TileEntityCrafter.java:79-84} currently builds an
 * {@code InventoryCrafter} and calls
 * {@code CraftingManager.findMatchingRecipe(inventoryCrafter, this.world)};
 * try {@code CrafterMechanicalRecipes.getRecipeForInput(grid)} on the
 * {@code convertToGrid} output first and fall back to vanilla matching on a miss.
 * Registration (NEEDS-LEAD): {@code RecipeRegistry.registerServer("create:mechanical_crafting", ...)}
 * in {@code CommonProxy.java:213-223}, client side in {@code ClientProxy.java:272-283},
 * accessor in {@code RecipeInit.java:1104-1118}, defaults call in {@code RecipeInit.init()}
 * ({@code RecipeInit.java:77-85}).
 */
public class CrafterMechanicalRecipes implements ISyncedRecipeType<CrafterMechanicalRecipes.Recipe> {
    public static final CrafterMechanicalRecipes instance = new CrafterMechanicalRecipes();

    private CrafterMechanicalRecipes() {

    }

    public final HashMap<String, Recipe> recipes = new HashMap<>();

    /** Starter set scaled to 3x3 from the reference 5x5 mechanical table (1.12 items only). */
    public static void initDefaults() {
        CrafterMechanicalRecipes recipes = CrafterMechanicalRecipes.instance;
        recipes.addRecipe("create:mechanical_crafting/casing_andesite",
                new ItemStack(BlockInit.CASING, 1, 0), false,
                keyOf('P', "plankWood", 'A', new ItemStack(ItemInit.INGREDIENT, 1, 15)),
                "PPP",
                "PAP",
                "PPP");
        recipes.addRecipe("create:mechanical_crafting/cogwheel",
                new ItemStack(BlockInit.COG_SMALL), false,
                keyOf('P', "plankWood", 'S', new ItemStack(BlockInit.SHAFT)),
                " P ",
                "PSP",
                " P ");
        recipes.addRecipe("create:mechanical_crafting/large_cogwheel",
                new ItemStack(BlockInit.COG_LARGE), false,
                keyOf('P', "plankWood", 'C', new ItemStack(BlockInit.COG_SMALL)),
                " P ",
                "PCP",
                " P ");
        recipes.addRecipe("create:mechanical_crafting/crushing_wheel",
                new ItemStack(BlockInit.CRUSHING_WHEEL), false,
                keyOf('A', new ItemStack(ItemInit.INGREDIENT, 1, 15),
                        'P', "plankWood", 'S', new ItemStack(BlockInit.SHAFT)),
                "AAA",
                "ASA",
                "AAA");
        recipes.addRecipe("create:mechanical_crafting/brass_hand",
                new ItemStack(ItemInit.INGREDIENT, 1, 9), false,
                keyOf('B', "ingotBrass", 'A', new ItemStack(ItemInit.INGREDIENT, 1, 15)),
                "BBB",
                "B B",
                " A ");
        recipes.addRecipe("create:mechanical_crafting/electron_tube",
                new ItemStack(ItemInit.INGREDIENT, 1, 10), false,
                keyOf('I', "plateIron", 'Q', "gemPolishedRoseQuartz"),
                " I ",
                "IQI",
                " I ");
        recipes.addRecipe("create:mechanical_crafting/precision_mechanism",
                new ItemStack(ItemInit.INGREDIENT, 1, 14), true,
                keyOf('G', "plateGold", 'C', new ItemStack(BlockInit.COG_SMALL),
                        'N', "nuggetIron"),
                " G ",
                "GCG",
                " N ");
    }

    /**
     * Key helper mirroring the flexible {@code Object...} idiom of the mixing builder:
     * pairs of (Character or single-char String, Ingredient or ItemStack or ore String).
     */
    public static Map<Character, Ingredient> keyOf(Object... pairs) {
        if ((pairs.length & 1) != 0) throw new IllegalArgumentException("keyOf needs even args (key, value, ...)");
        Map<Character, Ingredient> key = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            Object rawKey = pairs[i];
            char c;
            if (rawKey instanceof Character) c = (Character) rawKey;
            else if (rawKey instanceof String && ((String) rawKey).length() == 1) c = ((String) rawKey).charAt(0);
            else throw new IllegalArgumentException("Invalid key: " + rawKey);
            Object rawValue = pairs[i + 1];
            if (rawValue instanceof Ingredient) key.put(c, (Ingredient) rawValue);
            else if (rawValue instanceof ItemStack) key.put(c, Ingredient.of((ItemStack) rawValue, false));
            else if (rawValue instanceof String) key.put(c, Ingredient.of((String) rawValue));
            else throw new IllegalArgumentException("Invalid value: " + rawValue);
            if (c == ' ') throw new IllegalArgumentException("Space is reserved for empty slots");
        }
        return key;
    }

    public void addRecipe(String recipeID, ItemStack result, boolean mirrored, Map<Character, Ingredient> key, String... pattern) {
        this.addRecipe(recipeID, new Recipe(pattern, key, result, mirrored));
    }

    /**
     * Crafter-chain lookup over the {@code convertToGrid} array.
     * Iterates the server map directly until the lead registers the synced type
     * (then switch to a {@code RecipeInit.getMechanicalRecipes(client)} accessor).
     */
    @Nullable
    public static Recipe getRecipeForInput(TileEntityCrafter[][] grid) {
        if (grid == null || grid.length == 0 || grid[0].length == 0) return null;
        for (Recipe recipe : instance.recipes.values()) {
            if (recipe.matches(grid)) {
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
        throw new RecipeException("unsupported");
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
        buf.writeInt(recipe.pattern.length);
        for (String row : recipe.pattern) {
            byte[] bytes = row.getBytes(StandardCharsets.UTF_8);
            buf.writeInt(bytes.length);
            buf.writeBytes(bytes);
        }
        buf.writeInt(recipe.key.size());
        for (Map.Entry<Character, Ingredient> entry : recipe.key.entrySet()) {
            buf.writeChar(entry.getKey());
            entry.getValue().serialize(buf);
        }
        StackUtil.writeItemStack(recipe.result, buf, true, true);
        buf.writeBoolean(recipe.mirrored);
    }

    @Override
    public Recipe read(String recipeID, ByteBuf buf) throws IOException {
        int rows = buf.readInt();
        String[] pattern = new String[rows];
        for (int i = 0; i < rows; i++) {
            int len = buf.readInt();
            pattern[i] = buf.readCharSequence(len, StandardCharsets.UTF_8).toString();
        }
        int keys = buf.readInt();
        Map<Character, Ingredient> key = new HashMap<>();
        for (int i = 0; i < keys; i++) {
            key.put(buf.readChar(), Ingredient.read(buf));
        }
        ItemStack result = StackUtil.readItemStack(buf, true, true);
        boolean mirrored = buf.readBoolean();
        return new Recipe(pattern, key, result, mirrored);
    }

    /** Mechanical-crafting data: shaped pattern + key + result. */
    public static class Recipe {
        public final String[] pattern;
        public final Map<Character, Ingredient> key;
        public final ItemStack result;
        public final boolean mirrored;

        public Recipe(String[] pattern, Map<Character, Ingredient> key, ItemStack result, boolean mirrored) {
            if (pattern == null || pattern.length == 0) throw new IllegalArgumentException("Pattern cannot be empty");
            int width = pattern[0].length();
            for (String row : pattern) {
                if (row.length() != width) throw new IllegalArgumentException("Pattern rows must be equal width");
            }
            this.pattern = pattern;
            this.key = key;
            this.result = result;
            this.mirrored = mirrored;
        }

        public boolean matches(TileEntityCrafter[][] grid) {
            ItemStack[][] trimmedGrid = trimGrid(grid);
            char[][] trimmedPattern = trimPattern(this.pattern);
            if (trimmedGrid.length == 0 || trimmedPattern.length == 0) return false;
            if (matchesAt(trimmedPattern, trimmedGrid)) return true;
            return this.mirrored && matchesAt(mirror(trimmedPattern), trimmedGrid);
        }

        private boolean matchesAt(char[][] pattern, ItemStack[][] grid) {
            if (pattern.length != grid.length) return false;
            if (pattern[0].length != grid[0].length) return false;
            for (int x = 0; x < pattern.length; x++) {
                for (int y = 0; y < pattern[x].length; y++) {
                    char c = pattern[x][y];
                    ItemStack stack = grid[x][y];
                    if (c == ' ') {
                        if (!stack.isEmpty()) return false;
                    } else {
                        Ingredient ingredient = this.key.get(c);
                        if (ingredient == null || !ingredient.matches(stack)) return false;
                    }
                }
            }
            return true;
        }

        private static ItemStack[][] trimGrid(TileEntityCrafter[][] grid) {
            int w = grid.length;
            int h = grid[0].length;
            int minX = w, maxX = -1, minY = h, maxY = -1;
            ItemStack[][] stacks = new ItemStack[w][h];
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    TileEntityCrafter cell = grid[x][y];
                    ItemStack stack = cell == null ? ItemStack.EMPTY : cell.containedItem;
                    if (stack != null && stack.getItem() == ItemInit.CRAFTER_COVER) stack = ItemStack.EMPTY;
                    if (stack == null) stack = ItemStack.EMPTY;
                    stacks[x][y] = stack;
                    if (!stack.isEmpty()) {
                        if (x < minX) minX = x;
                        if (x > maxX) maxX = x;
                        if (y < minY) minY = y;
                        if (y > maxY) maxY = y;
                    }
                }
            }
            if (maxX < 0) return new ItemStack[0][0];
            ItemStack[][] trimmed = new ItemStack[maxX - minX + 1][maxY - minY + 1];
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    trimmed[x - minX][y - minY] = stacks[x][y];
                }
            }
            return trimmed;
        }

        private static char[][] trimPattern(String[] pattern) {
            int h = pattern.length;
            int w = pattern[0].length();
            int minX = w, maxX = -1, minY = h, maxY = -1;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (pattern[y].charAt(x) != ' ') {
                        if (x < minX) minX = x;
                        if (x > maxX) maxX = x;
                        if (y < minY) minY = y;
                        if (y > maxY) maxY = y;
                    }
                }
            }
            if (maxX < 0) return new char[0][0];
            char[][] trimmed = new char[maxX - minX + 1][maxY - minY + 1];
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    trimmed[x - minX][y - minY] = pattern[y].charAt(x);
                }
            }
            return trimmed;
        }

        private static char[][] mirror(char[][] pattern) {
            char[][] mirrored = new char[pattern.length][pattern[0].length];
            for (int x = 0; x < pattern.length; x++) {
                for (int y = 0; y < pattern[x].length; y++) {
                    mirrored[pattern.length - 1 - x][y] = pattern[x][y];
                }
            }
            return mirrored;
        }

        /** Live grid as row-major stacks, for result handling after a match. */
        public static List<ItemStack> flattenGrid(TileEntityCrafter[][] grid) {
            List<ItemStack> stacks = new ArrayList<>();
            for (int y = 0; y < grid[0].length; y++) {
                for (int x = 0; x < grid.length; x++) {
                    TileEntityCrafter cell = grid[x][y];
                    stacks.add(cell == null ? ItemStack.EMPTY : cell.containedItem);
                }
            }
            return stacks;
        }
    }
}
