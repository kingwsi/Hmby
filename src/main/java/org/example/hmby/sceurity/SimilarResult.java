package org.example.hmby.sceurity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.example.hmby.emby.MovieItem;

import java.util.List;

/**
 * @author ws </br>
 * 2025/6/22
 */
@Data
public class SimilarResult {
    @JsonProperty("Items")
    private List<Item> items;
    
    @JsonProperty("TotalRecordCount")
    private String totalRecordCount;
    
    @Data
    public static class Item {
        @JsonProperty("Name")
        private String name;
        
        @JsonProperty("ServerId")
        private String serverId;
        
        @JsonProperty("Id")
        private String id;
        
        @JsonProperty("ImageTags")
        private MovieItem.ImageTags imageTags;
        
        @JsonProperty("BackdropImageTags")
        private List<String> backdropImageTags;
    }
}
