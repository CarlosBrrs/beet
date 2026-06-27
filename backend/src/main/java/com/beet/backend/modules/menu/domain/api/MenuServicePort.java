package com.beet.backend.modules.menu.domain.api;

import com.beet.backend.modules.menu.domain.model.MenuDomain;
import com.beet.backend.modules.menu.domain.model.SubmenuDomain;

public interface MenuServicePort {
    MenuDomain createMenu(MenuDomain menu);

    MenuDomain updateMenu(MenuDomain menu);

    SubmenuDomain createSubmenu(SubmenuDomain submenu);

    SubmenuDomain updateSubmenu(SubmenuDomain submenu);
}
