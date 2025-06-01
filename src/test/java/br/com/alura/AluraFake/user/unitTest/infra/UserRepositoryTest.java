package br.com.alura.AluraFake.user.unitTest.infra;

import br.com.alura.AluraFake.domain.enumeration.Role;
import br.com.alura.AluraFake.domain.model.User;
import br.com.alura.AluraFake.domain.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Testcontainers
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(SpringExtension.class)
class UserRepositoryTest {

    @Container
    private static final MySQLContainer<?> mysqlContainer =
            new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                    .withDatabaseName("alurafake_testdb")
                    .withUsername("testuser")
                    .withPassword("testpass");

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mysqlContainer::getUsername);
        registry.add("spring.datasource.password", mysqlContainer::getPassword);
        registry.add("spring.datasource.driverClassName", () -> "com.mysql.cj.jdbc.Driver");

        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate"); // Ou "none"
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
    }

    @Autowired
    private UserRepository userRepository;

    private static final String EXISTING_EMAIL = "caio@alura.com.br";
    private static final String NON_EXISTENT_EMAIL = "teste@alura.com.br";
    private static final String USER_NAME = "Caio";

    @BeforeEach
    void setUp() {
        userRepository.deleteAllInBatch();

        User userToPersist = new User(USER_NAME, EXISTING_EMAIL, Role.STUDENT, "teste123");
        userRepository.saveAndFlush(userToPersist);
    }

    @Test
    @DisplayName("findByEmail should return user when email exists")
    void findByEmailShouldReturnUserWhenEmailExists() {
        Optional<User> result = userRepository.findByEmail(EXISTING_EMAIL);

        assertThat(result).isPresent()
                .hasValueSatisfying(user -> {
                    assertThat(user.getName()).isEqualTo(USER_NAME);
                    assertThat(user.getEmail()).isEqualTo(EXISTING_EMAIL);
                    assertThat(user.getRole()).isEqualTo(Role.STUDENT);
                });
    }

    @Test
    @DisplayName("findByEmail should return empty when email does not exist")
    void findByEmailShouldReturnEmptyWhenEmailDoesNotExist() {
        Optional<User> result = userRepository.findByEmail(NON_EXISTENT_EMAIL);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("existsByEmail should return true when email exists")
    void existsByEmailShouldReturnTrueWhenEmailExists() {
        boolean exists = userRepository.existsByEmail(EXISTING_EMAIL);
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByEmail should return false when email does not exist")
    void existsByEmailShouldReturnFalseWhenEmailDoesNotExist() {
        boolean exists = userRepository.existsByEmail(NON_EXISTENT_EMAIL);
        assertThat(exists).isFalse();
    }
}