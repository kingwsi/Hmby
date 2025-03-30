package org.example.hmby.mapstract;

import org.example.hmby.entity.MediaInfo;
import org.example.hmby.vo.MediaInfoDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MediaInfoConvertMapper {
    MediaInfo toMediaInfo(MediaInfoDTO mediaInfoDTO);

    MediaInfoDTO toMediaInfoDTO(MediaInfo mediainfo);
}