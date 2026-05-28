package com.beet.backend.modules.menu.domain.spi;

import com.beet.backend.modules.menu.domain.model.SubmenuNodeDomain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubmenuNodeQueryPort {
    List<SubmenuNodeDomain> findNodesBySubmenu(UUID submenuId);

    Optional<SubmenuNodeDomain> findNodeById(UUID nodeId);
}
