package nl.melonstudios.create.recipe.client;

import com.melonstudios.melonlib.recipe.IRecipeTypeClient;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.recipe.server.DeployerApplicationRecipes;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

@SideOnly(Side.CLIENT)
public class DeployerApplicationRecipesClient implements IRecipeTypeClient<DeployerApplicationRecipes.Recipe, DeployerApplicationRecipes> {
    public static final DeployerApplicationRecipesClient instance = new DeployerApplicationRecipesClient();

    private DeployerApplicationRecipesClient() {

    }

    private final Map<String, DeployerApplicationRecipes.Recipe> recipes = new HashMap<>();

    @Nonnull
    @Override
    public Map<String, DeployerApplicationRecipes.Recipe> getRecipeMap() {
        return this.recipes;
    }
}
