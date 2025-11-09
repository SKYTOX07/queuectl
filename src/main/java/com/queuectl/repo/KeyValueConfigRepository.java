
package com.queuectl.repo;

import com.queuectl.domain.KeyValueConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KeyValueConfigRepository extends JpaRepository<KeyValueConfig, String> {
}
