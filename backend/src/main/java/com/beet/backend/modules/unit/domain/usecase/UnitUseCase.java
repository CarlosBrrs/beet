package com.beet.backend.modules.unit.domain.usecase;

import com.beet.backend.modules.unit.domain.api.UnitServicePort;
import com.beet.backend.modules.unit.domain.model.UnitDomain;
import com.beet.backend.modules.unit.domain.spi.UnitPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UnitUseCase implements UnitServicePort {

    private final UnitPersistencePort persistencePort;

    @Override
    public List<UnitDomain> getAllUnits() {
        return persistencePort.findAllWithConversions();
    }
}
