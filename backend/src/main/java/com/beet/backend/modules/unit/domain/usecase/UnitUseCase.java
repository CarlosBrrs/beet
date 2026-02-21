package com.beet.backend.modules.unit.domain.usecase;

import com.beet.backend.modules.unit.domain.api.UnitServicePort;
import com.beet.backend.modules.unit.domain.model.UnitDomain;
import com.beet.backend.modules.unit.domain.spi.UnitPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UnitUseCase implements UnitServicePort {

    private final UnitPersistencePort persistencePort;

    @Override
    public List<UnitDomain> getAllUnits() {
        return persistencePort.findAllWithConversions();
    }

    @Override
    public UnitDomain findById(UUID id) {
        return getAllUnits().stream()
                .filter(u -> u.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
}
