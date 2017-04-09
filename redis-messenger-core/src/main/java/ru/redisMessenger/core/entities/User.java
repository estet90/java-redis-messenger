package ru.redisMessenger.core.entities;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * abstract class user with parameters
 */
@Data
@JsonFilter("User")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
public abstract class User {

    private String name;
    private String description;
    private List<Message> messages;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    private Date dateCreate;

    protected abstract int getRights();

    public boolean canRead(){
        return (getRights() & Right.READ.getValue()) == Right.READ.getValue();
    }

    public boolean canWrite(){
        return (getRights() & Right.WRITE.getValue()) == Right.WRITE.getValue();
    }

    public boolean canUpload(){
        return (getRights() & Right.UPLOAD.getValue()) == Right.UPLOAD.getValue();
    }

    public boolean canCreateUser(){
        return (getRights() & Right.CREATE_USER.getValue()) == Right.CREATE_USER.getValue();
    }

    /**
     * user's rights
     */
    enum Right {

        READ(1), WRITE(2), UPLOAD(4), CREATE_USER(8);

        private int value;

        Right(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }

    }

}
