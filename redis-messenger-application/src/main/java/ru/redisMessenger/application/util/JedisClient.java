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

    /**
     * default constructor
     */
    private JedisClient(){
        String redisHost = Configuration.getInstance().getProperty(Configuration.Property.REDIS_HOST.getPropertyName());
        int redisPort = Integer.parseInt(Configuration.getInstance().getProperty(Configuration.Property.REDIS_PORT.getPropertyName()));
        try {
            jedis = new Jedis(redisHost, redisPort);
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
        }
    }

    private static JedisClient instance;

    /**
     * method for get instance of {@link JedisClient}
     * @return {@link JedisClient}
     */
    public static JedisClient getInstance(){
        if (instance == null)
            synchronized (JedisClient.class){
                instance = new JedisClient();
            }
         return instance;
    }

    /**
     * get value by key
     * @param key {@link String} for value
     * @return {@link String} value
     */
    public String getValue(String key){
        String value = jedis.get(key);
        if (value == null)
            log.debug("KEY\nvalue for key {} doesn't exist", key);
        else
            log.debug("KEY\nget value {} by key {}", value, key);
        return value;
    }

    /**
     * set value
     * @param key {@link String} for value
     * @param value {@link String} string for insert/update
     * @return {@link String} status
     */
    public String setValue(String key, String value){
        String status = jedis.set(key, value);
        log.debug("SET\nstatus of create value ({},{}) is {}", key, value, status);
        return status;
    }

    /**
     * delete by pattern
     * @param pattern {@link String} for delete values
     * @return int countDeletedValues
     */
    public int deleteValuesByPattern(String pattern){
        Set<String> keys = jedis.keys(pattern);
        for (String key : keys) {
            jedis.del(key);
        }
        int countDeletedValues = keys.size();
        log.debug("DEL\ndeleted {} values by pattern {}", countDeletedValues, pattern);
        return countDeletedValues;
    }

    /**
     * delete by key
     * @param key {@link String}
     * @return countDeletedValues {@link Long}
     */
    public Long deleteValuesByKey(String key){
        Long countDeletedValues = jedis.del(key);
        log.debug("DEL\ndeleted {} values by key {}", countDeletedValues, key);
        return countDeletedValues;
    }

    /**
     * get keys by pattern
     * @param pattern {@link String} for search
     * @return {@link Set<String>} keys
     */
    public Set<String> getKeys(String pattern){
        Set<String> keys = jedis.keys(pattern);
        log.debug("KEYS\nfound {} keys by pattern {}:\n{}", keys.size(), pattern, keys);
        return  keys;
    }

    /**
     * get set of values
     * @param key {@link String} for values
     * @return {@link Set<String>} values
     */
    public Set<String> getValues(String key){
        Set<String> values = jedis.smembers(key);
        if (values == null)
            log.debug("SMEMBERS\nvalue for key {} doesn't exist");
        else
            log.debug("SMEMBERS\nget values {} by key {}", values, key);
        return values;
    }

    /**
     * put values
     * @param key {@link String} for values
     * @param values {@link String[]}
     * @return {@link Long} countInsertedValues
     */
    public Long putValues(String key, String[] values){
        Long countInsertedValues = jedis.sadd(key, values);
        log.debug("SADD\n{} values added to key {}", countInsertedValues, key);
        return  countInsertedValues;
    }

    /**
     * create hash value
     * @param key {@link String} hash name
     * @param field {@link String} hash field
     * @param value {@link String} for create
     */
    public Long createHashValue(String key, String field, String value){
        Long countInsertedValues = jedis.hset(key, field, value);
        log.debug("HSET\n{} values {} added to field {} of key {}", value, countInsertedValues, field, key);
        return countInsertedValues;
    }

    /**
     * get hash value
     * @param key {@link String} hash name
     * @param field {@link String} hash field
     * @return hashValue
     */
    public String getHashValue(String key, String field){
        String hashValue = jedis.hget(key, field);
        log.debug("HGET\nget hash value {} added by field {} of key {}", hashValue, field, key);
        return hashValue;
    }

    /**
     * get hash keys
     * @param key {@link String}
     * @return {@link Set<String>}
     */
    public Set<String> getHashKeys(String key){
        Set<String> keys = jedis.hkeys(key);
        log.debug("HKEYS\nfound {} keys:\n{}", keys.size(), keys);
        return keys;
    }

    /**
     * publish messages
     * @param channel {@link String} for publish
     * @param message {@link String} for publish
     * @return countPublishedMessages
     */
    public Long publish(String channel, String message){
        Long countPublishedMessages = jedis.publish(channel, message);
        log.debug("PUBLISH\n{} messages published to channel {}", countPublishedMessages, channel);
        return countPublishedMessages;
    }

    /**
     * subscribe to channel
     * @param channel {@link String} for subscribe
     */
    public void subscribe(String channel){
        jedis.subscribe(new Subscriber(), channel);
    }

    private class Subscriber extends JedisPubSub {

        public void onMessage(String channel, String message) {
            log.debug("SUBSCRIBE\nreceived a new message on channel {}:\n{}", channel, message);
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
