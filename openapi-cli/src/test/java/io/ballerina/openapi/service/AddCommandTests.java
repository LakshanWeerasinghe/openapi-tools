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
import io.ballerina.cli.service.types.CommandResponse;
import io.ballerina.cli.service.types.ResultType;
import io.ballerina.cli.service.types.Status;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
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
        Map<String, Object> context = Map.of("projectPath", projectPath.toString());
        String[] arguments = new String[]{
                "--input", openApiContractPath.toString(),
                "--id", "petstore",
                "--module", "petstore"
        };

        ServiceLoader<CliToolService> services = ServiceLoader.load(CliToolService.class,
                Thread.currentThread().getContextClassLoader());
        for (CliToolService service : services) {
            if (!isOpenApiToolService(service)) {
                continue;
            }
            CommandResponse commandResponse = service.executeCommand(command, arguments, context);
            Assert.assertEquals(Status.SUCCESS, commandResponse.status());
            Assert.assertTrue(commandResponse.resultTypes().contains(ResultType.TEXT_EDIT));
            Assert.assertEquals(commandResponse.textEdits().size(), 4);
            return;
        }
        Assert.fail("OpenAPI tool service not found");
    }

    private static boolean isOpenApiToolService(CliToolService service) {
        return service.getClass().getAnnotation(ToolName.class) != null &&
                service.getClass().getAnnotation(ToolName.class).value().equals("openapi");
    }
}
