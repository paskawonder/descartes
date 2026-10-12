package com.descartes.config;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

public final class DynamoDbConfig {

    private static final String HARDCODED_REGION     = "eu-north-1";

    private DynamoDbConfig() {}

    public static DynamoDbClient dynamoDbClient() {
        DynamoDbClientBuilder builder = DynamoDbClient.builder();
        builder.region(Region.of(HARDCODED_REGION));
        return builder.build();
    }

    public static DynamoDbEnhancedClient enhancedClient(DynamoDbClient client) {
        return DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
    }

}