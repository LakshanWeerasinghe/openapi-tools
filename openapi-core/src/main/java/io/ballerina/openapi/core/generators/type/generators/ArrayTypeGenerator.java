/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

package io.ballerina.openapi.core.generators.type.generators;

import io.ballerina.compiler.syntax.tree.ArrayDimensionNode;
import io.ballerina.compiler.syntax.tree.ArrayTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.NameReferenceNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.OptionalTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.openapi.core.generators.common.GeneratorUtils;
import io.ballerina.openapi.core.generators.common.exception.BallerinaOpenApiException;
import io.ballerina.openapi.core.generators.common.exception.UnsupportedOASDataTypeException;
import io.ballerina.openapi.core.generators.type.GeneratorConstants;
import io.ballerina.openapi.core.generators.type.TypeGeneratorUtils;
import io.ballerina.openapi.core.generators.type.diagnostic.TypeGenerationDiagnosticMessages;
import io.ballerina.openapi.core.generators.type.diagnostic.TypeGeneratorDiagnostic;
import io.ballerina.openapi.core.generators.type.exception.OASTypeGenException;
import io.ballerina.openapi.core.generators.type.model.GeneratorMetaData;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createArrayTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBuiltinSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createParenthesisedTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.UNION_TYPE_DESC;
import static io.ballerina.openapi.core.generators.common.GeneratorUtils.convertOpenAPITypeToBallerina;

/**
 * Generate TypeDefinitionNode and TypeDescriptorNode for array schemas.
 * -- ex:
 * Sample OpenAPI :
 * <pre>
 *     Pets:
 *       type: array
 *       items:
 *         $ref: "#/components/schemas/Pet"
 *  </pre>
 * Generated Ballerina type for the schema `Pet` :
 * <pre>
 *      public type Pets Pet[];
 * </pre>
 *
 * @since 1.3.0
 */
public class ArrayTypeGenerator extends TypeGenerator {
    private String parentType;

    public ArrayTypeGenerator(Schema schema, String typeName, boolean ignoreNullableFlag,
                              String parentType, HashMap<String, TypeDefinitionNode> subTypesMap,
                              HashMap<String, NameReferenceNode> pregeneratedTypeMap) {
        super(schema, typeName, ignoreNullableFlag, subTypesMap, pregeneratedTypeMap);
        this.parentType = parentType;
    }

    /**
     * Generate TypeDescriptorNode for array type schemas. If array type is not given, type will be `AnyData`
     * public type StringArray string[];
     */
    @Override
    public TypeDescriptorNode generateTypeDescriptorNode() throws OASTypeGenException {
        Schema<?> items = schema.getItems();
        boolean isConstraintsAvailable = !GeneratorMetaData.getInstance().isNullable() &&
                        GeneratorUtils.hasConstraints(items) && typeName != null;
        TypeGenerator typeGenerator;
        if (isConstraintsAvailable) {
            String normalizedTypeName = typeName.replaceAll(GeneratorConstants.SPECIAL_CHARACTER_REGEX, "").trim();
            String itemType = StringUtils.capitalize(GeneratorUtils.getOpenAPIType(items));
            String fieldName = StringUtils.capitalize(normalizedTypeName);
            typeName = GeneratorUtils.escapeIdentifier(
                    parentType != null ?
                            parentType + fieldName + "Items" + itemType :
                            fieldName + "Items" + itemType);
            typeGenerator = TypeGeneratorUtils.getTypeGenerator(items, typeName, null, ignoreNullableFlag,
                    subTypesMap, pregeneratedTypeMap);
            if (!pregeneratedTypeMap.containsKey(typeName)) {
                pregeneratedTypeMap.put(typeName, createSimpleNameReferenceNode(createIdentifierToken(typeName)));
                TypeDefinitionNode arrayItemWithConstraint = typeGenerator.generateTypeDefinitionNode(
                        createIdentifierToken(typeName));
                imports.addAll(typeGenerator.getImports());
                subTypesMap.put(typeName, arrayItemWithConstraint);
            }
        } else {
            typeGenerator = TypeGeneratorUtils.getTypeGenerator(items, typeName, null, ignoreNullableFlag,
                    subTypesMap, pregeneratedTypeMap);
        }

        TypeDescriptorNode typeDescriptorNode;
        if ((typeGenerator instanceof PrimitiveTypeGenerator ||
                typeGenerator instanceof ArrayTypeGenerator) && isConstraintsAvailable) {
            typeDescriptorNode = NodeParser.parseTypeDescriptor(typeName);
        } else {
            typeDescriptorNode = typeGenerator.generateTypeDescriptorNode();
        }

        if (typeGenerator instanceof UnionTypeGenerator || (items.getEnum() != null && !items.getEnum().isEmpty())) {
            typeDescriptorNode = createParenthesisedTypeDescriptorNode(
                    createToken(OPEN_PAREN_TOKEN), typeDescriptorNode, createToken(CLOSE_PAREN_TOKEN));
        }
        if (typeDescriptorNode instanceof OptionalTypeDescriptorNode) {
            Node node = ((OptionalTypeDescriptorNode) typeDescriptorNode).typeDescriptor();
            typeDescriptorNode = (TypeDescriptorNode) node;
        }

        if (schema.getMaxItems() != null) {
            if (schema.getMaxItems() > GeneratorConstants.MAX_ARRAY_LENGTH) {
                diagnostics.add(new TypeGeneratorDiagnostic(TypeGenerationDiagnosticMessages.OAS_TYPE_101,
                        schema.getMaxItems().toString()));
                schema.setMaxItems(GeneratorConstants.MAX_ARRAY_LENGTH);
            }
        }
        NodeList<ArrayDimensionNode> arrayDimensions = NodeFactory.createEmptyNodeList();
        if (typeDescriptorNode.kind() == SyntaxKind.ARRAY_TYPE_DESC) {
            ArrayTypeDescriptorNode innerArrayType = (ArrayTypeDescriptorNode) typeDescriptorNode;
            arrayDimensions = innerArrayType.dimensions();
            typeDescriptorNode = innerArrayType.memberTypeDesc();
        }

        ArrayDimensionNode arrayDimension = NodeFactory.createArrayDimensionNode(
                createToken(SyntaxKind.OPEN_BRACKET_TOKEN), null,
                createToken(SyntaxKind.CLOSE_BRACKET_TOKEN));
        arrayDimensions = arrayDimensions.add(arrayDimension);
        ArrayTypeDescriptorNode arrayTypeDescriptorNode = createArrayTypeDescriptorNode(typeDescriptorNode,
                arrayDimensions);
        imports.addAll(typeGenerator.getImports());
        return TypeGeneratorUtils.getNullableType(schema, arrayTypeDescriptorNode, ignoreNullableFlag);
    }

    /**
     * Generate {@code TypeDescriptorNode} for ArraySchema in OAS.
     */
    public Optional<TypeDescriptorNode> getTypeDescNodeForArraySchema(Schema schema,
                                                                      HashMap<String, TypeDefinitionNode> subTypesMap)
            throws OASTypeGenException {
        TypeDescriptorNode member;
        String schemaType = GeneratorUtils.getOpenAPIType(schema.getItems());
        if (schema.getItems().get$ref() != null) {
            String typeName = null;
            try {
                typeName = GeneratorUtils.extractReferenceType(schema.getItems().get$ref());
            } catch (BallerinaOpenApiException e) {
                throw new OASTypeGenException(e.getMessage());
            }
            String validTypeName = GeneratorUtils.escapeIdentifier(typeName);
            TypeGenerator typeGenerator = TypeGeneratorUtils.getTypeGenerator(
                    GeneratorMetaData.getInstance().getOpenAPI().getComponents().getSchemas().get(typeName),
                    validTypeName, null, ignoreNullableFlag, subTypesMap, pregeneratedTypeMap);
            if (!pregeneratedTypeMap.containsKey(validTypeName)) {
                pregeneratedTypeMap.put(validTypeName, createSimpleNameReferenceNode(
                        createIdentifierToken(validTypeName)));
                TypeDefinitionNode typeDefinitionNode = typeGenerator.generateTypeDefinitionNode(
                        createIdentifierToken(validTypeName));
                subTypesMap.put(validTypeName, typeDefinitionNode);
            }
            try {
                member = createBuiltinSimpleNameReferenceNode(null,
                        createIdentifierToken(GeneratorUtils.escapeIdentifier(GeneratorUtils.
                                extractReferenceType(schema.getItems().get$ref()))));
            } catch (BallerinaOpenApiException e) {
                throw new OASTypeGenException(e.getMessage());
            }
        } else if (schemaType != null && (schemaType.equals(GeneratorConstants.INTEGER) ||
                schemaType.equals(GeneratorConstants.NUMBER) || schemaType.equals(GeneratorConstants.BOOLEAN) ||
                schemaType.equals(GeneratorConstants.STRING))) {
            try {
                member = createBuiltinSimpleNameReferenceNode(null, createIdentifierToken(
                        convertOpenAPITypeToBallerina(schema.getItems(), ignoreNullableFlag)));
            } catch (UnsupportedOASDataTypeException e) {
                throw new OASTypeGenException(e.getDiagnostic().message());
            }
        } else if (schemaType != null && schemaType.equals(GeneratorConstants.ARRAY)) {
            member = getTypeDescNodeForArraySchema(schema.getItems(), subTypesMap).orElse(null);
        } else if (schema.getItems() != null) {
            TypeGenerator typeGenerator = TypeGeneratorUtils.getTypeGenerator(schema.getItems(), typeName,
                    parentType, ignoreNullableFlag, subTypesMap, pregeneratedTypeMap);
            member = typeGenerator.generateTypeDescriptorNode();
        } else {
            return Optional.empty();
        }
        if (schema.getItems().getEnum() != null || (Objects.nonNull(member) && Objects.nonNull(member.kind())
                && member.kind().equals(UNION_TYPE_DESC))) {
            member = createParenthesisedTypeDescriptorNode(
                    createToken(OPEN_PAREN_TOKEN), member, createToken(CLOSE_PAREN_TOKEN));
        }
        return Optional.ofNullable(getArrayTypeDescriptorNodeFromTypeDescriptorNode(member));
    }

    private ArrayTypeDescriptorNode getArrayTypeDescriptorNodeFromTypeDescriptorNode(TypeDescriptorNode
                                                                                             typeDescriptorNode) {
        ArrayDimensionNode arrayDimensionNode = NodeFactory.createArrayDimensionNode(
                createToken(SyntaxKind.OPEN_BRACKET_TOKEN), null,
                createToken(SyntaxKind.CLOSE_BRACKET_TOKEN));
        NodeList<ArrayDimensionNode> nodeList = createNodeList(arrayDimensionNode);
        return createArrayTypeDescriptorNode(typeDescriptorNode, nodeList);
    }
}
