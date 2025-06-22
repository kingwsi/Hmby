package org.example.hmby.repository;

import org.example.hmby.entity.Config;
import org.example.hmby.enumerate.ConfigKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfigRepository extends JpaRepository<Config, Long> {
    Config findOneByKey(ConfigKey key);
}
