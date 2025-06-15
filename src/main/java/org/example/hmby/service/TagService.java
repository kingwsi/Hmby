package org.example.hmby.service;

import feign.Response;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.hmby.emby.ItemTag;
import org.example.hmby.emby.Metadata;
import org.example.hmby.emby.MovieItem;
import org.example.hmby.emby.PageWrapper;
import org.example.hmby.emby.request.EmbyItemRequest;
import org.example.hmby.emby.request.MetadataRequest;
import org.example.hmby.entity.Tag;
import org.example.hmby.exception.BusinessException;
import org.example.hmby.emby.EmbyFeignClient;
import org.example.hmby.repository.TagRepository;
import org.example.hmby.sceurity.SecurityUtils;
import org.example.hmby.vo.TagVO;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TagService {

    private final TagRepository tagRepository;

    private final EmbyFeignClient embyFeignClient;
    private final PgVectorStore vectorStore;

    public TagService(TagRepository tagRepository, EmbyFeignClient embyFeignClient, PgVectorStore vectorStore) {
        this.tagRepository = tagRepository;
        this.embyFeignClient = embyFeignClient;
        this.vectorStore = vectorStore;
    }


    public void create(Tag tag) {
        tagRepository.save(tag);
    }

    /**
     * 删除一条记录
     *
     * @param id
     * @return
     */
    public Integer removeById(Long id) {
        Tag tags = tagRepository.findById(id).orElseThrow(() -> new BusinessException("记录不存在！"));
        EmbyItemRequest embyItemRequest = new EmbyItemRequest();
        embyItemRequest.setLimit(9999L);
        embyItemRequest.setTags(tags.getName());
        AtomicReference<Integer> result = new AtomicReference<>(0);
        PageWrapper<MovieItem> pageWrapper = embyFeignClient.getItems(embyItemRequest);
        MetadataRequest metadataRequest = new MetadataRequest();
        pageWrapper.getItems().forEach(movieItem -> {
            Metadata metadata = embyFeignClient.getItemMetadata(movieItem.getId());
            if (metadata.getTagItems() != null) {
                List<ItemTag> newTag = metadata.getTagItems().stream().filter(o -> !tags.getName().equals(o.getName())).collect(Collectors.toList());
                metadataRequest.setTagItems(newTag);
                metadataRequest.setId(movieItem.getId());
                metadataRequest.setProviderIds(metadata.getProviderIds());
                metadataRequest.setName(metadata.getSortName());
                metadataRequest.setId(metadata.getId());
                try (Response response = embyFeignClient.updateMetadata(movieItem.getId(), metadataRequest)) {
                    if (response.status() != 204) {
                        throw new BusinessException(response.toString());
                    }
                    result.getAndSet(result.get() + 1);
                }
            }

        });
        tagRepository.deleteById(id);
        return result.get();
    }

    public Page<Tag> listOfPage(TagVO vo, Pageable pageable) {
        PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.Direction.DESC, "lastUpdateDate", "createdDate");
        if (vo.getEmbyMediaId() != null) {
            Metadata metadata = embyFeignClient.getItemMetadata(vo.getEmbyMediaId());
            if (metadata != null && metadata.getTagItems() != null) {
                vo.setNames(metadata.getTagItems().stream().map(ItemTag::getName).collect(Collectors.toList()));
            }
        }

        return tagRepository.findAll((Specification<Tag>) (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (vo.getNames() != null && !vo.getNames().isEmpty()) {
                predicates.add(criteriaBuilder.in(root.get("name")).value(vo.getNames()));
            }
            if (StringUtils.isNotBlank(vo.getName())) {
                predicates.add(criteriaBuilder.equal(root.get("name"), vo.getName()));
            }
            if (vo.getShow() != null) {
                predicates.add(criteriaBuilder.equal(root.get("show"), vo.getShow()));
            }
            if (vo.getCount() != null) {
                predicates.add(criteriaBuilder.ge(root.get("count"), vo.getCount()));
            }
            predicates.add(criteriaBuilder.equal(root.get("userId"), SecurityUtils.getUserId()));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    }

    public void updateShow(Long id, Boolean show) {
        tagRepository.findById(id)
                .map(tag -> {
                    tag.setShow(show);
                    return tagRepository.save(tag);
                });

    }

    public Optional<Tag> getById(Long id) {
        return tagRepository.findById(id);
    }

    public List<Tag> listByName(String name) {
        Tag probe = new Tag();
        if (name != null && !name.isEmpty()) {
            probe.setName(name);
        }

        ExampleMatcher matcher = ExampleMatcher
                .matching()
                .withMatcher("name", ExampleMatcher.GenericPropertyMatchers.contains().ignoreCase());
        Example<Tag> example = Example.of(probe, matcher);

        // 第 0 页，每页 limit 条，按 createTime 降序排序
        Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "lastUpdateDate"));
        Page<Tag> resultPage = tagRepository.findAll(example, pageable);
        List<Tag> tags = resultPage.getContent();

        if (tags.isEmpty()) {
            Tag tag = new Tag();
            tag.setName(name);
            tag.setId(0L);
            return List.of(tag);
        }
        return tags;
    }

    public int save(TagVO tagVO) {
        Tag tag = tagRepository.findById(tagVO.getId()).orElseThrow();
        String originalName = tag.getName();
        String newTagName = tagVO.getName();

        EmbyItemRequest embyItemRequest = new EmbyItemRequest();
        embyItemRequest.setLimit(9999L);
        embyItemRequest.setTags(originalName);
        AtomicInteger result = new AtomicInteger();
        PageWrapper<MovieItem> pageWrapper = embyFeignClient.getItems(embyItemRequest);
        MetadataRequest metadataRequest = new MetadataRequest();
        pageWrapper.getItems().parallelStream().forEach(movieItem -> {
            Metadata metadata = embyFeignClient.getItemMetadata(movieItem.getId());
            if (metadata.getTagItems() != null) {
                boolean needUpdate = false;
                for (ItemTag itemTag : metadata.getTagItems()) {
                    if (itemTag.getName().equals(originalName)) {
                        itemTag.setName(newTagName);
                        needUpdate = true;
                    }
                }
                if (needUpdate) {
                    metadataRequest.setTagItems(metadata.getTagItems());
                    metadataRequest.setId(movieItem.getId());
                    metadataRequest.setProviderIds(metadata.getProviderIds());
                    metadataRequest.setName(metadata.getSortName());
                    metadataRequest.setId(metadata.getId());
                    try (Response response = embyFeignClient.updateMetadata(movieItem.getId(), metadataRequest)) {
                        log.info("更新标签 id:{} {}", metadata.getId(), response.status());
                        result.getAndSet(result.get() + 1);
                    }
                }
            }
        });
        Tag newTag = tagRepository.findTagByName(newTagName);
        if (newTag == null) {
            tag.setUserId(SecurityUtils.getUserId());
            tagRepository.save(tag);
        } else {
            tagRepository.deleteById(tag.getId());
        }
        return result.get();
    }

    public void saveIfNotExist(Set<String> tags) {
        List<String> existTags = tagRepository.findByNameIn(tags).stream().map(Tag::getName).toList();
        tags.removeIf(existTags::contains);
        if (!tags.isEmpty()) {
            List<Tag> list = tags.stream()
                    .map(n -> new Tag(n, 0L, true))
                    .toList();
            tagRepository.saveAll(list);
        }
    }
    
    public void embeddingTags() {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        vectorStore.delete(b.eq("index", "tag").build());
        
        int pageSize = 100;
        PageWrapper<ItemTag> pageWrapper = embyFeignClient.getTags(null, 0, pageSize);
        for (int i = 0; i < pageWrapper.getTotalPages(); i++) {
            pageWrapper = embyFeignClient.getTags(null, i * pageSize, pageSize);
            List<Document> list = pageWrapper.getItems().stream()
                    .map(t -> new Document(t.getName(), Map.of("id", t.getId(), "index", "tag")))
                    .toList();
            vectorStore.add(list);
        }
    }
}
