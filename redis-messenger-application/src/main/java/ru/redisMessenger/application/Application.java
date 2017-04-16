package ru.redisMessenger.application;


/**
 * main class
 */
public class Application {

    /**
     * entry point
     * @param arr {@link String[]} arguments
     */
    public static void main(String[] arr){
        UserActionHandler actionHandler = new UserActionHandler();
        actionHandler.startConsole();
    }

}