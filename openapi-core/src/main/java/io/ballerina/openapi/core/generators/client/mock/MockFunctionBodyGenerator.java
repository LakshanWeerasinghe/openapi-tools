/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.openapi.core.generators.client.mock;

import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import io.ballerina.compiler.syntax.tree.FunctionBodyNode;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.openapi.core.generators.client.FunctionBodyGenerator;
import io.ballerina.openapi.core.generators.client.diagnostic.ClientDiagnostic;
import io.ballerina.openapi.core.generators.client.diagnostic.ClientDiagnosticImp;
import io.ballerina.openapi.core.generators.client.diagnostic.DiagnosticMessages;
import io.ballerina.openapi.core.generators.common.GeneratorConstants;
import io.ballerina.openapi.core.generators.common.GeneratorUtils;
import io.ballerina.openapi.core.generators.common.exception.InvalidReferenceException;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionBodyBlockNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_TOKEN;
import static io.ballerina.openapi.core.generators.common.GeneratorConstants.HTTP_201;
import static io.ballerina.openapi.core.generators.common.GeneratorConstants.POST;
import static io.ballerina.openapi.core.generators.common.GeneratorConstants.RESPONSE;
import static io.ballerina.openapi.core.generators.common.GeneratorUtils.generateStatusCodeTypeInclusionRecord;

/**
 * Mock function body generator.
 *
 * @since 2.1.0
 */
public class MockFunctionBodyGenerator implements FunctionBodyGenerator {
    String path;
    Map.Entry<PathItem.HttpMethod, Operation> operation;
    OpenAPI openAPI;
    List<ClientDiagnostic> diagnostics = new ArrayList<>();
    boolean isAdvanceClient;

    public MockFunctionBodyGenerator(String path, Map.Entry<PathItem.HttpMethod, Operation> operation,
                                     OpenAPI openAPI, boolean isAdvanceClient) {
        this.path = path;
        this.operation = operation;
        this.openAPI = openAPI;
        this.isAdvanceClient = isAdvanceClient;
    }

    @Override
    public Optional<FunctionBodyNode> getFunctionBodyNode() {

        ApiResponses responses = operation.getValue().getResponses();
        String method = operation.getKey().toString().toLowerCase(Locale.ENGLISH);
        //Get the successful response
        ApiResponse successResponse;
        String code;
        //Collect all success responses
        Map<String, ApiResponse> successResponses = new HashMap<>();
        for (Map.Entry<String, ApiResponse> response : responses.entrySet()) {
            if (response.getKey().startsWith("2")) {
                successResponses.put(response.getKey(), response.getValue());
            }
        }
        if (successResponses.isEmpty()) {
            ClientDiagnosticImp diagnosticImp = new ClientDiagnosticImp(DiagnosticMessages.OAS_CLIENT_115,
                    path, operation.getKey().toString());
            diagnostics.add(diagnosticImp);
            return Optional.empty();
        }
        if (method.equals(POST) && successResponses.containsKey(HTTP_201)) {
            successResponse = successResponses.get(HTTP_201);
            code = HTTP_201;
        } else {
            Optional<Map.Entry<String, ApiResponse>> firstRes = successResponses.entrySet().stream().findFirst();
            successResponse = firstRes.get().getValue();
            code = firstRes.get().getKey().toLowerCase(Locale.ENGLISH);
        }

        // Here only consider 2xx response, since this range consider for success status code
        if (successResponse.getContent() == null) {
            ClientDiagnosticImp diagnosticImp = new ClientDiagnosticImp(DiagnosticMessages.OAS_CLIENT_115,
                    path, operation.getKey().toString());
            diagnostics.add(diagnosticImp);
            return Optional.empty();
        }
        Map<String, Example> examples = new HashMap<>();
        for (Map.Entry<String, MediaType> mediaType : successResponse.getContent().entrySet()) {
            MediaType value = mediaType.getValue();
            Schema<?> schema = value.getSchema();

            if (value.getExamples() != null) {
                examples = value.getExamples();
            } else if (value.getExample() != null) {
                Object exampleObject = value.getExample();
                Example example = new Example();
                example.setValue(exampleObject);
                examples.put(RESPONSE, example);
            } else {
                try {
                    examples = getExamplesFromSchema(schema);
                } catch (InvalidReferenceException e) {
                    ClientDiagnosticImp diagnosticImp = new ClientDiagnosticImp(DiagnosticMessages.OAS_CLIENT_117,
                            path, operation.getKey().toString());
                    diagnostics.add(diagnosticImp);
                    return Optional.empty();
                }
            }
        }

        if (examples.isEmpty()) {
            ClientDiagnosticImp diagnosticImp = new ClientDiagnosticImp(DiagnosticMessages.OAS_CLIENT_116,
                    path, operation.getKey().toString());
            diagnostics.add(diagnosticImp);
            return Optional.empty();
        }

        Example example = examples.get(RESPONSE);
        if (example == null) {
            example = examples.values().iterator().next();
        }
        String statement;
        try {
            if (example.get$ref() != null) {
                String exampleName = GeneratorUtils.extractReferenceType(example.get$ref());
                Map<String, Example> exampleMap = openAPI.getComponents().getExamples();
                example = exampleMap.get(exampleName);
            }
            String exampleValue = example.getValue().toString();
            //TODO implement if the response has header example
            if (isAdvanceClient) {
                List<Diagnostic> newDiagnostics = new ArrayList<>();
                statement = getReturnForAdvanceClient(successResponse, code, exampleValue, newDiagnostics);
                diagnostics.addAll(newDiagnostics.stream().map(ClientDiagnosticImp::new).toList());
            } else {
                statement = "return " + exampleValue + ";";
            }
        } catch (InvalidReferenceException e) {
            diagnostics.add(new ClientDiagnosticImp(e.getDiagnostic()));
            return Optional.empty();
        }
        StatementNode returnNode = NodeParser.parseStatement(statement);
        NodeList<StatementNode> statementList = createNodeList(returnNode);

        FunctionBodyBlockNode fBodyBlock = createFunctionBodyBlockNode(createToken(OPEN_BRACE_TOKEN),
                null, statementList, createToken(CLOSE_BRACE_TOKEN), null);
        return Optional.of(fBodyBlock);
    }

    private String getReturnForAdvanceClient(ApiResponse successResponse, String code, String exampleValue,
                                             List<Diagnostic> newDiagnostics) throws InvalidReferenceException {
        String statement;
        code = GeneratorConstants.HTTP_CODES_DES.get(code);
        String method = operation.getKey().toString().toLowerCase(Locale.ENGLISH);
        TypeDescriptorNode typeDescriptorNode = generateStatusCodeTypeInclusionRecord(code,
                successResponse, method, openAPI, path, newDiagnostics);
        statement = "return  <" + typeDescriptorNode.toSourceCode() + " > {\n" +
                "            body : " + exampleValue + "\n" +
                "        };";
        return statement;
    }

    private Map<String, Example> getExamplesFromSchema(Schema<?> schema) throws InvalidReferenceException {
        Map<String, Example> examples = new HashMap<>();
        if (schema.getExample() != null) {
            Object exampleObject = schema.getExample();
            Example example = new Example();
            example.setValue(exampleObject);
            examples.put(RESPONSE, example);
        } else if (schema.getExamples() != null) {
            List schemaExamples = schema.getExamples();
            Object exampleObject = schemaExamples.get(0);
            Example example = new Example();
            example.setValue(exampleObject);
            examples.put(RESPONSE, example);
        } else if (schema.get$ref() != null) {
            String ref = schema.get$ref();
            String refName = GeneratorUtils.extractReferenceType(ref);
            schema = openAPI.getComponents().getSchemas().get(refName);
            if (schema != null) {
                return getExamplesFromSchema(schema);
            }
        }
        return examples;
    }

    @Override
    public List<ClientDiagnostic> getDiagnostics() {
        return diagnostics;
    }
}
