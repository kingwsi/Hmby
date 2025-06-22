package org.example.hmby.emby;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * description:  <br>
 * date: 2022/4/6 21:33 <br>
 * author:  <br>
 */
@Data
public class Library {

    @JsonProperty("DateCreated")
    String dateCreated;

    @JsonProperty("Id")
    private String id;

    @JsonProperty("Name")
    private String name;

    @JsonProperty("ImageTags")
    private MovieItem.ImageTags imageTags;
}
