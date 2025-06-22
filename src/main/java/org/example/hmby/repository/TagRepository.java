package org.example.hmby.repository;

import org.example.hmby.entity.Tag;
import org.example.hmby.vo.TagVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    Page<Tag> findAll(Specification<Tag> specification, Pageable pageable);

    Tag findTagByName(String name);

    List<Tag> findByNameIn(Collection<String> names);

    List<Tag> findAllByNameContainsOrderByUpdatedAtDesc(String name);
}
