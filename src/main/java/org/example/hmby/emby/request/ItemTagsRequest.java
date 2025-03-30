package org.example.hmby.emby.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.example.hmby.emby.ItemTag;

import java.util.List;

@Data
public class ItemTagsRequest {

    @JsonProperty("TagItems")
    private List<ItemTag> tagItems;

    @JsonProperty("Tags")
    private List<String> tags;

    @JsonProperty("Id")
    private Long id;

    @JsonProperty("Source")
    private String source = "Emby";
}
