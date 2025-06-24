package org.example.hmby.controller;

import org.example.hmby.Response;
import org.example.hmby.sceurity.AuthRequest;
import org.example.hmby.sceurity.EmbyUser;
import org.example.hmby.sceurity.SecurityUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;

    public AuthController(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
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
}