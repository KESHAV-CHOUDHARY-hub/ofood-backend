package com.ofood.security.service;

import com.ofood.auth.model.User;
import com.ofood.auth.repository.UserRepository;
import com.ofood.role.Role;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override

    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> maybe = userRepository.findByEmail(username.toLowerCase());
        User user = maybe.orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String status = user.getStatus() == null ? "INACTIVE" : user.getStatus();

        Collection<GrantedAuthority> authorities = user.getRoles().stream()
                .map(Role::getName)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .accountLocked("SUSPENDED".equalsIgnoreCase(status)
                        || (user.getLockUntil() != null && user.getLockUntil().isAfter(java.time.Instant.now())))
                .disabled(!"ACTIVE".equalsIgnoreCase(status))
                .build();
    }
}
