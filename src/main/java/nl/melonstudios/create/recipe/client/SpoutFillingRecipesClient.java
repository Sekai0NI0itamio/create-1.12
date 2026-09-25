package nl.melonstudios.create.recipe.client;

import com.melonstudios.melonlib.recipe.IRecipeTypeClient;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.recipe.server.SpoutFillingRecipes;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

@SideOnly(Side.CLIENT)
public class SpoutFillingRecipesClient implements IRecipeTypeClient<SpoutFillingRecipes.Recipe, SpoutFillingRecipes> {
    public static final SpoutFillingRecipesClient instance = new SpoutFillingRecipesClient();

    private SpoutFillingRecipesClient() {

    }

    private final Map<String, SpoutFillingRecipes.Recipe> recipes = new HashMap<>();

    @Nonnull
    @Override
    public Map<String, SpoutFillingRecipes.Recipe> getRecipeMap() {
        return this.recipes;
    }
}
