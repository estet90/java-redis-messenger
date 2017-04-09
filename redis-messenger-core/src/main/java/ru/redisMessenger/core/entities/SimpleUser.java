package ru.redisMessenger.core.entities;

/**
 * user with read-only right
 */
public class SimpleUser extends User{

    @Override
    public int getRights() {
        return Right.READ.getValue();
    }
}
