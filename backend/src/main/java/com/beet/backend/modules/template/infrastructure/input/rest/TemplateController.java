package com.beet.backend.modules.template.infrastructure.input.rest;
import com.beet.backend.modules.item.application.dto.ActivationRequest;
import com.beet.backend.modules.role.domain.model.*;
import com.beet.backend.modules.template.application.dto.*;
import com.beet.backend.modules.template.application.handler.TemplateHandler;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import com.beet.backend.shared.infrastructure.security.RequiresPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/restaurants/{restaurantId}/templates") @RequiredArgsConstructor
public class TemplateController {
 private final TemplateHandler handler;
 @GetMapping @RequiresPermission(module=PermissionModule.TEMPLATES,action=PermissionAction.VIEW)
 public ResponseEntity<ApiGenericResponse<PageResponse<TemplateResponse>>> list(
         @PathVariable UUID restaurantId,
         @RequestParam(defaultValue = "0") int page,
         @RequestParam(defaultValue = "10") int size,
         @RequestParam(required = false) String search){
     return ResponseEntity.ok(handler.findAll(restaurantId,page,size,search));
 }
 @PostMapping @RequiresPermission(module=PermissionModule.TEMPLATES,action=PermissionAction.CREATE)
 public ResponseEntity<ApiGenericResponse<TemplateResponse>> create(@PathVariable UUID restaurantId,@Valid @RequestBody CreateTemplateRequest request){return ResponseEntity.status(HttpStatus.CREATED).body(handler.createTemplate(restaurantId,request));}
 @GetMapping("/{templateId}") @RequiresPermission(module=PermissionModule.TEMPLATES,action=PermissionAction.VIEW)
 public ResponseEntity<ApiGenericResponse<TemplateResponse>> get(@PathVariable UUID restaurantId,@PathVariable UUID templateId){return ResponseEntity.ok(handler.getById(restaurantId,templateId));}
 @PutMapping("/{templateId}") @RequiresPermission(module=PermissionModule.TEMPLATES,action=PermissionAction.EDIT)
 public ResponseEntity<ApiGenericResponse<TemplateResponse>> update(@PathVariable UUID restaurantId,@PathVariable UUID templateId,@Valid @RequestBody CreateTemplateRequest request){return ResponseEntity.ok(handler.updateTemplate(restaurantId,templateId,request));}
 @PatchMapping("/{templateId}/activation") @RequiresPermission(module=PermissionModule.TEMPLATES,action=PermissionAction.EDIT)
 public ResponseEntity<ApiGenericResponse<TemplateResponse>> activate(@PathVariable UUID restaurantId,@PathVariable UUID templateId,@Valid @RequestBody ActivationRequest request){return ResponseEntity.ok(handler.setActive(restaurantId,templateId,request.isActive()));}
}
