package org.example.hmby.emby.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmbyUser {
    @JsonProperty("Name")
    private String name;

    @JsonProperty("Id")
    private String id;
}
