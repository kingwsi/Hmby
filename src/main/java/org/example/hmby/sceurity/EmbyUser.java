package org.example.hmby.sceurity;

import lombok.Getter;
import org.springframework.security.core.userdetails.User;

import java.util.Collections;

@Getter
public class EmbyUser extends User {
    private final String userId;
    private final String thirdPartyToken;

    public EmbyUser(String userId, String username, String thirdPartyToken) {
        super(username, "", Collections.emptyList());
        this.userId = userId;
        this.thirdPartyToken = thirdPartyToken;
    }

}
