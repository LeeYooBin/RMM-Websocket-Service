package com.example.websocket_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.websocket_service.model.Agent;
import java.util.Optional;


public interface AgentRepository extends JpaRepository<Agent, String> {
  boolean existsByIpAddress(String ipAddress);
  Optional<Agent> findByIpAddress(String ipAddress);
}
