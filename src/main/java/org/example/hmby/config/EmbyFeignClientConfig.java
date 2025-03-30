package org.example.hmby.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import feign.Logger;
import feign.QueryMapEncoder;
import feign.codec.EncodeException;
import feign.querymap.FieldQueryMapEncoder;
import lombok.extern.slf4j.Slf4j;
import org.example.hmby.emby.EmbyAccessTokenInterceptor;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * description:  <br>
 * date: 2022/3/31 14:03 <br>
 * author: ws <br>
 */
@Slf4j
public class EmbyFeignClientConfig {

    private final Map<Class<?>, ObjectParamMetadata> classToMetadata = new HashMap<>();
    
    @Bean
    public EmbyAccessTokenInterceptor embyAccessTokenInterceptor() {
        return new EmbyAccessTokenInterceptor();
    }


    /**
     * 实现自定义Query参数编码器 以支持JsonProperty注解
     * @see FieldQueryMapEncoder
     * @return
     */
    @Bean
    public QueryMapEncoder queryMapEncoder(){
        return object -> {
            try {
                ObjectParamMetadata metadata = getMetadata(object.getClass());
                Map<String, Object> fieldNameToValue = new HashMap<>();
                for (Field field : metadata.objectFields) {
                    Object value = field.get(object);
                    if (value != null && value != object) {
                        JsonProperty alias = field.getAnnotation(JsonProperty.class);
                        String name = alias != null ? alias.value() : field.getName();
                        fieldNameToValue.put(name, value);
                    }
                }
                return fieldNameToValue;
            } catch (IllegalAccessException e) {
                throw new EncodeException("Failure encoding object into query map", e);
            }
        };
    }


    private ObjectParamMetadata getMetadata(Class<?> objectType) {
        ObjectParamMetadata metadata = classToMetadata.get(objectType);
        if (metadata == null) {
            metadata = ObjectParamMetadata.parseObjectType(objectType);
            classToMetadata.put(objectType, metadata);
        }
        return metadata;
    }

    private static class ObjectParamMetadata {

        private final List<Field> objectFields;

        private ObjectParamMetadata(List<Field> objectFields) {
            this.objectFields = Collections.unmodifiableList(objectFields);
        }

        private static ObjectParamMetadata parseObjectType(Class<?> type) {
            List<Field> allFields = new ArrayList<>();

            for (Class<?> currentClass = type; currentClass != null; currentClass =
                    currentClass.getSuperclass()) {
                Collections.addAll(allFields, currentClass.getDeclaredFields());
            }

            return new ObjectParamMetadata(allFields.stream()
                    .filter(field -> !field.isSynthetic())
                    .peek(field -> field.setAccessible(true))
                    .collect(Collectors.toList()));
        }
    }
    
    // 开启Feign的日志
    @Bean
    public Logger.Level logger() {
        return Logger.Level.FULL;
    }
}
