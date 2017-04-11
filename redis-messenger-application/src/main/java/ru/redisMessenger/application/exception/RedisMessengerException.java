package ru.redisMessenger.application.exception;

/**
 * custom exception class
 */
public class RedisMessengerException extends Exception {

    /**
     * constructor
     * @param errorMessage {@link String}
     */
    public RedisMessengerException(String errorMessage) {
        super(errorMessage);
    }
}
