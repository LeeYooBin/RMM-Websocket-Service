package com.example.websocket_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ScheduledSqsConsumer {
  @Autowired
  private SqsService sqsService;

  @Scheduled(fixedRate = 1000)
  public void run() {
    try {
      sqsService.consumeCommandQueue();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

