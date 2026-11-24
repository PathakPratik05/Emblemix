package com.emblemix.team;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {
    List<Team> findByOrganizationId(UUID organizationId);
    Optional<Team> findByIdAndOrganizationId(UUID id, UUID organizationId);
    boolean existsByOrganizationIdAndName(UUID organizationId, String name);
    boolean existsByOrganizationIdAndNameAndIdNot(UUID organizationId, String name, UUID id);
}
