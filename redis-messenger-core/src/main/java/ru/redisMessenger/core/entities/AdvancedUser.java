package ru.redisMessenger.core.entities;

/**
 * user with advanced rights
 */
public class AdvancedUser extends User {

    @Override
    public int getRights() {
        return Right.READ.getValue() | Right.WRITE.getValue() | Right.UPLOAD.getValue();
    }
}
