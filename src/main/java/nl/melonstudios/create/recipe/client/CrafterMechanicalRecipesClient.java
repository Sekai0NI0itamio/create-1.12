package nl.melonstudios.create.recipe.client;

import com.melonstudios.melonlib.recipe.IRecipeTypeClient;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.recipe.server.CrafterMechanicalRecipes;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

@SideOnly(Side.CLIENT)
public class CrafterMechanicalRecipesClient implements IRecipeTypeClient<CrafterMechanicalRecipes.Recipe, CrafterMechanicalRecipes> {
    public static final CrafterMechanicalRecipesClient instance = new CrafterMechanicalRecipesClient();

    private CrafterMechanicalRecipesClient() {

    }

    private final Map<String, CrafterMechanicalRecipes.Recipe> recipes = new HashMap<>();

    @Nonnull
    @Override
    public Map<String, CrafterMechanicalRecipes.Recipe> getRecipeMap() {
        return this.recipes;
    }
}
