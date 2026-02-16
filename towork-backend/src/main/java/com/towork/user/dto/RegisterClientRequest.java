package com.towork.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterClientRequest {

      @NotBlank(message = "First name is required")
      private String firstName;

      @NotBlank(message = "Last name is required")
      private String lastName;

      @NotBlank(message = "Email is required")
      @Email(message = "Email should be valid")
      private String email;

      @NotBlank(message = "Password is required")
      private String password;

      private String phone;

      private String address;
      private String city;
}