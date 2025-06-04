package io.ballerina.openapi.service;

import io.ballerina.cli.service.CliToolService;
import io.ballerina.cli.service.ToolName;
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

        SourceGenerator sourceGenerator = new SourceGenerator(arguments[0], context.get("projectPath").toString());
        try {
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
        return List.of();
    }

    @Override
    public boolean isCommandAvailable(String subCommand) {
        return "add".equals(subCommand);
    }
}
