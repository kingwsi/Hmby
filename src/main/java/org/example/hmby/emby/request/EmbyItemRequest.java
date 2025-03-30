package org.example.hmby.emby.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class EmbyItemRequest {

    @JsonProperty("SortBy")
    private String sortBy = "DateCreated,SortName";

    @JsonProperty("SortOrder")
    private String sortOrder = "Descending";

    @JsonProperty("IncludeItemTypes")
    private String includeItemTypes = "Movie,Series,Episode";

    @JsonProperty("Recursive")
    private Boolean recursive = true;

    @JsonProperty("SearchTerm")
    private String searchTerm;

    @JsonProperty("Fields")
    private String fields = "BasicSyncInfo,CanDelete,PrimaryImageAspectRatio,ProductionYear,Path,Container,Size,Tags";

    @JsonProperty("ImageTypeLimit")
    private Integer imageTypeLimit = 1;

    @JsonProperty("EnableImageTypes")
    private String enableImageTypes = "Primary,Backdrop,Banner,Thumb";

    @JsonProperty("StartIndex")
    private Long startIndex = 0L;

    @JsonProperty("ParentId")
    private String parentId;

    @JsonProperty("Limit")
    private Long limit = 50L;

    @JsonProperty("TagIds")
    private String tagIds;

    @JsonProperty("Tags")
    private String tags;

    @JsonProperty("X-Emby-Client")
    private String embyClient = "Dashboard";

    @JsonProperty("X-Emby-Device-Name")
    private String embyDeviceName = "Dashboard";

    @JsonProperty("X-Emby-Device-Id")
    private String embyDeviceId = "100001";

    @JsonProperty("X-Emby-Client-Version")
    private String embyClientVersion = "0";
}
