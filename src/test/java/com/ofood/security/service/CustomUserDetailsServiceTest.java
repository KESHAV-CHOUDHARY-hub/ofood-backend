package com.ofood.security.service;

import com.ofood.auth.model.User;
import com.ofood.role.Role;
import com.ofood.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CustomUserDetailsServiceTest {

    private UserRepository userRepository;
    private CustomUserDetailsService service;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        service = new CustomUserDetailsService(userRepository);
    }

    private User makeUser(String email, String status, Set<Role> roles) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail(email);
        u.setPasswordHash(new BCryptPasswordEncoder(12).encode("password"));
        u.setStatus(status);
        u.setCreatedAt(Instant.now());
        u.setUpdatedAt(Instant.now());
        u.setRoles(roles);
        return u;
    }

    @Test
    void loadUser_success() {
        User u = makeUser("alice@example.com", "ACTIVE", Set.of(new Role(UUID.randomUUID(), "ROLE_CUSTOMER")));
        Mockito.when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(u));

        UserDetails ud = service.loadUserByUsername("alice@example.com");
        assertNotNull(ud);
        assertEquals("alice@example.com", ud.getUsername());
        assertTrue(ud.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER")));
    }

    @Test
    void loadUser_unknown() {
        Mockito.when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("bob@example.com"));
    }

    @Test
    void loadUser_inactive() {
        User u = makeUser("c@example.com", "INACTIVE", Set.of(new Role(UUID.randomUUID(), "ROLE_CUSTOMER")));
        Mockito.when(userRepository.findByEmail("c@example.com")).thenReturn(Optional.of(u));
        assertFalse(service.loadUserByUsername("c@example.com").isEnabled());
    }

    @Test
    void loadUser_suspended() {
        User u = makeUser("d@example.com", "SUSPENDED", Set.of(new Role(UUID.randomUUID(), "ROLE_CUSTOMER")));
        Mockito.when(userRepository.findByEmail("d@example.com")).thenReturn(Optional.of(u));
        assertFalse(service.loadUserByUsername("d@example.com").isAccountNonLocked());
    }

    @Test
    void passwordEncoder_matches() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        String raw = "secret123";
        String hashed = encoder.encode(raw);
        assertTrue(encoder.matches(raw, hashed));
    }
}
