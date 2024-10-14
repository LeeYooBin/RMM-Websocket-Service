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
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WebSocketService extends TextWebSocketHandler {
  private static final Logger logger = LoggerFactory.getLogger(WebSocketService.class);

  private String response;
  private Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

  @Autowired
  private AgentRepository agentRepository;

  public String getResponse() {
    return response;
  }

  public void setResponse(String response) {
    this.response = response;
  }

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
      logger.info(msg);
      setResponse(msg);
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
}