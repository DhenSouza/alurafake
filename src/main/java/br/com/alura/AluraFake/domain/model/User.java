package br.com.alura.AluraFake.domain.model;

import br.com.alura.AluraFake.domain.enumeration.Role;
import br.com.alura.AluraFake.util.PasswordGeneration;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
public class User {

    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime createdAt = LocalDateTime.now();
    private String name;
    @Enumerated(EnumType.STRING)
    private Role role;
    private String email;
    // Por questões didáticas, a senha será armazenada em texto plano.
    @Setter
    @Column(length = 100)
    private String password;

    @Deprecated
    public User() {}

    public User(String name, String email, Role role, String password) {
        this.name = name;
        this.role = role;
        this.email = email;
        this.password = password;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    public boolean isInstructor() {
        return Role.INSTRUCTOR.equals(this.role);
    }

    public String getPassword() {
        return password;
    }
}
