package org.example.hmby.repository;

import org.example.hmby.entity.Subtitle;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * description:  <br>
 * date: 2025/5/9 20:40 <br>
 */
@Repository
public interface SubtitleRepository extends JpaRepository<Subtitle, Long> {
    List<Subtitle> findAllByMediaId(Long mediaId, Sort age);

    @Query(nativeQuery = true, value = """
            select *
            from subtitles where sequence >= :start AND sequence <= :end AND media_id = :mediaId ORDER BY sequence
            """)
    List<Subtitle> findChunks(Long mediaId, int start, int end);

    long countByMediaId(Long mediaId);
}
