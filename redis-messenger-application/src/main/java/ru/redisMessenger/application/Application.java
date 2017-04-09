package ru.redisMessenger.application;

import ru.redisMessenger.application.service.RedisMessengerService;
import ru.redisMessenger.application.util.Configuration;
import ru.redisMessenger.application.util.FileUploader;
import ru.redisMessenger.application.exception.RedisMessengerException;
import ru.redisMessenger.core.entities.AdvancedUser;
import ru.redisMessenger.core.entities.Message;
import ru.redisMessenger.core.entities.User;
import ru.redisMessenger.core.util.JacksonHelper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Created by admin on 07.04.2017.
 */
public class Application {

    public static void main(String[] arr){
        RedisMessengerService redisMessengerService = new RedisMessengerService();
        try {
            String userToKey = redisMessengerService.getUser("user:AdvancedUser:advanced");
            String userFromKey = redisMessengerService.getUser("user:AdvancedUser:advancedUserName");
            User from = new JacksonHelper<AdvancedUser>(RedisMessengerService.USER_ONLY_FILTER_PROVIDER).getDeserializedObject(userToKey, AdvancedUser.class);
            User to = new JacksonHelper<AdvancedUser>(RedisMessengerService.USER_ONLY_FILTER_PROVIDER).getDeserializedObject(userFromKey, AdvancedUser.class);
            Message message = new Message();
            message.setFrom(from);
            message.setTo(to);
            message.setText("123");
            redisMessengerService.sendMessage(message);
//            redisMessengerService.getMessages(from, to).forEach(System.out::println);
            redisMessengerService.getUsers();
            LocalDateTime date = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddhhmmssSSS");
            String text = date.format(formatter);
            String fileName = Configuration.getInstance().getProperty(Configuration.Property.FILE_OUTPUT_DIRECTORY.getPropertyName())
                    .concat(from.getName()).concat("_").concat(to.getName()).concat("_").concat(text);
            new FileUploader().writeLines(fileName, redisMessengerService.getMessages(from, to));
        } catch (RedisMessengerException | IOException e) {
            e.printStackTrace();
        }

//        User user = new AdvancedUser();
//        user.setName("advanced");
//        user.setDescription("qwweeqwe");
//        try {
//            redisMessengerService.addUser(user);
//        } catch (RedisMessengerException e) {
//            e.printStackTrace();
//        }
    }
}