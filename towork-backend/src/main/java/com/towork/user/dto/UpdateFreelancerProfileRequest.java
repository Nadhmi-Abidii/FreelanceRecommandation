package com.towork.user.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class UpdateFreelancerProfileRequest {
    private String firstName;
    private String lastName;
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
    private Boolean isAvailable;
}
