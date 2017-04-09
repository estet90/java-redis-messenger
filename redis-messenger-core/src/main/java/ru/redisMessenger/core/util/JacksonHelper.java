package ru.redisMessenger.core.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * serialize/deserialize
 */
@Log4j2
public class JacksonHelper<T> {

    private ObjectMapper mapper;
    private FilterProvider filterProvider;

    /**
     * default constructor
     * @param filterProvider contains ignorable fields
     */
    public JacksonHelper(FilterProvider filterProvider){
        this.mapper = new ObjectMapper();
        this.filterProvider = filterProvider;
    }

    /**
     * Object -> String
     * @param object POJO
     * @return serializedObject
     * @throws JsonProcessingException when object is incorrect
     */
    public String getSerializedObject(T object) throws JsonProcessingException {
        String serializedObject = mapper.writer(filterProvider).writeValueAsString(object);
        log.debug("serialized {}:\n {}", object.getClass(), serializedObject);
        return serializedObject;
    }

    /**
     * Collecion<Object> -> Set<String>
     * @param objects POJO
     * @return set of serialized objects
     * @throws JsonProcessingException when objects are incorrect
     */
    public Set<String> getSerializedObjects(Collection<T> objects) throws JsonProcessingException {
        Set<String> serializedObjects = new HashSet<>();
        for (T object : objects)
            serializedObjects.add(getSerializedObject(object));
        return serializedObjects;
    }

    /**
     * String -> Object
     * @param objectStr object as String
     * @param clazz target class
     * @return deserialized object
     * @throws IOException when readValue
     */
    public T getDeserializedObject(String objectStr, Class<T> clazz) throws IOException {
        T object = mapper.readValue(objectStr, clazz);
        log.debug("deserialize \n{} \nto {}", objectStr, clazz);
        return object;
    }

    /**
     * Collection<String> -> Set<Object>
     * @param objectsStr objects as Collection<String>
     * @param clazz target class
     * @return set of deserialized object
     * @throws IOException when readValue
     */
    public Set<T> getDeserializedObjects(Collection<String> objectsStr, Class<T> clazz) throws IOException {
        Set<T> deserializedObjects = new HashSet<>();
        for (String objectStr : objectsStr)
            deserializedObjects.add(getDeserializedObject(objectStr, clazz));
        return deserializedObjects;
    }

}
