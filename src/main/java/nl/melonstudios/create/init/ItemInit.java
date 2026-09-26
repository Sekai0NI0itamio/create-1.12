package nl.melonstudios.create.init;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.*;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nl.melonstudios.create.CreateLegacy;
import nl.melonstudios.create.block.BlockCasing;
import nl.melonstudios.create.block.deco.BlockFramedGlass;
import nl.melonstudios.create.block.deco.BlockWindowIron;
import nl.melonstudios.create.block.deco.BlockWindowWood;
import nl.melonstudios.create.block.state.EnumOrestoneVariant;
import nl.melonstudios.create.item.*;
import nl.melonstudios.create.item.train.ItemTrainSchedule;
import nl.melonstudios.create.util.FunnelSets;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class ItemInit {
    public static void load() {}


    public static final ArrayList<Item> ITEMS = new ArrayList<>();

    public static final ItemWrench WRENCH = (ItemWrench)
            registerItem(new ItemWrench()
            .setRegistryName("wrench").setUnlocalizedName("create.wrench"));
    public static final ItemIngredient INGREDIENT = registerItem(new ItemIngredient());
    public static final ItemGoggles GOGGLES = registerItem(new ItemGoggles());
    public static final ItemSandpaper SANDPAPER = registerItem(new ItemSandpaper());
    public static final ItemBeltConnector BELT_CONNECTOR = (ItemBeltConnector)
            registerItem(new ItemBeltConnector()
            .setRegistryName("belt_connector").setUnlocalizedName("create.belt_connector"));
    public static final ItemTreeFertilizer TREE_FERTILIZER = (ItemTreeFertilizer)
            registerItem(new ItemTreeFertilizer()
            .setRegistryName("tree_fertilizer").setUnlocalizedName("create.tree_fertilizer"));
    public static final ItemGlue SUPER_GLUE = (ItemGlue)
            registerItem(new ItemGlue(99)
            .setRegistryName("superglue").setUnlocalizedName("create.superglue"));
    public static final Item CRAFTER_COVER = registerItem(new Item()
            .setMaxStackSize(64).setCreativeTab(CreateTabs.TAB_CREATE)
            .setRegistryName("crafter_cover").setUnlocalizedName("create.crafter_cover"));
    public static final ItemArmorCardboard HELMET_CARDBOARD = registerItem(new ItemArmorCardboard(EntityEquipmentSlot.HEAD, "helmet"));
    public static final ItemArmorCardboard CHESTPLATE_CARDBOARD = registerItem(new ItemArmorCardboard(EntityEquipmentSlot.CHEST, "chestplate"));
    public static final ItemArmorCardboard LEGGINGS_CARDBOARD = registerItem(new ItemArmorCardboard(EntityEquipmentSlot.LEGS, "leggings"));
    public static final ItemArmorCardboard BOOTS_CARDBOARD = registerItem(new ItemArmorCardboard(EntityEquipmentSlot.FEET, "boots"));
    public static final ItemAssembly ASSEMBLY = registerItem(new ItemAssembly());
    public static final ItemSceneWand SCENE_WAND = registerItem(new ItemSceneWand());
    public static final ItemSchematic SCHEMATIC = (ItemSchematic)
            registerItem(new ItemSchematic()
            .setRegistryName("schematic").setUnlocalizedName("create.schematic").setCreativeTab(CreateTabs.TAB_CREATE));
    public static final ItemPackage PACKAGE = (ItemPackage)
            registerItem(new ItemPackage()
            .setRegistryName("package").setUnlocalizedName("create.package").setCreativeTab(CreateTabs.TAB_CREATE));

    public static final ItemBlockFunnel FUNNEL_ANDESITE = registerItem(new ItemBlockFunnel("andesite"));
    public static final ItemBlockFunnel FUNNEL_BRASS = registerItem(new ItemBlockFunnel("brass"));

    public static final ItemChromaticCompound CHROMATIC_COMPOUND = registerItem(new ItemChromaticCompound());
    public static final ItemShadowSteel SHADOW_STEEL = registerItem(new ItemShadowSteel());
    public static final ItemRefinedRadiance REFINED_RADIANCE = registerItem(new ItemRefinedRadiance());
    public static final ItemExperienceNugget EXPERIENCE_NUGGET = registerItem(new ItemExperienceNugget());
    public static final ItemBuildersTea BUILDERS_TEA = registerItem(new ItemBuildersTea());
    public static final ItemSweetRoll SWEET_ROLL = registerItem(new ItemSweetRoll());
    public static final ItemChocolateBar CHOCOLATE_BAR = registerItem(new ItemChocolateBar());
    public static final ItemCreativeBlazeCake CREATIVE_BLAZE_CAKE = registerItem(new ItemCreativeBlazeCake());
    public static final ItemCardboardSword CARDBOARD_SWORD = registerItem(new ItemCardboardSword());
    public static final ItemRedSandpaper RED_SANDPAPER = registerItem(new ItemRedSandpaper());
    public static final ItemRawZinc RAW_ZINC = registerItem(new ItemRawZinc());
    public static final ItemPotatoCannon POTATO_CANNON = (ItemPotatoCannon)
            registerItem(new ItemPotatoCannon()
            .setRegistryName("potato_cannon").setUnlocalizedName("create.potato_cannon"));
    public static final ItemZapperWorldshaper WORLDSHAPER = (ItemZapperWorldshaper)
            registerItem(new ItemZapperWorldshaper()
            .setRegistryName("handheld_worldshaper").setUnlocalizedName("create.handheld_worldshaper"));
    public static final ItemClipboard CLIPBOARD = (ItemClipboard)
            registerItem(new ItemClipboard());
    public static final ItemSymmetryWand SYMMETRY_WAND = (ItemSymmetryWand)
            registerItem(new ItemSymmetryWand()
            .setRegistryName("wand_of_symmetry").setUnlocalizedName("create.wand_of_symmetry"));
    public static final ItemExtendoGrip EXTENDO_GRIP = (ItemExtendoGrip)
            registerItem(new ItemExtendoGrip()
            .setRegistryName("extendo_grip").setUnlocalizedName("create.extendo_grip"));
    public static final ItemHat HAT = (ItemHat)
            registerItem(new ItemHat()
            .setRegistryName("hat").setUnlocalizedName("create.hat"));
    public static final ItemBlueprint BLUEPRINT = (ItemBlueprint)
            registerItem(new ItemBlueprint()
            .setRegistryName("crafting_blueprint").setUnlocalizedName("create.crafting_blueprint"));
    public static final ItemTrainSchedule SCHEDULE = registerItem(new ItemTrainSchedule());
    public static final ItemDoor ANDESITE_DOOR = (ItemDoor)
            registerItem(new ItemDoor(BlockInit.ANDESITE_DOOR)
            .setRegistryName("andesite_door").setUnlocalizedName("create.andesite_door"));
    public static final ItemDoor BRASS_DOOR = (ItemDoor)
            registerItem(new ItemDoor(BlockInit.BRASS_DOOR)
            .setRegistryName("brass_door").setUnlocalizedName("create.brass_door"));
    public static final ItemDoor COPPER_DOOR = (ItemDoor)
            registerItem(new ItemDoor(BlockInit.COPPER_DOOR)
            .setRegistryName("copper_door").setUnlocalizedName("create.copper_door"));
    public static final ItemDoor FRAMED_GLASS_DOOR = (ItemDoor)
            registerItem(new ItemDoor(BlockInit.FRAMED_GLASS_DOOR)
            .setRegistryName("framed_glass_door").setUnlocalizedName("create.framed_glass_door"));
    public static final ItemDoor TRAIN_DOOR = (ItemDoor)
            registerItem(new ItemDoor(BlockInit.TRAIN_DOOR)
            .setRegistryName("train_door").setUnlocalizedName("create.train_door"));
    public static final ItemAttributeFilter ATTRIBUTE_FILTER = (ItemAttributeFilter)
            registerItem(new ItemAttributeFilter()
            .setRegistryName("attribute_filter").setUnlocalizedName("create.attribute_filter"));
    public static final ItemPackageFilter PACKAGE_FILTER = (ItemPackageFilter)
            registerItem(new ItemPackageFilter()
            .setRegistryName("package_filter").setUnlocalizedName("create.package_filter"));
    public static final ItemEmptySchematic EMPTY_SCHEMATIC = (ItemEmptySchematic)
            registerItem(new ItemEmptySchematic()
            .setRegistryName("empty_schematic").setUnlocalizedName("create.empty_schematic"));
    public static final ItemSchematicAndQuill SCHEMATIC_AND_QUILL = (ItemSchematicAndQuill)
            registerItem(new ItemSchematicAndQuill()
            .setRegistryName("schematic_and_quill").setUnlocalizedName("create.schematic_and_quill"));
    public static final ItemLinkedController LINKED_CONTROLLER = (ItemLinkedController)
            registerItem(new ItemLinkedController()
            .setRegistryName("linked_controller").setUnlocalizedName("create.linked_controller"));
    public static final ItemMinecartContraption MINECART_CONTRAPTION = (ItemMinecartContraption)
            registerItem(new ItemMinecartContraption()
            .setRegistryName("minecart_contraption").setUnlocalizedName("create.minecart_contraption"));
    public static final ItemMinecartCoupling MINECART_COUPLING = (ItemMinecartCoupling)
            registerItem(new ItemMinecartCoupling()
            .setRegistryName("minecart_coupling").setUnlocalizedName("create.minecart_coupling"));
    public static final ItemCopperDivingHelmet COPPER_DIVING_HELMET = (ItemCopperDivingHelmet)
            registerItem(new ItemCopperDivingHelmet()
            .setRegistryName("copper_diving_helmet").setUnlocalizedName("create.copper_diving_helmet"));
    public static final ItemCopperDivingBoots COPPER_DIVING_BOOTS = (ItemCopperDivingBoots)
            registerItem(new ItemCopperDivingBoots()
            .setRegistryName("copper_diving_boots").setUnlocalizedName("create.copper_diving_boots"));
    public static final ItemShoppingList SHOPPING_LIST = (ItemShoppingList)
            registerItem(new ItemShoppingList()
            .setRegistryName("shopping_list").setUnlocalizedName("create.shopping_list"));

    private static <T extends Item> T registerItem(T item) {
        ITEMS.add(item);
        return item;
    }

    @SideOnly(Side.CLIENT)
    public static void setItemModels() {
        // items
        CreateLegacy.proxy.setItemModel(WRENCH);
        for (int i = 0; i < ItemIngredient.NAME_LOOKUP.length; i++) {
            CreateLegacy.proxy.setItemModel(INGREDIENT, i, "ingredient/" + ItemIngredient.NAME_LOOKUP[i]);
        }
        CreateLegacy.proxy.setItemModel(GOGGLES);
        CreateLegacy.proxy.setItemModel(SANDPAPER);
        CreateLegacy.proxy.setItemModel(BELT_CONNECTOR);
        CreateLegacy.proxy.setItemModel(TREE_FERTILIZER);
        CreateLegacy.proxy.setItemModel(SUPER_GLUE);
        CreateLegacy.proxy.setItemModel(CRAFTER_COVER);
        CreateLegacy.proxy.setItemModel(HELMET_CARDBOARD);
        CreateLegacy.proxy.setItemModel(CHESTPLATE_CARDBOARD);
        CreateLegacy.proxy.setItemModel(LEGGINGS_CARDBOARD);
        CreateLegacy.proxy.setItemModel(BOOTS_CARDBOARD);
        CreateLegacy.proxy.setItemModel(SCENE_WAND);
        CreateLegacy.proxy.setItemModel(SCHEMATIC);
        CreateLegacy.proxy.setItemModel(PACKAGE);
        CreateLegacy.proxy.setItemModel(ASSEMBLY, 0, "assembly/" + ItemAssembly.NAME_LOOKUP[0]);
        CreateLegacy.proxy.setItemModel(ASSEMBLY, 1, "assembly/" + ItemAssembly.NAME_LOOKUP[1]);
        CreateLegacy.proxy.setItemModel(FUNNEL_ANDESITE);
        CreateLegacy.proxy.setItemModel(FUNNEL_BRASS);
        CreateLegacy.proxy.setItemModel(CHROMATIC_COMPOUND);
        CreateLegacy.proxy.setItemModel(SHADOW_STEEL);
        CreateLegacy.proxy.setItemModel(REFINED_RADIANCE);
        CreateLegacy.proxy.setItemModel(EXPERIENCE_NUGGET);
        CreateLegacy.proxy.setItemModel(BUILDERS_TEA);
        CreateLegacy.proxy.setItemModel(SWEET_ROLL);
        CreateLegacy.proxy.setItemModel(CHOCOLATE_BAR);
        CreateLegacy.proxy.setItemModel(CREATIVE_BLAZE_CAKE);
        CreateLegacy.proxy.setItemModel(CARDBOARD_SWORD);
        CreateLegacy.proxy.setItemModel(RED_SANDPAPER);
        CreateLegacy.proxy.setItemModel(RAW_ZINC);
        CreateLegacy.proxy.setItemModel(POTATO_CANNON);
        CreateLegacy.proxy.setItemModel(CLIPBOARD);
        CreateLegacy.proxy.setItemModel(EXTENDO_GRIP);
        CreateLegacy.proxy.setItemModel(HAT, 0, "hat/conductor");
        CreateLegacy.proxy.setItemModel(HAT, 1, "hat/engineer");
        CreateLegacy.proxy.setItemModel(MINECART_CONTRAPTION, 0, "minecart_contraption/0");
        CreateLegacy.proxy.setItemModel(MINECART_CONTRAPTION, 1, "minecart_contraption/1");
        CreateLegacy.proxy.setItemModel(MINECART_CONTRAPTION, 2, "minecart_contraption/2");
        CreateLegacy.proxy.setItemModel(BLUEPRINT, 0, "blueprint");
        CreateLegacy.proxy.setItemModel(SYMMETRY_WAND, 0, "symmetry_wand");
        CreateLegacy.proxy.setItemModel(SCHEDULE);

        // blocks
        CreateLegacy.proxy.setItemModel(BlockInit.ORE, 0, "ore_copper");
        CreateLegacy.proxy.setItemModel(BlockInit.ORE, 1, "ore_zinc");

        CreateLegacy.proxy.setItemModel(BlockInit.METAL, 0, "block_andesite_alloy");
        CreateLegacy.proxy.setItemModel(BlockInit.METAL, 1, "block_copper");
        CreateLegacy.proxy.setItemModel(BlockInit.METAL, 2, "block_zinc");
        CreateLegacy.proxy.setItemModel(BlockInit.METAL, 3, "block_brass");

        for (int i = 0; i < 4; i++) {
            CreateLegacy.proxy.setItemModel(BlockInit.CASING, i, "casing_" + BlockCasing.Variant.byID(i).getName());
        }

        CreateLegacy.proxy.setItemModel(BlockInit.SHAFT);
        CreateLegacy.proxy.setItemModel(BlockInit.COG_SMALL);
        CreateLegacy.proxy.setItemModel(BlockInit.COG_LARGE);
        CreateLegacy.proxy.setItemModel(BlockInit.GEARBOX, 0, "gearbox");
        CreateLegacy.proxy.setItemModel(BlockInit.GEARBOX, 1, "gearbox_vertical");
        CreateLegacy.proxy.setItemModel(BlockInit.GEARSHIFT);
        CreateLegacy.proxy.setItemModel(BlockInit.CLUTCH);
        CreateLegacy.proxy.setItemModel(BlockInit.HAND_CRANK);
        CreateLegacy.proxy.setItemModel(BlockInit.WATER_WHEEL);
        CreateLegacy.proxy.setItemModel(BlockInit.CREATIVE_MOTOR);
        CreateLegacy.proxy.setItemModel(BlockInit.TURNTABLE);
        CreateLegacy.proxy.setItemModel(BlockInit.BEARING);
        CreateLegacy.proxy.setItemModel(BlockInit.BEARING_WINDMILL);
        CreateLegacy.proxy.setItemModel(BlockInit.CHASSIS_RADIAL);
        CreateLegacy.proxy.setItemModel(BlockInit.CHASSIS_LINEAR, 0, "chassis_linear");
        CreateLegacy.proxy.setItemModel(BlockInit.CHASSIS_LINEAR, 1, "chassis_linear_secondary");
        CreateLegacy.proxy.setItemModel(BlockInit.SAIL_ITEM);
        CreateLegacy.proxy.setItemModel(BlockInit.PISTON_POLE);
        CreateLegacy.proxy.setItemModel(BlockInit.MECHANICAL_PISTON);
        CreateLegacy.proxy.setItemModel(BlockInit.MECHANICAL_PISTON_STICKY);
        CreateLegacy.proxy.setItemModel(BlockInit.SPEEDOMETER);
        CreateLegacy.proxy.setItemModel(BlockInit.STRESSOMETER);
        CreateLegacy.proxy.setItemModel(BlockInit.PRESS);
        CreateLegacy.proxy.setItemModel(BlockInit.MIXER);
        CreateLegacy.proxy.setItemModel(BlockInit.DRILL);
        CreateLegacy.proxy.setItemModel(BlockInit.SAW);
        CreateLegacy.proxy.setItemModel(BlockInit.DEPLOYER);
        CreateLegacy.proxy.setItemModel(BlockInit.AUTO_FARM, 0, "plough");
        CreateLegacy.proxy.setItemModel(BlockInit.AUTO_FARM, 1, "harvester");
        CreateLegacy.proxy.setItemModel(BlockInit.CONTRAPTION_INTERFACE, 0, "interface_storage");
        CreateLegacy.proxy.setItemModel(BlockInit.CONTRAPTION_INTERFACE, 1, "interface_fluid");
        CreateLegacy.proxy.setItemModel(BlockInit.MILLSTONE);
        CreateLegacy.proxy.setItemModel(BlockInit.CRUSHING_WHEEL);
        CreateLegacy.proxy.setItemModel(BlockInit.ENCASED_FAN);
        CreateLegacy.proxy.setItemModel(BlockInit.FLUID_TANK);
        CreateLegacy.proxy.setItemModel(BlockInit.SPOUT);
        CreateLegacy.proxy.setItemModel(BlockInit.HOSE_PULLEY);
        CreateLegacy.proxy.setItemModel(BlockInit.PORTABLE_FLUID_INTERFACE);
        CreateLegacy.proxy.setItemModel(BlockInit.STEAM_ENGINE);
        CreateLegacy.proxy.setItemModel(BlockInit.SPEED_CONTROLLER);
        CreateLegacy.proxy.setItemModel(BlockInit.SEQUENCED_GEARSHIFT);
        CreateLegacy.proxy.setItemModel(BlockInit.DISPLAY_LINK);
        CreateLegacy.proxy.setItemModel(BlockInit.NIXIE_TUBE);
        CreateLegacy.proxy.setItemModel(BlockInit.DISPLAY_BOARD);
        CreateLegacy.proxy.setItemModel(BlockInit.PACKAGER);
        CreateLegacy.proxy.setItemModel(BlockInit.STOCK_TICKER);
        CreateLegacy.proxy.setItemModel(BlockInit.ELEVATOR_PULLEY);
        CreateLegacy.proxy.setItemModel(BlockInit.ELEVATOR_CONTACT);
        CreateLegacy.proxy.setItemModel(BlockInit.GANTRY_CARRIAGE);
        CreateLegacy.proxy.setItemModel(BlockInit.ROPE_PULLEY);
        CreateLegacy.proxy.setItemModel(BlockInit.SCHEMATICANNON);
        CreateLegacy.proxy.setItemModel(BlockInit.TRAIN_TRACK);
        CreateLegacy.proxy.setItemModel(BlockInit.BOGEY);
        CreateLegacy.proxy.setItemModel(BlockInit.STATION);
        CreateLegacy.proxy.setItemModel(BlockInit.CRAFTER);
        CreateLegacy.proxy.setItemModel(BlockInit.BLAZE_BURNER, 0, "blaze_burner_empty");
        CreateLegacy.proxy.setItemModel(BlockInit.BLAZE_BURNER, 1, "blaze_burner");
        CreateLegacy.proxy.setItemModel(BlockInit.DEPOT);
        CreateLegacy.proxy.setItemModel(BlockInit.BASIN);
        CreateLegacy.proxy.setItemModel(BlockInit.CHUTE);
        CreateLegacy.proxy.setItemModel(BlockInit.ITEM_DRAIN);

        CreateLegacy.proxy.setItemModel(BlockInit.LATCH);
        CreateLegacy.proxy.setItemModel(BlockInit.TOGGLE_LATCH);
        CreateLegacy.proxy.setItemModel(BlockInit.REDSTONE_LINKER);

        for (int i = 0; i < 4; i++) {
            String type = BlockFramedGlass.Variant.byId(i).getName();
            CreateLegacy.proxy.setItemModel(BlockInit.FRAMED_GLASS, i, i != 0 ? "framed_glass_" + type : "framed_glass");
        }
        for (int i = 0; i < 6; i++) {
            String type = BlockWindowWood.Variant.byId(i).getName();
            CreateLegacy.proxy.setItemModel(BlockInit.WINDOW_WOOD, i, "window_" + type);
        }
        for (int i = 0; i < 3; i++) {
            String type = BlockWindowIron.Variant.byId(i).getName();
            CreateLegacy.proxy.setItemModel(BlockInit.WINDOW_IRON, i, "window_" + type);
        }

        for (int i = 0; i < 16; i++) {
            EnumDyeColor color = EnumDyeColor.byMetadata(i);
            CreateLegacy.proxy.setItemModel(BlockInit.POUF, i, "pouf/" + color.getDyeColorName());
        }

        for (int i = 0; i < 7; i++) {
            String type = EnumOrestoneVariant.byId(i).getName();
            CreateLegacy.proxy.setItemModel(BlockInit.ORESTONE, i ,"orestone/" + type);
            CreateLegacy.proxy.setItemModel(BlockInit.ORESTONE_CUT, i ,"orestone/cut/" + type);
            CreateLegacy.proxy.setItemModel(BlockInit.ORESTONE_POLISHED, i, "orestone/polished/" + type);
            CreateLegacy.proxy.setItemModel(BlockInit.ORESTONE_BRICKS, i ,"orestone/bricks/" + type);
            CreateLegacy.proxy.setItemModel(BlockInit.ORESTONE_BRICKS_FANCY, i ,"orestone/bricks_fancy/" + type);
            CreateLegacy.proxy.setItemModel(BlockInit.ORESTONE_LAYERED, i, "orestone/layered/" + type);
            CreateLegacy.proxy.setItemModel(BlockInit.ORESTONE_PILLAR_Y, i, "orestone/pillar/" + type);
        }
    }

    private ItemInit() {
        throw new AssertionError("no");
    }
}
