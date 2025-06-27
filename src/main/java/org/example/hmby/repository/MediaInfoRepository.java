package org.example.hmby.repository;

import org.example.hmby.entity.MediaInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface MediaInfoRepository extends JpaRepository<MediaInfo, Long>, JpaSpecificationExecutor<MediaInfo> {

    MediaInfo findByInputPath(String absolutePath);

    MediaInfo findByOutputPath(String outputPath);

    MediaInfo findByInputPathOrOutputPath(String inputPath, String outputPath);

    List<MediaInfo> findByEmbyIdIn(Collection<Long> embyIds);

    List<MediaInfo> findByOutputPathIn(Collection<String> outputPaths);
}
