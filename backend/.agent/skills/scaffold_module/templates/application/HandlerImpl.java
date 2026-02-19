package com.beet.backend.modules.{moduleName}.application.handler;

import com.beet.backend.modules.{moduleName}.application.dto.{Aggregate}Request;import com.beet.backend.modules.{moduleName}.application.dto.{Aggregate}Response;import com.beet.backend.modules.{moduleName}.application.mapper.{Aggregate}ServiceMapper;import com.beet.backend.modules.{moduleName}.domain.api.{Aggregate}ServicePort;import com.beet.backend.modules.{moduleName}.domain.model.{Aggregate}Domain;

import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component @RequiredArgsConstructor public class{Aggregate}HandlerImpl implements{Aggregate}Handler{

// You can inject multiple service ports here for same domain (e.g., specific UseCases)
private final{Aggregate}ServicePort servicePort;private final{Aggregate}ServiceMapper mapper;

@Override public ApiGenericResponse<{Aggregate}Response>create({Aggregate}Request request){{Aggregate}Domain domain=mapper.toDomain(request);{Aggregate}Domain created=servicePort.create(domain);return ApiGenericResponse.success(mapper.toResponse(created));}

@Override public ApiGenericResponse<{Aggregate}Response>getById(UUID id){{Aggregate}Domain found=servicePort.getById(id);return ApiGenericResponse.success(mapper.toResponse(found));}}
