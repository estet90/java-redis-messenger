package ru.redisMessenger.core.entities;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * test for {@link User}
 */
public class UserTest {

    @Test
    public void rightTest(){
        User simpleUser = new SimpleUser();
        User advancedUser = new AdvancedUser();
        User superUser = new SuperUser();

        assertTrue(simpleUser.canRead());
        assertFalse(simpleUser.canCreateUser());
        assertFalse(simpleUser.canWrite());
        assertFalse(simpleUser.canUpload());

        assertTrue(advancedUser.canRead());
        assertFalse(advancedUser.canCreateUser());
        assertTrue(advancedUser.canWrite());
        assertTrue(advancedUser.canUpload());

        assertTrue(superUser.canRead());
        assertTrue(superUser.canCreateUser());
        assertTrue(superUser.canWrite());
        assertTrue(superUser.canUpload());
    }

}
