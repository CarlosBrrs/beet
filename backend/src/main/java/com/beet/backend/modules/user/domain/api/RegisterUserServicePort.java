package com.beet.backend.modules.user.domain.api;

import com.beet.backend.modules.user.domain.model.User;

public interface RegisterUserServicePort {

    User register(User user);

}
