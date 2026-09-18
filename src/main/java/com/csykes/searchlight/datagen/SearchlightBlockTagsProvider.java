package com.csykes.searchlight.datagen;

import com.csykes.searchlight.Searchlight;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class SearchlightBlockTagsProvider extends BlockTagsProvider {

    public SearchlightBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Searchlight.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        var pickaxeTag = this.tag(BlockTags.MINEABLE_WITH_PICKAXE);
        var stoneToolTag = this.tag(BlockTags.NEEDS_STONE_TOOL);

        Searchlight.BLOCKS.getEntries().forEach(blockHolder -> {
            if (blockHolder != Searchlight.LIGHT_SOURCE_BLOCK) {
                pickaxeTag.add(blockHolder.get());
                stoneToolTag.add(blockHolder.get());
            }
        });

        var dyenamicsColors = new String[]{
                "peach", "aquamarine", "fluorescent", "mint", "maroon", "bubblegum",
                "lavender", "persimmon", "cherenkov", "amber", "honey", "ultramarine",
                "spring_green", "rose", "navy", "icy_blue", "wine", "conifer"
        };

        var families = new String[]{
                "wall_light", "corner_light", "centre_light",
                "edge_light", "colour_lamp", "colour_lamp_slab", "searchlight"
        };

        for (String family : families) {
            for (String color : dyenamicsColors) {
                ResourceLocation loc = ResourceLocation.fromNamespaceAndPath("searchlight", family + "_" + color);
                pickaxeTag.addOptional(loc);
                stoneToolTag.addOptional(loc);
            }
        }
    }
}