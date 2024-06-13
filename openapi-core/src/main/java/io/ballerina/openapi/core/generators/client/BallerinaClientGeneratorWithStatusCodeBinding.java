/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org).
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
package io.ballerina.openapi.core.generators.client;

import io.ballerina.compiler.syntax.tree.AnnotationDeclarationNode;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.ExpressionStatementNode;
import io.ballerina.compiler.syntax.tree.ExternalFunctionBodyNode;
import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import io.ballerina.compiler.syntax.tree.FunctionBodyNode;
import io.ballerina.compiler.syntax.tree.FunctionCallExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.RecordFieldNode;
import io.ballerina.compiler.syntax.tree.RecordTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.openapi.core.generators.client.diagnostic.ClientDiagnosticImp;
import io.ballerina.openapi.core.generators.client.diagnostic.DiagnosticMessages;
import io.ballerina.openapi.core.generators.client.model.OASClientConfig;
import io.ballerina.openapi.core.generators.common.GeneratorConstants;
import io.ballerina.openapi.core.generators.common.GeneratorUtils;
import io.ballerina.openapi.core.generators.common.exception.BallerinaOpenApiException;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyMinutiaeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createLiteralValueToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createSeparatedNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createAnnotationDeclarationNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createAnnotationNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBasicLiteralNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createExpressionStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createExternalFunctionBodyNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionBodyBlockNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionCallExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionDefinitionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionSignatureNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMappingConstructorExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createQualifiedNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRecordFieldNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRecordTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSpecificFieldNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypeDefinitionNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ANNOTATION_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.AT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_PIPE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.EQUAL_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.EXTERNAL_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FUNCTION_DEFINITION;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FUNCTION_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ISOLATED_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OBJECT_METHOD_DEFINITION;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ON_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_PIPE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PRIVATE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RECORD_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.STRING_LITERAL;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.TYPE_KEYWORD;
import static io.ballerina.openapi.core.generators.common.GeneratorConstants.BALLERINA;
import static io.ballerina.openapi.core.generators.common.GeneratorConstants.HTTP;
import static io.ballerina.openapi.core.generators.common.GeneratorConstants.J_BALLERINA;

/**
 * This class is used to generate ballerina client file with the status code response return type support.
 *
 * @since 1.9.0
 */
public class BallerinaClientGeneratorWithStatusCodeBinding extends BallerinaClientGenerator {

    public BallerinaClientGeneratorWithStatusCodeBinding(OASClientConfig oasClientConfig) {
        super(oasClientConfig);
        authConfigGeneratorImp = new AuthConfigGeneratorWithStatusCodeBinding(false, false);
    }

    /**
     * Get the imports required for the client generation.
     * <pre>
     * import ballerina/http;
     * import ballerina/jballerina.java;
     * </pre>
     *
     * @return {@link List<ImportDeclarationNode>} List of ImportDeclarationNode
     */
    @Override
    protected List<ImportDeclarationNode> getImportDeclarationNodes() {
        List<ImportDeclarationNode> importDeclarationNodes = super.getImportDeclarationNodes();
        ImportDeclarationNode importForJBallerina = GeneratorUtils.getImportDeclarationNode(BALLERINA, J_BALLERINA);
        importDeclarationNodes.add(importForJBallerina);
        return importDeclarationNodes;
    }

    @Override
    protected List<ModuleMemberDeclarationNode> getModuleMemberDeclarationNodes() throws BallerinaOpenApiException {
        List<ModuleMemberDeclarationNode> nodes = new ArrayList<>();
        nodes.add(getSetModuleFunction());
        nodes.add(getModuleInitFunction());
        nodes.add(getClientMethodImplType());
        nodes.add(getMethodImplAnnotation());
        nodes.add(getClientErrorType());
        nodes.addAll(super.getModuleMemberDeclarationNodes());
        return nodes;
    }

    @Override
    protected FunctionGeneratorResults addRemoteFunction(Map.Entry<PathItem.HttpMethod, Operation> operationEntry,
                                                         String path, List<FunctionDefinitionNode> functionNodes) {
        FunctionGeneratorResults result = super.addRemoteFunction(operationEntry, path, functionNodes);
        if (result.isSuccess()) {
            addClientFunctionImpl(operationEntry, path, functionNodes, result.hasDefaultStatusCodeBinding(),
                    result.nonDefaultStatusCodes());
        }
        return result;
    }

    @Override
    protected RemoteFunctionGenerator getRemoteFunctionGenerator(Map.Entry<PathItem.HttpMethod, Operation> operation,
                                                                 String path) {
        return new RemoteExternalFunctionGenerator(path, operation, openAPI, authConfigGeneratorImp,
                ballerinaUtilGenerator, imports);
    }

    @Override
    protected QualifiedNameReferenceNode getHttpClientTypeName() {
        return createQualifiedNameReferenceNode(createIdentifierToken(HTTP), createToken(COLON_TOKEN),
                createIdentifierToken(GeneratorConstants.STATUS_CODE_CLIENT));
    }

    private void addClientFunctionImpl(Map.Entry<PathItem.HttpMethod, Operation> operationEntry, String path,
                                       List<FunctionDefinitionNode> clientFunctionNodes, boolean hasDefaultResponse,
                                       List<String> nonDefaultStatusCodes) {
        FunctionDefinitionNode clientExternFunction = clientFunctionNodes.get(clientFunctionNodes.size() - 1);
        Optional<FunctionDefinitionNode> implFunction = createImplFunction(path, operationEntry, openAPI,
                authConfigGeneratorImp, ballerinaUtilGenerator, clientExternFunction, hasDefaultResponse,
                nonDefaultStatusCodes);
        if (implFunction.isPresent()) {
            clientFunctionNodes.add(implFunction.get());
        } else {
            diagnostics.add(new ClientDiagnosticImp(DiagnosticMessages.OAS_CLIENT_112,
                    operationEntry.getValue().getOperationId()));
            clientFunctionNodes.remove(clientFunctionNodes.size() - 1);
        }
    }

    @Override
    protected FunctionGeneratorResults addResourceFunction(Map.Entry<PathItem.HttpMethod, Operation> operationEntry,
                                                           String path, List<FunctionDefinitionNode> functionNodes) {
        FunctionGeneratorResults result = super.addResourceFunction(operationEntry, path, functionNodes);
        if (result.isSuccess()) {
            addClientFunctionImpl(operationEntry, path, functionNodes, result.hasDefaultStatusCodeBinding(),
                    result.nonDefaultStatusCodes());
        }
        return result;
    }

    @Override
    protected ResourceFunctionGenerator getResourceFunctionGenerator(Map.Entry<PathItem.HttpMethod,
            Operation> operation, String path) {
       return new ResourceExternalFunctionGenerator(operation, path, openAPI, authConfigGeneratorImp,
               ballerinaUtilGenerator, imports);
    }

    protected Optional<FunctionDefinitionNode> createImplFunction(String path,
                                                                Map.Entry<PathItem.HttpMethod, Operation> operation,
                                                                OpenAPI openAPI,
                                                                AuthConfigGeneratorImp authConfigGeneratorImp,
                                                                BallerinaUtilGenerator ballerinaUtilGenerator,
                                                                FunctionDefinitionNode clientExternFunction,
                                                                boolean hasDefaultResponse,
                                                                List<String> nonDefaultStatusCodes) {
        //Create qualifier list
        NodeList<Token> qualifierList = createNodeList(createToken(PRIVATE_KEYWORD), createToken(ISOLATED_KEYWORD));
        Token functionKeyWord = createToken(FUNCTION_KEYWORD);
        IdentifierToken functionName = createIdentifierToken(operation.getValue().getOperationId() + "Impl");
        // Create function signature
        ImplFunctionSignatureGenerator signatureGenerator = getImplFunctionSignatureGenerator(path, operation,
                openAPI, clientExternFunction);
        //Create function body
        FunctionBodyGenerator functionBodyGenerator = getFunctionBodyGeneratorImp(path, operation, openAPI,
                authConfigGeneratorImp, ballerinaUtilGenerator, hasDefaultResponse, nonDefaultStatusCodes,
                signatureGenerator);
        Optional<FunctionBodyNode> functionBodyNodeResult = functionBodyGenerator.getFunctionBodyNode();
        diagnostics.addAll(functionBodyGenerator.getDiagnostics());
        if (functionBodyNodeResult.isEmpty()) {
            return Optional.empty();
        }
        FunctionBodyNode functionBodyNode = functionBodyNodeResult.get();
        Optional<FunctionSignatureNode> functionSignatureNode = signatureGenerator.generateFunctionSignature();
        return functionSignatureNode.map(signatureNode ->
                NodeFactory.createFunctionDefinitionNode(OBJECT_METHOD_DEFINITION, null, qualifierList,
                        functionKeyWord, functionName, createEmptyNodeList(), signatureNode, functionBodyNode));
    }

    protected ImplFunctionSignatureGenerator getImplFunctionSignatureGenerator(String path,
                                                                               Map.Entry<PathItem.HttpMethod, Operation>
                                                                                       operation, OpenAPI openAPI,
                                                                               FunctionDefinitionNode
                                                                                       clientExternFunction) {
        return new ImplFunctionSignatureGenerator(operation.getValue(),
                openAPI, operation.getKey().toString().toLowerCase(Locale.ROOT), path, clientExternFunction);
    }

    protected FunctionBodyGenerator getFunctionBodyGeneratorImp(String path,
                                                                Map.Entry<PathItem.HttpMethod, Operation> operation,
                                                                OpenAPI openAPI,
                                                                AuthConfigGeneratorImp authConfigGeneratorImp,
                                                                BallerinaUtilGenerator ballerinaUtilGenerator,
                                                                boolean hasDefaultResponse,
                                                                List<String> nonDefaultStatusCodes,
                                                                ImplFunctionSignatureGenerator signatureGenerator) {
        return new ImplFunctionBodyGenerator(path, operation, openAPI,
                authConfigGeneratorImp, ballerinaUtilGenerator, imports, signatureGenerator.hasHeaders(),
                signatureGenerator.hasDefaultHeaders(), signatureGenerator.hasQueries(), hasDefaultResponse,
                nonDefaultStatusCodes);
    }

    /**
     * Get the setModule function definition node.
     * <pre>
     * function setModule() = @java:Method {
     *     'class: "io.ballerina.openapi.client.ModuleUtils"
     * } external;
     * </pre>
     *
     * @return {@link FunctionDefinitionNode} FunctionDefinitionNode
     */
    private FunctionDefinitionNode getSetModuleFunction() {
        NodeList<Token> emptyQualifiers = createEmptyNodeList();
        NodeList<Node> emptyNodeList = createEmptyNodeList();
        SeparatedNodeList<ParameterNode> emptyParamList = createSeparatedNodeList();
        FunctionSignatureNode functionSignatureNode = createFunctionSignatureNode(createToken(OPEN_PAREN_TOKEN),
                emptyParamList, createToken(CLOSE_PAREN_TOKEN), null);
        QualifiedNameReferenceNode javaMethodToken = createQualifiedNameReferenceNode(
                createIdentifierToken("java"), createToken(COLON_TOKEN), createIdentifierToken("Method"));
        BasicLiteralNode classValueExp = createBasicLiteralNode(STRING_LITERAL,
                createLiteralValueToken(SyntaxKind.STRING_LITERAL_TOKEN,
                        "\"io.ballerina.openapi.client.ModuleUtils\"",
                        createEmptyMinutiaeList(),
                        createEmptyMinutiaeList()));
        SpecificFieldNode classFieldNode = createSpecificFieldNode(null, createIdentifierToken("'class"),
                createToken(COLON_TOKEN), classValueExp);
        MappingConstructorExpressionNode methodMapExp = createMappingConstructorExpressionNode(
                createToken(OPEN_BRACE_TOKEN), createSeparatedNodeList(classFieldNode),
                createToken(CLOSE_BRACE_TOKEN));
        AnnotationNode javaMethodAnnot = createAnnotationNode(createToken(AT_TOKEN), javaMethodToken, methodMapExp);
        ExternalFunctionBodyNode externalFunctionBodyNode = createExternalFunctionBodyNode(createToken(EQUAL_TOKEN),
                createNodeList(javaMethodAnnot), createToken(EXTERNAL_KEYWORD), createToken(SEMICOLON_TOKEN));
        return createFunctionDefinitionNode(FUNCTION_DEFINITION, null, emptyQualifiers, createToken(FUNCTION_KEYWORD),
                createIdentifierToken("setModule"), emptyNodeList, functionSignatureNode, externalFunctionBodyNode);
    }

    /**
     * Get the module init function definition node.
     * <pre>
     * function init() {
     *    setModule();
     * }
     * </pre>
     *
     * @return {@link FunctionDefinitionNode} FunctionDefinitionNode
     */
    private FunctionDefinitionNode getModuleInitFunction() {
        NodeList<Token> emptyQualifiers = createEmptyNodeList();
        NodeList<Node> emptyNodeList = createEmptyNodeList();
        SeparatedNodeList<ParameterNode> emptyParamList = createSeparatedNodeList();
        FunctionSignatureNode functionSignatureNode = createFunctionSignatureNode(createToken(OPEN_PAREN_TOKEN),
                emptyParamList, createToken(CLOSE_PAREN_TOKEN), null);
        FunctionCallExpressionNode setModuleFuncCall = createFunctionCallExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken("setModule")), createToken(OPEN_PAREN_TOKEN),
                createSeparatedNodeList(), createToken(CLOSE_PAREN_TOKEN));
        ExpressionStatementNode setModuleStatement = createExpressionStatementNode(null, setModuleFuncCall,
                createToken(SEMICOLON_TOKEN));
        NodeList<StatementNode> statementsNodeList = createNodeList(setModuleStatement);
        FunctionBodyBlockNode functionBodyNode = createFunctionBodyBlockNode(createToken(OPEN_BRACE_TOKEN),
                null, statementsNodeList, createToken(CLOSE_BRACE_TOKEN), null);
        return createFunctionDefinitionNode(FUNCTION_DEFINITION, null, emptyQualifiers, createToken(FUNCTION_KEYWORD),
                createIdentifierToken("init"), emptyNodeList, functionSignatureNode, functionBodyNode);
    }

    /**
     * Get the ClientMethodImpl record type definition node.
     * <pre>
     * type ClientMethodImpl record {|
     *    string name;
     * |};
     * </pre>
     *
     * @return {@link TypeDefinitionNode} TypeDefinitionNode
     */
    private TypeDefinitionNode getClientMethodImplType() {
        RecordFieldNode nameFieldNode = createRecordFieldNode(null, null, createIdentifierToken("string"),
                createIdentifierToken("name"), null, createToken(SEMICOLON_TOKEN));
        RecordTypeDescriptorNode recordDescriptor = createRecordTypeDescriptorNode(createToken(RECORD_KEYWORD),
                createToken(OPEN_BRACE_PIPE_TOKEN), createNodeList(nameFieldNode), null,
                createToken(CLOSE_BRACE_PIPE_TOKEN));
        return createTypeDefinitionNode(null, null, createToken(TYPE_KEYWORD),
                createIdentifierToken("ClientMethodImpl"), recordDescriptor, createToken(SEMICOLON_TOKEN));
    }

    /**
     * Get the MethodImpl annotation declaration node.
     * <pre>
     * annotation ClientMethodImpl MethodImpl on function;
     * </pre>
     *
     * @return {@link AnnotationDeclarationNode} AnnotationDeclarationNode
     */
    private AnnotationDeclarationNode getMethodImplAnnotation() {
        return createAnnotationDeclarationNode(null, null, null, createToken(ANNOTATION_KEYWORD),
                createIdentifierToken("ClientMethodImpl"), createIdentifierToken("MethodImpl"),
                createToken(ON_KEYWORD), createSeparatedNodeList(createToken(FUNCTION_KEYWORD)),
                createToken(SEMICOLON_TOKEN));
    }

    /**
     * Get the ClientError error definition node.
     * <pre>
     * type ClientError http:ClientError;
     * </pre>
     *
     * @return {@link TypeDefinitionNode} TypeDefinitionNode
     */
    protected TypeDefinitionNode getClientErrorType() {
        return createTypeDefinitionNode(null, null, createToken(TYPE_KEYWORD),
                createIdentifierToken("ClientMethodInvocationError"),
                createQualifiedNameReferenceNode(createIdentifierToken("http"), createToken(COLON_TOKEN),
                        createIdentifierToken("ClientError")),
                createToken(SEMICOLON_TOKEN));
    }
}
