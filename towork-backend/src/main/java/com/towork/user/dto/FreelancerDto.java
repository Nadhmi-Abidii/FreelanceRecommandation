package com.towork.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FreelancerDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String title;
    private String bio;
    private List<String> skills;
    private BigDecimal hourlyRate;
    private BigDecimal dailyRate;
    private String availability;
    private String address;
    private String city;
    private String country;
    private String postalCode;
    private String profilePicture;
    private String portfolioUrl;
    private String linkedinUrl;
    private String githubUrl;
    private LocalDate dateOfBirth;
    private String gender;
    private Boolean isVerified;
    private Boolean isAvailable;
    private BigDecimal rating;
    private Integer totalProjects;
    private BigDecimal successRate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
