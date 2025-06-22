package org.example.hmby.repository;

import org.example.hmby.entity.Param;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParamRepository extends PagingAndSortingRepository<Param, Long> {
    String findValueByParamCode(String paramCode);
}
