package ru.redisMessenger.application.util;

import org.junit.Test;
import ru.redisMessenger.application.exception.RedisMessengerException;

import static org.junit.Assert.*;

/**
 * test for {@link Configuration}
 */
public class ConfigurationTest {

    @Test(expected = NullPointerException.class)
    public void readUnknownParameter() throws RedisMessengerException {
        Configuration.getInstance().getProperty("unknown");
    }

    @Test
    public void readParameter() throws RedisMessengerException {
        assertNotNull(Configuration.getInstance().getProperty("file.output.directory"));
    }

}