package org.example.hmby.emby;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class SpecialFeature {
    
    @JsonProperty("Name")
    private String name;
    
    @JsonProperty("Id")
    private Long id;
    
    @JsonProperty("Type")
    private String type;

    @JsonProperty("ImageTags")
    private Map<String, String> imageTags; 
}
