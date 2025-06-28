package org.example.hmby.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import feign.QueryMapEncoder;
import feign.RequestInterceptor;
import feign.codec.EncodeException;
import feign.querymap.FieldQueryMapEncoder;
import lombok.extern.slf4j.Slf4j;
import org.example.hmby.entity.Config;
import org.example.hmby.enumerate.ConfigKey;
import org.example.hmby.exception.BusinessException;
import org.example.hmby.repository.ConfigRepository;
import org.example.hmby.sceurity.EmbyUser;
import org.example.hmby.sceurity.SecurityUtils;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * description:  <br>
 * date: 2022/3/31 14:03 <br>
 * author: ws <br>
 */
@Slf4j
public class EmbyFeignClientConfig {

    private final Map<Class<?>, ObjectParamMetadata> classToMetadata = new HashMap<>();
    
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
    
    @Bean
    public RequestInterceptor requestInterceptor(ConfigRepository configRepository) {
        return requestTemplate -> {
            EmbyUser userDetails = SecurityUtils.getUserInfo()
                    .orElseThrow(() -> new BusinessException("emby认证信息获取异常！"));
            String url = requestTemplate.url().replace("USER_ID", userDetails.getUserId());
            requestTemplate.header("X-Emby-Token", userDetails.getThirdPartyToken());
            requestTemplate.header("Accept", "*/*");
            requestTemplate.uri(url);
            String embyServer = Optional.ofNullable(configRepository.findOneByKey(ConfigKey.emby_server))
                    .map(Config::getVal)
                    .orElseThrow(() -> new BusinessException("emby地址未配置"));
            requestTemplate.target(embyServer);
            if (log.isDebugEnabled()) {
                log.debug("Userid: {}", userDetails.getUserId());
                log.debug("X-Emby-Token: {}", userDetails.getThirdPartyToken());
                if (requestTemplate.body() != null && requestTemplate.body().length > 0) {
                    log.debug("Feign远程调用: {} \n{}", embyServer + requestTemplate.url(), new String(requestTemplate.body()));
                } else {
                    log.debug("Feign远程调用: {}", embyServer + requestTemplate.url());
                }
            }
        };
    }
}
