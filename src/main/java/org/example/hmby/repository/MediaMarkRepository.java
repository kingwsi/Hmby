package org.example.hmby.repository;

import org.example.hmby.entity.MediaMark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediaMarkRepository extends JpaRepository<MediaMark, Long> {
    void deleteMediaMarksByMediaId(Long mediaId);

    List<MediaMark> findByMediaId(Long mediaId);
}
