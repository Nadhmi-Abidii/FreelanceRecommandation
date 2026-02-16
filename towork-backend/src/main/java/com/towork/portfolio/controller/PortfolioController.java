package com.towork.portfolio.controller;

import com.towork.config.MessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.towork.portfolio.service.PortfolioService;

@RestController
@RequestMapping("/api/freelancer")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping("/portfolio")
    @PreAuthorize("hasRole('FREELANCER')")
    public ResponseEntity<MessageResponse> getPortfolio(Authentication authentication) {
        var portfolio = portfolioService.getPortfolioForFreelancer(authentication.getName());
        return ResponseEntity.ok(MessageResponse.success("Portfolio fetched", portfolio));
    }
}
