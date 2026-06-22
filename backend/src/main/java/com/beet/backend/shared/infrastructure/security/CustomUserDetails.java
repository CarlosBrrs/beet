package com.beet.backend.shared.infrastructure.security;

import com.beet.backend.modules.user.domain.model.User;
import com.beet.backend.modules.user.domain.model.UserAccountStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

@Getter
public class CustomUserDetails implements UserDetails {

    private final UUID id;
    private final UUID ownerId; // null for owners, points to owner for employees
    private final String email;
    private final String password;
    private final UserAccountStatus accountStatus;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this.id = user.getId();
        this.ownerId = user.getOwnerId();
        this.email = user.getEmail();
        this.password = user.getPasswordHash();
        this.accountStatus = user.getAccountStatus() == null ? UserAccountStatus.ACTIVE : user.getAccountStatus();
        this.authorities = Collections.emptyList(); // Handles roles later
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return accountStatus == UserAccountStatus.ACTIVE;
    }
}
