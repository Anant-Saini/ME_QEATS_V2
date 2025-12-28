package com.crio.qeats.repositoryservices;

import java.io.IOException;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import redis.embedded.RedisServer;

@Component
public class EmbededRedis {

  @Value("${spring.redis.port}")
  private int redisPort;

  private RedisServer redisServer;

  @PostConstruct
  public void startRedis() throws IOException {
    System.out.println("🚀 Starting Embedded Redis on port: " + redisPort);
    redisServer = new RedisServer(redisPort);
    redisServer.start();
    System.out.println("✅ Redis Server Started Successfully!");
  }

  @PreDestroy
  public void stopRedis() {
    redisServer.stop();
  }
}
