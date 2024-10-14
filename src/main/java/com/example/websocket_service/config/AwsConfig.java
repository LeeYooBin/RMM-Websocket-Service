package com.example.websocket_service.config;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class AwsConfig {
  private static final String ENDPOINT_URL = "http://localhost:4566";
  private static final Region DEFAULT_REGION = Region.US_EAST_1;

  @Bean
  public SqsClient sqsClient() {
    return SqsClient.builder()
      .region(DEFAULT_REGION)
      .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
      .applyMutation(builder -> {
        builder.endpointOverride(URI.create(ENDPOINT_URL));
      })
      .build();
  }

  @Bean
  @Autowired
  public String commandQueueUrl(SqsClient sqsClient) {
    return sqsClient.getQueueUrl(builder -> {
      builder.queueName("command-queue");
    }).queueUrl();
  }

  @Bean
  @Autowired
  public String responseQueue(SqsClient sqsClient) {
    return sqsClient.getQueueUrl(builder -> {
      builder.queueName("response-queue");
    }).queueUrl();
  }
}