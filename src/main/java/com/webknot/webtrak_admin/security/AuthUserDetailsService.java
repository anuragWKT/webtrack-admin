package com.webknot.webtrak_admin.security;

import com.webknot.webtrak_admin.client.AuthClient;
import com.webknot.webtrak_admin.dto.AuthUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthUserDetailsService implements UserDetailsService {

    private final AuthClient authClient;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        try {
            AuthUserResponse user = authClient.getUserByEmail(email);
            Set<SimpleGrantedAuthority> authorities = user.getRoles() == null
                    ? Collections.emptySet()
                    : user.getRoles().stream()
                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toSet());
            return new User(user.getEmail(), "", authorities);
        } catch (Exception ex) {
            throw new UsernameNotFoundException("User not found: " + email, ex);
        }
    }
}
