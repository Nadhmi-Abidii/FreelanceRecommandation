package com.towork.wallet.controller;

import com.towork.config.MessageResponse;
import com.towork.exception.ResourceNotFoundException;
import com.towork.wallet.mapper.WalletMapper;
import com.towork.wallet.dto.WalletSnapshotDto;
import com.towork.wallet.entity.Wallet;
import com.towork.user.entity.Client;
import com.towork.user.entity.Freelancer;
import com.towork.user.repository.ClientRepository;
import com.towork.user.repository.FreelancerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.towork.wallet.service.PaymentService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WalletApiController {

    private final PaymentService paymentService;
    private final ClientRepository clientRepository;
    private final FreelancerRepository freelancerRepository;

    @GetMapping("/client/wallet/me")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<MessageResponse> getClientWallet(Authentication authentication) {
        Client client = clientRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        Wallet wallet = paymentService.getWalletByClient(client.getId());
        var txs = paymentService.getWalletTransactionsByClient(client.getId());
        WalletSnapshotDto snapshot = WalletMapper.toSnapshot(wallet, txs);
        return ResponseEntity.ok(MessageResponse.success("Wallet fetched", snapshot));
    }

    @GetMapping("/freelancer/wallet/me")
    @PreAuthorize("hasRole('FREELANCER')")
    public ResponseEntity<MessageResponse> getFreelancerWallet(Authentication authentication) {
        Freelancer freelancer = freelancerRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer not found"));
        Wallet wallet = paymentService.getWalletByFreelancer(freelancer.getId());
        var txs = paymentService.getWalletTransactionsByFreelancer(freelancer.getId());
        WalletSnapshotDto snapshot = WalletMapper.toSnapshot(wallet, txs);
        return ResponseEntity.ok(MessageResponse.success("Wallet fetched", snapshot));
    }
}
