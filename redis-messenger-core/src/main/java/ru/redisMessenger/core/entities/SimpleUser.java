package ru.redisMessenger.core.entities;

/**
 * user with read-only right
 */
public class SimpleUser extends User{

    public SimpleUser(){

    }

    public SimpleUser(String name) {
        super(name);
    }

    @Override
    public int getRights() {
        return Right.READ.getValue();
    }
}
