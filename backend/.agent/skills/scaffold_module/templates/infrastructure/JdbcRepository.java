package com.beet.backend.modules.{moduleName}.infrastructure.output.persistence.jdbc.repository;

import com.beet.backend.modules.{moduleName}.infrastructure.output.persistence.jdbc.aggregate.{Aggregate}Aggregate;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository public interface{Aggregate}JdbcRepository extends CrudRepository<{Aggregate}Aggregate,UUID>,PagingAndSortingRepository<{Aggregate}Aggregate,UUID>{
// Add custom queries here
}
