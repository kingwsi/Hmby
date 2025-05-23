package org.example.hmby.emby;

import feign.Response;
import org.example.hmby.config.EmbyFeignClientConfig;
import org.example.hmby.emby.request.EmbyItemRequest;
import org.example.hmby.emby.request.MetadataRequest;
import org.example.hmby.emby.request.UpdateItemRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;

@FeignClient(name = "emby-server", url = "${hmby.config.emby-server}", configuration = EmbyFeignClientConfig.class)
public interface EmbyFeignClient {

    /**
     * 获取列表
     * @param embyItemRequest
     * @return
     */
    @GetMapping(value = "/emby/Users/USER_ID/Items")
    PageWrapper<MovieItem> getItems(@SpringQueryMap EmbyItemRequest embyItemRequest);

    /**
     * 获取详细信息
     * @param id
     * @return
     */
    @GetMapping(value = "/emby/Users/USER_ID/Items/{id}")
    Metadata getItemMetadata(@PathVariable("id") Long id);

    @GetMapping(value = "/emby/Users/USER_ID/Items/{id}/SpecialFeatures")
    ResponseEntity<List<SpecialFeature>> getSpecialFeatures(@PathVariable("id") Long id);

    @PostMapping("/emby/Items/{id}")
    Response updateMetadata(@PathVariable Long id, @RequestBody MetadataRequest request);

    @PostMapping("/Items/{id}/Tags/Add")
    Response addTags(@PathVariable("id") Long id, @RequestBody HashMap<String, List<ItemTag>> tags);

    @GetMapping("/emby/Users/USER_ID/Views")
    PageWrapper<Library> getLibraries();

    /**
     * 删除文件
     * @return
     */
    @DeleteMapping("/emby/Items/{id}")
    Response deleteItem(@PathVariable String id);

    /**
     * 获取所有
     * @return
     */
    @GetMapping("/Tags")
    PageWrapper<ItemTag> getTags(@RequestParam("SearchTerm") String searchTerm, @RequestParam(value = "StartIndex", defaultValue = "0") Integer startIndex, @RequestParam(value = "Limit", defaultValue = "15") Integer limit);

    /**
     * 新增文件
     * @param request
     * @return
     */
    @PostMapping("/Library/Media/Updated")
    Response updateItem(@RequestBody UpdateItemRequest request);

    @PostMapping(value = "/emby/Users/authenticatebyname?X-Emby-Client=Emby%20homest&X-Emby-Device-Name=Home-st&X-Emby-Device-Id=999&X-Emby-Client-Version=1.0",
            consumes = "application/x-www-form-urlencoded;charset=UTF-8")
    ResponseEntity<EmbyAuthResult> auth(String params);

    @DeleteMapping("/Videos/ActiveEncodings")
    Response deleteActiveEncodings(@RequestParam(value = "DeviceId") String deviceId, @RequestParam("PlaySessionId") String playSessionId);
}
