package ru.redisMessenger.core.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;

/**
 * serialize/deserialize
 */
@Log4j2
public class JacksonHelper<T> {

    private ObjectMapper mapper;
    private FilterProvider filterProvider;

    /**
     * default constructor
     * @param filterProvider {@link FilterProvider} contains ignorable fields
     */
    public JacksonHelper(FilterProvider filterProvider){
        this.mapper = new ObjectMapper();
        this.filterProvider = filterProvider;
    }

    /**
     * Object -> String
     * @param object {@link java.lang.invoke.MethodHandleImpl.BindCaller.T} POJO
     * @return {@link String} serializedObject
     * @throws JsonProcessingException when object is incorrect
     */
    public String getSerializedObject(T object) throws JsonProcessingException {
        String serializedObject = mapper.writer(filterProvider).writeValueAsString(object);
        log.debug("serialized {}:\n {}", object.getClass(), serializedObject);
        return serializedObject;
    }

    /**
     * String -> Object
     * @param objectStr {@link String} object
     * @param clazz {@link Class} target class
     * @return {@link java.lang.invoke.MethodHandleImpl.BindCaller.T} deserialized object
     * @throws IOException when readValue
     */
    public T getDeserializedObject(String objectStr, Class<T> clazz) throws IOException {
        T object = mapper.readValue(objectStr, clazz);
        log.debug("deserialize \n{} \nto {}", objectStr, clazz);
        return object;
    }
}
