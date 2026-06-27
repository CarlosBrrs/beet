package com.beet.backend.modules.menu.domain.spi;

import com.beet.backend.modules.menu.domain.model.MenuDomain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuPersistencePort {
    MenuDomain save(MenuDomain menu);

    MenuDomain update(MenuDomain menu);

    Optional<MenuDomain> findMenuById(UUID menuId);

    List<MenuDomain> findAllWithSubmenus(UUID restaurantId);

    boolean existsByNameInRestaurant(String name, UUID restaurantId);
}
