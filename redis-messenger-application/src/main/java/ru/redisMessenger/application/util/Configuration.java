package ru.redisMessenger.application.util;

import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * get properties from config.properties
 */
@Log4j2
public class Configuration {

    private Properties properties;
    private static final String PROPERTIES_FILE_NAME = "config.properties";

    /**
     * private constructor
     */
    private Configuration(){
        properties = new Properties();
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE_NAME);
        try {
            properties.load(inputStream);
        } catch (IOException | NullPointerException e) {
            log.error(e.getLocalizedMessage());
        }
    }

    private static Configuration instance;

    /**
     * method for get instance of {@link Configuration}
     * @return {@link Configuration} current instance of Configuration
     */
    public static Configuration getInstance() {
        if (instance == null)
            synchronized (Configuration.class){
                instance = new Configuration();
            }
        return instance;
    }


    /**
     * get property
     * @param propertyName {@link String} name of property
     * @return {@link String} value of property
     */
    public String getProperty(String propertyName){
        return properties.get(propertyName).toString();
    }

    /**
     * enum with properties
     */
    public enum Property{
        REDIS_HOST("redis.host"), REDIS_PORT("redis.port"), FILE_OUTPUT_DIRECTORY("file.output.directory"), REDIS_KEY_USERS("redis.key.users"),
        REDIS_KEY_MESSAGES_PREFIX("redis.key.messages.prefix"), REDIS_KEY_USER_PREFIX("redis.key.user.prefix"), REDIS_CHANNEL_CHAT_PREFIX("redis.channel.chat.prefix"),
        REDIS_HASH_PREFIX("redis.hash.prefix");

        private String propertyName;


        /**
         * get property name
         * @return {@link String} propertyName
         */
        public String getPropertyName(){
            return this.propertyName;
        }

        /**
         * default constructor
         * @param propertyName {@link String}
         */
        Property(String propertyName){
            this.propertyName = propertyName;
        }
    }

}
