package ru.redisMessenger.application.util;

import lombok.extern.log4j.Log4j2;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.Set;

/**
 * redis service
 */
@Log4j2
public class JedisClient {

    private Jedis jedis;

    private JedisClient(){
        String redisHost = Configuration.getInstance().getProperty(Configuration.Property.REDIS_HOST.getPropertyName());
        int redisPort = Integer.parseInt(Configuration.getInstance().getProperty(Configuration.Property.REDIS_PORT.getPropertyName()));
        try {
            jedis = new Jedis(redisHost, redisPort);
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
        }
    }

    private static volatile JedisClient instance;

    public static JedisClient getInstance(){
        if (instance == null)
            synchronized (JedisClient.class){
                instance = new JedisClient();
            }
         return instance;
    }

    /**
     * get value by key
     * @param key for value
     * @return value
     */
    public String getValue(String key){
        String value = jedis.get(key);
        if (value == null)
            log.info("KEY\nvalue for key {} doesn't exist", key);
        else
            log.info("KEY\nget value {} by key {}", value, key);
        return value;
    }

    /**
     * set value
     * @param key for value
     * @param value string for insert/update
     * @return status
     */
    public String setValue(String key, String value){
        String status = jedis.set(key, value);
        log.info("SET\nstatus of create value ({},{}) is {}", key, value, status);
        return status;
    }

    /**
     * get keys by pattern
     * @param pattern for search
     * @return keys
     */
    public Set<String> getKeys(String pattern){
        Set<String> keys = jedis.keys(pattern);
        log.info("KEYS\nfound {} keys:\n{}", keys.size(), keys);
        return  keys;
    }

    /**
     * get set of values
     * @param key for values
     * @return values
     */
    public Set<String> getValues(String key){
        Set<String> values = jedis.smembers(key);
        if (values == null)
            log.info("SMEMBERS\nvalue for key {} doesn't exist");
        else
            log.info("SMEMBERS\nget values {} by key {}", values, key);
        return values;
    }

    /**
     * put values
     * @param key for values
     * @param values in set
     * @return countInsertedValues
     */
    public Long putValues(String key, String[] values){
        Long countInsertedValues = jedis.sadd(key, values);
        log.info("SADD\n{} values added to key {}", countInsertedValues, key);
        return  countInsertedValues;
    }

    /**
     * create hash value
     * @param key hash name
     * @param field hash field
     * @param value for create
     */
    public Long createHashValue(String key, String field, String value){
        Long countInsertedValues = jedis.hset(key, field, value);
        log.info("HSET\n{} values added to field {} of key {}", countInsertedValues, field, key);
        return countInsertedValues;
    }

    /**
     * get hash value
     * @param key hash name
     * @param field hash field
     * @return hashValue
     */
    public String getHashValue(String key, String field){
        String hashValue = jedis.hget(key, field);
        log.info("HGET\nget hash value {} added by field {} of key {}", hashValue, field, key);
        return hashValue;
    }

    public Set<String> getHashKeys(String key){
        Set<String> keys = jedis.hkeys(key);
        log.info("HKEYS\nfound {} keys:\n{}", keys.size(), keys);
        return keys;
    }

    /**
     * publish messages
     * @param channel for publish
     * @param message for publish
     * @return countPublishedMessages
     */
    public Long publish(String channel, String message){
        Long countPublishedMessages = jedis.publish(channel, message);
        log.info("PUBLISH\n{} messages published to channel {}", countPublishedMessages, channel);
        return countPublishedMessages;
    }

    /**
     * subscribe to channel
     * @param channel for subscribe
     */
    public void subscribe(String channel){
        jedis.subscribe(new Subscriber(), channel);
    }

    private class Subscriber extends JedisPubSub {

        public void onMessage(String channel, String message) {
            log.info("SUBSCRIBE\nreceived a new message on channel {}:\n{}", channel, message);
        }

        public void onSubscribe(String channel, int subscribedChannels) {
            log.debug("SUBSCRIBE\nsubscribe to channel {}", channel);
        }

        public void onUnsubscribe(String channel, int subscribedChannels) {
            log.debug("SUBSCRIBE\nunsubscribe from channel {}", channel);
        }

        public void onPSubscribe(String pattern, int subscribedChannels) {
        }

        public void onPUnsubscribe(String pattern, int subscribedChannels) {
        }

        public void onPMessage(String pattern, String channel,
                               String message) {
        }
    }
}
