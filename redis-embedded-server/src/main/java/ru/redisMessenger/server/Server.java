package ru.redisMessenger.server;

import redis.embedded.RedisServer;

import java.io.IOException;

/**
 * embedded redis server
 */
public class Server {

    public static void main(String[] arr){
        try {
            RedisServer redisServer = new RedisServer().builder().port(6379).build();
            redisServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
