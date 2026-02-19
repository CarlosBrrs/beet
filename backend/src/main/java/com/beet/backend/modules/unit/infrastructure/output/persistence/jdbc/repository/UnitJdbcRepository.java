package com.beet.backend.modules.unit.infrastructure.output.persistence.jdbc.repository;

import com.beet.backend.modules.unit.infrastructure.output.persistence.jdbc.aggregate.UnitAggregate;
import com.beet.backend.modules.unit.infrastructure.output.persistence.jdbc.aggregate.UnitConversionProjection;
import com.beet.backend.modules.unit.infrastructure.output.persistence.jdbc.mapper.UnitConversionRowMapper;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UnitJdbcRepository extends ListCrudRepository<UnitAggregate, UUID> {

    /**
     * Returns all units with their factorToBase resolved in a single JOIN.
     * Base units have no row in unit_conversions, so COALESCE defaults to 1.
     *
     * NOTE: unit_conversions has a composite PK (from_unit_id, to_unit_id),
     * so it cannot have its own CrudRepository (Spring Data JDBC requires
     * a single @Id). We resolve the data here via a JOIN instead.
     */
    @Query(value = """
               SELECT u.id,
                      u.name,
                      u.abbreviation,
                      u.type,
                      u.is_base,
                      COALESCE(uc.factor, 1) AS factor_to_base
                 FROM units u
            LEFT JOIN unit_conversions uc ON uc.from_unit_id = u.id
               """, rowMapperClass = UnitConversionRowMapper.class)
    List<UnitConversionProjection> findAllWithConversions();
}
