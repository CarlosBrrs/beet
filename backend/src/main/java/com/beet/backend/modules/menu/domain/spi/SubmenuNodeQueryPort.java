package com.beet.backend.modules.menu.domain.spi;

import com.beet.backend.modules.menu.domain.model.SubmenuNodeDomain;
import com.beet.backend.modules.menu.domain.model.SubmenuNodeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubmenuNodeQueryPort {
    List<SubmenuNodeDomain> findNodesBySubmenu(UUID restaurantId, UUID submenuId);

    Optional<SubmenuNodeDomain> findNodeById(UUID nodeId);

    SubmenuNodeDomain saveNode(UUID restaurantId, UUID submenuId, SubmenuNodeType nodeType,
            UUID referenceId, int sortOrder);

    void deleteNode(UUID restaurantId, UUID submenuId, UUID nodeId);
}
