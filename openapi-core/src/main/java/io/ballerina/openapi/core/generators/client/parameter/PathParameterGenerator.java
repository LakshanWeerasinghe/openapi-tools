/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
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

package io.ballerina.openapi.core.generators.client.parameter;

import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.openapi.core.generators.client.diagnostic.ClientDiagnostic;
import io.ballerina.openapi.core.generators.client.diagnostic.ClientDiagnosticImp;
import io.ballerina.openapi.core.generators.client.diagnostic.DiagnosticMessages;
import io.ballerina.openapi.core.generators.common.TypeHandler;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRequiredParameterNode;
import static io.ballerina.openapi.core.generators.client.diagnostic.DiagnosticMessages.OAS_CLIENT_101;
import static io.ballerina.openapi.core.generators.common.GeneratorUtils.escapeIdentifier;

public class PathParameterGenerator implements ParameterGenerator {
    Parameter parameter;

    List<ClientDiagnostic> diagnostics = new ArrayList<>();
    public PathParameterGenerator(Parameter parameter, OpenAPI openAPI) {
        this.parameter = parameter;
    }

    @Override
    public Optional<ParameterNode> generateParameterNode() {
        IdentifierToken paramName = createIdentifierToken(escapeIdentifier(parameter.getName()));
        // type should be a any type node.
        Schema parameterSchema = parameter.getSchema();
        // Reference type resolve
        Optional<TypeDescriptorNode> typeNode = TypeHandler.getInstance()
                .getTypeNodeFromOASSchema(parameterSchema, true);
        if (typeNode.isEmpty()) {
            diagnostics.add(new ClientDiagnosticImp(DiagnosticMessages.OAS_CLIENT_101, parameter.getName()));
            return Optional.empty();
        }
        TypeDescriptorNode typeDescNode = typeNode.get();
        if (typeDescNode.kind().equals(SyntaxKind.ARRAY_TYPE_DESC) ||
                typeDescNode.kind().equals(SyntaxKind.RECORD_TYPE_DESC)) {
            ClientDiagnosticImp diagnostic = new ClientDiagnosticImp(OAS_CLIENT_101, parameter.getName());
            diagnostics.add(diagnostic);
            return Optional.empty();
        }

        return Optional.of(createRequiredParameterNode(createEmptyNodeList(), typeDescNode, paramName));
    }

    @Override
    public List<ClientDiagnostic> getDiagnostics() {
        return diagnostics;
    }
}
