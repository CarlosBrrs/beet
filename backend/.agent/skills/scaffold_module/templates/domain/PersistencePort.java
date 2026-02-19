package com.beet.backend.modules.{moduleName}.domain.spi;

import com.beet.backend.modules.{moduleName}.domain.model.{Aggregate}Domain;

import java.util.UUID;
import java.util.Optional;

public interface{Aggregate}PersistencePort{{Aggregate}Domain save({Aggregate}Domain domain);Optional<{Aggregate}Domain>findById(UUID id);boolean existsById(UUID id);}
