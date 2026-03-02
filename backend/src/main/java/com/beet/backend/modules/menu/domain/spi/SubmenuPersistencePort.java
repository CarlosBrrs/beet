package com.beet.backend.modules.menu.domain.spi;

import com.beet.backend.modules.menu.domain.model.SubmenuDomain;

import java.util.Optional;
import java.util.UUID;

public interface SubmenuPersistencePort {
    SubmenuDomain save(SubmenuDomain submenu);

    SubmenuDomain update(SubmenuDomain submenu);

    Optional<SubmenuDomain> findSubmenuById(UUID submenuId);

    boolean existsByNameInMenu(String name, UUID menuId);
}
