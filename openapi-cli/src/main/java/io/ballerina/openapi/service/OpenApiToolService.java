package io.ballerina.openapi.service;

import io.ballerina.cli.service.CliToolService;
import io.ballerina.cli.service.types.CommandResponse;
import io.ballerina.cli.service.types.Context;
import io.ballerina.cli.service.types.ResultType;
import io.ballerina.cli.service.types.Status;
import io.ballerina.cli.service.types.SubCommand;
import io.ballerina.tools.text.TextEdit;

import java.util.List;
import java.util.Map;

public class OpenApiToolService implements CliToolService {
    @Override
    public CommandResponse executeCommand(String command, String[] arguments, Context context) {
        Builder builder = new Builder();
        if (!command.equals("add")) {
            return builder.status(Status.FAILURE)
                    .resultTypes(List.of(ResultType.ERROR))
                    .errors(List.of(new Error("invalid command: " + command)))
                    .build();
        }

        SourceGenerator sourceGenerator = new SourceGenerator(arguments[0],
                context.contextMap().get("projectPath").toString());

        try {
            Map<String, List<TextEdit>> generate = sourceGenerator.generate();
            return builder.status(Status.SUCCESS)
                    .resultTypes(List.of(ResultType.TEXT_EDIT))
                    .textEdits(Map.of())
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

    static class Builder {
        private Status status;
        private List<ResultType> resultTypes;
        private Map<String, TextEdit> textEdits;
        private List<Error> errors;

        public Builder status(Status status) {
            this.status = status;
            return this;
        }

        public Builder resultTypes(List<ResultType> resultTypes) {
            this.resultTypes = resultTypes;
            return this;
        }

        public Builder textEdits(Map<String, TextEdit> textEdits) {
            this.textEdits = textEdits;
            return this;
        }

        public Builder errors(List<Error> errors) {
            this.errors = errors;
            return this;
        }

        public CommandResponse build() {
            return new CommandResponse(this.status, this.resultTypes, this.textEdits, this.errors);
        }
    }
}
