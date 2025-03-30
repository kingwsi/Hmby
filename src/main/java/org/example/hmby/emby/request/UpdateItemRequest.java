package org.example.hmby.emby.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
public class UpdateItemRequest {

    @JsonProperty("Updates")
    public List<MediaUpdateInfo> updates;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class MediaUpdateInfo{
        @JsonProperty("Path")
        private String path;
        @JsonProperty("UpdateType")
        private String updateType;
    }

    public UpdateItemRequest(String path) {
        this.updates = Collections.singletonList(new MediaUpdateInfo(path, "Update"));
    }

    public UpdateItemRequest() {
    }
}


