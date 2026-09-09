package com.mat.structure;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Utility for discovering GameTests across source code and generating corresponding
 * structure NBT files programmatically using {@link StructureBuilder}.
 */
@UtilityClass
public class MatStructureGenerator {

    private static final Pattern HOLDER_PATTERN = Pattern.compile(
            "@GameTestHolder\\s*\\(\\s*(?:value\\s*=\\s*)?(?:\"([^\"]+)\"|([A-Za-z0-9_.]+))\\s*\\)"
    );

    private static final Pattern CLASS_PATTERN = Pattern.compile(
            "(?:class|record)\\s+([A-Za-z0-9_]+)"
    );

    private static final Pattern CONSTANT_PATTERN = Pattern.compile(
            "(?:final\\s+)?String\\s+([A-Za-z0-9_]+)\\s*=\\s*\"([^\"]+)\""
    );

    private static final Pattern METHOD_PATTERN = Pattern.compile(
            "(?s)@GameTest(?:\\s*\\(([^)]*)\\))?\\s*(?:public|private|protected|static|final|\\s)*\\s*(?:void|[A-Za-z0-9_<>]+)\\s+([A-Za-z0-9_]+)\\s*\\("
    );

    private static final Pattern TEMPLATE_ARG_PATTERN = Pattern.compile(
            "template\\s*=\\s*(?:\"([^\"]+)\"|([A-Za-z0-9_]+))"
    );

    private static final Pattern DIMENSION_PATTERN = Pattern.compile(
            "(\\d+)[xX](\\d+)[xX](\\d+)"
    );

    /**
     * Parses 3D dimensions from a template name string (e.g., "empty3x3x3" -> [3, 3, 3]).
     */
    public static int[] parseDimensions(@NonNull String templateName) {
        Matcher matcher = DIMENSION_PATTERN.matcher(templateName);
        if (matcher.find()) {
            return new int[]{
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3))
            };
        }
        return new int[]{8, 8, 8};
    }

    /**
     * Programmatically writes a collection of {@link StructureBuilder} instances to a target directory.
     */
    public static void generateStructures(@NonNull Path outputDir, @NonNull String namespace, @NonNull Collection<StructureBuilder> structures) throws IOException {
        for (StructureBuilder structure : structures) {
            structure.writeToResourceDirectory(outputDir, namespace);
        }
    }

    /**
     * Scans Java source roots for GameTest annotations and generates all needed structure templates.
     */
    public static void scanAndGenerate(@NonNull Path outputDir, @NonNull String defaultNamespace, @NonNull Path... sourceRoots) throws IOException {
        Map<String, Map<String, int[]>> requiredStructures = new HashMap<>();

        // Register default fallbacks
        registerStructure(requiredStructures, defaultNamespace, "empty3x3x3", new int[]{3, 3, 3});
        registerStructure(requiredStructures, defaultNamespace, "empty5x5x5", new int[]{5, 5, 5});
        registerStructure(requiredStructures, defaultNamespace, "empty8x8x8", new int[]{8, 8, 8});
        registerStructure(requiredStructures, defaultNamespace, "empty", new int[]{8, 8, 8});

        for (Path root : sourceRoots) {
            if (!Files.exists(root)) continue;
            try (Stream<Path> stream = Files.walk(root)) {
                stream.filter(p -> p.toString().endsWith(".java")).forEach(path -> {
                    try {
                        String text = Files.readString(path);
                        if (text.contains("@GameTestHolder") || text.contains("@GameTest")) {
                            parseSourceFile(text, path.getFileName().toString(), defaultNamespace, requiredStructures);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Failed reading source file: " + path, e);
                    }
                });
            }
        }

        // Write all structures
        for (Map.Entry<String, Map<String, int[]>> nsEntry : requiredStructures.entrySet()) {
            String namespace = nsEntry.getKey();
            for (Map.Entry<String, int[]> entry : nsEntry.getValue().entrySet()) {
                String name = entry.getKey();
                int[] dims = entry.getValue();
                StructureBuilder.empty(name, dims[0], dims[1], dims[2])
                        .writeToResourceDirectory(outputDir, namespace);
            }
        }
    }

    private static void parseSourceFile(String text, String fileName, String defaultNamespace, Map<String, Map<String, int[]>> requiredStructures) {
        String ns = defaultNamespace;
        Matcher holderMatcher = HOLDER_PATTERN.matcher(text);
        if (holderMatcher.find() && holderMatcher.group(1) != null) {
            ns = holderMatcher.group(1);
        }

        Matcher classMatcher = CLASS_PATTERN.matcher(text);
        String className = classMatcher.find() ? classMatcher.group(1) : fileName.replace(".java", "");
        String classPrefix = className.toLowerCase(Locale.ROOT);

        Map<String, String> stringConstants = new HashMap<>();
        Matcher constMatcher = CONSTANT_PATTERN.matcher(text);
        while (constMatcher.find()) {
            stringConstants.put(constMatcher.group(1), constMatcher.group(2));
        }

        Matcher methodMatcher = METHOD_PATTERN.matcher(text);
        while (methodMatcher.find()) {
            String annotationArgs = methodMatcher.group(1) != null ? methodMatcher.group(1) : "";
            String methodName = methodMatcher.group(2);
            String methodPrefix = methodName.toLowerCase(Locale.ROOT);

            String template = null;
            if (!annotationArgs.isEmpty()) {
                Matcher templateMatcher = TEMPLATE_ARG_PATTERN.matcher(annotationArgs);
                if (templateMatcher.find()) {
                    if (templateMatcher.group(1) != null) {
                        template = templateMatcher.group(1);
                    } else if (templateMatcher.group(2) != null) {
                        template = stringConstants.getOrDefault(templateMatcher.group(2), templateMatcher.group(2));
                    }
                }
            }

            int[] dims = template != null ? parseDimensions(template) : new int[]{8, 8, 8};

            if (template != null) {
                registerStructure(requiredStructures, ns, template, dims);
                registerStructure(requiredStructures, ns, classPrefix + "." + template, dims);
                registerStructure(requiredStructures, ns, className + "." + template, dims);
                registerStructure(requiredStructures, ns, classPrefix + "." + methodPrefix, dims);
                registerStructure(requiredStructures, ns, className + "." + methodName, dims);
            } else {
                registerStructure(requiredStructures, ns, classPrefix + "." + methodPrefix, dims);
                registerStructure(requiredStructures, ns, className + "." + methodName, dims);
            }
        }
    }

    private static void registerStructure(Map<String, Map<String, int[]>> map, String namespace, String name, int[] dims) {
        String cleanName = name.endsWith(".nbt") ? name : (name + ".nbt");
        map.computeIfAbsent(namespace, k -> new HashMap<>()).put(cleanName, dims);
    }

    /**
     * CLI entry point for executing structure generation via Gradle tasks.
     *
     * @param args [outputDir, defaultNamespace, sourceRoots...]
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            throw new IllegalArgumentException("Usage: MatStructureGenerator <outputDir> <defaultNamespace> <sourceRoot1> [sourceRoot2...]");
        }
        Path outputDir = Path.of(args[0]);
        String defaultNamespace = args[1];
        Path[] sourceRoots = new Path[args.length - 2];
        for (int i = 2; i < args.length; i++) {
            sourceRoots[i - 2] = Path.of(args[i]);
        }
        scanAndGenerate(outputDir, defaultNamespace, sourceRoots);
    }
}
