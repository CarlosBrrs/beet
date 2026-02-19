package com.beet.backend.modules.{moduleName}.domain.exception;

import com.beet.backend.shared.domain.exception.ResourceAlreadyExistsException;

public class{Aggregate}AlreadyExistsException extends ResourceAlreadyExistsException{

private static final String ALREADY_EXISTS_TEMPLATE="{Aggregate} with %s already exists: %s";

// Add other specific templates as constants

private{Aggregate}AlreadyExistsException(String message){super(message); // Uses super(String message) from shared
                                                                         // exception
}

public static{Aggregate}AlreadyExistsException forField(String fieldName,String value){return new{Aggregate}AlreadyExistsException(String.format(ALREADY_EXISTS_TEMPLATE,fieldName,value));}}
