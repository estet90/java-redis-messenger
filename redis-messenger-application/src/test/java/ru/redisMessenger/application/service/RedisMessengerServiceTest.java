package ru.redisMessenger.application.service;

import lombok.extern.log4j.Log4j2;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.redisMessenger.application.exception.RedisMessengerException;
import ru.redisMessenger.application.util.JedisClient;
import ru.redisMessenger.core.entities.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * tests for {@link RedisMessengerService}
 */
@Log4j2
public class RedisMessengerServiceTest {

    private RedisMessengerService service;

    @Before
    public void setUp() throws Exception {
        JedisClient.getInstance().deleteValuesByPattern("user*");
        JedisClient.getInstance().deleteValuesByPattern("message*");
        JedisClient.getInstance().deleteValuesByPattern("chat*");

        service = new RedisMessengerService();

        User superUser = new SuperUser();
        superUser.setName("superUser");
        service.addUser(superUser);

        User advancedUser = new AdvancedUser();
        advancedUser.setName("advancedUser");
        service.addUser(advancedUser);
    }

    @After
    public void tearDown() throws Exception {
        JedisClient.getInstance().deleteValuesByPattern("user*");
        JedisClient.getInstance().deleteValuesByPattern("message*");
        JedisClient.getInstance().deleteValuesByPattern("chat*");
    }

    @Test
    //предполагается, что префиксы для ключей не должны меняться. в противном случае этот тест не будет проходить
    public void keyConstructor(){
        User fromUser = new AdvancedUser();
        fromUser.setName("fromUser");
        User toUser = new SimpleUser();
        toUser.setName("toUser");

        String userFromKey = service.userKey(fromUser);
        String userToKey = service.userKey(toUser);

        assertEquals(service.messagesUserKey(userToKey), "messages:user:SimpleUser:toUser");
        assertEquals(service.messagesUsersKey(userFromKey, userToKey), "messages:user:AdvancedUser:fromUser:user:SimpleUser:toUser");
        assertEquals(service.hashUserKey(userFromKey), "hash:user:AdvancedUser:fromUser");
        assertEquals(service.chatUserChannel(userToKey), "chat:user:SimpleUser:toUser");
        assertEquals(service.chatUsersChannel(userFromKey, userToKey), "chat:user:AdvancedUser:fromUser:user:SimpleUser:toUser");
    }

    @Test
    public void addAndDeleteUser() throws RedisMessengerException {
        User addedUser = new SimpleUser();
        addedUser.setName("addedUser");
        service.addUser(addedUser);
        service.deleteUser(addedUser);
    }

    @Test
    public void chat() throws RedisMessengerException, IOException {
        User superUser = service.getUser("user:SuperUser:superUser");
        User advancedUser = service.getUser("user:AdvancedUser:advancedUser");

        Message message = new Message();
        message.setFrom(superUser);
        message.setTo(advancedUser);
        message.setText("hello!!!");
        service.sendMessage(message);
        message.setText("zdarova!!!");
        service.sendMessage(message);

        Set<String> sentMessages = service.getMessages(superUser, advancedUser);
        Set<String> receivedMessages = service.getMessages(advancedUser, superUser);

        assertArrayEquals(sentMessages.toArray(), receivedMessages.toArray());
    }

    @Test
    public void users(){
        User superUser = new SuperUser();
        superUser.setName("superUser");

        User advancedUser = new AdvancedUser();
        advancedUser.setName("advancedUser");

        String superUserKey = service.userKey(superUser);
        String advancedUserKey = service.userKey(advancedUser);

        Set<String> userSet = new HashSet<>();
        userSet.add(superUserKey);
        userSet.add(advancedUserKey);

        assertArrayEquals(service.getUsersKeys().toArray(), userSet.toArray());
    }
}