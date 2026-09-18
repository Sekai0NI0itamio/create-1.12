package nl.melonstudios.create.recipe.sequence;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.MapMaker;
import com.melonstudios.melonlib.misc.Localizer;
import com.melonstudios.melonlib.recipe.Ingredient;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import nl.melonstudios.create.CreateLegacy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentMap;

public class SequenceRecipe {
    public final Ingredient input;
    public final ItemStack processing;
    public final List<SequenceStep> steps;
    public final int repetitions;
    public final SequenceResult result;

    public SequenceRecipe(Ingredient input, ItemStack processing, List<SequenceStep> steps, int repetitions, SequenceResult result) {
        this.input = input;
        this.processing = processing;
        this.steps = steps;
        this.repetitions = repetitions;
        this.result = result;
        this.validate();
    }

    public SequenceRecipe(NBTTagCompound nbt) {
        this.input = Ingredient.read(nbt.getCompoundTag("Input"));
        this.processing = new ItemStack(nbt.getCompoundTag("Processing"));
        NBTTagList stepsNBT = nbt.getTagList("Steps", 10);
        List<SequenceStep> list = new ArrayList<>();
        for (int i = 0; i < stepsNBT.tagCount(); i++) {
            NBTTagCompound stepNBT = stepsNBT.getCompoundTagAt(i);
            list.add(new SequenceStep(stepNBT.getString("name"), stepNBT.getCompoundTag("Data")));
        }
        this.steps = ImmutableList.copyOf(list);
        this.repetitions = Math.max(nbt.getInteger("repetitions"), 1);
        this.result = new SequenceResult(nbt.getCompoundTag("Result"));
        this.validate();
    }

    private void validate() {
        if (this.steps.isEmpty()) throw new RuntimeException("Stepless sequence recipe");
        if (this.repetitions <= 0) throw new RuntimeException("Stepless sequence recipe");
    }
    public SequenceStep getFirstStep() {
        return this.steps.get(0);
    }
    public SequenceStep getStep(int index) {
        return this.steps.get(index % this.steps.size());
    }

    public static boolean isInSequence(ItemStack processing) {
        return processing.getSubCompound("SequencedAssembly") != null;
    }
    public static SequenceStep getNextStep(ItemStack processing) {
        NBTTagCompound data = processing.getOrCreateSubCompound("SequencedAssembly");
        String id = data.getString("id");
        int step = data.getInteger("step");
        SequenceRecipe recipe = SequencedRecipes.instance.getRecipe(id);
        return recipe.steps.get(step % recipe.steps.size());
    }
    public static void initialize(ItemStack processing, String id) {
        NBTTagCompound data = processing.getOrCreateSubCompound("SequencedAssembly");
        data.setString("id", id);
        data.setInteger("step", 0);
    }
    public static ItemStack advance(ItemStack processing) {
        NBTTagCompound data = processing.getOrCreateSubCompound("SequencedAssembly");
        String id = data.getString("id");
        int nextStep = data.getInteger("step") + 1;
        SequenceRecipe recipe = SequencedRecipes.instance.getRecipe(id);
        int max = recipe.steps.size() * recipe.repetitions;
        if (nextStep >= max) {
            Random rnd = CreateLegacy.rand;
            if (rnd.nextFloat() < recipe.result.chance) {
                return recipe.result.expected.copy();
            } else {
                Object2FloatMap<ItemStack> waste = recipe.result.waste;
                float weight = 0.0F;
                for (float f : waste.values()) {
                    weight += f;
                }
                float selector = rnd.nextFloat() * weight;
                for (Object2FloatMap.Entry<ItemStack> entry : waste.object2FloatEntrySet()) {
                    if (selector < entry.getFloatValue()) {
                        return entry.getKey().copy();
                    }
                    selector -= entry.getFloatValue();
                }
            }
        }
        data.setInteger("step", nextStep);
        return processing;
    }

    private static final ConcurrentMap<SequenceStep, Ingredient> STACK_DISPLAY_CACHE = new MapMaker().weakKeys().makeMap();
    private static final HashMap<String, Format> FORMATTERS = new HashMap<>();
    public static void registerFormat(String id, Format format) {
        FORMATTERS.put(id, format);
    }
    public static Format getFormat(String id) {
        return FORMATTERS.getOrDefault(id, Format.DEFAULT);
    }
    public interface Format {
        Format DEFAULT = (step) -> step.name;

        String getDisplayName(SequenceStep step);
    }
    static {
        registerFormat("pressing", (step) -> Localizer.translate("sequence.pressing"));
        registerFormat("deploying", (step) -> {
            Ingredient applied = STACK_DISPLAY_CACHE.get(step);
            if (applied == null) {
                applied = Ingredient.read(step.data.getCompoundTag("Applied"));
                STACK_DISPLAY_CACHE.put(step, applied);
            }
            return Localizer.translate("sequence.deploying", applied.getDisplayName());
        });
        registerFormat("cutting", (step) -> Localizer.translate("sequence.cutting"));
    }
}
