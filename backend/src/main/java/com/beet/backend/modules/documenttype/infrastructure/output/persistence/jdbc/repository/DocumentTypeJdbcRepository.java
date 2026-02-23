package com.beet.backend.modules.documenttype.infrastructure.output.persistence.jdbc.repository;

import com.beet.backend.modules.documenttype.infrastructure.output.persistence.jdbc.aggregate.DocumentTypeAggregate;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DocumentTypeJdbcRepository extends CrudRepository<DocumentTypeAggregate, UUID> {

    @Query("SELECT dt.id, dt.name, dt.description, dt.country_id " +
            "FROM document_types dt " +
            "JOIN countries c ON dt.country_id = c.id " +
            "WHERE c.country_code = :countryCode")
    List<DocumentTypeAggregate> findByCountryCode(@Param("countryCode") String countryCode);
}
