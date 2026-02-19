package com.beet.backend.modules.{moduleName}.infrastructure.output.persistence.jdbc.adapter;

import com.beet.backend.modules.{moduleName}.domain.model.{Aggregate}Domain;import com.beet.backend.modules.{moduleName}.domain.spi.{Aggregate}PersistencePort;import com.beet.backend.modules.{moduleName}.infrastructure.output.persistence.jdbc.aggregate.{Aggregate}Aggregate;import com.beet.backend.modules.{moduleName}.infrastructure.output.persistence.jdbc.mapper.{Aggregate}AggregateMapper;import com.beet.backend.modules.{moduleName}.infrastructure.output.persistence.jdbc.repository.{Aggregate}JdbcRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component @RequiredArgsConstructor public class{Aggregate}JdbcAdapter implements{Aggregate}PersistencePort{

private final{Aggregate}JdbcRepository repository;private final{Aggregate}AggregateMapper mapper;

@Override public{Aggregate}Domain save({Aggregate}Domain domain){{Aggregate}Aggregate aggregate=mapper.toAggregate(domain);{Aggregate}Aggregate saved=repository.save(aggregate);return mapper.toDomain(saved);}

@Override public Optional<{Aggregate}Domain>findById(UUID id){return repository.findById(id).map(mapper::toDomain);}

@Override public boolean existsById(UUID id){return repository.existsById(id);}}
