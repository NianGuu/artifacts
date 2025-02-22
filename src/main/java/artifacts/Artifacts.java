package artifacts;

import artifacts.common.capability.SwimHandler;
import artifacts.common.config.ModConfig;
import artifacts.common.init.*;
import artifacts.common.network.NetworkHandler;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotTypeMessage;
import top.theillusivec4.curios.api.SlotTypePreset;

@Mod(Artifacts.MODID)
public class Artifacts {

    public static final String MODID = "artifacts";

    public Artifacts() {
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ArtifactsClient::new);

        ModConfig.registerCommon();
        SwimHandler.init();

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.REGISTRY.register(modBus);
        ModEntityTypes.REGISTRY.register(modBus);
        ModSoundEvents.REGISTRY.register(modBus);
        ModFeatures.REGISTRY.register(modBus);
        ModLootModifiers.REGISTRY.register(modBus);

        modBus.addListener(this::commonSetup);
        modBus.addListener(this::enqueueIMC);

        modBus.addGenericListener(GlobalLootModifierSerializer.class, ModLootConditions::register);
        modBus.addListener(ModEntityTypes::registerAttributes);

        MinecraftForge.EVENT_BUS.addListener(this::addFeatures);
    }

    public void addFeatures(BiomeLoadingEvent event) {
        if (ModConfig.common.campsiteRarity.get() >= 10000) {
            return;
        }
        if (event.getCategory() != Biome.BiomeCategory.NETHER && event.getCategory() != Biome.BiomeCategory.THEEND && !ModConfig.common.isBlacklisted(event.getName())) {
            event.getGeneration().getFeatures(GenerationStep.Decoration.UNDERGROUND_STRUCTURES).add(
                    BuiltinRegistries.PLACED_FEATURE.getHolderOrThrow(
                            BuiltinRegistries.PLACED_FEATURE.getResourceKey(ModFeatures.UNDERGROUND_CAMPSITE).orElseThrow()
                    )
            );
        }
    }

    public void commonSetup(final FMLCommonSetupEvent event) {
        ModConfig.registerServer();
        event.enqueueWork(() -> {
            ModFeatures.register();
            NetworkHandler.register();
        });
    }

    public void enqueueIMC(final InterModEnqueueEvent event) {
        SlotTypePreset[] types = {SlotTypePreset.HEAD, SlotTypePreset.NECKLACE, SlotTypePreset.BELT};
        for (SlotTypePreset type : types) {
            InterModComms.sendTo(CuriosApi.MODID, SlotTypeMessage.REGISTER_TYPE, () -> type.getMessageBuilder().build());
        }
        InterModComms.sendTo(CuriosApi.MODID, SlotTypeMessage.REGISTER_TYPE, () -> SlotTypePreset.HANDS.getMessageBuilder().size(2).build());
        InterModComms.sendTo(CuriosApi.MODID, SlotTypeMessage.REGISTER_TYPE, () -> new SlotTypeMessage.Builder("feet").priority(220).icon(InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS).build());
    }
}
