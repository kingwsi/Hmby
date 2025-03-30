package org.example.hmby.emby.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TagsRequest {
    
    @JsonProperty("SearchTerm")
    private String searchTerm;
    
    @JsonProperty("Limit")
    private Long limit = 15L;

    @JsonProperty("Ids")
    private String ids;
}
