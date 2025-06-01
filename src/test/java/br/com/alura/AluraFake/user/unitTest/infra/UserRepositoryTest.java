package br.com.alura.AluraFake.user.unitTest.infra;

import br.com.alura.AluraFake.domain.enumeration.Role;
import br.com.alura.AluraFake.domain.model.User;
import br.com.alura.AluraFake.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager; // Opcional, para controle mais fino
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest // Configura um ambiente de teste focado em JPA, incluindo um EntityManager e transacionalidade
@ActiveProfiles("test") // Garante que application-test.properties (com H2) seja carregado
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private static final String EXISTING_EMAIL     = "caio@alura.com.br";
    private static final String NON_EXISTENT_EMAIL = "teste@alura.com.br";
    private static final String USER_NAME          = "Caio";

    private User userToPersist;

    @BeforeEach
    void setUp() {

        userToPersist = new User(USER_NAME, EXISTING_EMAIL, Role.STUDENT, "teste123");
        userRepository.save(userToPersist);
    }

    @Test
    @DisplayName("findByEmail should return user when email exists")
    void findByEmailShouldReturnUserWhenEmailExists() {
        Optional<User> result = userRepository.findByEmail(EXISTING_EMAIL);

        assertThat(result).isPresent()
                .hasValueSatisfying(user -> {
                    assertThat(user.getId()).isEqualTo(userToPersist.getId());
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