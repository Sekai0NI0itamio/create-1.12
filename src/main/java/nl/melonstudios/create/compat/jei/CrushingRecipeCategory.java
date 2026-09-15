package nl.melonstudios.create.compat.jei;

import com.melonstudios.melonlib.misc.Localizer;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.util.ResourceLocation;

public class CrushingRecipeCategory implements IRecipeCategory<JEICrushingRecipe> {
    private static final ResourceLocation TEXTURES =
            new ResourceLocation("create", "textures/gui/jei/crushing.png");

    protected static final int input = 0;

    private final IDrawable background;

    @Override
    public String getUid() {
        return "create.crushing";
    }

    @Override
    public String getTitle() {
        return Localizer.translate("recipe.create.crushing");
    }

    @Override
    public String getModName() {
        return "create";
    }

    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public void setRecipe(IRecipeLayout iRecipeLayout, JEICrushingRecipe wrapper, IIngredients iIngredients) {
        IGuiItemStackGroup stacks = iRecipeLayout.getItemStacks();
        stacks.init(input, true, 1, 7);
        int x = 45;
        for (int i = 0; i < wrapper.outputs.size() && i < 7; i++) {
            stacks.init(input + 1 + i, false, x, 7);
            x += 18;
        }
        stacks.set(iIngredients);
    }

    public CrushingRecipeCategory(IGuiHelper helper) {
        this.background = helper.drawableBuilder(TEXTURES, 0, 0, 170, 32).setTextureSize(170, 32).build();
    }
}
