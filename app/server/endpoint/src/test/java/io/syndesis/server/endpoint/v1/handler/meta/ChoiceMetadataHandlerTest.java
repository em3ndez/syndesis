/*
 * Copyright (C) 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.syndesis.server.endpoint.v1.handler.meta;

import java.util.Collections;

import io.syndesis.common.model.DataShape;
import io.syndesis.common.model.DataShapeKinds;
import io.syndesis.common.model.action.ConnectorAction;
import io.syndesis.common.model.action.ConnectorDescriptor;
import io.syndesis.common.model.connection.DynamicActionMetadata;
import io.syndesis.common.model.integration.Step;
import io.syndesis.common.model.integration.StepKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Christoph Deppisch
 */
public class ChoiceMetadataHandlerTest {

    private final ChoiceMetadataHandler metadataHandler = new ChoiceMetadataHandler();

    private final Step choiceStep = new Step.Builder()
            .stepKind(StepKind.choice)
            .build();

    @Test
    public void shouldCreateMetaDataFromSurroundingSteps() {
        Step previousStep = new Step.Builder()
                .stepKind(StepKind.endpoint)
                .action(new ConnectorAction.Builder()
                        .descriptor(new ConnectorDescriptor.Builder()
                                .inputDataShape(StepMetadataHelper.NO_SHAPE)
                                .outputDataShape(new DataShape.Builder()
                                        .kind(DataShapeKinds.JSON_INSTANCE)
                                        .specification("{}")
                                        .description("previousOutput")
                                        .addVariant(testShape("variant1"))
                                        .addVariant(testShape("variant2"))
                                        .build())
                                .build())
                        .build())
                .build();

        Step subsequentStep = new Step.Builder()
                .stepKind(StepKind.endpoint)
                .action(new ConnectorAction.Builder()
                        .descriptor(new ConnectorDescriptor.Builder()
                                .inputDataShape(new DataShape.Builder()
                                        .kind(DataShapeKinds.JSON_INSTANCE)
                                        .specification("{}")
                                        .description("subsequentInput")
                                        .addVariant(testShape("variant3"))
                                        .addVariant(testShape("variant4"))
                                        .build())
                                .outputDataShape(StepMetadataHelper.NO_SHAPE)
                                .build())
                        .build())
                .build();

        DynamicActionMetadata metadata = metadataHandler.createMetadata(choiceStep, Collections.singletonList(previousStep), Collections.singletonList(subsequentStep));

        Assertions.assertNotNull(metadata.outputShape());
        Assertions.assertEquals(DataShapeKinds.ANY, metadata.outputShape().getKind());

        Assertions.assertNotNull(metadata.inputShape());
        Assertions.assertEquals(DataShapeKinds.JSON_INSTANCE, metadata.inputShape().getKind());
        Assertions.assertEquals("previousOutput", metadata.inputShape().getDescription());
        Assertions.assertEquals(2, metadata.inputShape().getVariants().size());
        Assertions.assertEquals("variant1", metadata.inputShape().getVariants().get(0).getMetadata().get("name"));
        Assertions.assertEquals("variant2", metadata.inputShape().getVariants().get(1).getMetadata().get("name"));
    }

    @Test
    public void shouldCreateMetaDataFromAnyShapes() {
        Step previousStep = new Step.Builder()
                .stepKind(StepKind.endpoint)
                .action(new ConnectorAction.Builder()
                        .descriptor(new ConnectorDescriptor.Builder()
                                .inputDataShape(StepMetadataHelper.NO_SHAPE)
                                .outputDataShape(StepMetadataHelper.ANY_SHAPE)
                                .build())
                        .build())
                .build();

        Step subsequentStep = new Step.Builder()
                .stepKind(StepKind.endpoint)
                .action(new ConnectorAction.Builder()
                        .descriptor(new ConnectorDescriptor.Builder()
                                .inputDataShape(StepMetadataHelper.ANY_SHAPE)
                                .outputDataShape(StepMetadataHelper.NO_SHAPE)
                                .build())
                        .build())
                .build();

        DynamicActionMetadata metadata = metadataHandler.createMetadata(choiceStep, Collections.singletonList(previousStep), Collections.singletonList(subsequentStep));

        Assertions.assertEquals(StepMetadataHelper.ANY_SHAPE, metadata.inputShape());
        Assertions.assertEquals(StepMetadataHelper.ANY_SHAPE, metadata.outputShape());
    }

    @Test
    public void shouldCreateMetaDataFromEmptySurroundingSteps() {
        DynamicActionMetadata metadata = metadataHandler.createMetadata(choiceStep, Collections.emptyList(), Collections.emptyList());

        Assertions.assertEquals(StepMetadataHelper.NO_SHAPE, metadata.inputShape());
        Assertions.assertEquals(StepMetadataHelper.ANY_SHAPE, metadata.outputShape());
    }

    @Test
    public void shouldPreserveGivenMetadata() {
        DynamicActionMetadata metadata = new DynamicActionMetadata.Builder()
                .inputShape(testShape("input"))
                .outputShape(testShape("output"))
                .build();

        DynamicActionMetadata enrichedMetadata = metadataHandler.handle(metadata);
        Assertions.assertEquals(metadata, enrichedMetadata);
    }

    private static DataShape testShape(String name) {
        return new DataShape.Builder()
                .kind(DataShapeKinds.JSON_INSTANCE)
                .description(name)
                .specification("{}")
                .putMetadata("name", name)
                .build();
    }
}
