package ru.redisMessenger.application;

import lombok.extern.log4j.Log4j2;
import ru.redisMessenger.application.exception.RedisMessengerException;
import ru.redisMessenger.application.service.RedisMessengerService;
import ru.redisMessenger.application.util.Configuration;
import ru.redisMessenger.application.util.FileUploader;
import ru.redisMessenger.core.entities.*;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

/**
 * console interface
 */
@Log4j2
class UserActionHandler {

    private RedisMessengerService service;
    private User currentUser;

    private final String REDIS_KEY_USER_PREFIX_PROPERTY = Configuration.getInstance().getProperty(Configuration.Property.REDIS_KEY_USER_PREFIX.getPropertyName());
    private final String FILE_OUTPUT_DIRECTORY_PROPERTY = Configuration.getInstance().getProperty(Configuration.Property.FILE_OUTPUT_DIRECTORY.getPropertyName());

    private final int COMMAND_STATUS_SUCCESS = 1;
    private final int COMMAND_STATUS_WARNING = 2;
    private final int COMMAND_STATUS_ERROR = 3;

    private final String MESSAGE_INFO_OPEN_CONSOLE =     "============console opened==============";
    private final String MESSAGE_INFO_CLOSE_CONSOLE =    "============console closed==============";
    private final String MESSAGE_INFO_CREATED_USERS =    "=============created users==============";
    private final String MESSAGE_INFO_ENABLED_COMMANDS = "===========enabled commands=============";
    private final String MESSAGE_INFO_END =              "========================================";
    private final String MESSAGE_INFO_ENTER_THE_COMMAND = "enter the command -> ";
    private final String MESSAGE_ERROR_COMMAND_NOT_FOUND = "command not found";
    private final String MESSAGE_ERROR_USER_NOT_FOUND = "user not found";
    private final String MESSAGE_ERROR_USER_CLASS_NOT_FOUND = "user class not found";
    private final String MESSAGE_ERROR_USER_CLASS_NAME_CANNOT_BE_EMPTY = "user classname cannot be empty";
    private final String MESSAGE_ERROR_USER_NAME_CANNOT_BE_EMPTY = "user name cannot be empty";
    private final String MESSAGE_SUCCESS = "success!!!";
    private final String MESSAGE_WARNING = "warning!!!";
    private final String MESSAGE_ERROR = "error!!!";
    private final String MESSAGE_WARNING_NOT_FOUND_ANY_USERS = "not found any users";
    private final String MESSAGE_WARNING_TOO_SHORT_USERNAME = "too short username";

    private final String MESSAGE_ACTION_INPUT_FIELD_USER_CLASS = "enter classname -> ";
    private final String MESSAGE_ACTION_INPUT_FIELD_USER_NAME = "enter username -> ";
    private final String MESSAGE_ACTION_INPUT_FIELD_MESSAGE = "enter message -> ";

    final String MESSAGE_ACTION_GET_MESSAGES = "get messages";
    private final String MESSAGE_INFO_GET_MESSAGES =    "=============get messages===============";
    final String MESSAGE_ACTION_UPLOAD_MESSAGES = "upload messages";
    private final String MESSAGE_INFO_UPLOAD_MESSAGES = "============upload messages=============";
    final String MESSAGE_ACTION_SEND_MESSAGE = "send message";
    private final String MESSAGE_INFO_SEND_MESSAGES =   "=============send messages==============";
    final String MESSAGE_ACTION_ADD_USER = "add user";
    private final String MESSAGE_INFO_ADD_USER =        "===============add user=================";
    final String MESSAGE_ACTION_DELETE_USER = "delete user";
    private final String MESSAGE_INFO_DELETE_USER =     "==============delete user===============";
    final String MESSAGE_ACTION_GET_USERS = "get users";
    private final String MESSAGE_INFO_GET_USERS =       "===============get users================";
    final String MESSAGE_ACTION_START_CHAT = "start chat";
    private final String MESSAGE_INFO_START_CHAT =      "==============start chat================";
    final String MESSAGE_ACTION_CHOICE_USER = "choice user";
    private final String MESSAGE_INFO_CHOICE_USER =     "==============choice user===============";
    final String MESSAGE_ACTION_RESET_USER = "reset user";
    private final String MESSAGE_INFO_RESET_USER =      "==============reset user================";
    final String MESSAGE_ACTION_CLOSE_CONSOLE = "close console";

    /**
     * default constructor
     */
    UserActionHandler() {
        service = new RedisMessengerService();
    }

    /**
     * entry point
     */
    void startConsole() {
        System.out.println(MESSAGE_INFO_OPEN_CONSOLE);

        Set<String> keys = service.getUsersKeys();
        if (keys.size() > 0) {
            System.out.println(MESSAGE_INFO_CREATED_USERS);
            keys.forEach(System.out::println);
            System.out.println(MESSAGE_INFO_END);
        } else {
            System.out.println(MESSAGE_WARNING_NOT_FOUND_ANY_USERS);
            User user = new SuperUser("super");
            try {
                service.addUser(user);
            } catch (RedisMessengerException e) {
                System.out.println(e.getLocalizedMessage());
            }
            currentUser = user;
        }
        command();
    }

    /**
     * set of commands
     */
    private void command() {
        System.out.println(MESSAGE_INFO_ENABLED_COMMANDS);
        Set<String> commands = enabledCommandsSet(currentUser);
        commands.forEach(System.out::println);
        String command = System.console().readLine(MESSAGE_INFO_ENTER_THE_COMMAND).toLowerCase().trim();
        int status = COMMAND_STATUS_SUCCESS;
        if (commands.contains(command)) {
            switch (command) {
                case MESSAGE_ACTION_GET_USERS:
                    status = commandGetUsers();
                    break;
                case MESSAGE_ACTION_ADD_USER:
                    status = commandAddUser();
                    break;
                case MESSAGE_ACTION_DELETE_USER:
                    status = commandDeleteUser();
                    break;
                case MESSAGE_ACTION_CHOICE_USER:
                    status = commandChoiceUser();
                    break;
                case MESSAGE_ACTION_GET_MESSAGES:
                    status = commandGetMessages();
                    break;
                case MESSAGE_ACTION_SEND_MESSAGE:
                    status = commandSendMessage();
                    break;
                case MESSAGE_ACTION_START_CHAT:
                    status = commandStartChat();
                    break;
                case MESSAGE_ACTION_UPLOAD_MESSAGES:
                    status = commandUploadMessages();
                    break;
                case MESSAGE_ACTION_CLOSE_CONSOLE:
                    System.out.println(MESSAGE_INFO_CLOSE_CONSOLE);
                    break;
                case MESSAGE_ACTION_RESET_USER:
                    status = commandResetUser();
                    break;
                default:
                    System.out.println(MESSAGE_ERROR_COMMAND_NOT_FOUND);
                    status = COMMAND_STATUS_ERROR;
                    break;
            }
            if (!command.equals(MESSAGE_ACTION_CLOSE_CONSOLE)) {
                if (status == COMMAND_STATUS_SUCCESS)
                    System.out.println(MESSAGE_SUCCESS);
                else if (status == COMMAND_STATUS_WARNING)
                    System.out.println(MESSAGE_WARNING);
                else if (status == COMMAND_STATUS_ERROR)
                    System.out.println(MESSAGE_ERROR);
                command();
            }
        } else {
            System.out.println(MESSAGE_ERROR_COMMAND_NOT_FOUND);
            command();
        }
    }

    /**
     * add user
     * @return int status
     */
    private int commandAddUser() {
        System.out.println(MESSAGE_INFO_ADD_USER);
        User user;
        String className = System.console().readLine(MESSAGE_ACTION_INPUT_FIELD_USER_CLASS).trim();
        if (className.equals(SuperUser.class.getSimpleName()))
            user = new SuperUser();
        else if (className.equals(AdvancedUser.class.getSimpleName()))
            user = new AdvancedUser();
        else if (className.equals(SimpleUser.class.getSimpleName()))
            user = new SimpleUser();
        else {
            System.out.println(MESSAGE_ERROR_USER_CLASS_NOT_FOUND);
            System.out.println(MESSAGE_INFO_END);
            return COMMAND_STATUS_ERROR;
        }
        String userName = System.console().readLine(MESSAGE_ACTION_INPUT_FIELD_USER_NAME).trim();
        if (userName.length() > 0) {
            user.setName(userName);
            try {
                service.addUser(user);
            } catch (RedisMessengerException e) {
                System.out.println(e.getLocalizedMessage());
                System.out.println(MESSAGE_INFO_END);
                return COMMAND_STATUS_ERROR;
            }
            if (userName.length() < 3) {
                System.out.println(MESSAGE_WARNING_TOO_SHORT_USERNAME);
                System.out.println(MESSAGE_INFO_END);
                return COMMAND_STATUS_WARNING;
            } else
                System.out.println(MESSAGE_INFO_END);
                return COMMAND_STATUS_SUCCESS;
        } else {
            System.out.println(MESSAGE_ERROR_USER_NAME_CANNOT_BE_EMPTY);
            System.out.println(MESSAGE_INFO_END);
            return COMMAND_STATUS_ERROR;
        }
    }

    /**
     * delete user
     * @return int status
     */
    private int commandDeleteUser(){
        System.out.println(MESSAGE_INFO_DELETE_USER);
        User contact = fillContact();
        if (contact == null)
            return COMMAND_STATUS_ERROR;
        try {
            service.deleteUser(contact);
        } catch (RedisMessengerException e) {
            System.out.println(e.getLocalizedMessage());
            System.out.println(MESSAGE_INFO_END);
            return COMMAND_STATUS_ERROR;
        }
        System.out.println(MESSAGE_INFO_END);
        return COMMAND_STATUS_SUCCESS;
    }

    /**
     * get users
     * @return int status
     */
    private int commandGetUsers() {
        System.out.println(MESSAGE_INFO_GET_USERS);
        Set<String> keys = service.getUsersKeys();
        if (keys.size() > 0){
            keys.forEach(System.out::println);
            System.out.println(MESSAGE_INFO_END);
            return COMMAND_STATUS_SUCCESS;
        } else {
            System.out.println(MESSAGE_WARNING_NOT_FOUND_ANY_USERS);
            System.out.println(MESSAGE_INFO_END);
            return COMMAND_STATUS_WARNING;
        }
    }

    /**
     * choice user
     * @return int status
     */
    private int commandChoiceUser() {
        System.out.println(MESSAGE_INFO_CHOICE_USER);
        String userKey;
        try {
            userKey = getUserKey();
        } catch (RedisMessengerException e) {
            System.out.println(e.getLocalizedMessage());
            System.out.println(MESSAGE_INFO_END);
            return COMMAND_STATUS_ERROR;
        }
        try {
            this.currentUser = service.getUser(userKey);
            System.out.println(MESSAGE_INFO_END);
            return COMMAND_STATUS_SUCCESS;
        } catch (RedisMessengerException e) {
            System.out.println(MESSAGE_ERROR_USER_NOT_FOUND);
            System.out.println(MESSAGE_INFO_END);
            return COMMAND_STATUS_WARNING;
        } catch (IOException e) {
            System.out.println(e.getLocalizedMessage());
            System.out.println(MESSAGE_INFO_END);
            return COMMAND_STATUS_ERROR;
        }
    }

    /**
     * start chat
     * @return int status
     */
    private int commandStartChat() {
        System.out.println(MESSAGE_INFO_START_CHAT);
        User contact = fillContact();
        if (contact == null)
            return COMMAND_STATUS_ERROR;
        service.subscribe(currentUser, contact);
        return COMMAND_STATUS_SUCCESS;
    }

    /**
     * get messages
     * @return int status
     */
    private int commandGetMessages() {
        System.out.println(MESSAGE_INFO_GET_MESSAGES);
        User contact = fillContact();
        if (contact == null)
            return COMMAND_STATUS_ERROR;
        Set<String> messages = service.getMessages(currentUser, contact);
        messages.forEach(System.out::println);
        System.out.println(MESSAGE_INFO_END);
        return COMMAND_STATUS_SUCCESS;
    }

    /**
     * upload messages
     * @return int status
     */
    private int commandUploadMessages() {
        System.out.println(MESSAGE_INFO_UPLOAD_MESSAGES);
        User contact = fillContact();
        if (contact == null)
            return COMMAND_STATUS_ERROR;
        Set<String> messages = service.getMessages(currentUser, contact);
        try {
            new FileUploader().writeLines(uploadedFileName(currentUser.getName(), contact.getName()), messages);
            log.debug(uploadedFileName(currentUser.getName(), contact.getName()));
            System.out.println(MESSAGE_INFO_END);
        } catch (IOException | InvalidPathException e) {
            System.out.println(e.getLocalizedMessage());
            System.out.println(MESSAGE_INFO_END);
            return COMMAND_STATUS_ERROR;
        }
        return COMMAND_STATUS_SUCCESS;
    }

    /**
     * send message
     * @return int status
     */
    private int commandSendMessage() {
        System.out.println(MESSAGE_INFO_SEND_MESSAGES);
        User contact = fillContact();
        if (contact == null)
            return COMMAND_STATUS_ERROR;
        Message message = new Message();
        message.setFrom(currentUser);
        message.setTo(contact);
        String text = System.console().readLine(MESSAGE_ACTION_INPUT_FIELD_MESSAGE).trim();
        message.setText(text);
        try {
            service.sendMessage(message);
            System.out.println(MESSAGE_INFO_END);
        } catch (RedisMessengerException e) {
            System.out.println(e.getLocalizedMessage());
            System.out.println(MESSAGE_INFO_END);
            return COMMAND_STATUS_ERROR;
        }
        return COMMAND_STATUS_SUCCESS;
    }

    /**
     * reset currentUser
     * @return int status
     */
    private int commandResetUser(){
        System.out.println(MESSAGE_INFO_RESET_USER);
        currentUser = null;
        System.out.println(MESSAGE_INFO_END);
        return COMMAND_STATUS_SUCCESS;
    }

    /**
     * create name for uploaded file
     * @return {@link String} filename
     */
    private String uploadedFileName(String currentUserName, String contactName){
        LocalDateTime date = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddhhmmssSSS");
        String dateStr = date.format(formatter);
        return FILE_OUTPUT_DIRECTORY_PROPERTY.concat(String.join("_", currentUserName, contactName, dateStr).concat(".txt"));
    }

    /**
     * fill contact variable
     * @return int status
     */
    private User fillContact() {
        String userToKey;
        User contact;
        try {
            userToKey = getUserKey();
        } catch (RedisMessengerException e) {
            System.out.println(e.getLocalizedMessage());
            return null;
        }
        try {
            contact = service.getUser(userToKey);
        } catch (RedisMessengerException e) {
            System.out.println(MESSAGE_ERROR_USER_NOT_FOUND);
            return null;
        } catch (IOException e) {
            System.out.println(e.getLocalizedMessage());
            return null;
        }
        return contact;
    }

    /**
     * get user key
     * @return {@link String} user key
     * @throws RedisMessengerException when username or user class is empty
     */
    private String getUserKey() throws RedisMessengerException {
        String userClass = System.console().readLine(MESSAGE_ACTION_INPUT_FIELD_USER_CLASS).trim();
        if (userClass.length() == 0) {
            System.out.println(MESSAGE_ERROR_USER_CLASS_NAME_CANNOT_BE_EMPTY);
            throw new RedisMessengerException(MESSAGE_ERROR_USER_CLASS_NAME_CANNOT_BE_EMPTY);
        }
        String userName = System.console().readLine(MESSAGE_ACTION_INPUT_FIELD_USER_NAME).trim();
        if (userName.length() == 0) {
            System.out.println(MESSAGE_ERROR_USER_NAME_CANNOT_BE_EMPTY);
            throw new RedisMessengerException(MESSAGE_ERROR_USER_NAME_CANNOT_BE_EMPTY);
        }
        return String.join(":", REDIS_KEY_USER_PREFIX_PROPERTY, userClass, userName);
    }

    /**
     * set of enabled commands for current user
     * @param user {@link User} current user
     * @return {@link Set<String>} commands
     */
    Set<String> enabledCommandsSet(User user) {
        Set<String> commands = new HashSet<>();
        commands.add(MESSAGE_ACTION_GET_USERS);
        commands.add(MESSAGE_ACTION_CHOICE_USER);
        if (user == null) {
            commands.add(MESSAGE_ACTION_ADD_USER);
        } else {
            commands.add(MESSAGE_ACTION_RESET_USER);
            if (user.canRead()) {
                commands.add(MESSAGE_ACTION_GET_MESSAGES);
                commands.add(MESSAGE_ACTION_START_CHAT);
            }
            if (user.canWrite()) {
                commands.add(MESSAGE_ACTION_SEND_MESSAGE);
            }
            if (user.canUpload()) {
                commands.add(MESSAGE_ACTION_UPLOAD_MESSAGES);
            }
            if (user.canCreateUser()) {
                commands.add(MESSAGE_ACTION_ADD_USER);
            }
            if (user.canDeleteUser()) {
                commands.add(MESSAGE_ACTION_DELETE_USER);
            }
        }
        commands.add(MESSAGE_ACTION_CLOSE_CONSOLE);
        return commands;
    }

}