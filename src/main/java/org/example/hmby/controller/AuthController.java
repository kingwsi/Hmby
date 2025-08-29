package org.example.hmby.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.hmby.Response;
import org.example.hmby.config.PropertiesConfig;
import org.example.hmby.repository.ConfigRepository;
import org.example.hmby.sceurity.AuthRequest;
import org.example.hmby.sceurity.EmbyUser;
import org.example.hmby.sceurity.SecurityUtils;
import org.example.hmby.sceurity.UserContextHolder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final ConfigRepository configRepository;
    private final PropertiesConfig propertiesConfig;
    private final ObjectMapper objectMapper;

    public AuthController(AuthenticationManager authenticationManager, ConfigRepository configRepository, PropertiesConfig propertiesConfig, ObjectMapper objectMapper) {
        this.authenticationManager = authenticationManager;
        this.configRepository = configRepository;
        this.propertiesConfig = propertiesConfig;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/login")
    public Response<String> login(@RequestBody AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        if (authentication.isAuthenticated()) {
            EmbyUser userDetails = (EmbyUser) authentication.getPrincipal();
            String token = SecurityUtils.generateToken(userDetails.getUserId(), userDetails.getUsername(), userDetails.getThirdPartyToken());
            return Response.success(token);
        }
        return Response.fail("登录失败");
    }

    @GetMapping("/validate")
    public Response<String> validate() {
        return Response.success();
    }

    @GetMapping("/userinfo")
    public Response<Map<String, Object>> userinfo() {
        String username = UserContextHolder.getUsername();
        String userid = UserContextHolder.getUserid();
        Map<String, String> configMap = Optional.ofNullable(configRepository.findConfigBy())
                .map(c -> objectMapper.convertValue(c, new TypeReference<Map<String, String>>() {
                })).orElse(new HashMap<>());
        configMap.put("emby_server", propertiesConfig.getEmbyServer());
        return Response.success(Map.of("username", username, "userid", userid, "config", configMap));
    }
    
}