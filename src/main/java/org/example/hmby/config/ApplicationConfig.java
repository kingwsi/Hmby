package org.example.hmby.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.DateSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;
import org.apache.commons.lang3.StringUtils;
import org.example.hmby.ffmpeg.FFmpegManager;
import org.example.hmby.sceurity.SecurityUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ApplicationConfig {
    @Bean
    public ConcurrentHashMap<Object, Object> localCache(){
        return new ConcurrentHashMap<>();
    }

    @Bean
    public ObjectMapper objectMapper() {
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(format));
        javaTimeModule.addSerializer(Instant.class, new JsonSerializer<Instant>() {
            @Override
            public void serialize(Instant instant, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
                if (instant == null) {
                    return;
                }
                String jsonValue = format.format(instant.atZone(ZoneId.systemDefault()));
                jsonGenerator.writeString(jsonValue);
            }
        });
        javaTimeModule.addSerializer(Date.class, new DateSerializer(false, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")));
        javaTimeModule.addDeserializer(Instant.class, new JsonDeserializer<Instant>() {
            @Override
            public Instant deserialize(JsonParser p, DeserializationContext ctxt)
                    throws IOException {
                String dateString = p.getText().trim();
                if (StringUtils.isNotBlank(dateString)) {
                    LocalDateTime parse = LocalDateTime.parse(dateString, format);
                    //+8 小时，offset 可以理解为时间偏移量
                    ZoneOffset offset = OffsetDateTime.now().getOffset();
                    return parse.toInstant(offset);
                }
                return null;
            }
        });

        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new ParameterNamesModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .registerModule(javaTimeModule);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }
    
    @Bean
    public FFmpegManager ffmpegManager(PropertiesConfig propertiesConfig) {
        try {
            FFmpeg ffmpeg = new FFmpeg(propertiesConfig.getFfmpegPath());
            FFprobe ffprobe = new FFprobe(propertiesConfig.getFfprobePath());
            return new FFmpegManager(ffmpeg, ffprobe);
        } catch (Exception e) {
            throw new RuntimeException("ffmpeg配置错误！" + e.getMessage());
        }
    }

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of(SecurityUtils.getUserId());
    }

    @Bean
    public ThreadPoolExecutor singleThreadExecutor(){
        return new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(100), Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
    }
}
