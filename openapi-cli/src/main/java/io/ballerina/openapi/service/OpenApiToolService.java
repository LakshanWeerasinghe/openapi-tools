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

import io.ballerina.cli.service.CliToolService;
import io.ballerina.cli.service.ToolName;
import io.ballerina.cli.service.types.Argument;
import io.ballerina.cli.service.types.CommandResponse;
import io.ballerina.cli.service.types.ResultType;
import io.ballerina.cli.service.types.Status;
import io.ballerina.cli.service.types.SubCommand;
import io.ballerina.tools.text.LSPTextEdit;

import java.util.List;
import java.util.Map;

@ToolName(value = "openapi")
public class OpenApiToolService implements CliToolService {
    @Override
    public CommandResponse executeCommand(String command, String[] arguments, Map<String, Object> context) {
        CommandResponse.Builder builder = new CommandResponse.Builder();
        if (!command.equals("add")) {
            return builder.status(Status.FAILURE)
                    .resultTypes(List.of(ResultType.ERROR))
                    .errors(List.of(new Error("invalid command: " + command)))
                    .build();
        }
        try {
            SourceGenerator sourceGenerator = new SourceGenerator(arguments, context.get("projectPath").toString());
            Map<String, List<LSPTextEdit>> generate = sourceGenerator.generate();
            return builder.status(Status.SUCCESS)
                    .resultTypes(List.of(ResultType.TEXT_EDIT))
                    .textEdits(generate)
                    .build();
        } catch (Exception e) {
            return builder.status(Status.FAILURE)
                    .resultTypes(List.of(ResultType.ERROR))
                    .errors(List.of(new Error("Error generating source files: " + e.getMessage())))
                    .build();
        }
    }

    @Override
    public List<SubCommand> getAvailableCommands() {
        List<Argument> addCommandArguments = List.of(
                new Argument("--input", "contractPath", "",
                        false, "Path to the OpenAPI contract file"),
                new Argument("--id", "name",
                        "", false, "Connector name to be generated"),
                new Argument("--module", "name", "",
                        false, "Connector module name to be generated"),
                new Argument("-n", "nullable", "false",
                        true, "Generate nullable types in the connector")
                );
        SubCommand subCommand = new SubCommand("openapi", "add",
                addCommandArguments, ResultType.TEXT_EDIT);
        return List.of(subCommand);
    }

    @Override
    public boolean isCommandAvailable(String subCommand) {
        return "add".equals(subCommand);
    }
}
