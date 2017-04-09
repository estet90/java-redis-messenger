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

    public static final FilterProvider USER_ONLY_FILTER_PROVIDER = new SimpleFilterProvider()
            .addFilter("User", SimpleBeanPropertyFilter.serializeAllExcept(MESSAGE_IGNORABLE_FILTER));
    private static final FilterProvider MESSAGES_FILTER_PROVIDER = new SimpleFilterProvider()
            .addFilter("User", SimpleBeanPropertyFilter.serializeAllExcept(USER_DETAILS_IGNORABLE_FILTER))
            .addFilter("Message", SimpleBeanPropertyFilter.serializeAll());

    /**
     * get user by name and class
     * @param userKey {@link String} "user:{@link User}.getClass.getSimpleName:{@link User}.getName"
     * @return {@link User} as {@link String}
     * @throws RedisMessengerException when user doesn't exist
     */
    public String getUser(String userKey) throws RedisMessengerException {
        String userValue = JedisClient.getInstance().getValue(userKey);
        if (userValue == null)
            throw new RedisMessengerException("user with userKey ".concat(userKey).concat(" doesn't exist"));
        return userValue;
    }

    /**
     * add new {@link User}
     * @param user {@link User}
     * @return userKey "user:{@link User}.getClass.getSimpleName:{@link User}.getName"
     * @throws RedisMessengerException when {@link User} is existing or incorrect
     */
    public String addUser(User user) throws RedisMessengerException {
        if (user.getName() == null || user.getDescription() == null)
            throw new RedisMessengerException("all fields must be filled");
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

        String messagesUsersKey = messagesUsersKey(userFromKey, userToKey);
        checkHashKeys(hashUserFromKey, hashUserToKey, messagesUserFromKey, messagesUserToKey, messagesUsersKey);
        JedisClient.getInstance().putValues(JedisClient.getInstance().getHashValue(hashUserFromKey, messagesUserToKey), new String[]{messageValue});

        String chatUserFromChannel = chatUserChannel(userFromKey);
        String chatUserToChannel = chatUserChannel(userToKey);

        String chatUsersChannel = chatUsersChannel(userFromKey, userToKey);
        checkHashKeys(hashUserFromKey, hashUserToKey, chatUserFromChannel, chatUserToChannel, chatUsersChannel);
        JedisClient.getInstance().publish(JedisClient.getInstance().getHashValue(hashUserFromKey, chatUserToChannel), messageValue);
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
        String messagesUsersKey = messagesUsersKey(userFromKey, userToKey);
        checkHashKeys(hashUserKey(userFromKey), hashUserKey(userToKey), messagesUserKey(userFromKey), messagesUserKey(userToKey), messagesUsersKey);
        return JedisClient.getInstance().getValues(messagesUsersKey);
    }

    /**
     * subscribe to channel
     * @param userFrom {@link User}
     * @param userTo {@link User}
     */
    public void subscribe(User userFrom, User userTo) {
        String userFromKey = userKey(userFrom);
        String userToKey = userKey(userTo);
        String chatUsersChannel = chatUsersChannel(userFromKey, userToKey);
        checkHashKeys(hashUserKey(userFromKey), hashUserKey(userToKey), chatUserChannel(userFromKey), chatUserChannel(userToKey), chatUsersChannel);
        JedisClient.getInstance().subscribe(chatUsersChannel);
    }

    /**
     * get all {@link User} by key "{@link Configuration}.getProperty()"
     * @return
     */
    public Set<String> getUsers() {
        return JedisClient.getInstance().getValues(REDIS_KEY_USERS_PROPERTY);
    }

    /**
     *
     * @param hashUserFromKey
     * @param hashUserToKey
     * @param actionUserFromKey
     * @param actionUserToKey
     * @param hashValue
     */
    private void checkHashKeys(String hashUserFromKey, String hashUserToKey, String actionUserFromKey, String actionUserToKey, String hashValue) {
        if (!JedisClient.getInstance().getHashKeys(hashUserFromKey).contains(actionUserToKey))
            JedisClient.getInstance().createHashValue(hashUserFromKey, actionUserToKey, hashValue);
        if (!JedisClient.getInstance().getHashKeys(hashUserToKey).contains(actionUserFromKey))
            JedisClient.getInstance().createHashValue(hashUserToKey, actionUserFromKey, hashValue);
    }

    private String messagesUsersKey(String userFromKey, String userToKey) {
        return REDIS_KEY_MESSAGES_PREFIX_PROPERTY.concat(userFromKey).concat(":").concat(userToKey);
    }

    private String messagesUserKey(String userToKey) {
        return REDIS_KEY_MESSAGES_PREFIX_PROPERTY.concat(userToKey);
    }

    private String hashUserKey(String userFromKey) {
        return REDIS_HASH_PREFIX_PROPERTY.concat(userFromKey);
    }

    private String userKey(User user) {
        return REDIS_KEY_USER_PREFIX_PROPERTY.concat(user.getClass().getSimpleName()).concat(":").concat(user.getName());
    }

    private String chatUsersChannel(String userFromKey, String userToKey) {
        return REDIS_CHANNEL_CHAT_PREFIX_PROPERTY.concat(userFromKey).concat(":").concat(userToKey);
    }

    private String chatUserChannel(String userToKey) {
        return REDIS_CHANNEL_CHAT_PREFIX_PROPERTY.concat(userToKey);
    }
    //        if (JedisClient.getInstance().getHashKeys(hashUserFromKey).contains(messagesUserToKey))
//            JedisClient.getInstance().putValues(JedisClient.getInstance().getHashValue(hashUserFromKey, messagesUserToKey), new String[]{messageValue});
//        else {
//            JedisClient.getInstance().createHashValue(hashUserFromKey, messagesUserToKey, messagesUsersKey(userFromKey, userToKey));
//            if (!JedisClient.getInstance().getHashKeys(hashUserToKey).contains(messagesUserFromKey))
//                JedisClient.getInstance().createHashValue(hashUserToKey, messagesUserFromKey, messagesUsersKey(userFromKey, userToKey));
//            JedisClient.getInstance().putValues(messagesUsersKey(userFromKey, userToKey), new String[]{messageValue});
//        }
//        //add message to publish
//        if (JedisClient.getInstance().getHashKeys(hashUserFromKey).contains(chatUserToChannel))
//            JedisClient.getInstance().publish(JedisClient.getInstance().getHashValue(hashUserFromKey, chatUserToChannel), messageValue);
//        else {
//            JedisClient.getInstance().createHashValue(hashUserFromKey, chatUserToChannel, chatUsersChannel(userFromKey, userToKey));
//            if (!JedisClient.getInstance().getHashKeys(hashUserToKey).contains(chatUserFromChannel))
//                JedisClient.getInstance().createHashValue(hashUserToKey, chatUserFromChannel, chatUsersChannel(userFromKey, userToKey));
//            JedisClient.getInstance().publish(JedisClient.getInstance().getHashValue(hashUserFromKey, chatUserToChannel), messageValue);
//        }

}
