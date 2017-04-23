package ru.redisMessenger.application.util;

import org.junit.Test;
import ru.redisMessenger.application.exception.RedisMessengerException;

/**
 * test for {@link Configuration}
 */
public class ConfigurationTest {

    @Test(expected = NullPointerException.class)
    public void readUnknownParameter() throws RedisMessengerException {
        Configuration.getInstance().getProperty("unknown");
    }

}