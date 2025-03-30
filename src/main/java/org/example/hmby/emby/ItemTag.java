package org.example.hmby.emby;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemTag {

    @JsonProperty("Id")
    private Long id;

    @JsonProperty("Name")
    private String name;

    public ItemTag(String name) {
        this.name = name;
    }
}
