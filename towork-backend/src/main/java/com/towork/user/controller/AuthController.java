package com.towork.user.controller;

import com.towork.config.MessageResponse;
import com.towork.user.dto.AuthResponse;
import com.towork.user.dto.LoginRequest;
import com.towork.user.entity.Client;
import com.towork.user.entity.Freelancer;
import com.towork.user.entity.Admin;
import com.towork.user.repository.ClientRepository;
import com.towork.user.repository.FreelancerRepository;
import com.towork.user.repository.AdminRepository;
import com.towork.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;
import com.towork.user.dto.RegisterClientRequest;
import com.towork.user.dto.RegisterFreelancerRequest;
import com.towork.user.dto.RegisterAdminRequest;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication endpoints")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final ClientRepository clientRepository;
    private final FreelancerRepository freelancerRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT token")
    public ResponseEntity<MessageResponse> login(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
            );

            // qui s'est authentifié ? (on retrouve le profil et on attribue le bon rôle)
            Optional<Admin> admin = adminRepository.findByEmail(loginRequest.getEmail());
            if (admin.isPresent()) {
                AuthResponse auth = buildAuthResponse(
                        admin.get().getEmail(),
                        admin.get().getId(),
                        admin.get().getFirstName(),
                        admin.get().getLastName(),
                        "ROLE_ADMIN",
                        admin.get().getPassword()
                );
                return ResponseEntity.ok(MessageResponse.success("Login successful", auth));
            }

            Optional<Client> client = clientRepository.findByEmail(loginRequest.getEmail());
            if (client.isPresent()) {
                AuthResponse auth = buildAuthResponse(
                        client.get().getEmail(),
                        client.get().getId(),
                        client.get().getFirstName(),
                        client.get().getLastName(),
                        "ROLE_CLIENT",
                        client.get().getPassword()
                );
                return ResponseEntity.ok(MessageResponse.success("Login successful", auth));
            }

            Optional<Freelancer> freelancer = freelancerRepository.findByEmail(loginRequest.getEmail());
            if (freelancer.isPresent()) {
                AuthResponse auth = buildAuthResponse(
                        freelancer.get().getEmail(),
                        freelancer.get().getId(),
                        freelancer.get().getFirstName(),
                        freelancer.get().getLastName(),
                        "ROLE_FREELANCER",
                        freelancer.get().getPassword()
                );
                return ResponseEntity.ok(MessageResponse.success("Login successful", auth));
            }

            return ResponseEntity.badRequest().body(MessageResponse.error("Invalid credentials"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(MessageResponse.error("Invalid credentials"));
        }
    }

    @PostMapping("/register/client")
    @Operation(summary = "Client registration", description = "Register a new client account")
    public ResponseEntity<MessageResponse> registerClient(@Valid @RequestBody RegisterClientRequest request) {
        if (emailExists(request.getEmail())) {
            return ResponseEntity.badRequest().body(MessageResponse.error("Email already in use"));
        }

        Client client = new Client();
        client.setFirstName(request.getFirstName());
        client.setLastName(request.getLastName());
        client.setEmail(request.getEmail());
        client.setPassword(passwordEncoder.encode(request.getPassword()));
        client.setPhone(request.getPhone());
        client.setAddress(request.getAddress());
        client.setCity(request.getCity());

        Client saved = clientRepository.save(client);

        AuthResponse auth = buildAuthResponse(
                saved.getEmail(), saved.getId(), saved.getFirstName(), saved.getLastName(),
                "ROLE_CLIENT", saved.getPassword()
        );
        return ResponseEntity.ok(MessageResponse.success("Registration successful", auth));
    }

    @PostMapping("/register/freelancer")
    @Operation(summary = "Freelancer registration", description = "Register a new freelancer account")
    public ResponseEntity<MessageResponse> registerFreelancer(@Valid @RequestBody RegisterFreelancerRequest request) {
        if (emailExists(request.getEmail())) {
            return ResponseEntity.badRequest().body(MessageResponse.error("Email already in use"));
        }

        Freelancer fr = new Freelancer();
        fr.setFirstName(request.getFirstName());
        fr.setLastName(request.getLastName());
        fr.setEmail(request.getEmail());
        fr.setPassword(passwordEncoder.encode(request.getPassword()));
        fr.setPhone(request.getPhone());
        fr.setHourlyRate(request.getHourlyRate());
        fr.setAvailability(request.getAvailability());
        fr.setAddress(request.getAddress());

        Freelancer saved = freelancerRepository.save(fr);

        AuthResponse auth = buildAuthResponse(
                saved.getEmail(), saved.getId(), saved.getFirstName(), saved.getLastName(),
                "ROLE_FREELANCER", saved.getPassword()
        );
        return ResponseEntity.ok(MessageResponse.success("Registration successful", auth));
    }

    /** ===== Nouveau: inscription d'un ADMIN ===== */
    @PostMapping("/register/admin")
    @Operation(summary = "Admin registration", description = "Register a new admin account (ROLE_ADMIN)")
    public ResponseEntity<MessageResponse> registerAdmin(@Valid @RequestBody RegisterAdminRequest request) {
        if (emailExists(request.getEmail())) {
            return ResponseEntity.badRequest().body(MessageResponse.error("Email already in use"));
        }

        Admin admin = new Admin();
        admin.setFirstName(request.getFirstName());
        admin.setLastName(request.getLastName());
        admin.setEmail(request.getEmail());
        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        admin.setPhone(request.getPhone());
        admin.setAddress(request.getAddress());

        Admin saved = adminRepository.save(admin);

        AuthResponse auth = buildAuthResponse(
                saved.getEmail(), saved.getId(), saved.getFirstName(), saved.getLastName(),
                "ROLE_ADMIN", saved.getPassword()
        );
        return ResponseEntity.ok(MessageResponse.success("Registration successful", auth));
    }

    private boolean emailExists(String email) {
        return adminRepository.existsByEmail(email)
                || clientRepository.findByEmail(email).isPresent()
                || freelancerRepository.findByEmail(email).isPresent();
    }

    private AuthResponse buildAuthResponse(String email, Long userId, String firstName, String lastName,
                                           String role, String encodedPassword) {
        UserDetails userDetails = User.withUsername(email)
                .password(encodedPassword)
                .authorities(role)
                .build();

        String token = jwtService.generateToken(userDetails);

        return new AuthResponse(
                token,
                "Bearer",
                userId,
                email,
                role,
                firstName,
                lastName
        );
    }
}
