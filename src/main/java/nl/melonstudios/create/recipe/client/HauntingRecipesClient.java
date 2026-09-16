package nl.melonstudios.create.recipe.client;

import com.melonstudios.melonlib.recipe.IRecipeTypeClient;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.recipe.PulverizationRecipe;
import nl.melonstudios.create.recipe.server.HauntingRecipes;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

@SideOnly(Side.CLIENT)
public class HauntingRecipesClient implements IRecipeTypeClient<PulverizationRecipe, HauntingRecipes> {
    public static final HauntingRecipesClient instance = new HauntingRecipesClient();

    private HauntingRecipesClient() {

    }

    private final Map<String, PulverizationRecipe> recipes = new HashMap<>();

    @Nonnull
    @Override
    public Map<String, PulverizationRecipe> getRecipeMap() {
        return this.recipes;
    }
}
