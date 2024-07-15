package io.ballerina.openapi.core.generators.client;

import io.ballerina.compiler.syntax.tree.FunctionBodyNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.openapi.core.generators.client.diagnostic.ClientDiagnostic;
import io.ballerina.openapi.core.generators.client.diagnostic.ClientDiagnosticImp;
import io.ballerina.openapi.core.generators.client.diagnostic.DiagnosticMessages;
import io.ballerina.openapi.core.generators.common.GeneratorUtils;
import io.ballerina.openapi.core.generators.common.exception.BallerinaOpenApiException;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FUNCTION_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ISOLATED_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RESOURCE_ACCESSOR_DEFINITION;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RESOURCE_KEYWORD;

public class ResourceFunctionGenerator implements FunctionGenerator {
    OpenAPI openAPI;
    List<ImportDeclarationNode> imports;
    List<ClientDiagnostic> diagnostics = new ArrayList<>();
    Map.Entry<PathItem.HttpMethod, Operation> operation;
    String path;
    AuthConfigGeneratorImp authConfigGeneratorImp;
    BallerinaUtilGenerator ballerinaUtilGenerator;
    ResourceFunctionSignatureGenerator signatureGenerator;

    ResourceFunctionGenerator(Map.Entry<PathItem.HttpMethod, Operation> operation, String path, OpenAPI openAPI,
                              AuthConfigGeneratorImp authConfigGeneratorImp,
                              BallerinaUtilGenerator ballerinaUtilGenerator, List<ImportDeclarationNode> imports) {
        this.operation = operation;
        this.path = path;
        this.openAPI = openAPI;
        this.authConfigGeneratorImp = authConfigGeneratorImp;
        this.ballerinaUtilGenerator = ballerinaUtilGenerator;
        this.imports = imports;
        this.signatureGenerator = new ResourceFunctionSignatureGenerator(operation.getValue(), openAPI,
                operation.getKey().toString().toLowerCase(Locale.ROOT), path);
    }

    public List<ImportDeclarationNode> getImports() {
        return imports;
    }

    @Override
    public Optional<FunctionDefinitionNode> generateFunction() {
        GeneratorUtils.addCommonParamsToOperationParams(operation, openAPI, path);
        //Create qualifier list
        NodeList<Token> qualifierList = createNodeList(createToken(RESOURCE_KEYWORD), createToken(ISOLATED_KEYWORD));
        Token functionKeyWord = createToken(FUNCTION_KEYWORD);
        String method = operation.getKey().toString().toLowerCase(Locale.ROOT);
        IdentifierToken functionName = createIdentifierToken(method);
        // create relative path
        try {
            //TODO: this will be replace with the common diagnostic approach.
            List<Diagnostic> pathDiagnostics = new ArrayList<>();
            NodeList<Node> relativeResourcePath = GeneratorUtils.getRelativeResourcePath(path, operation.getValue(),
                    openAPI.getComponents(), false, pathDiagnostics);
            if (!pathDiagnostics.isEmpty()) {
                pathDiagnostics.forEach(diagnostic -> {
                    if (diagnostic.diagnosticInfo().code().equals("OAS_COMMON_204")) {
                        DiagnosticMessages message = DiagnosticMessages.OAS_CLIENT_110;
                        ClientDiagnosticImp clientDiagnostic = new ClientDiagnosticImp(message, path, method);
                        diagnostics.add(clientDiagnostic);
                    }
                });
                return Optional.empty();
            }
            // Create function signature
            Optional<FunctionSignatureNode> signatureNodeOptional = signatureGenerator.generateFunctionSignature();
            diagnostics.addAll(signatureGenerator.getDiagnostics());
            if (signatureNodeOptional.isEmpty()) {
                return Optional.empty();
            }
            //Create function body
            Optional<FunctionBodyNode> functionBodyNodeResult = getFunctionBodyNode(diagnostics,
                    signatureGenerator.hasHeaders(), signatureGenerator.hasDefaultHeaders(),
                    signatureGenerator.hasQueries());
            if (functionBodyNodeResult.isEmpty()) {
                return Optional.empty();
            }
            return getFunctionDefinitionNode(qualifierList, functionKeyWord, functionName, relativeResourcePath,
                    signatureNodeOptional.get(), functionBodyNodeResult.get());
        } catch (BallerinaOpenApiException e) {
            return Optional.empty();
        }
    }

    protected Optional<FunctionBodyNode> getFunctionBodyNode(List<ClientDiagnostic> diagnostics, boolean hasHeaders,
                                                             boolean hasDefaultHeaders, boolean hasQueries) {
        FunctionBodyGeneratorImp functionBodyGenerator = new FunctionBodyGeneratorImp(path, operation, openAPI,
                authConfigGeneratorImp, ballerinaUtilGenerator, imports, hasHeaders, hasDefaultHeaders, hasQueries);
        Optional<FunctionBodyNode> functionBodyNodeResult = functionBodyGenerator.getFunctionBodyNode();
        if (functionBodyNodeResult.isEmpty()) {
            diagnostics.addAll(functionBodyGenerator.getDiagnostics());
        }
        return functionBodyNodeResult;
    }

    protected Optional<FunctionDefinitionNode> getFunctionDefinitionNode(NodeList<Token> qualifierList,
                                                                         Token functionKeyWord,
                                                                         IdentifierToken functionName,
                                                                         NodeList<Node> relativeResourcePath,
                                                                         FunctionSignatureNode signatureNode,
                                                                         FunctionBodyNode functionBodyNode) {
        return Optional.of(NodeFactory.createFunctionDefinitionNode(RESOURCE_ACCESSOR_DEFINITION, null, qualifierList,
                functionKeyWord, functionName, relativeResourcePath, signatureNode, functionBodyNode));
    }

    @Override
    public List<ClientDiagnostic> getDiagnostics() {
        return diagnostics;
    }

    public boolean hasDefaultStatusCodeBinding() {
        return signatureGenerator.hasDefaultStatusCodeBinding();
    }

    public List<String> getNonDefaultStatusCodes() {
        return signatureGenerator.getNonDefaultStatusCodes();
    }
}
