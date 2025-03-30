package org.example.hmby.emby.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.example.hmby.emby.ItemTag;

import java.util.List;
import java.util.Set;

@Data
public class MetadataRequest {
    @JsonProperty("Id")
    private Long id;
    @JsonProperty("Name")
    private String name;
    @JsonProperty("ChannelNumber")
    private String channelNumber;
    @JsonProperty("OriginalTitle")
    private String originalTitle;
    @JsonProperty("ForcedSortName")
    private String forcedSortName;
    @JsonProperty("CommunityRating")
    private String communityRating;
    @JsonProperty("CriticRating")
    private String criticRating;
    @JsonProperty("IndexNumber")
    private String indexNumber;
    @JsonProperty("AirsBeforeSeasonNumber")
    private String airsBeforeSeasonNumber;
    @JsonProperty("AirsAfterSeasonNumber")
    private String airsAfterSeasonNumber;
    @JsonProperty("AirsBeforeEpisodeNumber")
    private String airsBeforeEpisodeNumber;
    @JsonProperty("ParentIndexNumber")
    private String parentIndexNumber;
    @JsonProperty("SortParentIndexNumber")
    private String sortParentIndexNumber;
    @JsonProperty("SortIndexNumber")
    private String sortIndexNumber;
    @JsonProperty("DisplayOrder")
    private String displayOrder;
    @JsonProperty("Album")
    private String album;
    @JsonProperty("AlbumArtists")
    private List albumArtists;
    @JsonProperty("ArtistItems")
    private List artistItems;
    @JsonProperty("Overview")
    private String overview;
    @JsonProperty("Status")
    private String status;
    @JsonProperty("Genres")
    private List genres;
    @JsonProperty("Tags")
    private Set<String> tags;
    @JsonProperty("TagItems")
    private List<ItemTag> tagItems;
    @JsonProperty("Studios")
    private List studios;
    @JsonProperty("PremiereDate")
    private String premiereDate;
    @JsonProperty("DateCreated")
    private String dateCreated;
    @JsonProperty("EndDate")
    private String endDate;
    @JsonProperty("ProductionYear")
    private String productionYear;
    @JsonProperty("Video3DFormat")
    private String video3Format;
    @JsonProperty("OfficialRating")
    private String officialRating;
    @JsonProperty("CustomRating")
    private String customRating;
    @JsonProperty("People")
    private List people;
    @JsonProperty("LockData")
    private boolean lockData;
    @JsonProperty("LockedFields")
    private List lockedFields;
    @JsonProperty("ProviderIds")
    private Object providerIds;
    @JsonProperty("PreferredMetadataLanguage")
    private String preferredMetadataLanguage;
    @JsonProperty("PreferredMetadataCountryCode")
    private String preferredMetadataCountryCode;
    @JsonProperty("Taglines")
    private List taglines;
}
