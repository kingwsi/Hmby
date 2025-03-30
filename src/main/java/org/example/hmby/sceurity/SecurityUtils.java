package org.example.hmby.sceurity;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

public class SecurityUtils {

    private static final Key key = Keys.hmacShaKeyFor(Base64.getEncoder().encodeToString("your-secure-key-32bytes".getBytes(StandardCharsets.UTF_8)).getBytes());
    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 24; // 1 小时

    /**
     * 生成 JWT 令牌，包含多个用户信息
     */
    public static String generateToken(String userId, String username, String thirdPartyToken) {
        return Jwts.builder()
                .subject(username).claims(Map.of(
                        "userId", userId,
                        "username", username,
                        "thirdPartyToken", thirdPartyToken  // 存储第三方 accessToken
                )).issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key)
                .compact();
    }

    /**
     * 解析 JWT 并提取 claims
     */
    private static Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) key)
                .build()
                .parseSignedClaims(token).getPayload();
    }

    public static String extractUserId(String token) {
        return extractClaims(token).get("userId", String.class);
    }

    public static String extractUsername(String token) {
        return extractClaims(token).get("username", String.class);
    }

    public static String extractThirdPartyToken(String token) {
        return extractClaims(token).get("thirdPartyToken", String.class);
    }

    public static boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith((SecretKey) key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    public static Optional<EmbyUser> getUserInfo() {
        return Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getPrincipal)
                .map(o->o instanceof EmbyUser ? (EmbyUser) o : null);
    }

    public static String getUserId() {
        return getUserInfo().map(EmbyUser::getUserId).orElse("Unknown");
    }
    
}
