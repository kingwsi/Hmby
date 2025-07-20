package org.example.hmby.controller;

import org.example.hmby.Response;
import org.example.hmby.entity.Tag;
import org.example.hmby.sceurity.EmbyUser;
import org.example.hmby.sceurity.SecurityUtils;
import org.example.hmby.service.TagService;
import org.example.hmby.vo.TagVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/tags")
public class TagsController {
    private final TagService tagService;

    public TagsController(TagService tagService) {
        this.tagService = tagService;
    }

    @PutMapping
    public Response<Integer> save(@RequestBody TagVO tags) {
        return Response.success(tagService.save(tags));
    }

    @PutMapping("/show")
    public Response<Integer> updateShow(@RequestBody Tag tag) {
        tagService.updateShow(tag.getId(), tag.getShow());
        return Response.success();
    }

    @DeleteMapping("/{id}")
    public Response<Integer> deleteById(@PathVariable Long id) {
        tagService.removeById(id);
        return Response.success();
    }

    @GetMapping("/page")
    public Response<Page<Tag>> listOfPage(TagVO tagsVO, Pageable pageable) {
        return Response.success(tagService.listOfPage(tagsVO, pageable));
    }

    @GetMapping("/list")
    public Response<List<Tag>> listByName(String name) {
        return Response.success(tagService.listByName(name));
    }

    @PutMapping("/sync")
    public Response<List<Tag>> syncTags() {
        SecurityContext context = SecurityContextHolder.getContext();
        tagService.syncTags(context);
        return Response.success();
    }

    @PutMapping("/sync/{name}")
    public Response<List<Tag>> syncTagByName(@PathVariable String name) {
        tagService.syncTagByName(name);
        return Response.success();
    }
}
