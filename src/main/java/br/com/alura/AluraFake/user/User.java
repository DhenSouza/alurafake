package br.com.alura.AluraFake.user;

import br.com.alura.AluraFake.util.PasswordGeneration;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime createdAt = LocalDateTime.now();
    private String name;
    @Enumerated(EnumType.STRING)
    private Role role;
    private String email;
    // Por questões didáticas, a senha será armazenada em texto plano.
    private String password;

    @Deprecated
    public User() {}

    public User(String name, String email, Role role, String password) {
        this.name = name;
        this.role = role;
        this.email = email;
        this.password = password;
    }

    public User(String name, String email, Role role) {
        this(name, email, role, PasswordGeneration.generatePassword());
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
