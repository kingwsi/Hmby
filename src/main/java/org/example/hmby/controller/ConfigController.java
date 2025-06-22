package org.example.hmby.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.hmby.Response;
import org.example.hmby.entity.Config;
import org.example.hmby.enumerate.ConfigKey;
import org.example.hmby.repository.ConfigRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @GetMapping
    public Response<Map<ConfigKey, String>> getConfig(){
        List<Config> all = configRepository.findAll();
        Map<ConfigKey, String> configMap = all.stream().collect(Collectors.toMap(o -> o.getKey(), o -> o.getVal()));
        return Response.success(configMap);
    }

    @PutMapping("/{key}/{val}")
    public Response<?> saveConfig(@PathVariable("key") ConfigKey key, @PathVariable("val") String val){
        Config config = new Config();
        Config existConfig = configRepository.findOneByKey(key);
        if(existConfig != null){
            config.setId(existConfig.getId());
        }
        config.setKey(key);
        config.setVal(val);
        configRepository.save(config);
        return Response.success(val);
    }
}
