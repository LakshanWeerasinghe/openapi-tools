package io.ballerina.openapi.core.generators.client;

import io.ballerina.compiler.syntax.tree.FunctionBodyNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.openapi.core.generators.client.diagnostic.ClientDiagnostic;
import io.ballerina.openapi.core.generators.common.GeneratorUtils;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FUNCTION_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ISOLATED_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OBJECT_METHOD_DEFINITION;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.REMOTE_KEYWORD;

public class RemoteFunctionGenerator implements FunctionGenerator {
    String path;
    Map.Entry<PathItem.HttpMethod, Operation> operation;
    List<ClientDiagnostic> diagnostics = new ArrayList<>();
    OpenAPI openAPI;
    AuthConfigGeneratorImp authConfigGeneratorImp;
    BallerinaUtilGenerator ballerinaUtilGenerator;
    List<ImportDeclarationNode> imports;
    RemoteFunctionSignatureGenerator signatureGenerator;

    RemoteFunctionGenerator(String path, Map.Entry<PathItem.HttpMethod, Operation> operation, OpenAPI openAPI,
                            AuthConfigGeneratorImp authConfigGeneratorImp,
                            BallerinaUtilGenerator ballerinaUtilGenerator, List<ImportDeclarationNode> imports) {
        this.path = path;
        this.operation = operation;
        this.openAPI = openAPI;
        this.authConfigGeneratorImp = authConfigGeneratorImp;
        this.ballerinaUtilGenerator = ballerinaUtilGenerator;
        this.imports = imports;
        this.signatureGenerator = new RemoteFunctionSignatureGenerator(operation.getValue(), openAPI,
                operation.getKey().toString().toLowerCase(Locale.ROOT), path);
    }

    public List<ImportDeclarationNode> getImports() {
        return imports;
    }

    @Override
    public Optional<FunctionDefinitionNode> generateFunction() {
        GeneratorUtils.addCommonParamsToOperationParams(operation, openAPI, path);
        //Create qualifier list
        NodeList<Token> qualifierList = createNodeList(createToken(REMOTE_KEYWORD), createToken(ISOLATED_KEYWORD));
        Token functionKeyWord = createToken(FUNCTION_KEYWORD);
        IdentifierToken functionName = createIdentifierToken(operation.getValue().getOperationId());
        // Create function signature
        Optional<FunctionSignatureNode> signatureNodeOptional = signatureGenerator.generateFunctionSignature();
        diagnostics.addAll(signatureGenerator.getDiagnostics());

        if (signatureNodeOptional.isEmpty()) {
            return Optional.empty();
        }
        FunctionSignatureNode functionSignatureNode = signatureNodeOptional.get();
        //Create function body
        Optional<FunctionBodyNode> functionBodyNodeResult = getFunctionBodyNode(diagnostics,
                signatureGenerator.hasHeaders(), signatureGenerator.hasDefaultHeaders(),
                signatureGenerator.hasQueries());
        if (functionBodyNodeResult.isEmpty()) {
            return Optional.empty();
        }
        FunctionBodyNode functionBodyNode = functionBodyNodeResult.get();

        return getFunctionDefinitionNode(qualifierList, functionKeyWord, functionName, functionSignatureNode,
                functionBodyNode);
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
                                                                         FunctionSignatureNode functionSignatureNode,
                                                                         FunctionBodyNode functionBodyNode) {
        return Optional.of(NodeFactory.createFunctionDefinitionNode(OBJECT_METHOD_DEFINITION, null,
                qualifierList, functionKeyWord, functionName, createEmptyNodeList(), functionSignatureNode,
                functionBodyNode));

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
