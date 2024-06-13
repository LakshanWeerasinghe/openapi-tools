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

package io.ballerina.openapi.core.generators.client.diagnostic;

import io.ballerina.tools.diagnostics.DiagnosticSeverity;

public enum DiagnosticMessages {
    OAS_CLIENT_100("OAS_CLIENT_100",
            "invalid reference value : %s ballerina only supports local reference values.",
            DiagnosticSeverity.ERROR),
    OAS_CLIENT_101("OAS_CLIENT_101",
            "encounter unsupported path parameter data type for the parameter: '%s'",
            DiagnosticSeverity.WARNING),
    OAS_CLIENT_102("OAS_CLIENT_102",
            "encounter unsupported query parameter data type for the parameter: '%s'",
            DiagnosticSeverity.WARNING),
    OAS_CLIENT_103("OAS_CLIENT_103", "please define the array item type of the parameter : '%s'",
            DiagnosticSeverity.ERROR),
    OAS_CLIENT_104("OAS_CLIENT_104", "error occurred while generating query parameter node: '%s'",
            DiagnosticSeverity.ERROR),
    OAS_CLIENT_105("OAS_CLIENT_105", "provide non-empty value for server variable ",
            DiagnosticSeverity.WARNING),
    OAS_CLIENT_106("OAS_CLIENT_106", "failed to read endpoint details of the server: '%s'",
            DiagnosticSeverity.WARNING),
    OAS_CLIENT_107("OAS_CLIENT_107", "error while generating ", DiagnosticSeverity.WARNING),
    OAS_CLIENT_108("OAS_CLIENT_108", "encounter unsupported header parameter data " +
            "type for the header: '%s'",
            DiagnosticSeverity.WARNING),
    OAS_CLIENT_109("OAS_CLIENT_109", "encounter issue while resolving reference: '%s'",
            DiagnosticSeverity.WARNING),
    OAS_CLIENT_110("OAS_CLIENT_110", "encounter unsupported path parameter data type, " +
            "therefore resource function generation is skipped for given path `%s` , method `%s`",
            DiagnosticSeverity.WARNING),
    OAS_CLIENT_111("OAS_CLIENT_111", "header parameter name can not be empty",
            DiagnosticSeverity.WARNING),
    OAS_CLIENT_112("OAS_CLIENT_112", "failed to generate implementation function for the operation: '%s'",
            DiagnosticSeverity.ERROR),
    OAS_CLIENT_113("OAS_CLIENT_113", "HTTP status code '%s' is not supported in Ballerina HTTP status code responses",
            DiagnosticSeverity.WARNING),
    OAS_CLIENT_114("OAS_CLIENT_114", "the operation: '%s' is skipped in the client generation since only a default" +
            " response found for the operation which is not supported with status code binding option",
            DiagnosticSeverity.WARNING),
    OAS_CLIENT_115("OAS_CLIENT_115", "the operation for given path `%s` , " +
            "method `%s` is skipped in the mock client function generation since it is not provided success response",
            DiagnosticSeverity.WARNING),
    OAS_CLIENT_116("OAS_CLIENT_116", "the operation for given path `%s` , method `%s` is skipped in " +
            "the mock client function generation since it is not provided with examples", DiagnosticSeverity.WARNING),
    OAS_CLIENT_117("OAS_CLIENT_117", "the operation for given path `%s` , method `%s` is skipped in " +
            "the mock client function generation since it has invalid reference", DiagnosticSeverity.WARNING);
    private final String code;
    private final String description;
    private final DiagnosticSeverity severity;

    DiagnosticMessages(String code, String description, DiagnosticSeverity severity) {
        this.code = code;
        this.description = description;
        this.severity = severity;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public DiagnosticSeverity getSeverity() {
        return severity;
    }
}
