package com.beet.backend.modules.{moduleName}.infrastructure.input.rest;

import com.beet.backend.modules.{moduleName}.application.dto.{Aggregate}Request;import com.beet.backend.modules.{moduleName}.application.dto.{Aggregate}Response;import com.beet.backend.modules.{moduleName}.application.handler.{Aggregate}Handler;

import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController @RequestMapping("/{moduleName}") // Global prefix /api/v1 is assumed
@RequiredArgsConstructor public class{Aggregate}Controller{

private final{Aggregate}Handler handler;

@PostMapping public ResponseEntity<ApiGenericResponse<{Aggregate}Response>>create(@Valid @RequestBody{Aggregate}Request request){return ResponseEntity.status(HttpStatus.CREATED).body(handler.create(request));}

@GetMapping("/{id}")public ResponseEntity<ApiGenericResponse<{Aggregate}Response>>getById(@PathVariable UUID id){return ResponseEntity.ok(handler.getById(id));}}
