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

import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.openapi.core.generators.client.diagnostic.ClientDiagnosticImp;
import io.ballerina.openapi.core.generators.client.diagnostic.DiagnosticMessages;
import io.ballerina.openapi.core.generators.common.GeneratorConstants;
import io.ballerina.openapi.core.generators.common.exception.InvalidReferenceException;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import static io.ballerina.openapi.core.generators.common.GeneratorConstants.DEFAULT;
import static io.ballerina.openapi.core.generators.common.GeneratorUtils.generateStatusCodeTypeInclusionRecord;

/**
 * This class is used to generate the status code response return types.
 *
 * @since 1.9.0
 */
public class FunctionStatusCodeReturnTypeGenerator extends FunctionReturnTypeGeneratorImp {
    private final String path;
    private final BallerinaUtilGenerator ballerinaUtilGenerator;
    private boolean hasDefaultStatusCodeBinding = false;
    private List<String> nonDefaultStatusCodes = new ArrayList<>();


    public FunctionStatusCodeReturnTypeGenerator(Operation operation, OpenAPI openAPI, String httpMethod,
                                                 String path, BallerinaUtilGenerator ballerinaUtilGenerator) {
        super(operation, openAPI, httpMethod);
        this.path = path;
        this.ballerinaUtilGenerator = ballerinaUtilGenerator;
    }

    @Override
    protected boolean populateReturnType(String statusCode, ApiResponse response, List<TypeDescriptorNode> returnTypes,
                                         HashSet<String> returnTypesSet) {
        try {
            String code = GeneratorConstants.HTTP_CODES_DES.get(statusCode);
            if (Objects.isNull(code)) {
                diagnostics.add(new ClientDiagnosticImp(DiagnosticMessages.OAS_CLIENT_113, statusCode));
                return false;
            }

            List<Diagnostic> newDiagnostics = new ArrayList<>();
            returnTypes.add(generateStatusCodeTypeInclusionRecord(code, response, httpMethod, openAPI, path,
                    newDiagnostics));
            if (statusCode.equals(DEFAULT)) {
                this.hasDefaultStatusCodeBinding = true;
                if (!ballerinaUtilGenerator.hasDefaultStatusCodeResponseBinding()) {
                    ballerinaUtilGenerator.setDefaultStatusCodeResponseBinding(true);
                }
            } else {
                nonDefaultStatusCodes.add(statusCode);
            }
            diagnostics.addAll(newDiagnostics.stream().map(ClientDiagnosticImp::new).toList());
        } catch (InvalidReferenceException e) {
            diagnostics.add(new ClientDiagnosticImp(e.getDiagnostic()));
        }
        return false;
    }

    @Override
    public boolean hasDefaultStatusCodeBinding() {
        return hasDefaultStatusCodeBinding;
    }

    @Override
    public List<String> getNonDefaultStatusCodes() {
        return nonDefaultStatusCodes;
    }
}
