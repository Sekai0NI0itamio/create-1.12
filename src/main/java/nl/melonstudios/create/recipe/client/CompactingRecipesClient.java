package nl.melonstudios.create.recipe.client;

import com.melonstudios.melonlib.recipe.IRecipeTypeClient;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.recipe.server.CompactingRecipes;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

@SideOnly(Side.CLIENT)
public class CompactingRecipesClient implements IRecipeTypeClient<CompactingRecipes.Recipe, CompactingRecipes> {
    public static final CompactingRecipesClient instance = new CompactingRecipesClient();

    private CompactingRecipesClient() {

    }

    private final Map<String, CompactingRecipes.Recipe> recipes = new HashMap<>();

    @Nonnull
    @Override
    public Map<String, CompactingRecipes.Recipe> getRecipeMap() {
        return this.recipes;
    }
}
