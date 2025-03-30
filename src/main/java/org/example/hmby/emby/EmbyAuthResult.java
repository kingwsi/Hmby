package org.example.hmby.emby;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.example.hmby.emby.request.EmbyUser;

import java.util.Collection;

/**
 * description:  <br>
 * date: 2022/9/3 21:04 <br>
 */
@Data
@ToString
public class EmbyAuthResult {
    @JsonProperty("AccessToken")
    private String accessToken;

    @JsonProperty("User")
    private EmbyUser user;

    @JsonProperty("SessionInfo")
    private SessionInfo sessionInfo;

    @Data
    public static class SessionInfo{
        @JsonProperty("Id")
        private String id;
    }
}
