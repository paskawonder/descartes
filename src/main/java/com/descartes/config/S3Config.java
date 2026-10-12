package com.descartes.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

    private static final String HARDCODED_BUCKET = "pavle-heap-12345672395";

    public record S3Settings(String bucket, String region) {}

    @Bean(destroyMethod = "close")
    public S3Presigner s3Presigner(DynamoDbClient dynamoDb) {
        var cfg = dynamoDb.serviceClientConfiguration();
        return S3Presigner.builder().region(cfg.region()).credentialsProvider(cfg.credentialsProvider()).build();
    }

    @Bean
    public S3Settings s3Settings(DynamoDbClient dynamoDb) {
        return new S3Settings(HARDCODED_BUCKET, dynamoDb.serviceClientConfiguration().region().id());
    }

}