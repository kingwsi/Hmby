package org.example.hmby.sceurity;

import lombok.Data;

@Data
public class AuthRequest {
    private String username;
    private String password;
}
