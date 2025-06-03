package io.ballerina.openapi.service;

import io.ballerina.cli.service.CliToolService;
import io.ballerina.cli.service.types.CommandResponse;
import io.ballerina.cli.service.types.Context;
import io.ballerina.cli.service.types.ResultType;
import io.ballerina.cli.service.types.Status;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

public class AddCommandTests {

    private static final Path RES_DIR = Paths.get("src/test/resources").toAbsolutePath();
    private static final Path SOURCE_RES_DIR = Paths.get("src/test/resources/service/add").toAbsolutePath();

    @Test(description = "Test add command for OpenAPI service")
    public void testAddCommand() {
        // get the project path
        Path projectPath = SOURCE_RES_DIR.resolve("project_1").toAbsolutePath();

        // get a path of an openapi contract
        Path openApiContractPath = RES_DIR.resolve("petstore.yaml").toAbsolutePath();

        String command = "add";
        List<String> arguments = List.of(openApiContractPath.toString());
        Map<String, Object> context = Map.of("projectPath", projectPath.toString());


        Context ctx = new Context() {
            @Override
            public Map<String, Object> contextMap() {
                return context;
            }

            @Override
            public String requiredKeys() {
                return "projectPath";
            }
        };

        ServiceLoader<CliToolService> services = ServiceLoader.load(CliToolService.class,
                Thread.currentThread().getContextClassLoader());
        for (CliToolService service : services) {
            CommandResponse commandResponse = service.executeCommand(command, arguments.toArray(new String[0]), ctx);
            Assert.assertEquals(Status.SUCCESS, commandResponse.status());
            Assert.assertTrue(commandResponse.resultTypes().contains(ResultType.TEXT_EDIT));
            Assert.assertEquals(commandResponse.textEdits().size(), 4);
        }
    }
}
