package org.example.hmby.emby;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * description:  <br>
 * date: 2022/4/6 14:09 <br>
 * author:  <br>
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MovieItem {

    @JsonProperty("Id")
    private Long id;

    @JsonProperty("Name")
    private String name;

    @JsonProperty("Path")
    private String path;

    @JsonProperty("ServerId")
    private String serverId;

    @JsonProperty("IsFolder")
    private boolean isFolder;

    @JsonProperty("Type")
    private String type;

    @JsonProperty("PrimaryImageAspectRatio")
    private double primaryImageAspectRatio;

    @JsonProperty("MediaType")
    private String mediaType;

    @JsonProperty("TagItems")
    private List<ItemTag> tagItems;

    @JsonProperty("ImageTags")
    private ImageTags imageTags;

    @JsonProperty("DateCreated")
    private LocalDateTime dateCreated;

    @JsonProperty("Cover")
    private String cover;

    @JsonProperty("DetailPage")
    private String detailPage;

    @JsonProperty("StreamUrl")
    private String streamUrl;

    @JsonProperty("BusPage")
    private String busPage;
    
    @JsonProperty("Container")
    private String container;

    @JsonProperty("MediaInfo")
    private org.example.hmby.entity.MediaInfo mediaInfo;
    
    @JsonProperty("Overview")
    private String overview;

    @JsonProperty("Size")
    private Long size;
    
    @Data
    public static class ImageTags{
        @JsonProperty("Primary")
        private String primary;
        @JsonProperty("Thumb")
        private String thumb;
    }
}
