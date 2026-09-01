package com.csykes.searchlight;

import com.csykes.searchlight.features.colour_lamp_slab.ColourLampSlabBlock;
import com.csykes.searchlight.features.wall_light.WallLightBlock;
import com.csykes.searchlight.integration.cc_tweaked.CCIntegration;
import com.csykes.searchlight.integration.dyenamics.DyenamicsIntegration;
import com.csykes.searchlight.network.SetLightAddressPayload;
import com.csykes.searchlight.utils.lighting.AddressableLight;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.csykes.searchlight.utils.lighting.AbstractLightBlock.LIT;
import static net.minecraft.world.level.block.SoundType.GLASS;
import static net.minecraft.world.level.block.SoundType.METAL;
import static net.minecraft.world.level.block.SoundType.STONE;
import static net.minecraft.world.level.material.PushReaction.DESTROY;

@Mod(Searchlight.MODID)
public class Searchlight {
    public static final String MODID = "searchlight";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final int MAX_DISTANCE = 256;
    public static final ResourceLocation LIGHT_DATA_COMPONENT = ResourceLocation.fromNamespaceAndPath(MODID, "light_data_component");

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);

    public static final Map<String, DeferredBlock<Block>> WALL_LIGHTS = new LinkedHashMap<>();
    public static final Map<String, DeferredBlock<Block>> CORNER_LIGHTS = new LinkedHashMap<>();
    public static final Map<String, DeferredBlock<Block>> CENTRE_LIGHTS = new LinkedHashMap<>();
    public static final Map<String, DeferredBlock<Block>> EDGE_LIGHTS = new LinkedHashMap<>();
    public static final Map<String, DeferredBlock<Block>> COLOUR_LAMPS = new LinkedHashMap<>();
    public static final Map<String, DeferredBlock<Block>> COLOUR_SLAB_LAMPS = new LinkedHashMap<>();
    public static final Map<String, DeferredBlock<Block>> SEARCHLIGHTS = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<? extends Item>> WALL_LIGHT_ITEMS = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<? extends Item>> CORNER_LIGHTS_ITEMS = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<? extends Item>> CENTRE_LIGHTS_ITEMS = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<? extends Item>> EDGE_LIGHTS_ITEMS = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<? extends Item>> COLOUR_LAMP_ITEMS = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<? extends Item>> COLOUR_SLAB_ITEMS = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<? extends Item>> SEARCHLIGHT_ITEMS = new LinkedHashMap<>();

    private static void registerWallLight(String postfix) {
        String wl_name = "wall_light_" + postfix;
        DeferredBlock<Block> block = BLOCKS.register(wl_name, () -> new WallLightBlock(BlockBehaviour.Properties.of()
                .lightLevel((state) -> state.hasProperty(LIT) && !state.getValue(LIT) ? 0 : 15)
                .strength(2.0f, 4.0f)
                .requiresCorrectToolForDrops()
                .sound(STONE)
                .noOcclusion()));

        WALL_LIGHTS.put(postfix, block);
        WALL_LIGHT_ITEMS.put(postfix, ITEMS.registerSimpleBlockItem(wl_name, block));
    }

    private static void registerSearchlight(String postfix) {
        String wl_name = "searchlight_" + postfix;
        final net.minecraft.world.item.DyeColor blockColor = net.minecraft.world.item.DyeColor.byName(postfix, net.minecraft.world.item.DyeColor.WHITE);

        DeferredBlock<Block> block = BLOCKS.register(wl_name, () -> new com.csykes.searchlight.features.searchlight.SearchlightBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                .lightLevel((state) -> state.hasProperty(LIT) && !state.getValue(LIT) ? 0 : 15)
                .pushReaction(DESTROY)
                .sound(METAL)
                .strength(2.0f, 4.0f)
                .requiresCorrectToolForDrops()
                .sound(STONE)
                .noOcclusion(), blockColor));

        SEARCHLIGHTS.put(postfix, block);
        SEARCHLIGHT_ITEMS.put(postfix, ITEMS.registerSimpleBlockItem(wl_name, block));
    }

    private static void registerCornerLight(String postfix) {

        String cl_name = "corner_light_" + postfix;

        final net.minecraft.world.item.DyeColor blockColor = net.minecraft.world.item.DyeColor.byName(postfix, net.minecraft.world.item.DyeColor.WHITE);

        DeferredBlock<Block> corner_light = BLOCKS.register(cl_name, () -> new com.csykes.searchlight.features.corner_light.CornerLightBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                .lightLevel((state) -> state.hasProperty(LIT) && !state.getValue(LIT) ? 0 : 15)
                .sound(GLASS)
                .strength(2.0f, 4.0f)
                .requiresCorrectToolForDrops()
                .noOcclusion(), blockColor));
        DeferredItem<net.minecraft.world.item.BlockItem> item = ITEMS.registerSimpleBlockItem(cl_name, corner_light);
        CORNER_LIGHTS.put(postfix, corner_light);
        CORNER_LIGHTS_ITEMS.put(postfix, item);
    }

    private static void registerEdgeLight(String postfix) {

        String cl_name = "edge_light_" + postfix;

        final net.minecraft.world.item.DyeColor blockColor = net.minecraft.world.item.DyeColor.byName(postfix, net.minecraft.world.item.DyeColor.WHITE);

        DeferredBlock<Block> edge_light = BLOCKS.register(cl_name, () -> new com.csykes.searchlight.features.edge_light.EdgeLightBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                .lightLevel((state) -> state.hasProperty(LIT) && !state.getValue(LIT) ? 0 : 15)
                .sound(GLASS)
                .strength(2.0f, 4.0f)
                .requiresCorrectToolForDrops()
                .noOcclusion(), blockColor));
        DeferredItem<net.minecraft.world.item.BlockItem> item = ITEMS.registerSimpleBlockItem(cl_name, edge_light);
        EDGE_LIGHTS.put(postfix, edge_light);
        EDGE_LIGHTS_ITEMS.put(postfix, item);
    }

    private static void registerCentreLight(String postfix) {

        String cl_name = "centre_light_" + postfix;

        final net.minecraft.world.item.DyeColor blockColor = net.minecraft.world.item.DyeColor.byName(postfix, net.minecraft.world.item.DyeColor.WHITE);

        DeferredBlock<Block> centre_light = BLOCKS.register(cl_name, () -> new com.csykes.searchlight.features.centre_light.CentreLightBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                .lightLevel((state) -> state.hasProperty(LIT) && !state.getValue(LIT) ? 0 : 15)
                .sound(GLASS)
                .strength(2.0f, 4.0f)
                .requiresCorrectToolForDrops()
                .noOcclusion(), blockColor));
        DeferredItem<net.minecraft.world.item.BlockItem> item = ITEMS.registerSimpleBlockItem(cl_name, centre_light);
        CENTRE_LIGHTS.put(postfix, centre_light);
        CENTRE_LIGHTS_ITEMS.put(postfix, item);
    }

    private static void registerColourLampLight(String postfix) {

        String cl_name = "colour_lamp_" + postfix;

        final net.minecraft.world.item.DyeColor blockColor = net.minecraft.world.item.DyeColor.byName(postfix, net.minecraft.world.item.DyeColor.WHITE);

        DeferredBlock<Block> centre_light = BLOCKS.register(cl_name, () -> new com.csykes.searchlight.features.colour_lamp.ColourLampBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                .lightLevel((state) -> state.hasProperty(LIT) && !state.getValue(LIT) ? 0 : 15)
                .sound(GLASS)
                .strength(2.0f, 4.0f)
                .requiresCorrectToolForDrops()
                .noOcclusion(), blockColor));
        DeferredItem<net.minecraft.world.item.BlockItem> item = ITEMS.registerSimpleBlockItem(cl_name, centre_light);
        COLOUR_LAMPS.put(postfix, centre_light);
        COLOUR_LAMP_ITEMS.put(postfix, item);
    }

    private static void registerColourLampSlabLight(String postfix) {

        String cl_name = "colour_lamp_slab_" + postfix;

        final DyeColor blockColor = DyeColor.byName(postfix, net.minecraft.world.item.DyeColor.WHITE);

        DeferredBlock<Block> colour_slab = BLOCKS.register(cl_name, () -> new ColourLampSlabBlock(BlockBehaviour.Properties.of()
                .lightLevel((state) -> state.hasProperty(LIT) && !state.getValue(LIT) ? 0 : 15)
                .sound(GLASS)
                .strength(2.0f, 4.0f)
                .requiresCorrectToolForDrops()
                .noOcclusion(), blockColor));
        DeferredItem<BlockItem> item = ITEMS.registerSimpleBlockItem(cl_name, colour_slab);
        COLOUR_SLAB_LAMPS.put(postfix, colour_slab);
        COLOUR_SLAB_ITEMS.put(postfix, item);
    }

    static {
        registerWallLight("iron");
        registerWallLight("copper");
        registerWallLight("prismarine");
        for (DyeColor color : DyeColor.values()) {
            registerWallLight(color.getName());
            registerCornerLight(color.getName());
            registerCentreLight(color.getName());
            registerEdgeLight(color.getName());
            registerColourLampLight(color.getName());
            registerColourLampSlabLight(color.getName());
            registerSearchlight(color.getName());
        }

        if (ModList.get().isLoaded("dyenamics")) {
            DyenamicsIntegration.init();
        }
    }

    public static final DeferredBlock<Block> SEARCHLIGHT_BLOCK = SEARCHLIGHTS.get("white");
    public static final DeferredItem<? extends Item> SEARCHLIGHT_ITEM = SEARCHLIGHT_ITEMS.get("white");

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.csykes.searchlight.features.wall_light.WallLightBlockEntity>> WALL_LIGHT_BE = BLOCK_ENTITY_TYPES.register("wall_light_entity", () -> {
        Block[] blocks = WALL_LIGHTS.values().stream().map(DeferredBlock::get).toArray(Block[]::new);
        return BlockEntityType.Builder.of(com.csykes.searchlight.features.wall_light.WallLightBlockEntity::new, blocks).build(null);
    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.csykes.searchlight.features.wall_light.WallLightBlockEntity>> CORNER_LIGHT_BE = BLOCK_ENTITY_TYPES.register("corner_light_entity", () -> {
        Block[] blocks = CORNER_LIGHTS.values().stream().map(DeferredBlock::get).toArray(Block[]::new);
        return BlockEntityType.Builder.of(com.csykes.searchlight.features.wall_light.WallLightBlockEntity::new, blocks).build(null);
    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.csykes.searchlight.features.wall_light.WallLightBlockEntity>> CENTRE_LIGHT_BE = BLOCK_ENTITY_TYPES.register("centre_light_entity", () -> {
        Block[] blocks = CENTRE_LIGHTS.values().stream().map(DeferredBlock::get).toArray(Block[]::new);
        return BlockEntityType.Builder.of(com.csykes.searchlight.features.wall_light.WallLightBlockEntity::new, blocks).build(null);
    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.csykes.searchlight.features.wall_light.WallLightBlockEntity>> COLOUR_LAMPS_BE = BLOCK_ENTITY_TYPES.register("colour_lamp_entity", () -> {
        Block[] blocks = COLOUR_LAMPS.values().stream().map(DeferredBlock::get).toArray(Block[]::new);
        return BlockEntityType.Builder.of(com.csykes.searchlight.features.wall_light.WallLightBlockEntity::new, blocks).build(null);
    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.csykes.searchlight.features.wall_light.WallLightBlockEntity>> COLOUR_LAMPS_SLAB_BE = BLOCK_ENTITY_TYPES.register("colour_lamp_slab_entity", () -> {
        Block[] blocks = COLOUR_SLAB_LAMPS.values().stream().map(DeferredBlock::get).toArray(Block[]::new);
        return BlockEntityType.Builder.of(com.csykes.searchlight.features.wall_light.WallLightBlockEntity::new, blocks).build(null);
    });

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.csykes.searchlight.features.wall_light.WallLightBlockEntity>> EDGE_LIGHT_BE = BLOCK_ENTITY_TYPES.register("edge_light_entity", () -> {
        Block[] blocks = EDGE_LIGHTS.values().stream().map(DeferredBlock::get).toArray(Block[]::new);
        return BlockEntityType.Builder.of(com.csykes.searchlight.features.wall_light.WallLightBlockEntity::new, blocks).build(null);
    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.csykes.searchlight.features.searchlight.SearchlightBlockEntity>> SEARCHLIGHT_BE = BLOCK_ENTITY_TYPES.register("searchlight_entity", () -> {
        Block[] blocks = SEARCHLIGHTS.values().stream().map(DeferredBlock::get).toArray(Block[]::new);
        return BlockEntityType.Builder.of(com.csykes.searchlight.features.searchlight.SearchlightBlockEntity::new, blocks).build(null);
    });

    public static final DeferredBlock<Block> LIGHTING_DIRECTOR_BLOCK = ModList.get().isLoaded("computercraft")
            ? BLOCKS.register("lighting_director", () -> new com.csykes.searchlight.features.lighting_director.LightingDirectorBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
            .sound(METAL)
            .strength(3.0f)
            .noOcclusion()))
            : null;

    public static final DeferredItem<net.minecraft.world.item.BlockItem> LIGHTING_DIRECTOR_ITEM = (LIGHTING_DIRECTOR_BLOCK != null)
            ? ITEMS.registerSimpleBlockItem("lighting_director", LIGHTING_DIRECTOR_BLOCK)
            : null;

    public static final DeferredItem<net.minecraft.world.item.Item> LIGHTING_LINKER_CARD = ModList.get().isLoaded("computercraft")
            ? ITEMS.register("lighting_linker_card", () -> new com.csykes.searchlight.features.lighting_director.LightingLinkerCardItem(new net.minecraft.world.item.Item.Properties().stacksTo(1)))
            : null;

    public static final DeferredBlock<Block> LIGHT_SOURCE_BLOCK = BLOCKS.register("searchlight_lightsource", () -> new com.csykes.searchlight.features.searchlight.SearchlightLightSourceBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
            .mapColor(net.minecraft.world.level.material.MapColor.NONE)
            .replaceable()
            .noOcclusion()
            .noLootTable()
            .pushReaction(DESTROY)
            .lightLevel((state) -> 15)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.csykes.searchlight.features.searchlight.SearchlightLightSourceBlockEntity>> LIGHT_SOURCE_BE = BLOCK_ENTITY_TYPES.register("searchlight_lightsource_entity", () -> BlockEntityType.Builder.of(com.csykes.searchlight.features.searchlight.SearchlightLightSourceBlockEntity::new, LIGHT_SOURCE_BLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.csykes.searchlight.features.lighting_director.LightingDirectorBlockEntity>> LIGHTING_DIRECTOR_BE = (LIGHTING_DIRECTOR_BLOCK != null)
            ? BLOCK_ENTITY_TYPES.register("lighting_director_entity", () -> BlockEntityType.Builder.of(com.csykes.searchlight.features.lighting_director.LightingDirectorBlockEntity::new, LIGHTING_DIRECTOR_BLOCK.get()).build(null))
            : null;
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = CREATIVE_MODE_TABS.register("searchlight_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.searchlight"))
            .icon(() -> new ItemStack(SEARCHLIGHT_ITEMS.get("white").get()))
            .displayItems((parameters, output) -> {

                if (LIGHTING_DIRECTOR_ITEM != null) {
                    output.accept(LIGHTING_DIRECTOR_ITEM.get());
                }
                if (LIGHTING_LINKER_CARD != null) {
                    output.accept(LIGHTING_LINKER_CARD.get());
                }
                WALL_LIGHT_ITEMS.values().forEach(item -> output.accept(item.get()));
                CORNER_LIGHTS_ITEMS.values().forEach(item -> output.accept(item.get()));
                CENTRE_LIGHTS_ITEMS.values().forEach(item -> output.accept(item.get()));
                EDGE_LIGHTS_ITEMS.values().forEach(item -> output.accept(item.get()));
                COLOUR_LAMP_ITEMS.values().forEach(item -> output.accept(item.get()));
                COLOUR_SLAB_ITEMS.values().forEach(item -> output.accept(item.get()));
                SEARCHLIGHT_ITEMS.values().forEach(item -> output.accept(item.get()));
            }).build());

    public Searchlight(IEventBus modEventBus) {
        modEventBus.addListener(this::registerCapabilities);
        if (ModList.get().isLoaded("computercraft")) {
            modEventBus.addListener(this::registerPayloads);
        }

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }

    private void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("searchlight");
        registrar.playToServer(
                SetLightAddressPayload.TYPE,
                SetLightAddressPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        Player player = context.player();
                        Level level = player.level();
                        BlockPos pos = payload.pos();
                        BlockEntity be = level.getBlockEntity(pos);
                        if (be instanceof AddressableLight addressable) {
                            addressable.setAddress(payload.address());
                            be.setChanged();
                            level.sendBlockUpdated(pos, be.getBlockState(), be.getBlockState(), 3);
                        }
                    });
                }
        );
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        if (ModList.get().isLoaded("computercraft")) {
            try {
                CCIntegration.register(event);
            } catch (Throwable e) {
                LOGGER.error("Failed to register ComputerCraft integration", e);
            }
        }
    }
}