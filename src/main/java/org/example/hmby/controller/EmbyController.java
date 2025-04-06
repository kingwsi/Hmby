package org.example.hmby.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.hmby.Response;
import org.example.hmby.config.PropertiesConfig;
import org.example.hmby.emby.ItemTag;
import org.example.hmby.emby.Library;
import org.example.hmby.emby.Metadata;
import org.example.hmby.emby.MovieItem;
import org.example.hmby.emby.PageWrapper;
import org.example.hmby.emby.request.EmbyItemRequest;
import org.example.hmby.emby.request.MetadataRequest;
import org.example.hmby.entity.MediaInfo;
import org.example.hmby.exception.BusinessException;
import org.example.hmby.emby.EmbyFeignClient;
import org.example.hmby.sceurity.EmbyUser;
import org.example.hmby.service.MediaInfoService;
import org.example.hmby.service.TagService;
import org.example.hmby.sceurity.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * description:  <br>
 * date: 2022/4/6 17:25 <br>
 * author:  <br>
 */
@RestController
@RequestMapping("/api/emby-item")
@Slf4j
public class EmbyController {

    private final EmbyFeignClient embyClient;

    private final PropertiesConfig propertiesConfig;
    
    private final MediaInfoService mediaInfoService;
    private final TagService tagService;

    public EmbyController(EmbyFeignClient embyClient, PropertiesConfig propertiesConfig, MediaInfoService mediaInfoService, TagService tagService) {
        this.embyClient = embyClient;
        this.propertiesConfig = propertiesConfig;
        this.mediaInfoService = mediaInfoService;
        this.tagService = tagService;
    }

    @GetMapping("/page")
    public Response<Page<MovieItem>> page(@RequestParam(value = "parentId", required = false) String parentId,
                                          @RequestParam(value = "keyword", required = false) String keyword,
                                          @RequestParam(value = "tag", required = false) String tag,
                                          @RequestParam(value = "size", required = false, defaultValue = "10") Integer pageSize,
                                          @RequestParam(value = "page", required = false, defaultValue = "1") Integer page) {
        EmbyItemRequest embyItemRequest = new EmbyItemRequest();
        embyItemRequest.setLimit((long) pageSize);
        long startIndex = (long) pageSize * page - pageSize;
        embyItemRequest.setStartIndex(startIndex);

        if (StringUtils.isNotBlank(keyword)) {
            embyItemRequest.setSearchTerm(keyword);
        }
        if (StringUtils.isNotBlank(parentId)) {
            embyItemRequest.setParentId(parentId);
        }
        if (StringUtils.isNotBlank(tag)) {
            embyItemRequest.setTags(tag);
        }
        PageWrapper<MovieItem> pageWrapper = embyClient.getItems(embyItemRequest);
        for (MovieItem item : pageWrapper.getItems()) {
            if (item.getImageTags() != null) {
                if (StringUtils.isNotBlank(item.getImageTags().getThumb())) {
                    item.setCover(String.format(propertiesConfig.getEmbyServer() + "/emby/Items/%s/Images/Thumb?maxWidth=300&quality=100", item.getId()));
                } else if (StringUtils.isNotBlank(item.getImageTags().getPrimary())) {
                    item.setCover(String.format(propertiesConfig.getEmbyServer() + "/emby/Items/%s/Images/Primary?maxWidth=300&quality=100", item.getId()));
                }
            }
            item.setDetailPage(String.format(propertiesConfig.getEmbyServer() + "/web/index.html#!/item?id=%s&serverId=%s", item.getId(), item.getServerId()));
            MediaInfo mediaInfo = mediaInfoService.getByInputPath(item.getPath());
            if (mediaInfo == null) {
                mediaInfo = mediaInfoService.getByOutputPath(item.getPath());
            }
            item.setMediaInfo(mediaInfo);
        }
        pageWrapper.setSize(pageSize);
        pageWrapper.setNumber(page);
        return Response.success(pageWrapper);
    }

    /**
     * 删除
     *
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    public Response<String> deleteEmbyItem(@PathVariable("id") Long id) {
        Metadata itemMetadata = embyClient.getItemMetadata(id);
        MediaInfo mediaInfo = mediaInfoService.getByInputPath(itemMetadata.getPath());
        if (mediaInfo == null) {
            mediaInfoService.createByMetadata(itemMetadata);
        }
        try (feign.Response response = embyClient.deleteItem(id.toString())) {
            log.info("deleteItem response: {}, {}", response.status(), response.reason());
            if (response.status() == 200 || response.status() == 204) {
                return Response.success();
            }
            return Response.fail(response.reason());
        } catch (Exception e) {
            return Response.fail(e.getMessage());
        }
    }

    @PostMapping("/{itemId}/tag")
    public Response<?> addTag(@RequestBody ItemTag tag, @PathVariable("itemId") Long itemId) {
        HashMap<String, List<ItemTag>> hashMap = new HashMap<>();
        ArrayList<ItemTag> list = new ArrayList<>(Collections.singletonList(tag));
        hashMap.put("Tags", list);
        try (feign.Response response = embyClient.addTags(itemId, hashMap)) {
            if (response.status() == 200) {
                return Response.success();
            } else {
                return Response.fail(response.reason(), response.status());
            }
        }
    }

    @GetMapping("/libraries")
    public Response<List<Library>> getLibraries() {
        PageWrapper<Library> libraries = embyClient.getLibraries();
        return Response.success(libraries.getItems());
    }

    /**
     * 查询tags
     *
     * @param name
     * @return
     */
    @GetMapping("/tags")
    public Response<List<ItemTag>> getTags(@RequestParam String name) {
        PageWrapper<ItemTag> pageWrapper = embyClient.getTags(name, 100L);
        return Response.success(pageWrapper.getItems());
    }

    /**
     * 更新tags
     *
     * @param itemId
     * @param metadataRequest
     * @return
     */
    @PostMapping("/{itemId}/tags")
    public Response<?> saveMediaTags(@PathVariable Long itemId, @RequestBody MetadataRequest metadataRequest) {
        Metadata metadata = embyClient.getItemMetadata(itemId);
        if (metadataRequest.getTagItems() == null) {
            return Response.fail("tags is null");
        }
        
        tagService.saveIfNotExist(metadataRequest.getTagItems().stream().map(ItemTag::getName).collect(Collectors.toSet()));
        
        metadataRequest.setProviderIds(metadata.getProviderIds());
        metadataRequest.setName(metadata.getSortName());
        metadataRequest.setId(metadata.getId());
        try (feign.Response response = embyClient.updateMetadata(itemId, metadataRequest)) {
            if (response.status() == 204) {
                return Response.success();
            }
            return Response.fail(response.reason(), response.status());
        }
    }

    @GetMapping("/detail/{itemId}")
    public Response<Metadata> getItemDetail(@PathVariable Long itemId) {
        String token = SecurityUtils.getUserInfo().map(EmbyUser::getThirdPartyToken).orElseThrow(() -> new BusinessException("Emby token is null"));
        Metadata itemMetadata = embyClient.getItemMetadata(itemId);
        itemMetadata.setStreamUrl(String.format("%s/Videos/%s/stream.mp4?static=true&api_key=%s",
                propertiesConfig.getEmbyServer(),
                itemMetadata.getId(),
                token));
        Optional.ofNullable(embyClient.getSpecialFeatures(itemId))
                .map(ResponseEntity::getBody)
                .ifPresent(itemMetadata::setSpecialFeatures);
        
        if (StringUtils.isNotBlank(itemMetadata.getPath())) {
            itemMetadata.setMediaInfo(mediaInfoService.getMediaDetail(itemMetadata.getPath()));
        }
        itemMetadata.setEmbyServer(propertiesConfig.getEmbyServer());
        return Response.success(itemMetadata);
    }

    @DeleteMapping("/active-encodings")
    public Response<String> deleteActiveEncodings() {
        String playSessionId = "";
        embyClient.deleteActiveEncodings(propertiesConfig.getDeviceId(), playSessionId);
        return Response.success();
    }

    @GetMapping("/player/{itemId}")
    public Response<String> getPlayer(@PathVariable Long itemId, boolean hls) {
        hls = false;
        EmbyUser userDetails = (EmbyUser) Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getPrincipal)
                .orElseThrow(() -> new BusinessException("emby认证信息获取异常！"));
        Metadata itemMetadata = embyClient.getItemMetadata(itemId);
        String serverUrl = propertiesConfig.getEmbyServer() + "/Videos/" + itemMetadata.getId();
        HashMap<String, String> params = new HashMap<>();
        params.put("X-Emby-Token", userDetails.getThirdPartyToken());
        params.put("DeviceId", propertiesConfig.getDeviceId());
        if (hls) {
            params.put("PlaySessionId", itemId.toString());
            params.put("DeviceId", propertiesConfig.getDeviceId());
            serverUrl += "/main.m3u8";
        } else {
            serverUrl += "/steam.mp4";
            params.put("Static", "true");
        }
        
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            builder.queryParam(entry.getKey(), entry.getValue());
        }
        String query = builder.build().getQuery();
        return Response.success(serverUrl + "?" + query);
    }
}
