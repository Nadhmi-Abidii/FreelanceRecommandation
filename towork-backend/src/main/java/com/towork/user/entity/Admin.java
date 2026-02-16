package com.towork.user.entity;

import com.towork.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "admins", uniqueConstraints = {
        @UniqueConstraint(name = "uk_admin_email", columnNames = "email")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Admin extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, length = 180)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(length = 30)
    private String phone;

    @Column(length = 255)
    private String address;
}
