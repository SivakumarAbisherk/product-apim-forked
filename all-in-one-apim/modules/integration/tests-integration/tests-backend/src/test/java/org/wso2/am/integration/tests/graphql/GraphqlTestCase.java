/*
 *   Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.am.integration.tests.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.gson.Gson;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIScopeDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.GraphQLSchemaDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.GraphQLValidationResponseDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.GraphQLValidationResponseGraphQLInfoDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.ScopeDTO;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.test.utils.token.TokenUtils;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.testng.Assert.assertEquals;

@SetEnvironment(executionEnvironments = { ExecutionEnvironment.ALL })
public class GraphqlTestCase extends APIMIntegrationBaseTest {
    private static final Log log = LogFactory.getLog(GraphqlTestCase.class);

    private final String GRAPHQL_API_NAME = "CountriesGraphqlAPI";
    private final String API_CONTEXT = "info";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String END_POINT_URL = "https://localhost:9943/am-graphQL-sample/api/graphql/";
    private final String RESPONSE_DATA = "[{\"name\":\"Afrikaans\",\"code\":\"af\"},{\"name\":\"Amharic\",\"code\":\"am\"}," +
            "{\"name\":\"Arabic\",\"code\":\"ar\"},{\"name\":\"Aymara\",\"code\":\"ay\"},{\"name\":\"Azerbaijani\"," +
            "\"code\":\"az\"},{\"name\":\"Belarusian\",\"code\":\"be\"}]";
    private static final String mockgraphQLAPIEndpoint = "/mock_graphql";
    private static final String mockgraphQLAPIIntrospectionResource = "/introspection";
    private static final String mockgraphQLAPISDLResource = "/sdl";
    private static final String GRAPHQL_TEST_USER = "graphqluser";
    private static final String GRAPHQL_TEST_USER_PASSWORD = "graphqlUser";
    private static final String GRAPHQL_ROLE = "graphqlrole";
    private static final long WAIT_TIME = 45 * 1000;

    private String schemaDefinition;
    private String graphqlAPIId;
    private String graphqlAPISDLId;
    private String graphqlAPIEndpointId;
    private String testAppId1;
    private String testAppId2;
    private String testAppId3;
    private String testAppId4;

    private WireMockServer wireMockServer;
    private int endpointPort;
    private String endpointHost = "http://localhost";
    private int lowerPortLimit = 9950;
    private int upperPortLimit = 9999;

    @Factory(dataProvider = "userModeDataProvider")
    public GraphqlTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        userManagementClient.addUser(GRAPHQL_TEST_USER, GRAPHQL_TEST_USER_PASSWORD, new String[]{}, null);
        userManagementClient.addRole(GRAPHQL_ROLE, new String[]{GRAPHQL_TEST_USER}, new String[]{});
        schemaDefinition = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream("graphql" + File.separator + "schema.graphql"),
                "UTF-8");

        File file = getTempFileWithContent(schemaDefinition);
        GraphQLValidationResponseDTO responseApiDto = restAPIPublisher.validateGraphqlSchemaDefinition(file);
        GraphQLValidationResponseGraphQLInfoDTO graphQLInfo = responseApiDto.getGraphQLInfo();
        String arrayToJson = new ObjectMapper().writeValueAsString(graphQLInfo.getOperations());
        JSONArray operations = new JSONArray(arrayToJson);

        ArrayList<String> environment = new ArrayList<String>();
        environment.add(Constants.GATEWAY_ENVIRONMENT);

        ArrayList<String> policies = new ArrayList<String>();
        policies.add("Unlimited");

        JSONObject additionalPropertiesObj = new JSONObject();
        additionalPropertiesObj.put("name", GRAPHQL_API_NAME);
        additionalPropertiesObj.put("context", API_CONTEXT);
        additionalPropertiesObj.put("version", API_VERSION_1_0_0);

        JSONObject url = new JSONObject();
        url.put("url", END_POINT_URL);
        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put("endpoint_type", "http");
        endpointConfig.put("sandbox_endpoints", url);
        endpointConfig.put("production_endpoints", url);
        additionalPropertiesObj.put("endpointConfig", endpointConfig);
        additionalPropertiesObj.put("policies", policies);
        additionalPropertiesObj.put("operations", operations);

        // create Graphql API
        APIDTO apidto = restAPIPublisher.importGraphqlSchemaDefinition(file, additionalPropertiesObj.toString());
        graphqlAPIId = apidto.getId();
        HttpResponse createdApiResponse = restAPIPublisher.getAPI(graphqlAPIId);
        assertEquals(Response.Status.OK.getStatusCode(), createdApiResponse.getResponseCode(),
                GRAPHQL_API_NAME + " API creation is failed");
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(graphqlAPIId, restAPIPublisher);
        // publish api
        restAPIPublisher.changeAPILifeCycleStatus(graphqlAPIId, Constants.PUBLISHED);
        waitForAPIDeploymentSync(user.getUserName(), GRAPHQL_API_NAME, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);
        startWiremockServer();
    }

    @Test(groups = {"wso2.am"}, description = "Create and publish GraphQL APIs by providing GraphQL schema with " +
            "interface types")
    public void createAndPublishGraphQLAPIUsingSchemaWithInterfaces() throws Exception {

        String schemaDefinitionWithInterface = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream("graphql" + File.separator
                        + "schemaWithInterface.graphql"), "UTF-8");
        File file = getTempFileWithContent(schemaDefinitionWithInterface);
        GraphQLValidationResponseDTO responseApiDto = restAPIPublisher.validateGraphqlSchemaDefinition(file);
        GraphQLValidationResponseGraphQLInfoDTO graphQLInfo = responseApiDto.getGraphQLInfo();
        String arrayToJson = new ObjectMapper().writeValueAsString(graphQLInfo.getOperations());
        JSONArray operations = new JSONArray(arrayToJson);

        ArrayList<String> policies = new ArrayList<String>();
        policies.add("Unlimited");

        JSONObject additionalPropertiesObj = new JSONObject();
        additionalPropertiesObj.put("name", "GraphQLAPIWithInterface");
        additionalPropertiesObj.put("context", "interface");
        additionalPropertiesObj.put("version", API_VERSION_1_0_0);

        JSONObject url = new JSONObject();
        url.put("url", END_POINT_URL);
        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put("endpoint_type", "http");
        endpointConfig.put("sandbox_endpoints", url);
        endpointConfig.put("production_endpoints", url);
        additionalPropertiesObj.put("endpointConfig", endpointConfig);
        additionalPropertiesObj.put("policies", policies);
        additionalPropertiesObj.put("operations", operations);

        // create Graphql API
        APIDTO apidto = restAPIPublisher.importGraphqlSchemaDefinition(file, additionalPropertiesObj.toString());
        String graphqlAPIId = apidto.getId();
        HttpResponse createdApiResponse = restAPIPublisher.getAPI(graphqlAPIId);
        assertEquals(Response.Status.OK.getStatusCode(), createdApiResponse.getResponseCode(),
                GRAPHQL_API_NAME + " API creation is failed");

        // publish api
        restAPIPublisher.changeAPILifeCycleStatus(graphqlAPIId, Constants.PUBLISHED);
        waitForAPIDeploymentSync(user.getUserName(), GRAPHQL_API_NAME, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);
    }

    @Test(groups = { "wso2.am" }, description =
            "Attempt GraphQL API creation using a malformed context")
    public void testCreateAndPublishGraphQLAPIUsingSchemaWithMalformedContext() throws Exception {

        String schemaDefinitionWithInterface = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream("graphql" + File.separator
                        + "schemaWithInterface.graphql"), "UTF-8");
        File file = getTempFileWithContent(schemaDefinitionWithInterface);
        GraphQLValidationResponseDTO responseApiDto = restAPIPublisher.validateGraphqlSchemaDefinition(file);
        GraphQLValidationResponseGraphQLInfoDTO graphQLInfo = responseApiDto.getGraphQLInfo();
        String arrayToJson = new ObjectMapper().writeValueAsString(graphQLInfo.getOperations());
        JSONArray operations = new JSONArray(arrayToJson);
        HttpResponse response = null;

        ArrayList<String> policies = new ArrayList<String>();
        policies.add(APIMIntegrationConstants.API_TIER.UNLIMITED);

        JSONObject additionalPropertiesObj = new JSONObject();
        additionalPropertiesObj.put(APIMIntegrationConstants.API_NAME, "GraphQLAPIWithInvalidContext");
        additionalPropertiesObj.put(APIMIntegrationConstants.API_CONTEXT, "invalidContext{version}");
        additionalPropertiesObj.put(APIMIntegrationConstants.API_VERSION, API_VERSION_1_0_0);

        JSONObject url = new JSONObject();
        url.put("url", END_POINT_URL);
        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put(APIMIntegrationConstants.ENDPOINT_TYPE, "http");
        endpointConfig.put(APIMIntegrationConstants.SANDBOX_ENDPOINTS, url);
        endpointConfig.put(APIMIntegrationConstants.PRODUCTION_ENDPOINTS, url);
        additionalPropertiesObj.put(APIMIntegrationConstants.ENDPOINT_CONFIG, endpointConfig);
        additionalPropertiesObj.put(APIMIntegrationConstants.POLICIES, policies);
        additionalPropertiesObj.put(APIMIntegrationConstants.OPERATIONS, operations);

        // create Graphql API
        response = restAPIPublisher.importGraphqlSchemaDefinitionWithInvalidContext(file, additionalPropertiesObj.toString());
        Assert.assertNotNull(response, "Response cannot be null");
        Assert.assertEquals(response.getResponseCode(), 400, "Response Code miss matched when creating the API");
    }

    @Test(groups = { "wso2.am" }, description =
            "Test GraphQL API creation using SDL URL")
    public void testCreateGraphQLAPIUsingSDLURL() throws Exception {
        String graphQLUrl = endpointHost + ":" + endpointPort + mockgraphQLAPIEndpoint + mockgraphQLAPISDLResource;
        GraphQLValidationResponseDTO responseApiDto = restAPIPublisher.validateGraphqlSchemaDefinitionByURL(graphQLUrl,
            false);
        GraphQLValidationResponseGraphQLInfoDTO graphQLInfo = responseApiDto.getGraphQLInfo();
        String arrayToJson = new ObjectMapper().writeValueAsString(graphQLInfo.getOperations());
        JSONArray operations = new JSONArray(arrayToJson);

        ArrayList<String> environment = new ArrayList<String>();
        environment.add(Constants.GATEWAY_ENVIRONMENT);

        ArrayList<String> policies = new ArrayList<String>();
        policies.add("Unlimited");

        JSONObject additionalPropertiesObj = new JSONObject();
        additionalPropertiesObj.put(APIMIntegrationConstants.API_NAME, GRAPHQL_API_NAME + "withSDL");
        additionalPropertiesObj.put(APIMIntegrationConstants.API_CONTEXT, API_CONTEXT + "_sdl");
        additionalPropertiesObj.put(APIMIntegrationConstants.API_VERSION, API_VERSION_1_0_0);

        JSONObject url = new JSONObject();
        url.put(APIMIntegrationConstants.URL, END_POINT_URL);
        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put(APIMIntegrationConstants.ENDPOINT_TYPE, "http");
        endpointConfig.put(APIMIntegrationConstants.SANDBOX_ENDPOINTS, url);
        endpointConfig.put(APIMIntegrationConstants.PRODUCTION_ENDPOINTS, url);
        additionalPropertiesObj.put(APIMIntegrationConstants.ENDPOINT_CONFIG, endpointConfig);
        additionalPropertiesObj.put(APIMIntegrationConstants.POLICIES, policies);
        additionalPropertiesObj.put(APIMIntegrationConstants.OPERATIONS, operations);

        // create Graphql API
        APIDTO apidto = restAPIPublisher.importGraphqlSchemaDefinitionByURL(graphQLUrl,
            additionalPropertiesObj.toString());
        graphqlAPISDLId = apidto.getId();
        HttpResponse createdApiResponse = restAPIPublisher.getAPI(graphqlAPISDLId);
        assertEquals(Response.Status.OK.getStatusCode(), createdApiResponse.getResponseCode(),
                GRAPHQL_API_NAME + "withSDL" + " API creation is failed");
    }

    @Test(groups = { "wso2.am" }, description = "Test GraphQL API creation using Endpoint")
    public void testCreateGraphQLAPIUsingEndpoint() throws Exception {
        String graphQLEndpointURL = endpointHost + ":" + endpointPort
            + mockgraphQLAPIEndpoint + mockgraphQLAPIIntrospectionResource;
        GraphQLValidationResponseDTO responseApiDto = restAPIPublisher.validateGraphqlSchemaDefinitionByURL(
            graphQLEndpointURL, true);
        GraphQLValidationResponseGraphQLInfoDTO graphQLInfo = responseApiDto.getGraphQLInfo();
        String arrayToJson = new ObjectMapper().writeValueAsString(graphQLInfo.getOperations());
        JSONArray operations = new JSONArray(arrayToJson);

        ArrayList<String> environment = new ArrayList<String>();
        environment.add(Constants.GATEWAY_ENVIRONMENT);

        ArrayList<String> policies = new ArrayList<String>();
        policies.add("Unlimited");

        JSONObject additionalPropertiesObj = new JSONObject();
        additionalPropertiesObj.put(APIMIntegrationConstants.API_NAME, GRAPHQL_API_NAME + "withEndpoint");
        additionalPropertiesObj.put(APIMIntegrationConstants.API_CONTEXT, API_CONTEXT + "_endpoint");
        additionalPropertiesObj.put(APIMIntegrationConstants.API_VERSION, API_VERSION_1_0_0);

        JSONObject url = new JSONObject();
        url.put(APIMIntegrationConstants.URL, graphQLEndpointURL);
        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put(APIMIntegrationConstants.ENDPOINT_TYPE, "http");
        endpointConfig.put(APIMIntegrationConstants.SANDBOX_ENDPOINTS, url);
        endpointConfig.put(APIMIntegrationConstants.PRODUCTION_ENDPOINTS, url);
        additionalPropertiesObj.put(APIMIntegrationConstants.ENDPOINT_CONFIG, endpointConfig);
        additionalPropertiesObj.put(APIMIntegrationConstants.POLICIES, policies);
        additionalPropertiesObj.put(APIMIntegrationConstants.OPERATIONS, operations);

        // create Graphql API
        APIDTO apidto = restAPIPublisher.importGraphqlSchemaDefinitionByURL(null, additionalPropertiesObj.toString());
        graphqlAPIEndpointId = apidto.getId();
        HttpResponse createdApiResponse = restAPIPublisher.getAPI(graphqlAPIEndpointId);
        assertEquals(Response.Status.OK.getStatusCode(), createdApiResponse.getResponseCode(),
            GRAPHQL_API_NAME + "withEndpoint" + " API creation is failed");
    }

    @Test(groups = {"wso2.am"}, description = "test retrieve schemaDefinition at publisher")
    public void testRetrieveSchemaDefinitionAtPublisher() throws Exception {
        GraphQLSchemaDTO schema = restAPIPublisher.getGraphqlSchemaDefinition(graphqlAPIId);
        Assert.assertEquals(schema.getSchemaDefinition(), schemaDefinition);
    }

    @Test(groups = {"wso2.am"}, description = "test update schemaDefinition at publisher",
            dependsOnMethods = "testRetrieveSchemaDefinitionAtPublisher")
    public void testUpdateSchemaDefinitionOfAPI() throws Exception {
        //UpdatedSchema
        String updatedSchemaDefinition = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream("graphql" + File.separator + "updatedSchema.graphql"),
                "UTF-8");
        restAPIPublisher.updateGraphqlSchemaDefinition(graphqlAPIId, updatedSchemaDefinition);
        GraphQLSchemaDTO schema = restAPIPublisher.getGraphqlSchemaDefinition(graphqlAPIId);
        Assert.assertEquals(schema.getSchemaDefinition(), updatedSchemaDefinition);
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(graphqlAPIId, restAPIPublisher);
    }


    @Test(groups = {"wso2.am"}, description = "API invocation using JWT App")
    public void testInvokeGraphqlAPIUsingJWTApplication() throws Exception {
        String graphqlJwtAppName = "CountriesJWTAPP";
        testAppId1 = createGraphqlAppAndSubscribeToAPI(graphqlJwtAppName, "JWT");

        // generate token
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(testAppId1, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        String invokeURL = getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + "/";
        Map<String, String> requestHeaders = new HashMap<String, String>();
        JSONObject queryObject = new JSONObject();
        queryObject.put("query", "{languages{code name}}");
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        requestHeaders.put("Content-Type",  "application/json");
        HttpResponse serviceResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, queryObject.toString());

        Assert.assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK,
                "Response code is not as expected");
        Assert.assertEquals(serviceResponse.getData(), RESPONSE_DATA, "Response data is not as expected");

    }

    @Test(groups = {"wso2.am"}, description = "API invocation using oauth App")
    public void testInvokeGraphqlAPIUsingOAuthApplication() throws Exception {
        String graphqlOAUTHAppName = "CountriesOauthAPP";
        testAppId2 = createGraphqlAppAndSubscribeToAPI(graphqlOAUTHAppName, "JWT");

        // generate token
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(testAppId2, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();

        String invokeURL = getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + "/";
        Map<String, String> requestHeaders = new HashMap<String, String>();
        JSONObject queryObject = new JSONObject();
        queryObject.put("query", "{languages{code name}}");
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        requestHeaders.put("Content-Type",  "application/json");
        HttpResponse serviceResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, queryObject.toString());

        Assert.assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK,
                "Response code is not as expected");
        Assert.assertEquals(serviceResponse.getData(), RESPONSE_DATA, "Response data is not as expected");
    }

    @Test(groups = {"wso2.am"}, description = "Oauth Scopes",
            dependsOnMethods = { "testInvokeGraphqlAPIUsingOAuthApplication","testInvokeGraphqlAPIUsingJWTApplication"})
    public void testOperationalLevelOAuthScopesForGraphql() throws Exception {
        ArrayList role = new ArrayList();
        role.add(GRAPHQL_ROLE);

        ScopeDTO scopeObject = new ScopeDTO();
        scopeObject.setName("subscriber");
        scopeObject.setBindings(role);

        APIScopeDTO apiScopeDTO = new APIScopeDTO();
        apiScopeDTO.setScope(scopeObject);

        ArrayList apiScopeList = new ArrayList();
        apiScopeList.add(apiScopeDTO);

        HttpResponse createdApiResponse = restAPIPublisher.getAPI(graphqlAPIId);
        Gson g = new Gson();
        APIDTO apidto = g.fromJson(createdApiResponse.getData(), APIDTO.class);
        apidto.setScopes(apiScopeList);
        APIDTO updatedAPI = restAPIPublisher.updateAPI(apidto, graphqlAPIId);
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(graphqlAPIId, restAPIPublisher);

        ArrayList scope = new ArrayList();
        scope.add("subscriber");
        List<APIOperationsDTO> operations = updatedAPI.getOperations();
        operations.forEach((item) ->
                {
                    if (item.getTarget().equals("languages")) {
                        item.setScopes(scope);
                    }
                }
        );

        apidto.operations(operations);
        restAPIPublisher.updateAPI(apidto, graphqlAPIId);
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(graphqlAPIId, restAPIPublisher);

        testAppId3 = createGraphqlAppAndSubscribeToAPI("testOperationalLevelOAuthScopesForGraphql", "OAUTH");
        // Keep sufficient time to update map
        Thread.sleep(10000);
        waitForAPIDeploymentSync(apidto.getProvider(), apidto.getName(), apidto.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
        // generate token
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);

        JSONObject queryObject = new JSONObject();
        queryObject.put("query", "{languages{code name}}");
        String invokeURL = getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + "/";
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Content-Type", "application/json");

        // invoke api without authorized scope
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(testAppId3, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        String tokenJti = TokenUtils.getJtiOfJwtToken(accessToken);
        log.info("Access Token response without scope: " + accessToken);
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + tokenJti);
        HttpResponse serviceResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, queryObject.toString());
        Assert.assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_FORBIDDEN,
                "Response code is not as expected");

        String consumerKey = applicationKeyDTO.getConsumerKey();
        String consumerSecret = applicationKeyDTO.getConsumerSecret();
        URL tokenEndpointURL = new URL(keyManagerHTTPSURL + "oauth2/token");
        HttpResponse response;
        String requestBody;
        JSONObject accessTokenGenerationResponse;
        String username = GRAPHQL_TEST_USER;
        //Obtain user access token for Admin
        if (userMode != TestUserMode.SUPER_TENANT_ADMIN) {
            username = username.concat("@").concat(user.getUserDomain());
        }
        requestBody =
                "grant_type=password&username=" + username + "&password=" + GRAPHQL_TEST_USER_PASSWORD +
                        "&scope=subscriber";

        response = restAPIStore.generateUserAccessKey(consumerKey, consumerSecret, requestBody, tokenEndpointURL);
        accessTokenGenerationResponse = new JSONObject(response.getData());
        log.info("Access Token response with scope: " + response.getData());
        accessToken = accessTokenGenerationResponse.getString("access_token");
        tokenJti = TokenUtils.getJtiOfJwtToken(accessToken);
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + tokenJti);
        serviceResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, queryObject.toString());
        Assert.assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK,
                "Response code is not as expected");
        Assert.assertEquals(serviceResponse.getData(), RESPONSE_DATA, "Response data is not as expected");

    }

    @Test(groups = { "wso2.am" }, description = "Oauth Scopes", dependsOnMethods = {
            "testOperationalLevelOAuthScopesForGraphql" })
    public void testOperationalLevelSecurityForGraphql()
            throws Exception {
        HttpResponse response = restAPIPublisher.getAPI(graphqlAPIId);
        Gson g = new Gson();
        APIDTO apidto = g.fromJson(response.getData(), APIDTO.class);

        List<APIOperationsDTO> operations = apidto.getOperations();
        operations.forEach((item) ->
                {
                    if (item.getTarget().equals("languages")) {
                        item.setAuthType("None");
                    }
                }
        );

        apidto.operations(operations);
        restAPIPublisher.updateAPI(apidto, graphqlAPIId);
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(graphqlAPIId, restAPIPublisher);
        waitForAPIDeployment();

        testAppId4 = createGraphqlAppAndSubscribeToAPI(  "CountriesOauthAPPForSecurityCheck","OAUTH");
        // Keep sufficient time to update map
        Thread.sleep(10000);
        waitForAPIDeploymentSync(apidto.getProvider(), apidto.getName(), apidto.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
        // generate token
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);

        JSONObject queryObject = new JSONObject();
        queryObject.put("query", "{languages{code name}}");

        String invokeURL = getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + "/";
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Content-Type",  "application/json");

        // invoke api without security
        String accessToken = "";
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        boolean status = false;
        long waitTime = System.currentTimeMillis() + WAIT_TIME;
        HttpResponse serviceResponse = null;
        while (waitTime > System.currentTimeMillis()) {
            serviceResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, queryObject.toString());
            if (HttpStatus.SC_OK == serviceResponse.getResponseCode()) {
                status = true;
                break;
            } else {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                }
            }
        }
        Assert.assertTrue(status,
                "Response code is not as expected");
        Assert.assertEquals(serviceResponse.getData(), RESPONSE_DATA, "Response data is not as expected");
    }

    private String createGraphqlAppAndSubscribeToAPI(String appName, String tokenType) throws ApiException,
            APIManagerIntegrationTestException {

        ApplicationDTO applicationDTO = restAPIStore.addApplicationWithTokenType(appName,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "",
                "test app for countries API", tokenType);
        String testApiId = applicationDTO.getApplicationId();
        restAPIStore.subscribeToAPI(graphqlAPIId, testApiId, APIMIntegrationConstants.API_TIER.UNLIMITED);
        return testApiId;
    }

    private File getTempFileWithContent(String schema) throws Exception {
        File temp = File.createTempFile("schema", ".graphql");
        temp.deleteOnExit();
        BufferedWriter out = new BufferedWriter(new FileWriter(temp));
        out.write(schema);
        out.close();
        return temp;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

    private void startWiremockServer() throws Exception {

        if (wireMockServer == null) {
            String urlResponse;
            String introspectionResponse;

            ClassLoader classLoader = getClass().getClassLoader();
            urlResponse = new String(
                Files.readAllBytes(Paths.get(classLoader.getResource("graphql/schema.graphql").toURI())));
            introspectionResponse = new String(Files.readAllBytes(
                Paths.get(classLoader.getResource("graphql/schemaResponseFromIntrospection.json").toURI())));

            endpointPort = getAvailablePort();
            wireMockServer = new WireMockServer(options().port(endpointPort));

            // SDL URL
            wireMockServer.stubFor(WireMock.get(urlEqualTo(mockgraphQLAPIEndpoint + mockgraphQLAPISDLResource))
                .willReturn(
                    aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(urlResponse)));

            // Introspection endpoint
            wireMockServer.stubFor(
                WireMock.post(urlEqualTo(mockgraphQLAPIEndpoint + mockgraphQLAPIIntrospectionResource)).willReturn(
                    aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(introspectionResponse)));

            wireMockServer.start();
            log.info("Wiremock server started on port " + endpointPort);
        }
    }

    private void stopWireMockServer() throws Exception {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    /**
     * Find a free port to start backend WebSocket server in given port range
     *
     * @return Available Port Number
     */
    private int getAvailablePort() {

        while (lowerPortLimit < upperPortLimit) {
            if (isPortFree(lowerPortLimit)) {
                return lowerPortLimit;
            }
            lowerPortLimit++;
        }
        return -1;
    }

    /**
     * Check whether give port is available
     *
     * @param port Port Number
     * @return status
     */
    private boolean isPortFree(int port) {

        Socket s = null;
        try {
            s = new Socket(endpointHost, port);
            // something is using the port and has responded.
            return false;
        } catch (IOException e) {
            // port available
            return true;
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (IOException e) {
                    throw new RuntimeException("Unable to close connection ", e);
                }
            }
        }
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        userManagementClient.deleteRole(GRAPHQL_ROLE);
        userManagementClient.deleteUser(GRAPHQL_TEST_USER);
        restAPIStore.deleteApplication(testAppId1);
        restAPIStore.deleteApplication(testAppId2);
        restAPIStore.deleteApplication(testAppId3);
        restAPIStore.deleteApplication(testAppId4);
        undeployAndDeleteAPIRevisionsUsingRest(graphqlAPIId, restAPIPublisher);
        restAPIPublisher.deleteAPI(graphqlAPIId);
        restAPIPublisher.deleteAPI(graphqlAPISDLId);
        restAPIPublisher.deleteAPI(graphqlAPIEndpointId);
        stopWireMockServer();
        super.cleanUp();
    }
}
