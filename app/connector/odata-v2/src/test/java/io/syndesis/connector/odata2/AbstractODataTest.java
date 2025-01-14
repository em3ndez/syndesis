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
package io.syndesis.connector.odata2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.syndesis.common.model.Dependency;
import io.syndesis.common.model.action.ConnectorAction;
import io.syndesis.common.model.action.ConnectorDescriptor;
import io.syndesis.common.model.connection.ConfigurationProperty;
import io.syndesis.common.model.connection.Connector;
import io.syndesis.common.model.integration.Flow;
import io.syndesis.common.model.integration.Integration;
import io.syndesis.common.model.integration.Step;
import io.syndesis.common.model.integration.StepKind;
import io.syndesis.connector.odata2.server.ODataTestServer;
import io.syndesis.connector.odata2.server.util.TestDataGenerator;
import io.syndesis.connector.support.util.PropertyBuilder;
import io.syndesis.integration.runtime.IntegrationRouteBuilder;
import org.apache.camel.CamelContext;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.properties.DefaultPropertiesParser;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.component.properties.PropertiesParser;
import org.apache.camel.spring.SpringCamelContext;
import org.junit.jupiter.api.BeforeAll;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.PropertyResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class AbstractODataTest implements ODataConstants {

    protected final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    protected static final int MOCK_TIMEOUT_MILLISECONDS = 60000;

    protected static final String OLINGO2_READ_FROM_ENDPOINT = "olingo2-olingo2-0-0://read";

    public static final String MANUFACTURERS = "Manufacturers";
    public static final String CARS = "Cars";
    public static final String DRIVERS = "Drivers";

    protected static final String REF_SERVICE_URI = "https://services.odata.org/V2/(S(hhjvqx1ctzambsoerj0ksd1p))/OData/OData.svc/";
    public static final String PRODUCTS = "Products";
    public static final String SUPPLIERS = "Suppliers";
    public static final String CATEGORIES = "Categories";

    @Autowired
    protected ApplicationContext applicationContext;

    protected static ODataTestServer odataTestServer;

    protected static ODataTestServer authTestServer;

    protected static ODataTestServer sslTestServer;

    protected static ODataTestServer sslAuthTestServer;

    protected CamelContext context;

    @Configuration
    public static class TestConfiguration {
        @Bean
        public PropertiesParser propertiesParser(PropertyResolver propertyResolver) {
            return new DefaultPropertiesParser() {
                @Override
                public String parseProperty(String key, String value, Properties properties) {
                    return propertyResolver.getProperty(key);
                }
            };
        }

        @Bean(destroyMethod = "")
        public PropertiesComponent properties(PropertiesParser parser) {
            PropertiesComponent pc = new PropertiesComponent();
            pc.setPropertiesParser(parser);
            return pc;
        }
    }

    @BeforeAll
    public static void startTestServer() throws Exception {
        if (odataTestServer == null) {
            odataTestServer = new ODataTestServer();
            odataTestServer.start();

            TestDataGenerator.generateData(odataTestServer.getServiceUri());
        }

        if (authTestServer == null) {
            authTestServer = new ODataTestServer(ODataTestServer.Options.AUTH_USER);
            authTestServer.start();
        }

        if (sslTestServer == null) {
            sslTestServer = new ODataTestServer(ODataTestServer.Options.SSL);
            sslTestServer.start();
        }

        if (sslAuthTestServer == null) {
            sslAuthTestServer = new ODataTestServer(ODataTestServer.Options.SSL, ODataTestServer.Options.AUTH_USER);
            sslAuthTestServer.start();
        }
    }

    /**
     * @return a string representation of the content of the given stream
     */
    public static String streamToString(InputStream inStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inStream, StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
            builder.append(NEW_LINE);
        }

        return builder.toString().trim();
    }

    protected Connector createODataConnector(PropertyBuilder<String> configurePropBuilder,
                                                                                       PropertyBuilder<ConfigurationProperty> propBuilder) {
        Connector.Builder builder = new Connector.Builder()
            .id("odata-v2")
            .name("OData V2")
            .componentScheme("olingo2")
            .description("Communicate with an OData Version 2.0 service")
            .addDependency(Dependency.maven("org.apache.camel:camel-olingo2"));

        if (configurePropBuilder != null) {
            builder.configuredProperties(configurePropBuilder.build());
        }

        if (propBuilder != null) {
            builder.properties(propBuilder.build());
        }

        return builder.build();
    }

    /**
     * Creates a camel context complete with a properties component that handles
     * lookups of secret values such as passwords. Fetches the values from external
     * properties file.
     */
    protected CamelContext createCamelContext() {
        CamelContext ctx = new SpringCamelContext(applicationContext);
        PropertiesComponent pc = new PropertiesComponent("classpath:odata-test-options.properties");
        ctx.addComponent("properties", pc);
        return ctx;
    }

    protected Connector createODataConnector(PropertyBuilder<String> configurePropBuilder) {
        return createODataConnector(configurePropBuilder, null);
    }

    protected Integration createIntegration(Step... steps) {

        Flow.Builder flowBuilder = new Flow.Builder();
        for (Step step : steps) {
            flowBuilder.addStep(step);
        }

        return new Integration.Builder()
            .id("i-LTS2tYXwF8odCm87k6gz")
            .name("MyODataInt")
            .addTags("log", "odata-v2")
            .addFlow(flowBuilder.build())
            .build();
    }

    protected static IntegrationRouteBuilder newIntegrationRouteBuilder(Integration integration) {
        return new IntegrationRouteBuilder("") {
            @Override
            protected Integration loadIntegration() {
                return integration;
            }
        };
    }

    protected String testData(String fileName, Class<?> tgtClass) throws IOException {
        try (InputStream in = tgtClass.getResourceAsStream(fileName)) {
            return streamToString(in);
        }
    }

    protected String testData(String fileName) throws IOException {
        return testData(fileName, getClass());
    }

    protected Step createMockStep() {
        return new Step.Builder()
            .stepKind(StepKind.endpoint)
            .action(new ConnectorAction.Builder()
                    .descriptor(new ConnectorDescriptor.Builder()
                                .componentScheme("mock")
                                .putConfiguredProperty("name", "result")
                                .build())
                    .build())
            .build();
    }

    @SuppressWarnings( "unchecked" )
    protected <T> T extractJsonFromExchgMsg(MockEndpoint result, int index, Class<T> bodyClass) {
        Object body = result.getExchanges().get(index).getIn().getBody();
        assertTrue(bodyClass.isInstance(body));
        return (T) body;
    }

    protected String extractJsonFromExchgMsg(MockEndpoint result, int index) {
        return extractJsonFromExchgMsg(result, index, String.class);
    }

    protected MockEndpoint initMockEndpoint() {
        MockEndpoint result = context.getEndpoint("mock:result", MockEndpoint.class);
        result.setResultWaitTime(MOCK_TIMEOUT_MILLISECONDS);
        return result;
    }

    protected void testResult(MockEndpoint result, int exchangeIdx, String testDataFile) throws Exception {
        String json = extractJsonFromExchgMsg(result, exchangeIdx);
        String expected = testData(testDataFile);
        JSONAssert.assertEquals(expected, json, JSONCompareMode.LENIENT);
    }

    @SuppressWarnings( "unchecked" )
    protected void testListResult(MockEndpoint result, int exchangeIdx, String... testDataFiles) throws Exception {
        List<String> json = extractJsonFromExchgMsg(result, exchangeIdx, List.class);
        assertEquals(testDataFiles.length, json.size());
        for (int i = 0; i < testDataFiles.length; ++i) {
            String expected = testData(testDataFiles[i]);
            JSONAssert.assertEquals(expected, json.get(i), JSONCompareMode.LENIENT);
        }
    }

}
