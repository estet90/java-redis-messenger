package ru.redisMessenger.core.util;

import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.junit.Test;
import ru.redisMessenger.core.entities.Message;
import ru.redisMessenger.core.entities.SuperUser;
import ru.redisMessenger.core.entities.User;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * tests for {@link JacksonHelper}
 */
public class JacksonHelperTest {

    private static final String[] MESSAGE_IGNORABLE_FILTER = new String[]{"messages"};
    private static final String[] USER_DETAILS_IGNORABLE_FILTER = new String[]{"messages", "description", "dateCreate", "rights"};

    private static final FilterProvider USER_ONLY_FILTER_PROVIDER = new SimpleFilterProvider()
            .addFilter("User", SimpleBeanPropertyFilter.serializeAllExcept(MESSAGE_IGNORABLE_FILTER));
    private static final FilterProvider MESSAGES_FILTER_PROVIDER = new SimpleFilterProvider()
            .addFilter("User", SimpleBeanPropertyFilter.serializeAllExcept(USER_DETAILS_IGNORABLE_FILTER))
            .addFilter("Message", SimpleBeanPropertyFilter.serializeAll());

    private static final String SUPERUSER = "{\"type\":\"ru.redisMessenger.core.entities.SuperUser\",\"name\":\"super\",\"dateCreate\":\"23-04-2017 07:58:50\",\"rights\":31}";
    private static final String MESSAGE = "{\"Message\":{\"text\":\"hello!!!\",\"from\":{\"type\":\"ru.redisMessenger.core.entities.SuperUser\",\"name\":\"super\"},\"to\":{\"type\":\"ru.redisMessenger.core.entities.SimpleUser\",\"name\":\"simple\"},\"dateCreate\":\"23-04-2017 08:24:37:168\"}}";

    @Test
    public void serializeDeserializeUser() throws Exception {
        User user = new JacksonHelper<User>(USER_ONLY_FILTER_PROVIDER).getDeserializedObject(SUPERUSER, User.class);
        assertEquals(user.getClass(), SuperUser.class);
        String superuser = new JacksonHelper<User>(USER_ONLY_FILTER_PROVIDER).getSerializedObject(user);
        assertEquals(superuser, SUPERUSER);
    }

    @Test
    public void serializeDeserializeMessage() throws Exception {
        Message message = new JacksonHelper<Message>(MESSAGES_FILTER_PROVIDER).getDeserializedObject(MESSAGE, Message.class);
        String messageStr = new JacksonHelper<Message>(MESSAGES_FILTER_PROVIDER).getSerializedObject(message);
        assertEquals(messageStr, MESSAGE);
    }

    @Test(expected = IOException.class)
    public void serializeException() throws Exception {
        String incorrectUserStr = SUPERUSER.replace("SuperUser", "");
        new JacksonHelper<User>(USER_ONLY_FILTER_PROVIDER).getDeserializedObject(incorrectUserStr, User.class);
    }

}