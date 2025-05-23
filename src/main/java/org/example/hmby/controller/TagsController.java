package org.example.hmby.controller;

import org.example.hmby.Response;
import org.example.hmby.entity.Tag;
import org.example.hmby.service.TagService;
import org.example.hmby.vo.TagVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
public class TagsController {
    private final TagService tagService;

    public TagsController(TagService tagService) {
        this.tagService = tagService;
    }

    @PostMapping
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
    
    @GetMapping("/fetch")
    public Response<?> fetchTags(){
        tagService.embeddingTags();
        return Response.success();
    }
}
