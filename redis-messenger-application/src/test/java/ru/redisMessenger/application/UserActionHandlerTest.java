package ru.redisMessenger.application;

import org.junit.Test;
import ru.redisMessenger.core.entities.AdvancedUser;
import ru.redisMessenger.core.entities.SimpleUser;
import ru.redisMessenger.core.entities.SuperUser;
import ru.redisMessenger.core.entities.User;

import java.util.Set;

import static org.junit.Assert.*;

/**
 * test for {@link UserActionHandler}
 */
public class UserActionHandlerTest {

    //предполагается, что права для разных классов пользователей не меняются, не менятеся также привязка действий к правам
    @Test
    public void enabledCommandsSet() throws Exception {
        User superUser = new SuperUser("superUser");
        User simpleUser = new SimpleUser("simpleUser");
        User advancedUser = new AdvancedUser("advancedUser");

        UserActionHandler actionHandler = new UserActionHandler();
        Set<String> simpleUserSet = actionHandler.enabledCommandsSet(simpleUser);
        assertTrue(simpleUserSet.contains(actionHandler.MESSAGE_ACTION_GET_USERS));
        assertTrue(simpleUserSet.contains(actionHandler.MESSAGE_ACTION_CHOICE_USER));
        assertTrue(simpleUserSet.contains(actionHandler.MESSAGE_ACTION_CLOSE_CONSOLE));
        assertTrue(simpleUserSet.contains(actionHandler.MESSAGE_ACTION_GET_MESSAGES));
        assertTrue(simpleUserSet.contains(actionHandler.MESSAGE_ACTION_START_CHAT));
        assertTrue(simpleUserSet.contains(actionHandler.MESSAGE_ACTION_RESET_USER));
        assertFalse(simpleUserSet.contains(actionHandler.MESSAGE_ACTION_SEND_MESSAGE));
        assertFalse(simpleUserSet.contains(actionHandler.MESSAGE_ACTION_UPLOAD_MESSAGES));
        assertFalse(simpleUserSet.contains(actionHandler.MESSAGE_ACTION_ADD_USER));
        assertFalse(simpleUserSet.contains(actionHandler.MESSAGE_ACTION_DELETE_USER));

        Set<String> advancedUserSet = actionHandler.enabledCommandsSet(advancedUser);
        assertTrue(advancedUserSet.containsAll(simpleUserSet));
        assertTrue(advancedUserSet.contains(actionHandler.MESSAGE_ACTION_SEND_MESSAGE));
        assertTrue(advancedUserSet.contains(actionHandler.MESSAGE_ACTION_UPLOAD_MESSAGES));
        assertFalse(advancedUserSet.contains(actionHandler.MESSAGE_ACTION_ADD_USER));
        assertFalse(advancedUserSet.contains(actionHandler.MESSAGE_ACTION_DELETE_USER));

        Set<String> superUserSet = actionHandler.enabledCommandsSet(superUser);
        assertTrue(superUserSet.containsAll(advancedUserSet));
        assertTrue(superUserSet.contains(actionHandler.MESSAGE_ACTION_ADD_USER));
        assertTrue(superUserSet.contains(actionHandler.MESSAGE_ACTION_DELETE_USER));

    }

}