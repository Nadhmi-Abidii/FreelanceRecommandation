package com.towork.security;
import com.towork.user.entity.Admin;
import com.towork.user.entity.Client;
import com.towork.user.entity.Freelancer;
import com.towork.user.repository.AdminRepository;
import com.towork.user.repository.ClientRepository;
import com.towork.user.repository.FreelancerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final ClientRepository clientRepository;
    private final FreelancerRepository freelancerRepository;
    private final AdminRepository adminRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        Admin admin = adminRepository.findByEmail(email).orElse(null);
        if (admin != null) {
            return User.withUsername(admin.getEmail())
                    .password(admin.getPassword()) // already encoded
                    .authorities("ROLE_ADMIN")
                    .build();
        }

        // Try to find as Client first
        Client client = clientRepository.findByEmail(email).orElse(null);
        if (client != null) {
            return new User(
                    client.getEmail(),
                    client.getPassword(),
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_CLIENT"))
            );
        }

        // Try to find as Freelancer
        Freelancer freelancer = freelancerRepository.findByEmail(email).orElse(null);
        if (freelancer != null) {
            return new User(
                    freelancer.getEmail(),
                    freelancer.getPassword(),
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_FREELANCER"))
            );
        }

        throw new UsernameNotFoundException("User not found with email: " + email);
    }
}
