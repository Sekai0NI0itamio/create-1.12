package nl.melonstudios.create.recipe.client;

import com.melonstudios.melonlib.recipe.IRecipeTypeClient;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.recipe.server.SandpaperPolishingRecipes;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

@SideOnly(Side.CLIENT)
public class SandpaperPolishingRecipesClient implements IRecipeTypeClient<SandpaperPolishingRecipes.Recipe, SandpaperPolishingRecipes> {
    public static final SandpaperPolishingRecipesClient instance = new SandpaperPolishingRecipesClient();

    private SandpaperPolishingRecipesClient() {

    }

    private final Map<String, SandpaperPolishingRecipes.Recipe> recipes = new HashMap<>();

    @Nonnull
    @Override
    public Map<String, SandpaperPolishingRecipes.Recipe> getRecipeMap() {
        return this.recipes;
    }
}
