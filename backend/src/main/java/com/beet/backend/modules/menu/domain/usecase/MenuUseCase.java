package com.beet.backend.modules.menu.domain.usecase;

import com.beet.backend.modules.menu.domain.api.MenuServicePort;
import com.beet.backend.modules.menu.domain.exception.MenuAlreadyExistsException;
import com.beet.backend.modules.menu.domain.exception.MenuNotFoundException;
import com.beet.backend.modules.menu.domain.exception.SubmenuAlreadyExistsException;
import com.beet.backend.modules.menu.domain.exception.SubmenuNotFoundException;
import com.beet.backend.modules.menu.domain.model.MenuDomain;
import com.beet.backend.modules.menu.domain.model.SubmenuDomain;
import com.beet.backend.modules.menu.domain.spi.MenuPersistencePort;
import com.beet.backend.modules.menu.domain.spi.SubmenuPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuUseCase implements MenuServicePort {

    private final MenuPersistencePort menuPersistencePort;
    private final SubmenuPersistencePort submenuPersistencePort;

    @Override
    @Transactional
    public MenuDomain createMenu(MenuDomain menu) {
        if (menuPersistencePort.existsByNameInRestaurant(menu.getName(), menu.getRestaurantId())) {
            throw MenuAlreadyExistsException.forName(menu.getName());
        }
        return menuPersistencePort.save(menu);
    }

    @Override
    @Transactional
    public MenuDomain updateMenu(MenuDomain updatedMenu) {
        MenuDomain existing = menuPersistencePort.findMenuById(updatedMenu.getId())
                .orElseThrow(() -> MenuNotFoundException.forId(updatedMenu.getId()));

        if (!existing.getName().equalsIgnoreCase(updatedMenu.getName()) &&
                menuPersistencePort.existsByNameInRestaurant(updatedMenu.getName(), existing.getRestaurantId())) {
            throw MenuAlreadyExistsException.forName(updatedMenu.getName());
        }

        existing.setName(updatedMenu.getName());
        existing.setDescription(updatedMenu.getDescription());

        return menuPersistencePort.update(existing);
    }

    @Override
    @Transactional
    public SubmenuDomain createSubmenu(SubmenuDomain submenu) {
        if (!menuPersistencePort.findMenuById(submenu.getMenuId()).isPresent()) {
            throw MenuNotFoundException.forId(submenu.getMenuId());
        }

        if (submenuPersistencePort.existsByNameInMenu(submenu.getName(), submenu.getMenuId())) {
            throw SubmenuAlreadyExistsException.forName(submenu.getName());
        }
        return submenuPersistencePort.save(submenu);
    }

    @Override
    @Transactional
    public SubmenuDomain updateSubmenu(SubmenuDomain updatedSubmenu) {
        SubmenuDomain existing = submenuPersistencePort.findSubmenuById(updatedSubmenu.getId())
                .orElseThrow(() -> SubmenuNotFoundException.forId(updatedSubmenu.getId()));

        if (!existing.getName().equalsIgnoreCase(updatedSubmenu.getName()) &&
                submenuPersistencePort.existsByNameInMenu(updatedSubmenu.getName(), existing.getMenuId())) {
            throw SubmenuAlreadyExistsException.forName(updatedSubmenu.getName());
        }

        existing.setName(updatedSubmenu.getName());
        existing.setDescription(updatedSubmenu.getDescription());
        existing.setSortOrder(updatedSubmenu.getSortOrder());

        return submenuPersistencePort.update(existing);
    }
}
