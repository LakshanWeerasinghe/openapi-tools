/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.openapi.service;

import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.openapi.core.generators.client.BallerinaClientGenerator;
import io.ballerina.openapi.core.generators.client.model.OASClientConfig;
import io.ballerina.openapi.core.generators.common.GeneratorUtils;
import io.ballerina.openapi.core.generators.common.TypeHandler;
import io.ballerina.openapi.core.generators.common.model.Filter;
import io.ballerina.openapi.core.generators.common.model.GenSrcFile;
import io.ballerina.toml.syntax.tree.DocumentMemberDeclarationNode;
import io.ballerina.toml.syntax.tree.DocumentNode;
import io.ballerina.toml.syntax.tree.KeyValueNode;
import io.ballerina.toml.syntax.tree.SyntaxKind;
import io.ballerina.toml.syntax.tree.TableArrayNode;
import io.ballerina.tools.text.LSPTextEdit;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;
import io.swagger.v3.oas.models.OpenAPI;
import org.ballerinalang.formatter.core.Formatter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

public class SourceGenerator {

    public static final String TYPE_FILE_NAME = "types.bal";
    public static final String CLIENT_FILE_NAME = "client.bal";
    public static final String UTIL_FILE_NAME = "utils.bal";
    private static final String BALLERINA_TOML = "Ballerina.toml";
    private static final String LS = System.lineSeparator();

    // keys for the toml table
    public static final String KEY_INPUT = "filePath";
    public static final String KEY_ID = "id";
    public static final String KEY_TARGET_MODULE = "targetModule";
    public static final String OPTION_MODE = "option.mode";
    public static final String OPTION_STATUS_CODE_BINDING = "option.statusCodeBinding";
    public static final String OPTION_TAGS = "option.tags";
    public static final String OPTION_NULLABLE = "option.nullable";
    public static final String OPTION_LICENSE = "option.license";
    public static final String OPTION_OPERATIONS = "option.operations";

    private final Path projectPath;
    private final Map<String, String> arguments;

    public SourceGenerator(String[] arguments, String projectPath) {
        this.arguments = argumentsParser(new ArrayList<>(Arrays.stream(arguments).toList()));
        this.projectPath = Path.of(projectPath);
    }

    private Map<String, String> argumentsParser(List<String> arguments) {
        Map<String, String> argsMap = new HashMap<>();
        while (!arguments.isEmpty()) {
            String option = arguments.removeFirst();
            try {
                switch (option) {
                    case "-i", "--input" -> argsMap.put(KEY_INPUT, arguments.removeFirst());
                    case "--id" -> argsMap.put(KEY_ID, arguments.removeFirst());
                    case "--module" -> argsMap.put(KEY_TARGET_MODULE, arguments.removeFirst());
                    case "--mode" -> argsMap.put(OPTION_MODE, arguments.removeFirst());
                    case "--status-code-binding" -> argsMap.put(OPTION_STATUS_CODE_BINDING, "true");
                    case "--tags" -> argsMap.put(OPTION_TAGS, arguments.removeFirst());
                    case "-n", "--nullable" -> argsMap.put(OPTION_NULLABLE, "true");
                    case "--license" -> argsMap.put(OPTION_LICENSE, arguments.removeFirst());
                    case "--operations" -> argsMap.put(OPTION_OPERATIONS, arguments.removeFirst());
                    default -> throw new IllegalArgumentException("Unknown option: " + option);
                }
            } catch (NoSuchElementException e) {
                throw new IllegalArgumentException("Missing value for option: " + option, e);
            }
        }
        return argsMap;
    }

    private void genBalTomlTableEntry(Map<String, List<LSPTextEdit>> textEditsMap)
            throws IOException {
        Path tomlPath = this.projectPath.resolve(BALLERINA_TOML);
        TextDocument balTomlDocument = TextDocuments.from(Files.readString(tomlPath));
        io.ballerina.toml.syntax.tree.SyntaxTree syntaxTree = io.ballerina.toml.syntax.tree.SyntaxTree
                .from(balTomlDocument);
        DocumentNode rootNode = syntaxTree.rootNode();

        LineRange lineRange = null;
        for (DocumentMemberDeclarationNode node : rootNode.members()) {
            if (node.kind() != SyntaxKind.TABLE_ARRAY) {
                continue;
            }
            TableArrayNode tableArrayNode = (TableArrayNode) node;
            if (!tableArrayNode.identifier().toSourceCode().equals("tool.openapi")) {
                continue;
            }

            for (KeyValueNode field : tableArrayNode.fields()) {
                String identifier = field.identifier().toSourceCode();
                if (identifier.trim().equals("targetModule")) {
                    if (field.value().toSourceCode().contains(warpWithQuotes(this.arguments.get(KEY_TARGET_MODULE)))) {
                        lineRange = tableArrayNode.lineRange();
                        break;
                    }
                }
            }
        }

        String tomlEntry = balTomlEntry();
        List<LSPTextEdit> textEdits = new ArrayList<>();
        textEditsMap.put(tomlPath.toString(), textEdits);
        if (lineRange != null) {
            textEdits.add(LSPTextEdit.from(lineRange, tomlEntry));
        } else {
            LinePosition startPos = LinePosition.from(rootNode.lineRange().endLine().line() + 2, 0);
            LineRange range = LineRange.from(tomlPath.toString(), startPos, startPos);
            textEdits.add(LSPTextEdit.from(range, tomlEntry));
        }
    }

    private String balTomlEntry() {
        StringBuilder entry = new StringBuilder(LS + "[[tool.openapi]]" + LS +
                "%s = %s%s".formatted(KEY_ID, warpWithQuotes(arguments.get(KEY_ID)), LS) +
                "%s = %s%s".formatted(KEY_TARGET_MODULE, warpWithQuotes(arguments.get(KEY_TARGET_MODULE)), LS) +
                "%s = %s%s".formatted(KEY_INPUT, warpWithQuotes(arguments.get(KEY_INPUT)), LS));

        for (Map.Entry<String, String> entryArg : arguments.entrySet()) {
            if (entryArg.getKey().equals(KEY_INPUT) || entryArg.getKey().equals(KEY_TARGET_MODULE) ||
                    entryArg.getKey().equals(KEY_ID)) {
                continue;
            }
            String key = entryArg.getKey();
            String value = entryArg.getValue();
            switch (key) {
                case OPTION_MODE, OPTION_LICENSE -> {
                    if (value != null && !value.isBlank()) {
                        entry.append("%s = %s%s".formatted(key, warpWithQuotes(value), LS));
                    }
                }
                case OPTION_STATUS_CODE_BINDING, OPTION_NULLABLE -> {
                    if (value != null && value.equals("true")) {
                        entry.append("%s = true%s".formatted(key, LS));
                    }
                }
                case OPTION_TAGS, OPTION_OPERATIONS -> {
                    if (value != null && !value.isBlank()) {
                        entry.append("%s = %s%s".formatted(key, value, LS));
                    }
                }
                default -> throw new IllegalArgumentException("Unknown option: " + key);
            }
        }
        return entry.toString();
    }

    private void validateArguments() throws Exception {
        if (!arguments.containsKey(KEY_INPUT)) {
            throw new Exception("Missing required argument: --input <path-to-openapi-contract>");
        }
        if (!arguments.containsKey(KEY_TARGET_MODULE)) {
            throw new Exception("Missing required argument: --module <target-module>");
        }
        if (!arguments.containsKey(KEY_ID)) {
            throw new Exception("Missing required argument: --id <module-id>");
        }
    }

    private static String warpWithQuotes(String value) {
        return "\"" + value + "\"";
    }

    public Map<String, List<LSPTextEdit>> generate() throws Exception {
        validateArguments();
        OASClientConfig clientConfig = createClientConfig();
        TypeHandler.createInstance(clientConfig.getOpenAPI(), clientConfig.isNullable());
        BallerinaClientGenerator balClientGenerator = new BallerinaClientGenerator(clientConfig);

        String licenceContent = clientConfig.getLicense();
        String licenceHeader = licenceContent == null || licenceContent.isBlank() ? "" : licenceContent + "\n";

        List<GenSrcFile> sourceFiles = generateSourceFiles(licenceHeader, balClientGenerator);
        Path outputPath = this.projectPath.resolve("generated").resolve(arguments.get(KEY_TARGET_MODULE));

        Map<String, List<LSPTextEdit>> textEditsMap = new HashMap<>();
        for (GenSrcFile sourceFile : sourceFiles) {
            List<LSPTextEdit> textEdits = new ArrayList<>();
            LinePosition position = LinePosition.from(0, 0);
            textEdits.add(LSPTextEdit.from(LineRange.from("", position, position), sourceFile.getContent()));
            textEditsMap.put(outputPath.resolve(sourceFile.getFileName()).toString(), textEdits);
        }
        genBalTomlTableEntry(textEditsMap);
        return textEditsMap;
    }

    private OASClientConfig createClientConfig() throws Exception {
        Path contractPath = Path.of(this.arguments.get(KEY_INPUT));
        OpenAPI openAPI = GeneratorUtils.normalizeOpenAPI(contractPath, false, true, true);
        if (Objects.isNull(openAPI.getInfo())) {
            throw new Exception("Info section is missing in the OpenAPI contract: " + contractPath);
        }
        OASClientConfig.Builder builder = new OASClientConfig.Builder()
                .withFilters(new Filter())
                .withOpenAPI(openAPI);
        if (this.arguments.containsKey(OPTION_NULLABLE)) {
            builder.withNullable(true);
        }
        return builder.build();
    }

    private List<GenSrcFile> generateSourceFiles(String licenceHeader,
                                                 BallerinaClientGenerator clientGenerator) throws Exception {
        List<GenSrcFile> sourceFiles = new ArrayList<>();

        SyntaxTree clientSyntaxTree = clientGenerator.generateSyntaxTree();
        String clientSourceContent = Formatter.format(clientSyntaxTree).toSourceCode();
        sourceFiles.add(new GenSrcFile(GenSrcFile.GenFileType.GEN_SRC, null, CLIENT_FILE_NAME,
                licenceHeader + clientSourceContent));

        SyntaxTree utilSyntaxTree = clientGenerator.getBallerinaUtilGenerator().generateUtilSyntaxTree();
        String utilSourceContent = Formatter.format(utilSyntaxTree).toSourceCode();
        if (!utilSourceContent.isBlank()) {
            sourceFiles.add(new GenSrcFile(GenSrcFile.GenFileType.GEN_SRC, null, UTIL_FILE_NAME,
                    licenceHeader + utilSourceContent));
        }

        List<TypeDefinitionNode> authNodes = clientGenerator.getBallerinaAuthConfigGenerator()
                .getAuthRelatedTypeDefinitionNodes();
        for (TypeDefinitionNode authNode : authNodes) {
            TypeHandler.getInstance().addTypeDefinitionNode(authNode.typeName().text(), authNode);
        }

        SyntaxTree typeSyntaxTree = TypeHandler.getInstance().generateTypeSyntaxTree();
        String typeSourceContent = Formatter.format(typeSyntaxTree).toSourceCode();
        if (!typeSourceContent.isBlank()) {
            sourceFiles.add(new GenSrcFile(GenSrcFile.GenFileType.GEN_SRC, null, TYPE_FILE_NAME,
                    licenceHeader + typeSourceContent));
        }

        return sourceFiles;
    }
}
