package com.example.websocket_service.service;

import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.example.websocket_service.model.Agent;
import com.example.websocket_service.model.Status;
import com.example.websocket_service.repository.AgentRepository;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WebSocketService extends TextWebSocketHandler {
  private static final Logger logger = LoggerFactory.getLogger(WebSocketService.class);

  private CompletableFuture<String> response = new CompletableFuture<>();
  private Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

  @Autowired
  private AgentRepository agentRepository;

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    InetSocketAddress remoteAddress = session.getRemoteAddress();
    String agentIP = remoteAddress.getAddress().getHostAddress();

    Optional<Agent> optAgent = agentRepository.findByIpAddress(agentIP);
    
    if (optAgent.isPresent()) {
      Agent agent = optAgent.get();
      agent.setStatus(Status.ACTIVE);
      agentRepository.save(agent);
      logger.info("Agent already registered: " + agent);
      this.resetResponse();
      sessions.put(agent.getId(), session);
    } else {
      session.sendMessage(new TextMessage("username request"));
    }
  }

  @Override
  public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    InetSocketAddress remoteAddress = session.getRemoteAddress();
    String agentIP = remoteAddress.getAddress().getHostAddress();
    String msg = message.getPayload();

    if (!agentRepository.existsByIpAddress(agentIP)) {
      Agent agent = new Agent();
      agent.setUsername(msg);
      agent.setIpAddress(agentIP);
      agent.setStatus(Status.ACTIVE);
      agent.setCreatedAt(LocalDateTime.now());

      agentRepository.save(agent);
      logger.info("Agent successfully registered: " + agent);

      sessions.put(agent.getId(), session);
      session.sendMessage(new TextMessage("You are connected"));
    } else {
      response.complete(msg);
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    InetSocketAddress remoteAddress = session.getRemoteAddress();
    String agentIP = remoteAddress.getAddress().getHostAddress();

    Optional<Agent> optAgent = agentRepository.findByIpAddress(agentIP);

    if (optAgent.isPresent()) {
      Agent agent = optAgent.get();
      agent.setStatus(Status.INACTIVE);
      agentRepository.save(agent);
    }

    sessions.values().remove(session);
    logger.info("Session closed: " + session.getId());
  }

  public void sendCommandToAgent(String agentId, String message) throws Exception {
    WebSocketSession session = sessions.get(agentId);
    if (session != null && session.isOpen()) {
      session.sendMessage(new TextMessage(message));
      logger.info("Command sent to agent: " + agentId);
    } else {
      logger.error("Agent with ID " + agentId + " is not connected");
      throw new Error("Agent with ID " + agentId + " is not connected");
    }
  }

  // public String getResponse() throws InterruptedException, ExecutionException {
  //   try {
  //     return response.get(1, TimeUnit.HOURS);
  //   } catch (TimeoutException e) {
  //     logger.error("Timeout while waiting for the agent's response");
  //     return "Timeout: no response from agent";
  //   }
  // }

  public String getResponse() throws InterruptedException, ExecutionException {
    return response.get();
  }

  public void resetResponse() {
    response = new CompletableFuture<>();
  }
}