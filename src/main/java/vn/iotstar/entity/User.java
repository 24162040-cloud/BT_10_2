package vn.iotstar.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 50)
    private String fullName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 500)
    private String images = "/images/avatar.svg";

    @Column(nullable = false)
    private String password;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    protected User() {}

    public User(String fullName, String email, String password) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
    }

    public Integer getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getImages() { return images; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @Override
    @JsonIgnore
    public String getPassword() { return password; }

    @Override
    @JsonIgnore
    public String getUsername() { return email; }

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(); }
}
