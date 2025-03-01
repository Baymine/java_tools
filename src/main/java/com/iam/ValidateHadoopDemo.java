package com.iam;


import com.rsa.conf.IamConfig;
import config.ConfigLoader;
import io.trino.jdbc.$internal.jackson.databind.JsonNode;
import io.trino.jdbc.$internal.jackson.databind.ObjectMapper;
import io.trino.jdbc.$internal.jackson.databind.node.ObjectNode;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;


public class ValidateHadoopDemo {

    private static final ConfigLoader configLoader = ConfigLoader.getInstance();
    private static final IamConfig iamConfig = configLoader.getIamConfig();

    private static String IAMEndpoint = iamConfig.getIAMEndpoint();
    private static String DEFAULT_SOURCE = iamConfig.getDefaultSource();
    private static String IAMToken = iamConfig.getIAMToken();
    private static final CloseableHttpClient httpClient = HttpClients.createDefault();
    private static final ThreadLocal<ObjectMapper> threadLocalObjectMapper = ThreadLocal.withInitial(
            () -> new ObjectMapper());

    public static ObjectMapper getObjectMapperInstance() {
        return threadLocalObjectMapper.get();
    }

    public static String getUserTokenFromIAMEndpoint(String erp, String hadoopUserName) {
        try {
            HttpPost post = new HttpPost(IAMEndpoint);
            post.setHeader("Content-Type", "application/json");
            post.setHeader("appId", DEFAULT_SOURCE);
            String time = String.valueOf(System.currentTimeMillis());
            String sign = DigestUtils.md5DigestAsHex((DEFAULT_SOURCE + IAMToken + time).getBytes(
                    StandardCharsets.UTF_8));
            post.setHeader("sign", sign);
            post.setHeader("time", time);

            ObjectNode json = getObjectMapperInstance().createObjectNode();
            json.put("proposer", erp);
            json.put("accountCode", hadoopUserName);
            json.put("appCode", DEFAULT_SOURCE);
            json.put("appName", DEFAULT_SOURCE + "平台");

            StringEntity entity = new StringEntity(json.toString());
            post.setEntity(entity);
            try (CloseableHttpResponse response = httpClient.execute(post)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String result = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

                if (statusCode == 200) {
                    JsonNode jsonNode = getObjectMapperInstance().readTree(result);
                    if (jsonNode.get("success").asBoolean()) {
                        return jsonNode.get("data").get("entity").asText();
                    } else {
                        System.out.println("IAM Error: {}" + jsonNode.get("message").asText());
                    }
                } else {
                    System.out.printf("HTTP Error: Status Code {}", statusCode);
                    System.out.printf("Response: {}", result);
                }
            }
        } catch (Exception e) {
            System.out.println("failed to get user token from iam endpoint" + e);
        }
        return null;
    }

    public static void main(String[] args) {
        String erp = iamConfig.getErp();
        String hadoopUserName = iamConfig.getHadoopUserName();

        System.out.println("The token for " + hadoopUserName +" is: " + getUserTokenFromIAMEndpoint(erp, hadoopUserName));
    }
}
