package com.towork.wallet.controller;

import com.towork.config.MessageResponse;
import com.towork.exception.ResourceNotFoundException;
import com.towork.user.entity.Client;
import com.towork.user.entity.Freelancer;
import com.towork.user.repository.ClientRepository;
import com.towork.user.repository.FreelancerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.towork.wallet.dto.RechargeRequest;
import com.towork.wallet.entity.Wallet;
import com.towork.wallet.entity.WalletTransaction;
import com.towork.wallet.service.PaymentService;

@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final PaymentService paymentService;
    private final ClientRepository clientRepository;
    private final FreelancerRepository freelancerRepository;

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('CLIENT','FREELANCER')")
    public ResponseEntity<MessageResponse> getMyWallet(Authentication authentication) {
        String email = authentication.getName();
        boolean isFreelancer = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_FREELANCER".equalsIgnoreCase(a.getAuthority()));

        Map<String, Object> payload = new HashMap<>();
        if (isFreelancer) {
            Freelancer freelancer = freelancerRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found"));
            Wallet wallet = paymentService.getWalletByFreelancer(freelancer.getId());
            List<WalletTransaction> txs = paymentService.getWalletTransactionsByFreelancer(freelancer.getId());
            payload.put("wallet", wallet);
            payload.put("transactions", txs);
        } else {
            Client client = clientRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
            Wallet wallet = paymentService.getWalletByClient(client.getId());
            List<WalletTransaction> txs = paymentService.getWalletTransactionsByClient(client.getId());
            payload.put("wallet", wallet);
            payload.put("transactions", txs);
        }

        return ResponseEntity.ok(MessageResponse.success("Wallet retrieved successfully", payload));
    }

    @PostMapping("/recharge")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<MessageResponse> recharge(Authentication authentication,
                                                    @RequestBody RechargeRequest request) {
        Client client = clientRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        Wallet wallet = paymentService.rechargeClientWallet(client.getId(), request.getAmount(),
                request.getDescription());
        Map<String, Object> payload = new HashMap<>();
        payload.put("wallet", wallet);
        payload.put("transactions", paymentService.getWalletTransactionsByClient(client.getId()));
        return ResponseEntity.ok(MessageResponse.success("Wallet recharged successfully", payload));
    }
}
