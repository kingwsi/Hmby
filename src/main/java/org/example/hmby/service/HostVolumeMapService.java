package org.example.hmby.service;

import lombok.extern.slf4j.Slf4j;
import org.example.hmby.config.PropertiesConfig;
import org.example.hmby.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * @author ws
 * @since 2025/8/24
 */
@Slf4j
@Service
public class HostVolumeMapService {

    private final PropertiesConfig propertiesConfig;
    
    public HostVolumeMapService(PropertiesConfig propertiesConfig) {
        this.propertiesConfig = propertiesConfig;
    }
    
    public String mapping(String path) {
        if (propertiesConfig.getVolumeBind() != null && !propertiesConfig.getVolumeBind().isEmpty()) {
            // 卷路径映射处理
            for (String item : propertiesConfig.getVolumeBind()) {
                String[] split = item.trim().split("->");
                if (split.length != 2) {
                    throw new BusinessException("Volume bind exception!");
                }
                String hostVolume = split[0];
                String embyVolume = split[1];
                if (path.startsWith(hostVolume)) {
                    return path;
                }
                if (path.startsWith(embyVolume)) {
                    String realPath = hostVolume.replace(File.separatorChar, '/') + path.substring(embyVolume.length());
//                    String realPath = path.replaceFirst(embyVolume, hostVolume.replace(File.separatorChar, '/'));
                    log.info("Volume path replace : {} ->{}", path, realPath);
                    return realPath.replace('/', File.separatorChar);
                }
            }
        }
        return path;
    }
}
