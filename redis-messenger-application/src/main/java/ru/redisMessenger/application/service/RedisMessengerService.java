package ru.redisMessenger.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import lombok.extern.log4j.Log4j2;
import ru.redisMessenger.application.exception.RedisMessengerException;
import ru.redisMessenger.application.util.Configuration;
import ru.redisMessenger.application.util.JedisClient;
import ru.redisMessenger.core.entities.Message;
import ru.redisMessenger.core.entities.User;
import ru.redisMessenger.core.util.JacksonHelper;

import java.io.IOException;
import java.util.Calendar;
import java.util.Set;

/**
 * business logic
 */
@Log4j2
public class RedisMessengerService {

    private static final String REDIS_KEY_MESSAGES_PREFIX_PROPERTY = Configuration.getInstance().getProperty(Configuration.Property.REDIS_KEY_MESSAGES_PREFIX.getPropertyName());
    private static final String REDIS_KEY_USER_PREFIX_PROPERTY = Configuration.getInstance().getProperty(Configuration.Property.REDIS_KEY_USER_PREFIX.getPropertyName());
    private static final String REDIS_KEY_USERS_PROPERTY = Configuration.getInstance().getProperty(Configuration.Property.REDIS_KEY_USERS.getPropertyName());
    private static final String REDIS_CHANNEL_CHAT_PREFIX_PROPERTY = Configuration.getInstance().getProperty(Configuration.Property.REDIS_CHANNEL_CHAT_PREFIX.getPropertyName());
    private static final String REDIS_HASH_PREFIX_PROPERTY = Configuration.getInstance().getProperty(Configuration.Property.REDIS_HASH_PREFIX.getPropertyName());

    private static final String[] MESSAGE_IGNORABLE_FILTER = new String[]{"messages"};
    private static final String[] USER_DETAILS_IGNORABLE_FILTER = new String[]{"messages", "description", "dateCreate", "rights"};

    private static final FilterProvider USER_ONLY_FILTER_PROVIDER = new SimpleFilterProvider()
            .addFilter("User", SimpleBeanPropertyFilter.serializeAllExcept(MESSAGE_IGNORABLE_FILTER));
    private static final FilterProvider MESSAGES_FILTER_PROVIDER = new SimpleFilterProvider()
            .addFilter("User", SimpleBeanPropertyFilter.serializeAllExcept(USER_DETAILS_IGNORABLE_FILTER))
            .addFilter("Message", SimpleBeanPropertyFilter.serializeAll());

    /**
     * get user by name and class
     * @param userKey {@link String} userKey({@link String} user)
     * @return {@link User} as {@link String}
     * @throws RedisMessengerException when user doesn't exist
     */
    public User getUser(String userKey) throws RedisMessengerException, IOException {
        String userValue = JedisClient.getInstance().getValue(userKey);
        if (userValue == null)
            throw new RedisMessengerException("user with userKey ".concat(userKey).concat(" doesn't exist"));
        return new JacksonHelper<User>(USER_ONLY_FILTER_PROVIDER).getDeserializedObject(userValue, User.class);
    }

    /**
     * add new {@link User}
     * @param user {@link User} created user
     * @return userKey {@link String} userKey({@link String} user)
     * @throws RedisMessengerException when {@link User} is existing or incorrect
     */
    public String addUser(User user) throws RedisMessengerException {
        if (user.getName() == null)
            throw new RedisMessengerException("username must be filled");
        String userKey = userKey(user);
        if (JedisClient.getInstance().getValue(userKey) != null)
            throw new RedisMessengerException("user with userKey ".concat(userKey).concat(" already exists"));
        String userStr = null;
        try {
            user.setDateCreate(Calendar.getInstance().getTime());
            userStr = new JacksonHelper<User>(USER_ONLY_FILTER_PROVIDER).getSerializedObject(user);
        } catch (JsonProcessingException e) {
            log.error("error while adding user {}:\n{}", user, e.getLocalizedMessage());
        }
        JedisClient.getInstance().setValue(userKey, userStr);
        JedisClient.getInstance().putValues(REDIS_KEY_USERS_PROPERTY, new String[]{userKey});
        return userStr;
    }

    /**
     * create new {@link Message} and send it to {@link User}
     * @param message {@link Message}
     * @return {@link Message} as {@link String}
     * @throws RedisMessengerException when target {@link User} doesn't exist or {@link Message} is incorrect
     */
    public String sendMessage(Message message) throws RedisMessengerException {
        if (message.getText() == null || message.getTo() == null)
            throw new RedisMessengerException("all fields must be filled");
        User userTo = message.getTo();
        User userFrom = message.getFrom();
        message.setDateCreate(Calendar.getInstance().getTime());
        String userToKey = userKey(userTo);
        String userToValue = JedisClient.getInstance().getValue(userToKey);
        if (userToValue == null)
            throw new RedisMessengerException("user with userKey ".concat(userKey(userTo)).concat(" doesn't exist"));
        String messageValue = null;
        try {
            messageValue = new JacksonHelper<Message>(MESSAGES_FILTER_PROVIDER).getSerializedObject(message);
        } catch (JsonProcessingException e) {
            log.error("error while sending message {}:\n{}", message, e.getLocalizedMessage());
        }
        String userFromKey = userKey(userFrom);

        String hashUserFromKey = hashUserKey(userFromKey);
        String hashUserToKey = hashUserKey(userToKey);
        String messagesUserFromKey = messagesUserKey(userFromKey);
        String messagesUserToKey = messagesUserKey(userToKey);

        String messagesUsersFromToKey = messagesUsersKey(userFromKey, userToKey);

        String userFromToKey = JedisClient.getInstance().getHashValue(hashUserFromKey, messagesUserToKey);
        if (userFromToKey != null)
            JedisClient.getInstance().putValues(userFromToKey, new String[]{messageValue});
        else {
            String userToFromKey = JedisClient.getInstance().getHashValue(hashUserToKey, messagesUserFromKey);
            if (userToFromKey != null)
                JedisClient.getInstance().putValues(userToFromKey, new String[]{messageValue});
            else {
                JedisClient.getInstance().createHashValue(hashUserFromKey, messagesUserToKey, messagesUsersFromToKey);
                JedisClient.getInstance().createHashValue(hashUserToKey, messagesUserFromKey, messagesUsersFromToKey);
                JedisClient.getInstance().putValues(messagesUsersFromToKey, new String[]{messageValue});
            }
        }

        String chatUserFromChannel = chatUserChannel(userFromKey);
        String chatUserToChannel = chatUserChannel(userToKey);

        String chatUsersFromToChannel = chatUsersChannel(userFromKey, userToKey);

        userFromToKey = JedisClient.getInstance().getHashValue(hashUserFromKey, chatUserToChannel);
        if (userFromToKey != null)
            JedisClient.getInstance().publish(userFromToKey, messageValue);
        else {
            String userToFromKey = JedisClient.getInstance().getHashValue(hashUserToKey, chatUserFromChannel);
            if (userToFromKey != null)
                JedisClient.getInstance().publish(userToFromKey, messageValue);
            else {
                JedisClient.getInstance().createHashValue(hashUserFromKey, chatUserToChannel, chatUsersFromToChannel);
                JedisClient.getInstance().createHashValue(hashUserToKey, chatUserFromChannel, chatUsersFromToChannel);
                JedisClient.getInstance().publish(chatUsersFromToChannel, messageValue);
            }
        }
        return messageValue;
    }

    /**
     * get all messages before two users
     * @param userFrom {@link User}
     * @param userTo {@link User}
     * @return {@link Set<String>}
     */
    public Set<String> getMessages(User userFrom, User userTo) {
        String userFromKey = userKey(userFrom);
        String userToKey = userKey(userTo);

        String messagesUserFromKey = messagesUserKey(userFromKey);
        String messagesUserToKey = messagesUserKey(userToKey);

        String hashUserFromKey = hashUserKey(userFromKey);
        String hashUserToKey = hashUserKey(userToKey);

        String messagesUsersFromToKey = messagesUsersKey(userFromKey, userToKey);

        String userFromToKey = JedisClient.getInstance().getHashValue(hashUserFromKey, messagesUserToKey);
        if (userFromToKey != null)
            return JedisClient.getInstance().getValues(userFromToKey);
        else {
            String userToFromKey = JedisClient.getInstance().getHashValue(hashUserToKey, messagesUserFromKey);
            if (userToFromKey != null)
                return JedisClient.getInstance().getValues(userToFromKey);
            else {
                JedisClient.getInstance().createHashValue(hashUserFromKey, messagesUserToKey, messagesUsersFromToKey);
                JedisClient.getInstance().createHashValue(hashUserToKey, messagesUserFromKey, messagesUsersFromToKey);
                return JedisClient.getInstance().getValues(messagesUsersFromToKey);
            }
        }
    }

    /**
     * subscribe to channel
     * @param userFrom {@link User}
     * @param userTo {@link User}
     */
    public void subscribe(User userFrom, User userTo) {
        String userFromKey = userKey(userFrom);
        String userToKey = userKey(userTo);

        String chatUserFromChannel = chatUserChannel(userFromKey);
        String chatUserToChannel = chatUserChannel(userToKey);

        String hashUserFromKey = hashUserKey(userFromKey);
        String hashUserToKey = hashUserKey(userToKey);

        String chatUsersFromToChannel = chatUsersChannel(userFromKey, userToKey);

        String userFromToKey = JedisClient.getInstance().getHashValue(hashUserFromKey, chatUserToChannel);
        if (userFromToKey != null)
            JedisClient.getInstance().subscribe(userFromToKey);
        else {
            String userToFromKey = JedisClient.getInstance().getHashValue(hashUserToKey, chatUserFromChannel);
            if (userToFromKey != null)
                JedisClient.getInstance().subscribe(userToFromKey);
            else {
                JedisClient.getInstance().createHashValue(hashUserFromKey, chatUserToChannel, chatUsersFromToChannel);
                JedisClient.getInstance().createHashValue(hashUserToKey, chatUserFromChannel, chatUsersFromToChannel);
                JedisClient.getInstance().subscribe(chatUsersFromToChannel);
            }
        }
    }

    /**
     * get keys of users
     * @return {@link Set<String>}
     */
    public Set<String> getUsersKeys() {
        return JedisClient.getInstance().getValues(REDIS_KEY_USERS_PROPERTY);
    }

    /**
     * redis key
     * @param userFromKey {@link String} current user
     * @param userToKey {@link String} contact
     * @return {@link String}
     */
    private String messagesUsersKey(String userFromKey, String userToKey) {
        return String.join(":", REDIS_KEY_MESSAGES_PREFIX_PROPERTY, userFromKey, userToKey);
    }

    /**
     * redis key
     * @param userToKey {@link String} contact
     * @return {@link String}
     */
    private String messagesUserKey(String userToKey) {
        return String.join(":", REDIS_KEY_MESSAGES_PREFIX_PROPERTY, userToKey);
    }

    /**
     * redis key
     * @param userFromKey {@link String} current user
     * @return {@link String}
     */
    private String hashUserKey(String userFromKey) {
        return String.join(":", REDIS_HASH_PREFIX_PROPERTY, userFromKey);
    }

    /**
     * redis key
     * @param user {@link String} any user
     * @return {@link String}
     */
    private String userKey(User user) {
        return String.join(":", REDIS_KEY_USER_PREFIX_PROPERTY, user.getClass().getSimpleName(), user.getName());
    }

    /**
     * redis key
     * @param userFromKey {@link String} current user
     * @param userToKey {@link String} contact
     * @return {@link String}
     */
    private String chatUsersChannel(String userFromKey, String userToKey) {
        return String.join(":", REDIS_CHANNEL_CHAT_PREFIX_PROPERTY, userFromKey, userToKey);
    }

    /**
     * redis key
     * @param userToKey {@link String} contact
     * @return {@link String}
     */
    private String chatUserChannel(String userToKey) {
        return String.join(":", REDIS_CHANNEL_CHAT_PREFIX_PROPERTY, userToKey);
    }

}
