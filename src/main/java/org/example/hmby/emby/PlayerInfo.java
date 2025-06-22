package org.example.hmby.emby;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author ws </br>
 * 2025/6/21
 */
@Data
public class PlayerInfo{
    private Long id;
    private String streamUrl;
    private String thumbImage;
    private String primaryImage;
    private List<Map<String, String>> subtitles;
}
