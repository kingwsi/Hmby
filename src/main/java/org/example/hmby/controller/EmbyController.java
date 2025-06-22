package org.example.hmby.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.hmby.Response;
import org.example.hmby.config.PropertiesConfig;
import org.example.hmby.emby.ItemTag;
import org.example.hmby.emby.Library;
import org.example.hmby.emby.Metadata;
import org.example.hmby.emby.MovieItem;
import org.example.hmby.emby.PageWrapper;
import org.example.hmby.emby.PlayerInfo;
import org.example.hmby.emby.request.EmbyItemRequest;
import org.example.hmby.emby.request.MetadataRequest;
import org.example.hmby.entity.Config;
import org.example.hmby.entity.MediaInfo;
import org.example.hmby.enumerate.ConfigKey;
import org.example.hmby.exception.BusinessException;
import org.example.hmby.emby.EmbyFeignClient;
import org.example.hmby.repository.ConfigRepository;
import org.example.hmby.sceurity.EmbyUser;
import org.example.hmby.sceurity.SimilarResult;
import org.example.hmby.sceurity.UserContextHolder;
import org.example.hmby.service.MediaInfoService;
import org.example.hmby.service.TagService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
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
    private final ObjectMapper objectMapper;
    private final ConfigRepository configRepository;

    public EmbyController(EmbyFeignClient embyClient, PropertiesConfig propertiesConfig, MediaInfoService mediaInfoService, TagService tagService, ObjectMapper objectMapper, ConfigRepository configRepository) {
        this.embyClient = embyClient;
        this.propertiesConfig = propertiesConfig;
        this.mediaInfoService = mediaInfoService;
        this.tagService = tagService;
        this.objectMapper = objectMapper;
        this.configRepository = configRepository;
    }

    @GetMapping("/page")
    public Response<Page<MovieItem>> page(EmbyItemRequest embyItemRequest) {
        embyItemRequest.setLimit(embyItemRequest.getSize());
        embyItemRequest.setStartIndex(embyItemRequest.getSize() * embyItemRequest.getPage() - embyItemRequest.getSize());
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
        pageWrapper.setSize(embyItemRequest.getSize());
        pageWrapper.setNumber(embyItemRequest.getPage());
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
        PageWrapper<ItemTag> pageWrapper = embyClient.getTags(name, 0, 100);
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
        Assert.notNull(metadataRequest.getTags(), "tags is null");

        tagService.saveIfNotExist(metadataRequest.getTags());

        metadataRequest.setProviderIds(metadata.getProviderIds());
        metadataRequest.setName(metadata.getSortName());
        metadataRequest.setId(metadata.getId());
        metadataRequest.setTagItems(metadataRequest.getTags().stream().map(ItemTag::new).collect(Collectors.toList()));
        try (feign.Response response = embyClient.updateMetadata(itemId, metadataRequest)) {
            if (response.status() == 204) {
                return Response.success();
            }
            return Response.fail(response.reason(), response.status());
        }
    }

    @GetMapping("/detail/{itemId}")
    public Response<Metadata> getItemDetail(@PathVariable Long itemId) {
        Metadata itemMetadata = embyClient.getItemMetadata(itemId);
        Optional.ofNullable(embyClient.getSpecialFeatures(itemId))
                .map(ResponseEntity::getBody)
                .ifPresent(itemMetadata::setSpecialFeatures);

        if (StringUtils.isNotBlank(itemMetadata.getPath())) {
            itemMetadata.setMediaInfo(mediaInfoService.getMediaDetail(itemMetadata.getPath()));
        }
        itemMetadata.setEmbyServer(propertiesConfig.getEmbyServer());
        return Response.success(itemMetadata);
    }

    @GetMapping("/similar/{itemId}")
    public Response<List<SimilarResult.Item>> similar(@PathVariable Long itemId) {
        ResponseEntity<SimilarResult> similarResponse = embyClient.similar(itemId, "BasicSyncInfo", UserContextHolder.getUserid(), 10);
        if (similarResponse.getBody() != null) {
            return Response.success(similarResponse.getBody().getItems());
        }
        return Response.success(null);
    }

    @GetMapping("/subtitle/detail/{embyId}/{language}")
    public Response<Metadata> getSubtitleDetail(@PathVariable Long embyId, @PathVariable String language) {
        Metadata itemMetadata = embyClient.getItemMetadata(embyId);
        itemMetadata.setEmbyServer(propertiesConfig.getEmbyServer());
        itemMetadata.setMediaInfo(mediaInfoService.getSubtitleMediaInfo(embyId));
        return Response.success(itemMetadata);
    }

    @DeleteMapping("/active-encodings")
    public Response<String> deleteActiveEncodings() {
        String playSessionId = "";
        embyClient.deleteActiveEncodings(propertiesConfig.getDeviceId(), playSessionId);
        return Response.success();
    }

    @GetMapping("/player/{itemId}")
    public Response<PlayerInfo> getPlayer(@PathVariable Long itemId) {
        EmbyUser userDetails = (EmbyUser) Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getPrincipal)
                .orElseThrow(() -> new BusinessException("emby认证信息获取异常！"));
        String embyServer = Optional.ofNullable(configRepository.findOneByKey(ConfigKey.emby_server))
                .map(Config::getVal)
                .orElseThrow(() -> new BusinessException(ConfigKey.emby_server + "未配置！"));
        Metadata itemMetadata = embyClient.getItemMetadata(itemId);

        Metadata.MediaSource mediaSource = Optional.of(itemMetadata)
                .map(Metadata::getMediaSources)
                .flatMap(o -> o.stream().findFirst()).orElseThrow(() -> new RuntimeException("Not Found Media Source " + itemId));

        List<Map<String, String>> subtitles = new ArrayList<>();

        PlayerInfo playerInfo = new PlayerInfo();
        playerInfo.setId(itemId);
        int index = 0;
        String subtitleUrl = embyServer + "/Videos/%s/%s/Subtitles/%s/Stream.vtt";

        for (Metadata.MediaStream mediaStream : mediaSource.getMediaStreams()) {
            if ("Subtitle".equals(mediaStream.getType())) {
                Map<String, String> subtitle = Map.of("label", mediaStream.getDisplayLanguage(),
                        "url", subtitleUrl.formatted(itemId, mediaSource.getId(), index),
                        "srclang", mediaStream.getLanguage());
                subtitles.add(subtitle);
            }
            index++;
        }
        playerInfo.setSubtitles(subtitles);

        if (itemMetadata.getImageTags() != null) {
            String thumbId = itemMetadata.getImageTags().get("Thumb");
            if (StringUtils.isNotBlank(thumbId)) {
                String thumbUrl = "%s/emby/Items/%s/Images/Thumb?maxWidth=700&quality=100".formatted(embyServer, thumbId);
                playerInfo.setThumbImage(thumbUrl);
            }
            String primaryId = itemMetadata.getImageTags().get("Primary");
            if (StringUtils.isNotBlank(primaryId)) {
                String primaryUrl = "%s/emby/Items/%s/Images/Primary?maxWidth=700&quality=100".formatted(embyServer, primaryId);
                playerInfo.setPrimaryImage(primaryUrl);
            }
        }
        String streamUrl = "%s/Videos/%s/steam.mp4?Static=true&X-Emby-Token=%s"
                .formatted(embyServer, itemId, userDetails.getThirdPartyToken());
        playerInfo.setStreamUrl(streamUrl);
        return Response.success(playerInfo);
    }
}
