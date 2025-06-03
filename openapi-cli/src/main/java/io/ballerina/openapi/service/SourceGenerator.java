package io.ballerina.openapi.service;

import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.openapi.core.generators.client.BallerinaClientGenerator;
import io.ballerina.openapi.core.generators.client.model.OASClientConfig;
import io.ballerina.openapi.core.generators.common.GeneratorUtils;
import io.ballerina.openapi.core.generators.common.TypeHandler;
import io.ballerina.openapi.core.generators.common.exception.BallerinaOpenApiException;
import io.ballerina.openapi.core.generators.common.model.Filter;
import io.ballerina.openapi.core.generators.common.model.GenSrcFile;
import io.ballerina.toml.syntax.tree.DocumentMemberDeclarationNode;
import io.ballerina.toml.syntax.tree.DocumentNode;
import io.ballerina.toml.syntax.tree.KeyValueNode;
import io.ballerina.toml.syntax.tree.SyntaxKind;
import io.ballerina.toml.syntax.tree.TableArrayNode;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;
import io.ballerina.tools.text.TextEdit;
import io.ballerina.tools.text.TextRange;
import io.swagger.v3.oas.models.OpenAPI;
import org.ballerinalang.formatter.core.Formatter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SourceGenerator {

    public static final String TYPE_FILE_NAME = "types.bal";
    public static final String CLIENT_FILE_NAME = "client.bal";
    public static final String UTIL_FILE_NAME = "utils.bal";
    private static final String BALLERINA_TOML = "Ballerina.toml";
    private static final String LS = System.lineSeparator();

    private Path contractPath;
    private Path projectPath;

    public SourceGenerator(String contractPath, String projectPath) {
        this.contractPath = Path.of(contractPath);
        this.projectPath = Path.of(projectPath);
    }

    private boolean genBalTomlTableEntry(String module, Map<String, List<TextEdit>> textEditsMap) throws IOException {
        Path tomlPath = this.projectPath.resolve(BALLERINA_TOML);
        TextDocument configDocument = TextDocuments.from(Files.readString(tomlPath));
        io.ballerina.toml.syntax.tree.SyntaxTree syntaxTree = io.ballerina.toml.syntax.tree.SyntaxTree.from(configDocument);
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
                    if (field.value().toSourceCode().contains("\"" + module + "\"")) {
                        lineRange = tableArrayNode.lineRange();
                        break;
                    }
                }
            }
        }

        String tomlEntry = getTomlEntry(module);
        List<TextEdit> textEdits = new ArrayList<>();
        textEditsMap.put(tomlPath.toString(), textEdits);
        if (lineRange != null) {
            TextRange from = TextRange.from(lineRange.startLine().offset(), lineRange.endLine().offset());
            textEdits.add(TextEdit.from(from, tomlEntry));
        } else {
            LinePosition startPos = LinePosition.from(rootNode.lineRange().endLine().line() + 1, 0);
            TextRange from = TextRange.from(0, 0);
            textEdits.add(TextEdit.from(from, tomlEntry));
        }
        return lineRange != null;
    }

    private String getTomlEntry(String module) {
        String moduleWithQuotes = "\"" + module + "\"";
        return LS + "[[tool.openapi]]" + LS +
                "id" + " = " + moduleWithQuotes + LS +
                "targetModule" + " = " + moduleWithQuotes + LS +
                "filePath" + " = " + "\"" +
                contractPath.toAbsolutePath().toString().replace("\\", "\\\\") + "\"" + LS;
    }

    public Map<String, List<TextEdit>> generate() throws IOException, BallerinaOpenApiException, Exception {
        Map<String, List<TextEdit>> textEditsMap = new HashMap<>();
        genBalTomlTableEntry("", textEditsMap);
        OpenAPI openAPI = GeneratorUtils.normalizeOpenAPI(this.contractPath, false,
                true, true);
        if (Objects.isNull(openAPI.getInfo())) {
            throw new Exception("Info section is missing in the OpenAPI contract: " + this.contractPath);
        }

        OASClientConfig clientConfig = new OASClientConfig.Builder()
                .withFilters(new Filter())
                .withOpenAPI(openAPI)
                .build();
        TypeHandler.createInstance(clientConfig.getOpenAPI(), clientConfig.isNullable());
        BallerinaClientGenerator balClientGenerator = new BallerinaClientGenerator(clientConfig);

        List<TypeDefinitionNode> authNodes = balClientGenerator.getBallerinaAuthConfigGenerator()
                .getAuthRelatedTypeDefinitionNodes();
        for (TypeDefinitionNode authNode : authNodes) {
            TypeHandler.getInstance().addTypeDefinitionNode(authNode.typeName().text(), authNode);
        }

        String licenceContent = clientConfig.getLicense();
        String licenceHeader = licenceContent == null || licenceContent.isBlank() ? "" : licenceContent + "\n";

        List<GenSrcFile> sourceFiles = generateSourceFiles(licenceHeader, balClientGenerator);
        Path outputPath = this.projectPath.resolve("generated").resolve("weather");
        for (GenSrcFile sourceFile : sourceFiles) {
            List<TextEdit> textEdits = new ArrayList<>();
            textEdits.add(TextEdit.from(TextRange.from(0, 0), sourceFile.getContent()));
            textEditsMap.put(outputPath.resolve(sourceFile.getFileName()).toString(), textEdits);
        }

        return textEditsMap;
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

        SyntaxTree typeSyntaxTree = TypeHandler.getInstance().generateTypeSyntaxTree();
        String typeSourceContent = Formatter.format(typeSyntaxTree).toSourceCode();
        if (!typeSourceContent.isBlank()) {
            sourceFiles.add(new GenSrcFile(GenSrcFile.GenFileType.GEN_SRC, null, TYPE_FILE_NAME,
                    licenceHeader + typeSourceContent));
        }

        return sourceFiles;
    }
}
