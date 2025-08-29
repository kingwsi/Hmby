package org.example.hmby.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.hmby.Response;
import org.example.hmby.config.PropertiesConfig;
import org.example.hmby.entity.Config;
import org.example.hmby.repository.ConfigRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


/**
 * @author ws </br>
 * 2025/6/20
 */
@Slf4j
@RestController
@RequestMapping("/api/config")
@AllArgsConstructor
public class ConfigController {
    private final ConfigRepository configRepository;
    private final ObjectMapper objectMapper;
    private final PropertiesConfig propertiesConfig;

    @GetMapping
    public Response<Map<String, String>> getConfig() {
        Map<String, String> configMap = Optional.ofNullable(configRepository.findConfigBy())
                .map(c -> objectMapper.convertValue(c, new TypeReference<Map<String, String>>() {
                })).orElse(new HashMap<>());
        configMap.put("emby_server", propertiesConfig.getEmbyServer());
        return Response.success(configMap);
    }

    @PutMapping
    public Response<Config> getConfig(@RequestBody Config config) {
        Optional.ofNullable(configRepository.findConfigBy())
                        .ifPresent(o->config.setId(o.getId()));
        config.setUserId(config.getUserId());
        configRepository.save(config);
        return Response.success();
    }
}
