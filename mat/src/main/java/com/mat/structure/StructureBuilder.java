package com.mat.structure;

import lombok.Getter;
import lombok.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.GZIPOutputStream;

/**
 * Fluent builder for programmatically creating Minecraft structure (.nbt) templates for GameTests.
 * Self-contained and portable with zero external runtime dependencies.
 */
@Getter
public class StructureBuilder {

    public static final int DEFAULT_DATA_VERSION = 3955; // Minecraft 1.21+

    private final String name;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final int dataVersion;

    private final List<PaletteEntry> palette = new ArrayList<>();
    private final List<BlockEntry> blocks = new ArrayList<>();

    private final Map<String, Integer> paletteIndexMap = new LinkedHashMap<>();

    public StructureBuilder(@NonNull String name, int sizeX, int sizeY, int sizeZ, int dataVersion) {
        if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) {
            throw new IllegalArgumentException(String.format("Structure dimensions must be strictly positive: [%d, %d, %d]", sizeX, sizeY, sizeZ));
        }
        this.name = name;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.dataVersion = dataVersion;
    }

    public StructureBuilder(String name, int sizeX, int sizeY, int sizeZ) {
        this(name, sizeX, sizeY, sizeZ, DEFAULT_DATA_VERSION);
    }

    public static StructureBuilder create(String name, int sizeX, int sizeY, int sizeZ) {
        return new StructureBuilder(name, sizeX, sizeY, sizeZ);
    }

    public static StructureBuilder empty(String name, int sizeX, int sizeY, int sizeZ) {
        return create(name, sizeX, sizeY, sizeZ);
    }

    public static StructureBuilder empty(int sizeX, int sizeY, int sizeZ) {
        return create(String.format("empty%dx%dx%d", sizeX, sizeY, sizeZ), sizeX, sizeY, sizeZ);
    }

    /**
     * Sets a block at the specified relative coordinates within structure bounds.
     */
    public StructureBuilder setBlock(int x, int y, int z, String blockId) {
        return setBlock(x, y, z, blockId, Collections.emptyMap());
    }

    /**
     * Sets a block at the specified relative coordinates with properties.
     */
    public StructureBuilder setBlock(int x, int y, int z, String blockId, Map<String, String> properties) {
        if (x < 0 || x >= sizeX || y < 0 || y >= sizeY || z < 0 || z >= sizeZ) {
            throw novelBoundsException(x, y, z);
        }

        int paletteIndex = getOrCreatePaletteIndex(blockId, properties != null ? properties : Collections.emptyMap());
        this.blocks.removeIf(b -> b.x() == x && b.y() == y && b.z() == z);
        this.blocks.add(new BlockEntry(x, y, z, paletteIndex));
        return this;
    }

    private int getOrCreatePaletteIndex(String blockId, Map<String, String> properties) {
        String key = blockId + properties.toString();
        if (paletteIndexMap.containsKey(key)) {
            return paletteIndexMap.get(key);
        }
        int newIndex = palette.size();
        palette.add(new PaletteEntry(blockId, new LinkedHashMap<>(properties)));
        paletteIndexMap.put(key, newIndex);
        return newIndex;
    }

    private IndexOutOfBoundsException novelBoundsException(int x, int y, int z) {
        return new IndexOutOfBoundsException(
                String.format("Position (%d, %d, %d) out of structure bounds [%d, %d, %d]", x, y, z, sizeX, sizeY, sizeZ)
        );
    }

    /**
     * Serializes this structure to GZIP-compressed NBT binary format.
     */
    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream uncompressed = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(uncompressed);

        // Root Compound tag
        dos.writeByte(10); // TAG_Compound
        dos.writeUTF("");  // empty root name

        // DataVersion (TAG_Int)
        dos.writeByte(3);
        dos.writeUTF("DataVersion");
        dos.writeInt(this.dataVersion);

        // size (TAG_List of TAG_Int)
        dos.writeByte(9);
        dos.writeUTF("size");
        dos.writeByte(3); // element type: TAG_Int
        dos.writeInt(3);  // list size: 3
        dos.writeInt(this.sizeX);
        dos.writeInt(this.sizeY);
        dos.writeInt(this.sizeZ);

        // palette (TAG_List of TAG_Compound)
        dos.writeByte(9);
        dos.writeUTF("palette");
        dos.writeByte(10); // element type: TAG_Compound
        dos.writeInt(this.palette.size());
        for (PaletteEntry entry : this.palette) {
            // Name (TAG_String)
            dos.writeByte(8);
            dos.writeUTF("Name");
            dos.writeUTF(entry.name());

            // Properties (TAG_Compound) if present
            if (!entry.properties().isEmpty()) {
                dos.writeByte(10);
                dos.writeUTF("Properties");
                for (Map.Entry<String, String> prop : entry.properties().entrySet()) {
                    dos.writeByte(8); // TAG_String
                    dos.writeUTF(prop.getKey());
                    dos.writeUTF(prop.getValue());
                }
                dos.writeByte(0); // TAG_End for Properties
            }
            dos.writeByte(0); // TAG_End for Palette entry
        }

        // blocks (TAG_List of TAG_Compound)
        dos.writeByte(9);
        dos.writeUTF("blocks");
        dos.writeByte(10); // element type: TAG_Compound
        dos.writeInt(this.blocks.size());
        for (BlockEntry block : this.blocks) {
            // pos (TAG_List of TAG_Int)
            dos.writeByte(9);
            dos.writeUTF("pos");
            dos.writeByte(3); // TAG_Int
            dos.writeInt(3);
            dos.writeInt(block.x());
            dos.writeInt(block.y());
            dos.writeInt(block.z());

            // state (TAG_Int)
            dos.writeByte(3);
            dos.writeUTF("state");
            dos.writeInt(block.stateIndex());

            dos.writeByte(0); // TAG_End for Block entry
        }

        // entities (TAG_List of TAG_Compound, empty)
        dos.writeByte(9);
        dos.writeUTF("entities");
        dos.writeByte(10); // element type: TAG_Compound
        dos.writeInt(0);

        // TAG_End for Root Compound
        dos.writeByte(0);
        dos.flush();

        // GZIP compression
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (GZIPOutputStream gzos = new GZIPOutputStream(compressed)) {
            gzos.write(uncompressed.toByteArray());
            gzos.finish();
        }
        return compressed.toByteArray();
    }

    /**
     * Writes this structure file to the given target path.
     */
    public Path writeTo(Path targetFile) throws IOException {
        if (targetFile.getParent() != null) {
            Files.createDirectories(targetFile.getParent());
        }
        byte[] bytes = toBytes();
        Files.write(targetFile, bytes);
        return targetFile;
    }

    /**
     * Writes this structure file to the given target file.
     */
    public File writeTo(File targetFile) throws IOException {
        writeTo(targetFile.toPath());
        return targetFile;
    }

    /**
     * Writes this structure to standard GameTest resource directories under the given namespace:
     * - {@code data/<namespace>/structure/<name>.nbt}
     * - {@code data/<namespace>/structures/<name>.nbt}
     */
    public List<Path> writeToResourceDirectory(Path baseResourceDir, String namespace) throws IOException {
        String fileName = this.name.endsWith(".nbt") ? this.name : (this.name + ".nbt");
        Path singular = baseResourceDir.resolve("data").resolve(namespace).resolve("structure").resolve(fileName);
        Path plural = baseResourceDir.resolve("data").resolve(namespace).resolve("structures").resolve(fileName);

        writeTo(singular);
        writeTo(plural);

        return List.of(singular, plural);
    }

    public record PaletteEntry(String name, Map<String, String> properties) {
    }

    public record BlockEntry(int x, int y, int z, int stateIndex) {
    }
}
