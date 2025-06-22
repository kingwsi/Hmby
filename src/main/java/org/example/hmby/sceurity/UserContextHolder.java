package org.example.hmby.sceurity;

import lombok.Getter;
import lombok.Setter;

import java.util.Optional;

/**
 * @author ws </br>
 * 2025/6/22
 */
public class UserContextHolder {
    private static final ThreadLocal<String> usernameHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> useridHolder = new ThreadLocal<>();

    public static void setUsername(String username) {
        usernameHolder.set(username);
    }
    
    public static String getUsername(){
        return Optional.ofNullable(useridHolder.get()).orElseThrow(() -> new RuntimeException("Unable to retrieve username"));
    }

    public static void setUserid(String userid) {
        useridHolder.set(userid);
    }

    public static String getUserid(){
        return Optional.ofNullable(useridHolder.get()).orElseThrow(() -> new RuntimeException("Unable to retrieve userid"));
    }
}
