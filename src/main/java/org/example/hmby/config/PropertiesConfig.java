package org.example.hmby.config;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.example.hmby.vo.MediaInfoDTO;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix="hmby.config")
@Data
public class PropertiesConfig {
    private String deviceId = "999";

    private String outputMediaPath;
    
    /**
     * 用于emby目录和主机目录绑定
     * 宿主机目录->emby目录
     * /Volumes/downloads->/downloads/movies
     */
    private List<String> volumeBind;
}
