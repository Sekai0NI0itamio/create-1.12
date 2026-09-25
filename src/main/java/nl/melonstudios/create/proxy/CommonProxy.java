package nl.melonstudios.create.proxy;

import com.melonstudios.melonlib.recipe.RecipeRegistry;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import nl.melonstudios.create.recipe.sequence.SequencedRecipes;
import nl.melonstudios.create.recipe.server.BlastingRecipes;
import nl.melonstudios.create.recipe.server.CrushingRecipes;
import nl.melonstudios.create.recipe.server.CuttingRecipes;
import nl.melonstudios.create.recipe.server.DeployerRecipes;
import nl.melonstudios.create.recipe.server.HauntingRecipes;
import nl.melonstudios.create.recipe.server.MixingRecipes;
import nl.melonstudios.create.recipe.server.PressingRecipes;
import nl.melonstudios.create.recipe.server.SmokingRecipes;
import nl.melonstudios.create.recipe.server.SplashingRecipes;
import nl.melonstudios.create.recipe.server.CompactingRecipes;
import nl.melonstudios.create.recipe.server.SpoutFillingRecipes;
import nl.melonstudios.create.recipe.server.DrainEmptyingRecipes;
import nl.melonstudios.create.recipe.server.CrafterMechanicalRecipes;
import nl.melonstudios.create.recipe.server.SandpaperPolishingRecipes;
import nl.melonstudios.create.recipe.server.DeployerApplicationRecipes;
import nl.melonstudios.create.tileentity.*;
import nl.melonstudios.create.tileentity.actor.*;
import nl.melonstudios.create.tileentity.fluid.*;
import nl.melonstudios.create.tileentity.funnel.TileEntityFunnelWall;
import nl.melonstudios.create.tileentity.funnel.TileEntityFunnelWallAdvanced;
import nl.melonstudios.create.tileentity.funnel.TileEntityFunnelDown;
import nl.melonstudios.create.tileentity.funnel.TileEntityFunnelDownAdvanced;
import nl.melonstudios.create.tileentity.generator.*;
import nl.melonstudios.create.tileentity.logistics.TileEntityPackager;
import nl.melonstudios.create.tileentity.logistics.TileEntityVault;
import nl.melonstudios.create.tileentity.logistics.TileEntityBrassTunnel;
import nl.melonstudios.create.tileentity.logistics.TileEntityCreativeCrate;
import nl.melonstudios.create.tileentity.logistics.TileEntityItemHatch;
import nl.melonstudios.create.tileentity.logistics.TileEntityFrogport;
import nl.melonstudios.create.tileentity.logistics.TileEntityPackagerLink;
import nl.melonstudios.create.tileentity.logistics.TileEntityRedstoneRequester;
import nl.melonstudios.create.tileentity.logistics.TileEntityTableCloth;
import nl.melonstudios.create.tileentity.logistics.TileEntityFactoryBoard;
import nl.melonstudios.create.tileentity.logistics.TileEntityAndesiteTunnel;
import nl.melonstudios.create.tileentity.logistics.TileEntityPostbox;
import nl.melonstudios.create.tileentity.logistics.TileEntityRepackager;
import nl.melonstudios.create.tileentity.deco.TileEntityCopycat;
import nl.melonstudios.create.tileentity.redstone.TileEntityDisplayLink;
import nl.melonstudios.create.tileentity.redstone.TileEntityRedstoneLinker;
import nl.melonstudios.create.tileentity.redstone.TileEntityAnalogLever;
import nl.melonstudios.create.tileentity.redstone.TileEntityDeskBell;
import nl.melonstudios.create.tileentity.redstone.TileEntitySmartObserver;
import nl.melonstudios.create.tileentity.redstone.TileEntityThresholdSwitch;
import nl.melonstudios.create.tileentity.redstone.TileEntityPulseRepeater;
import nl.melonstudios.create.tileentity.redstone.TileEntityPulseExtender;
import nl.melonstudios.create.tileentity.redstone.TileEntityPulseTimer;
import nl.melonstudios.create.tileentity.redstone.TileEntityPlacard;
import nl.melonstudios.create.tileentity.redstone.TileEntityLecternController;
import nl.melonstudios.create.tileentity.train.TileEntityBogey;
import nl.melonstudios.create.tileentity.train.TileEntityStation;
import nl.melonstudios.create.tileentity.train.TileEntityTrainSignal;
import nl.melonstudios.create.tileentity.train.TileEntitySteamWhistle;
import nl.melonstudios.create.tileentity.train.TileEntityTrainControls;
import nl.melonstudios.create.tileentity.train.TileEntityTrackObserver;

public class CommonProxy {
    public Side getSide() {
        return Side.SERVER;
    }

    protected static ResourceLocation create(String path) {
        return new ResourceLocation("create", path);
    }
    public void setItemModel(Item item, int meta, String file) {}
    public void setItemModel(Item item, String file) {
        this.setItemModel(item, 0, file);
    }
    public void setItemModel(Item item) {
        this.setItemModel(item, item.getRegistryName().getResourcePath());
    }

    public void setItemModel(Block item, int meta, String file) {
        this.setItemModel(Item.getItemFromBlock(item), meta, file);
    }
    public void setItemModel(Block item, String file) {
        this.setItemModel(Item.getItemFromBlock(item), file);
    }
    public void setItemModel(Block item) {
        this.setItemModel(Item.getItemFromBlock(item));
    }

    public void clientPreInit(FMLPreInitializationEvent event) {}
    public void clientInit(FMLInitializationEvent event) {}
    public void clientPostInit(FMLPostInitializationEvent event) {}

    public void registerTileEntities() {
        this.registerTE(TileEntityShaft.class, "shaft");
        this.registerTE(TileEntityCogwheel.class, "cogwheel");
        this.registerTE(TileEntityGearbox.class, "gearbox");
        this.registerTE(TileEntityGearshift.class, "gearshift");
        this.registerTE(TileEntityClutch.class, "clutch");
        this.registerTE(TileEntityHandCrank.class, "hand_crank");
        this.registerTE(TileEntityWaterWheel.class, "water_wheel");
        this.registerTE(TileEntityWaterWheelTemp.class, "water_wheel_temp");
        this.registerTE(TileEntityCreativeMotor.class, "creative_motor");
        this.registerTE(TileEntityTurntable.class, "turntable");
        this.registerTE(TileEntityBearing.class, "bearing");
        this.registerTE(TileEntityBearingWindmill.class, "bearing_windmill");
        this.registerTE(TileEntityDistanceController.class, "distance_controller");
        this.registerTE(TileEntityMechanicalPiston.class, "mechanical_piston");
        this.registerTE(TileEntitySpeedometer.class, "speedometer");
        this.registerTE(TileEntityStressometer.class, "stressometer");
        this.registerTE(TileEntityPress.class, "press");
        this.registerTE(TileEntityMixer.class, "mixer");
        this.registerTE(TileEntityDrill.class, "drill");
        this.registerTE(TileEntitySaw.class, "saw");
        this.registerTE(TileEntitySawProcessing.class, "saw_processing");
        this.registerTE(TileEntityDeployer.class, "deployer");
        this.registerTE(TileEntityPlough.class, "plough");
        this.registerTE(TileEntityHarvester.class, "harvester");
        this.registerTE(TileEntityStorageInterface.class, "storage_interface");
        this.registerTE(TileEntityMillstone.class, "millstone");
        this.registerTE(TileEntityCrushingWheel.class, "crushing_wheel");
        this.registerTE(TileEntityEncasedFan.class, "encased_fan");
        this.registerTE(TileEntityFluidTank.class, "fluid_tank");
        this.registerTE(TileEntitySpout.class, "spout");
        this.registerTE(TileEntityHosePulley.class, "hose_pulley");
        this.registerTE(TileEntityFluidPipe.class, "fluid_pipe");
        this.registerTE(TileEntityMechanicalPump.class, "mechanical_pump");
        this.registerTE(TileEntityFluidValve.class, "fluid_valve");
        this.registerTE(TileEntityMechanicalArm.class, "mechanical_arm");
        this.registerTE(TileEntityChainDrive.class, "chain_drive");
        this.registerTE(TileEntityChainConveyor.class, "chain_conveyor");
        this.registerTE(TileEntityBrassTunnel.class, "brass_tunnel");
        this.registerTE(TileEntityCreativeCrate.class, "creative_crate");
        this.registerTE(TileEntityItemHatch.class, "item_hatch");
        this.registerTE(TileEntityFrogport.class, "frogport");
        this.registerTE(TileEntityPackagerLink.class, "packager_link");
        this.registerTE(TileEntityRedstoneRequester.class, "redstone_requester");
        this.registerTE(TileEntityTableCloth.class, "table_cloth");
        this.registerTE(TileEntityFactoryBoard.class, "factory_board");
        this.registerTE(TileEntityAndesiteTunnel.class, "andesite_tunnel");
        this.registerTE(TileEntityPostbox.class, "postbox");
        this.registerTE(TileEntityRepackager.class, "repackager");
        this.registerTE(TileEntityPulseRepeater.class, "pulse_repeater");
        this.registerTE(TileEntityPulseExtender.class, "pulse_extender");
        this.registerTE(TileEntityPulseTimer.class, "pulse_timer");
        this.registerTE(TileEntityPlacard.class, "placard");
        this.registerTE(TileEntityLecternController.class, "lectern_controller");
        this.registerTE(TileEntitySchematicTable.class, "schematic_table");
        this.registerTE(TileEntityCopycat.class, "copycat");
        this.registerTE(TileEntityLargeWaterWheel.class, "large_water_wheel");
        this.registerTE(TileEntityAdjustableChainGearshift.class, "adjustable_chain_gearshift");
        this.registerTE(TileEntitySmartChute.class, "smart_chute");
        this.registerTE(TileEntitySmartFluidPipe.class, "smart_fluid_pipe");
        this.registerTE(TileEntityValveHandle.class, "valve_handle");
        this.registerTE(TileEntitySteamWhistle.class, "steam_whistle");
        this.registerTE(TileEntityTrainControls.class, "train_controls");
        this.registerTE(TileEntityTrackObserver.class, "track_observer");
        this.registerTE(TileEntityClockworkBearing.class, "clockwork_bearing");
        this.registerTE(TileEntityRoller.class, "mechanical_roller");
        this.registerTE(TileEntitySticker.class, "sticker");
        this.registerTE(TileEntityCopperBacktank.class, "copper_backtank");
        this.registerTE(TileEntityNetheriteBacktank.class, "netherite_backtank");
        this.registerTE(TileEntityToolbox.class, "toolbox");
        this.registerTE(TileEntityClipboard.class, "clipboard");
        this.registerTE(TileEntityAnalogLever.class, "analog_lever");
        this.registerTE(TileEntityDeskBell.class, "desk_bell");
        this.registerTE(TileEntitySmartObserver.class, "smart_observer");
        this.registerTE(TileEntityThresholdSwitch.class, "threshold_switch");
        this.registerTE(TileEntityTrainSignal.class, "train_signal");
        this.registerTE(TileEntityPortableFluidInterface.class, "portable_fluid_interface");
        this.registerTE(TileEntitySteamEngine.class, "steam_engine");
        this.registerTE(TileEntitySpeedController.class, "speed_controller");
        this.registerTE(TileEntitySequencedGearshift.class, "sequenced_gearshift");
        this.registerTE(TileEntityDisplayLink.class, "display_link");
        this.registerTE(TileEntityPackager.class, "packager");
        this.registerTE(TileEntityElevatorPulley.class, "elevator_pulley");
        this.registerTE(TileEntityElevatorContact.class, "elevator_contact");
        this.registerTE(TileEntityGantryCarriage.class, "gantry_carriage");
        this.registerTE(TileEntitySchematicannon.class, "schematicannon");
        this.registerTE(TileEntityBogey.class, "bogey");
        this.registerTE(TileEntityStation.class, "station");
        this.registerTE(TileEntityCrafter.class, "crafter");
        this.registerTE(TileEntityBlazeBurner.class, "blaze_burner");
        this.registerTE(TileEntityDepot.class, "depot");
        this.registerTE(TileEntityBasin.class, "basin");
        this.registerTE(TileEntityChute.class, "chute");
        this.registerTE(TileEntityBeltStraight.class, "belt_straight");
        this.registerTE(TileEntityBeltDiagonal.class, "belt_diagonal");
        this.registerTE(TileEntityItemDrain.class, "item_drain");
        this.registerTE(TileEntityFunnelWall.class, "funnel_wall");
        this.registerTE(TileEntityFunnelWallAdvanced.class, "funnel_wall_advanced");
        this.registerTE(TileEntityFunnelDown.class, "funnel_down");
        this.registerTE(TileEntityFunnelDownAdvanced.class, "funnel_down_advanced");
        this.registerTE(TileEntityRedstoneLinker.class, "redstone_linker");
        this.registerTE(TileEntityVault.class, "vault");
    }    public void registerEntityRenderers() {}

    public void pork() {}

    private void registerTE(Class<? extends TileEntity> te, String name) {
        GameRegistry.registerTileEntity(te, create(name));
    }

    public void spawnRedstoneFX(double x, double y, double z, double mx, double my, double mz, float size, float r, float g, float b) {

    }
    public void spawnItemFX(double x, double y, double z, double mx, double my, double mz, ItemStack stack) {
        this.spawnItemFX(x, y, z, mx, my, mz, Item.getIdFromItem(stack.getItem()), stack.getMetadata());
    }
    public void spawnItemFX(double x, double y, double z, double mx, double my, double mz, int id, int meta) {

    }
    public void millstoneFX(TileEntityMillstone millstone) {

    }
    public void mixerFX(TileEntityBasin basin, double x, double y, double z) {

    }
    public void airFlowFX(World world, double x, double y, double z, double mx, double my, double mz, int tint) {

    }
    public void wifiFX(World world, double x, double y, double z, int tint) {

    }
    public void spoutFX(World world, double x, double y, double z, int tint) {

    }

    public void initiatePonders() {}

    public void registerRecipeTypes() {
        RecipeRegistry.registerServer("create:pressing", PressingRecipes.instance);
        RecipeRegistry.registerServer("create:crushing", CrushingRecipes.instance);
        RecipeRegistry.registerServer("create:splashing", SplashingRecipes.instance);
        RecipeRegistry.registerServer("create:haunting", HauntingRecipes.instance);
        RecipeRegistry.registerServer("create:smoking", SmokingRecipes.instance);
        RecipeRegistry.registerServer("create:blasting", BlastingRecipes.instance);
        RecipeRegistry.registerServer("create:cutting", CuttingRecipes.instance);
        RecipeRegistry.registerServer("create:mixing", MixingRecipes.instance);
        RecipeRegistry.registerServer("create:deploying", DeployerRecipes.instance);
        RecipeRegistry.registerServer("create:sequence", SequencedRecipes.instance);
        RecipeRegistry.registerServer("create:compacting", CompactingRecipes.instance);
        RecipeRegistry.registerServer("create:filling", SpoutFillingRecipes.instance);
        RecipeRegistry.registerServer("create:emptying", DrainEmptyingRecipes.instance);
        RecipeRegistry.registerServer("create:mechanical_crafting", CrafterMechanicalRecipes.instance);
        RecipeRegistry.registerServer("create:sandpaper_polishing", SandpaperPolishingRecipes.instance);
        RecipeRegistry.registerServer("create:item_application", DeployerApplicationRecipes.instance);
    }
}
