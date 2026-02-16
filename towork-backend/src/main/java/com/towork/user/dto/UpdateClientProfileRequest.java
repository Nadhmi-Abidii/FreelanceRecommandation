package com.towork.user.dto;

import lombok.Data;

@Data
public class UpdateClientProfileRequest {
    private String firstName;
    private String lastName;
    private String phone;
    private String companyName;
    private String companySize;
    private String industry;
    private String website;
    private String address;
    private String city;
    private String country;
    private String postalCode;
    private String profilePicture;
    private String bio;
}
