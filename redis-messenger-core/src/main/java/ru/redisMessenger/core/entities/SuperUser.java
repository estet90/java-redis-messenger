package ru.redisMessenger.core.entities;

/**
 * superuser
 */
public class SuperUser extends User {

    public SuperUser(){

    }

    public SuperUser(String name) {
        super(name);
    }

    @Override
    public int getRights() {
        return Right.READ.getValue() | Right.WRITE.getValue() | Right.UPLOAD.getValue() | Right.CREATE_USER.getValue() | Right.DELETE_USER.getValue();
    }
}
