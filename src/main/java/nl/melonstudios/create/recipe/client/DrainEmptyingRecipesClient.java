package nl.melonstudios.create.recipe.client;

import com.melonstudios.melonlib.recipe.IRecipeTypeClient;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.recipe.server.DrainEmptyingRecipes;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

@SideOnly(Side.CLIENT)
public class DrainEmptyingRecipesClient implements IRecipeTypeClient<DrainEmptyingRecipes.Recipe, DrainEmptyingRecipes> {
    public static final DrainEmptyingRecipesClient instance = new DrainEmptyingRecipesClient();

    private DrainEmptyingRecipesClient() {

    }

    private final Map<String, DrainEmptyingRecipes.Recipe> recipes = new HashMap<>();

    @Nonnull
    @Override
    public Map<String, DrainEmptyingRecipes.Recipe> getRecipeMap() {
        return this.recipes;
    }
}
