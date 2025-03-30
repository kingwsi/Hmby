package org.example.hmby.emby;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * description:  <br>
 * date: 2022/4/6 16:21 <br>
 * author:  <br>
 */
@Data
public class ItemInfo {

    @JsonProperty("Name")
    private String name;

    @JsonProperty("ServerId")
    private String serverId;

    @JsonProperty("Id")
    private String id;

    @JsonProperty("Container")
    private String container;

    @JsonProperty("Path")
    private String path;

    @JsonProperty("IsFolder")
    private boolean isFolder;

    @JsonProperty("ParentId")
    private String parentId;

    @JsonProperty("Type")
    private String type;

    @JsonProperty("TagItems")
    private List<ItemTag> tagItems;

    @JsonProperty("Width")
    private Integer width;

    @JsonProperty("Height")
    private Integer height;
}
