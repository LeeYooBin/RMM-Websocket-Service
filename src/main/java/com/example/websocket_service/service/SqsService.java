package com.example.websocket_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;

@Service
public class SqsService {
  private static final Logger logger = LoggerFactory.getLogger(SqsService.class);

  @Autowired
  private SqsClient sqsClient;

  @Autowired
  private WebSocketService webSocketService;

  private final String commandQueueUrl = "https://localhost.localstack.cloud:4566/000000000000/command-queue";
  private final String responseQueueUrl = "https://localhost.localstack.cloud:4566/000000000000/response-queue";

  public void consumeCommandQueue() throws Exception {
    try {
      ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
        .queueUrl(commandQueueUrl)
        .maxNumberOfMessages(10)
        .build();

      List<Message> messages = sqsClient.receiveMessage(receiveRequest).messages();

      if (messages.isEmpty()) {
        return;
      }

      for (Message message : messages) {
        logger.info("Message received: " + message.body());

        String body = message.body();
        String agentId = extractMachineId(body);
        String action = extractAction(body);

        webSocketService.resetResponse();

        webSocketService.sendCommandToAgent(agentId, action);
        String result = webSocketService.getResponse();

        sendResponseToQueue(agentId, result);

        deleteMessageFromCommandQueue(message);
      }
    } catch (SqsException e) {
      logger.error("Error consuming command queue: " + e.getMessage());
    }
  }

  public void sendResponseToQueue(String agentId, String result) {
    try {
      SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
        .queueUrl(responseQueueUrl)
        .messageBody("{\"agentId\":\"" + agentId + "\", \"result\":\"" + result + "\"}")
        .build();

      sqsClient.sendMessage(sendMessageRequest);
      logger.info("Response sent to response queue: " + result);
    } catch (SqsException e) {
      logger.error("Error sending response to queue: " + e.getMessage());
    }
  }

  private void deleteMessageFromCommandQueue(Message message) {
    DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
      .queueUrl(commandQueueUrl)
      .receiptHandle(message.receiptHandle())
      .build();
    
    sqsClient.deleteMessage(deleteRequest);
    logger.info("Message deleted");
  }

  private String extractMachineId(String messageBody) {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      JsonNode jsonNode = objectMapper.readTree(messageBody);
      return jsonNode.get("machine_id").asText();
    } catch (Exception e) {
      throw new RuntimeException("Error extracting machine_id", e);
    }
  }

  private String extractAction(String messageBody) {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      JsonNode jsonNode = objectMapper.readTree(messageBody);
      return jsonNode.get("action").asText();
    } catch (Exception e) {
      throw new RuntimeException("Error extracting action", e);
    }
  }
}
