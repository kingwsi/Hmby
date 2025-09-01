package org.example.hmby.repository;

import org.example.hmby.entity.Config;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConfigRepository extends JpaRepository<Config, Long> {
    
    Config findConfigBy();

    Config findConfigByUserId(String userId);
}
