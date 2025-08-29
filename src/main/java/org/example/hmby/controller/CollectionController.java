package org.example.hmby.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.hmby.Response;
import org.example.hmby.service.HostVolumeMappingHelper;
import org.example.hmby.utils.EmbyCollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @author ws
 * @since 2025/8/24
 */
@RestController
@RequestMapping("/api/collection")
@AllArgsConstructor
@Slf4j
public class CollectionController {
    private final HostVolumeMappingHelper hostVolumeMappingHelper;

    @GetMapping("/nfo-files")
    public Response<List<Map<String, Object>>> scanCollectionNfo(@RequestParam String folderPath) throws Exception {
        return Response.success(EmbyCollectionUtils.scanNfoFiles(hostVolumeMappingHelper.mapping(folderPath)));
    }

    @PutMapping("/folder")
    public Response<List<Map<String, Object>>> updateCollectionInfoByFolder(@RequestParam String folderPath, @RequestParam String name) throws Exception {
        EmbyCollectionUtils.updateCollectionInfoByFolder(hostVolumeMappingHelper.mapping(folderPath), name);
        return Response.success();
    }

    @PutMapping("/file")
    public Response<List<Map<String, Object>>> updateCollectionInfoByFile(@RequestParam String tilePath, @RequestParam String name) throws Exception {
        EmbyCollectionUtils.updateCollectionInfoByFile(hostVolumeMappingHelper.mapping(tilePath), name);
        return Response.success();
    }
}
