package com.beet.backend.modules.{moduleName}.application.handler;

import com.beet.backend.modules.{moduleName}.application.dto.{Aggregate}Request;
import com.beet.backend.modules.{moduleName}.application.dto.{Aggregate}Response;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;

import java.util.UUID;

public interface {Aggregate}Handler {
    ApiGenericResponse<{Aggregate}Response> create({Aggregate}Request request);
    ApiGenericResponse<{Aggregate}Response> getById(UUID id);
}
