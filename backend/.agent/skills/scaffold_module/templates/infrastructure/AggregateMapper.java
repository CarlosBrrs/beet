package com.beet.backend.modules.{moduleName}.infrastructure.output.persistence.jdbc.mapper;

import com.beet.backend.modules.{moduleName}.domain.model.{Aggregate}Domain;import com.beet.backend.modules.{moduleName}.infrastructure.output.persistence.jdbc.aggregate.{Aggregate}Aggregate;

import org.springframework.stereotype.Component;

@Component public class{Aggregate}AggregateMapper{

public{Aggregate}Domain toDomain({Aggregate}Aggregate aggregate){if(aggregate==null)return null;return{Aggregate}Domain.builder().id(aggregate.getId()).name(aggregate.getName()).build();}

public{Aggregate}Aggregate toAggregate({Aggregate}Domain domain){if(domain==null)return null;return{Aggregate}Aggregate.builder().id(domain.getId()).name(domain.getName()).build();}}
