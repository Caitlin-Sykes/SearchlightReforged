package com.csykes.searchlight.docsGenerator.services;

import com.csykes.searchlight.docsGenerator.entities.BlockPeripheralPair;
import com.csykes.searchlight.docsGenerator.entities.LuaMethod;
import com.csykes.searchlight.docsGenerator.entities.LuaParameter;
import com.csykes.searchlight.docsGenerator.entities.PeripheralDocumentation;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import lombok.extern.slf4j.Slf4j;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class JavaParsingService {

    private final Path integrationDir = Path.of(
            "src/main/java/com/csykes/searchlight/integration/cc_tweaked"
    );

    // ==========================================
    // CC Integration Parsing
    // ==========================================

    public List<BlockPeripheralPair> parseBEs(Map<String, List<String>> blockEnitites) throws IOException {
        Path ccIntegrationPath = integrationDir.resolve("CCIntegration.java");
        CompilationUnit cu = StaticJavaParser.parse(ccIntegrationPath);

        log.info("BE Peripherals:");
        return cu.findAll(MethodCallExpr.class).stream()
                .filter(this::isRegisterBlockEntityCall)
                .map(e -> extractBlockPeripheralPair(e, blockEnitites))
                .filter(Objects::nonNull)
                .toList();
    }

    private boolean isRegisterBlockEntityCall(MethodCallExpr expr) {
        return expr.getNameAsString().equals("registerBlockEntity");
    }

    private BlockPeripheralPair extractBlockPeripheralPair(MethodCallExpr expr, Map<String, List<String>> blockEnitites) {
        return expr.getArgument(1)
                .asMethodCallExpr()
                .getScope()
                .map(scope -> scope.asFieldAccessExpr().getNameAsString())
                .map(blockId -> {
                    String peripheral = expr.getArgument(2)
                            .asLambdaExpr()
                            .getBody()
                            .asExpressionStmt()
                            .getExpression()
                            .asObjectCreationExpr()
                            .getType()
                            .getNameAsString();

                    return new BlockPeripheralPair(blockId, integrationDir.resolve(peripheral + ".java"), blockEnitites.get(blockId));
                })
                .orElse(null);
    }

    // ==========================================
    // Lua Method Parsing
    // ==========================================

    public Map<Path, PeripheralDocumentation> parseMethods(List<BlockPeripheralPair> peripheralBEs) {

        return peripheralBEs.stream()
                .map(BlockPeripheralPair::getPeripheralClass)
                .distinct()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        path -> path,
                        this::parsePeripheral,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));
    }

    public PeripheralDocumentation parsePeripheral(Path path) {
        log.info("Parsing BE Peripheral: {}", path.getFileName());
        try {
            CompilationUnit cu = StaticJavaParser.parse(path);

            // Extract the class-level Javadoc description
            String peripheralDescription = cu.findAll(ClassOrInterfaceDeclaration.class).stream()
                    .findFirst()
                    .flatMap(Node::getComment)
                    .filter(Comment::isJavadocComment)
                    .map(comment -> comment.asJavadocComment().parse().getDescription().toText().trim())
                    .orElse(""); // Defaults to empty string if no comment is found

            // Parse the methods
            List<LuaMethod> methods = cu.findAll(MethodDeclaration.class).stream()
                    .filter(this::isLuaFunction)
                    .map(this::buildLuaMethod)
                    .toList();

            return new PeripheralDocumentation(peripheralDescription, methods);
        } catch (IOException e) {
            log.error("Failed to parse peripheral class: {}", path.getFileName(), e);
            return new PeripheralDocumentation("", Collections.emptyList());
        }
    }

    private boolean isLuaFunction(MethodDeclaration method) {
        return method.getAnnotationByName("LuaFunction").isPresent();
    }

    private LuaMethod buildLuaMethod(MethodDeclaration method) {
        Map<String, String> paramDescriptions = extractParamDescriptions(method);
        String description = extractMethodDescription(method);

        List<LuaParameter> parameters = method.getParameters().stream()
                .map(param -> LuaParameter.builder()
                        .name(param.getNameAsString())
                        .type(param.getType().asString())
                        .description(paramDescriptions.getOrDefault(param.getNameAsString(), ""))
                        .build())
                .toList();

        return LuaMethod.builder()
                .name(method.getNameAsString())
                .description(description)
                .parameters(parameters)
                .build();
    }

    private String extractMethodDescription(MethodDeclaration method) {
        return method.getComment()
                .filter(comment -> comment.isJavadocComment())
                .map(comment -> comment.asJavadocComment().parse().getDescription().toText().trim())
                .orElse("");
    }

    private Map<String, String> extractParamDescriptions(MethodDeclaration method) {
        Map<String, String> descriptions = new LinkedHashMap<>();

        method.getComment()
                .filter(comment -> comment.isJavadocComment())
                .ifPresent(comment -> {
                    Javadoc javadoc = comment.asJavadocComment().parse();
                    for (JavadocBlockTag tag : javadoc.getBlockTags()) {
                        if (tag.getType() == JavadocBlockTag.Type.PARAM && tag.getName().isPresent()) {
                            descriptions.put(tag.getName().get(), tag.getContent().toText().trim());
                        }
                    }
                });

        return descriptions;
    }

    // ==========================================
    // Block Entity (BE) Block Parsing
    // ==========================================

    public Map<String, List<String>> parseBEBlocks() throws FileNotFoundException {
        StaticJavaParser.getParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);

        CompilationUnit cu = loadSearchlightCompilationUnit();
        List<String> dyeColors = getDynamicDyeColors();

        Map<String, String> entityToMap = extractEntityMapNames(cu);
        Map<String, HelperMethodData> helperMethods = extractHelperMethods(cu);
        Map<String, List<String>> mapToVariants = extractMapToVariants(cu, helperMethods, dyeColors);

        return buildFinalMapping(entityToMap, mapToVariants);
    }

    private CompilationUnit loadSearchlightCompilationUnit() throws FileNotFoundException {
        try {
            return StaticJavaParser.parse(Path.of("src/main/java/com/csykes/searchlight/Searchlight.java"));
        } catch (IOException e) {
            log.error("Failed to parse Searchlight.java", e);
            throw new FileNotFoundException("Failed to parse Searchlight.java");
        }
    }

    private Map<String, String> extractEntityMapNames(CompilationUnit cu) {
        Map<String, String> entityToMap = new HashMap<>();
        cu.findAll(VariableDeclarator.class).stream()
                .filter(var -> var.getType().asString().contains("BlockEntityType"))
                .forEach(var -> var.findFirst(MethodCallExpr.class, m -> m.getNameAsString().equals("register"))
                        .ifPresent(reg -> mapEntityRegistration(var, reg, entityToMap)));
        return entityToMap;
    }

    private void mapEntityRegistration(VariableDeclarator var, MethodCallExpr reg, Map<String, String> entityToMap) {
        String beVarName = var.getNameAsString();
        reg.findAll(MethodCallExpr.class, m -> m.getNameAsString().equals("values")).forEach(valuesCall ->
                valuesCall.getScope().ifPresent(scope -> {
                    if (scope instanceof NameExpr scopeExpr) {
                        entityToMap.put(beVarName, scopeExpr.getNameAsString());
                    }
                })
        );
    }

    private Map<String, HelperMethodData> extractHelperMethods(CompilationUnit cu) {
        Map<String, HelperMethodData> helperMethods = new HashMap<>();
        cu.findAll(MethodDeclaration.class).forEach(method ->
                method.findFirst(MethodCallExpr.class, m -> m.getNameAsString().equals("registerBlockAndItem"))
                        .ifPresent(call -> {
                            if (call.getArguments().size() >= 4 && call.getArgument(0).isBinaryExpr()) {
                                String prefix = call.getArgument(0).asBinaryExpr().getLeft().asStringLiteralExpr().getValue();
                                String mapName = call.getArgument(3).asNameExpr().getNameAsString();
                                helperMethods.put(method.getNameAsString(), new HelperMethodData(prefix, mapName));
                            }
                        })
        );
        return helperMethods;
    }

    private Map<String, List<String>> extractMapToVariants(CompilationUnit cu, Map<String, HelperMethodData> helperMethods, List<String> dyeColors) {
        Map<String, List<String>> mapToVariants = new HashMap<>();
        cu.findAll(InitializerDeclaration.class, InitializerDeclaration::isStatic).forEach(staticBlock ->
                staticBlock.findAll(MethodCallExpr.class).forEach(call ->
                        processVariantMethodCall(call, helperMethods, mapToVariants, dyeColors)
                )
        );
        return mapToVariants;
    }

    private void processVariantMethodCall(MethodCallExpr call, Map<String, HelperMethodData> helperMethods, Map<String, List<String>> mapToVariants, List<String> dyeColors) {
        String methodName = call.getNameAsString();
        HelperMethodData helper = helperMethods.get(methodName);
        if (helper == null) return;

        List<String> variantsList = mapToVariants.computeIfAbsent(helper.targetMap(), k -> new ArrayList<>());

        if (call.getArgument(0) instanceof StringLiteralExpr literalExpr) {
            variantsList.add(helper.prefix() + literalExpr.getValue());
        } else if (call.getArgument(0) instanceof MethodCallExpr argumentMethodCall &&
                argumentMethodCall.getNameAsString().equals("getName")) {

            dyeColors.stream()
                    .map(color -> helper.prefix() + color)
                    .filter(fullVariant -> !variantsList.contains(fullVariant))
                    .forEach(variantsList::add);
        }
    }

    private Map<String, List<String>> buildFinalMapping(Map<String, String> entityToMap, Map<String, List<String>> mapToVariants) {
        Map<String, List<String>> finalMapping = new HashMap<>();

        finalMapping.put("LIGHTING_DIRECTOR_BE", List.of("lighting_director"));

        entityToMap.forEach((beVarName, mapName) -> {
            if (mapToVariants.containsKey(mapName)) {
                finalMapping.put(beVarName, mapToVariants.get(mapName));
            }
        });

        return finalMapping;
    }

    // ==========================================
    // Dynamic Reflection Utilities
    // ==========================================

    private static List<String> getDynamicDyeColors() {
        List<String> colors = new ArrayList<>();
        try {
            Class<?> colorClass = Class.forName("cy.jdkdigital.dyenamics.core.util.DyenamicDyeColor");
            Object[] enumConstants = colorClass.getEnumConstants();

            if (enumConstants == null) {
                Method valuesMethod = colorClass.getMethod("values");
                enumConstants = (Object[]) valuesMethod.invoke(null);
            }

            if (enumConstants != null) {
                Method nameMethod = getColorNameMethod(colorClass);
                for (Object constant : enumConstants) {
                    colors.add((String) nameMethod.invoke(constant));
                }
            }
            log.info("Successfully loaded {} colors dynamically from Dyenamics.", colors.size());
        } catch (Exception e) {
            log.error("Could not dynamically load DyenamicDyeColor. Ensure Dyenamics is on your script's execution classpath.", e);
        }
        return colors;
    }

    private static Method getColorNameMethod(Class<?> colorClass) throws NoSuchMethodException {
        try {
            return colorClass.getMethod("getName");
        } catch (NoSuchMethodException e) {
            return colorClass.getMethod("getSerializedName");
        }
    }

    record HelperMethodData(String prefix, String targetMap) {
    }
}