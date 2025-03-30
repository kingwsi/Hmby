package org.example.hmby.emby;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.example.hmby.vo.MediaInfoDTO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * description:  <br>
 * date: 2022/4/8 <br>
 * author: <br>
 */
@Data
public class Metadata {
    private String embyServer;
    @JsonProperty("ServerId")
    private String serverId;
    @JsonProperty("Id")
    private Long id;
    @JsonProperty("Etag")
    private String etag;
    @JsonProperty("DateCreated")
    private String dateCreated;
    @JsonProperty("CanDelete")
    private boolean canDelete;
    @JsonProperty("CanDownload")
    private boolean canDownload;
    @JsonProperty("PresentationUniqueKey")
    private String presentationUniqueKey;
    @JsonProperty("SupportsSync")
    private boolean supportsSync;
    @JsonProperty("Container")
    private String container;
    @JsonProperty("SortName")
    private String sortName;
    @JsonProperty("ExternalUrls")
    private List externalUrls;
    @JsonProperty("MediaSources")
    private List<HashMap<String, Object>> mediaSources;
    @JsonProperty("ProductionLocations")
    private List productionLocations;
    @JsonProperty("Path")
    private String path;
    @JsonProperty("Taglines")
    private List taglines;
    @JsonProperty("Genres")
    private List genres;
    @JsonProperty("RunTimeTicks")
    private Long runTimeTicks;
    @JsonProperty("PlayAccess")
    private String playAccess;
    @JsonProperty("RemoteTrailers")
    private List remoteTrailers;
    @JsonProperty("ProviderIds")
    private Object providerIds;
    @JsonProperty("IsFolder")
    private boolean isFolder;
    @JsonProperty("ParentId")
    private String parentId;
    @JsonProperty("Type")
    private String type;
    @JsonProperty("People")
    private List people;
    @JsonProperty("Studios")
    private List studios;
    @JsonProperty("GenreItems")
    private List genreItems;
    @JsonProperty("TagItems")
    private List<ItemTag> tagItems;
    @JsonProperty("UserData")
    private Object userdata;
    @JsonProperty("DisplayPreferencesId")
    private String DisplayPreferencesId;
    @JsonProperty("PrimaryImageAspectRatio")
    private double PrimaryImageAspectRatio;
    @JsonProperty("MediaStreams")
    private List MediaStreams;
    
    @JsonProperty("ImageTags")
    private Map<String, String> imageTags;
    
    @JsonProperty("BackdropImageTags")
    private Set<String> backdropImageTags;
    @JsonProperty("Chapters")
    private List chapters;
    @JsonProperty("MediaType")
    private String mediaType;
    @JsonProperty("LockedFields")
    private List lockedFields;
    @JsonProperty("LockData")
    private Boolean lockData;
    @JsonProperty("Width")
    private Long width;
    @JsonProperty("Height")
    private Long height;
    @JsonProperty("StreamUrl")
    private String streamUrl;
    @JsonProperty("Size")
    private Long size;
    
    private MediaInfoDTO mediaInfo;
    
    private List<SpecialFeature> specialFeatures;
}
