package com.beet.backend.modules.{moduleName}.domain.exception;

import com.beet.backend.shared.domain.exception.ResourceNotFoundException;

import java.util.UUID;

public class{Aggregate}NotFoundException extends ResourceNotFoundException{

private static final String ID_NOT_FOUND_TEMPLATE="{Aggregate} not found with id: %s";
// Add other templates if needed

private{Aggregate}NotFoundException(String message){super(message);}

public static{Aggregate}NotFoundException forId(UUID id){return new{Aggregate}NotFoundException(String.format(ID_NOT_FOUND_TEMPLATE,id));}}
