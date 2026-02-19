package com.beet.backend.modules.{moduleName}.application.mapper;

import com.beet.backend.modules.{moduleName}.application.dto.{Aggregate}Request;import com.beet.backend.modules.{moduleName}.application.dto.{Aggregate}Response;import com.beet.backend.modules.{moduleName}.domain.model.{Aggregate}Domain;

import org.springframework.stereotype.Component;

@Component public class{Aggregate}ServiceMapper{

public{Aggregate}Domain toDomain({Aggregate}Request request){return{Aggregate}Domain.builder().name(request.name()) // Record
                                                                                                                    // accessor
.build();}

public{Aggregate}Response toResponse({Aggregate}Domain domain){return{Aggregate}Response.builder().id(domain.getId()).name(domain.getName()).build();}}
