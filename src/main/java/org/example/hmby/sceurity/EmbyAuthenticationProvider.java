package org.example.hmby.sceurity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.example.hmby.config.PropertiesConfig;
import org.example.hmby.emby.EmbyAuthResult;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Component
public class EmbyAuthenticationProvider implements AuthenticationProvider {
    
    private final PropertiesConfig propertiesConfig;
    private final ObjectMapper objectMapper;


    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();
        EmbyAuthResult embyAuthResult = this.embyAuth(username, password);

        // 存储完整用户信息
        return new UsernamePasswordAuthenticationToken(
                new EmbyUser(embyAuthResult.getUser().getId(), username, embyAuthResult.getAccessToken()),
                password,
                Collections.emptyList()
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }

    public EmbyAuthResult embyAuth(String username, String password) {

        String userId;

        OkHttpClient client = new OkHttpClient.Builder().build();

        Request request = new Request.Builder().url(propertiesConfig.getEmbyServer() + "/Users/Public").get().build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.code() == 200 && response.body() != null) {
                String responseStr = response.body().string();
                org.example.hmby.emby.request.EmbyUser embyUser = Optional.ofNullable(objectMapper.readValue(responseStr, new TypeReference<List<org.example.hmby.emby.request.EmbyUser>>() {
                        })).map(l -> l.stream().filter(o -> username.equals(o.getName())).findFirst().orElseThrow(() -> new InsufficientAuthenticationException("用户不存在")))
                        .orElseThrow(() -> new InsufficientAuthenticationException("用户不存在"));
                userId = embyUser.getId();
            } else {
                throw new InsufficientAuthenticationException(response.code() + " " + response.message());
            }
        } catch (Exception e) {
            throw new InsufficientAuthenticationException(e.getMessage());
        }


        String token = String.format("Emby UserId=%s, Client=Chrome, Device=Hmby, DeviceId=Hmby, Version=0.1,Token=", userId);

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create("{\"Username\":\"" + username + "\",\"Pw\":\"" + password + "\"}", JSON);
        request = new Request.Builder()
                .url(propertiesConfig.getEmbyServer() + "/Users/AuthenticateByName")
                .addHeader("X-Emby-Authorization", token)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.code() == 200 && response.body() != null) {
                String responseStr = response.body().string();
                return objectMapper.readValue(responseStr, EmbyAuthResult.class);
            }
            throw new InsufficientAuthenticationException(response.code() + " " + response.message());
        } catch (Exception e) {
            throw new InsufficientAuthenticationException("账号验证异常：" + e.getMessage());
        }
    }
}

