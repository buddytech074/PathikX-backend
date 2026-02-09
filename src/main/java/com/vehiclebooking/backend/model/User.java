package com.vehiclebooking.backend.model;

import com.vehiclebooking.backend.model.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;

    @Column(unique = true, nullable = true) // Allow null for Google login users
    private String phoneNumber;

    private String email;

    private String password; // Can be empty if using only OTP, but typically needed for spring security or
                             // token

    @Enumerated(EnumType.STRING)
    private Role role;

    private String profileImageUrl;

    // For drivers
    private String documentUrl; // Treating this as License Front Image
    private String licenseBackUrl; // License Back Image
    private String licenseNumber;
    private boolean isVerified;

    @Builder.Default
    @Column(precision = 10, scale = 2)
    private java.math.BigDecimal walletBalance = java.math.BigDecimal.ZERO;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return phoneNumber != null ? phoneNumber : email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
