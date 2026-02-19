package com.beet.backend.modules.{moduleName}.domain.usecase;

import com.beet.backend.modules.{moduleName}.domain.api.{Aggregate}ServicePort;import com.beet.backend.modules.{moduleName}.domain.model.{Aggregate}Domain;import com.beet.backend.modules.{moduleName}.domain.spi.{Aggregate}PersistencePort;import com.beet.backend.modules.{moduleName}.domain.exception.{Aggregate}NotFoundException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)public class{Aggregate}UseCase implements{Aggregate}ServicePort{

private final{Aggregate}PersistencePort persistencePort;
// You can inject other ServicePorts here (e.g., IngredientServicePort) for
// cross-domain logic

@Override @Transactional public{Aggregate}Domain create({Aggregate}Domain domain){
// Add business validation here
return persistencePort.save(domain);}

@Override public{Aggregate}Domain getById(UUID id){return persistencePort.findById(id).orElseThrow(()->{Aggregate}NotFoundException.forId(id));}}
