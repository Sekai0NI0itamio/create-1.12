package nl.melonstudios.create.recipe.client;

import com.melonstudios.melonlib.recipe.IRecipeTypeClient;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.recipe.PulverizationRecipe;
import nl.melonstudios.create.recipe.server.SplashingRecipes;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

@SideOnly(Side.CLIENT)
public class SplashingRecipesClient implements IRecipeTypeClient<PulverizationRecipe, SplashingRecipes> {
    public static final SplashingRecipesClient instance = new SplashingRecipesClient();

    private SplashingRecipesClient() {

    }

    private final Map<String, PulverizationRecipe> recipes = new HashMap<>();

    @Nonnull
    @Override
    public Map<String, PulverizationRecipe> getRecipeMap() {
        return this.recipes;
    }
}
