package br.com.alura.AluraFake.user.unitTest.domain;

import br.com.alura.AluraFake.domain.enumeration.Role;
import br.com.alura.AluraFake.domain.model.User;
import br.com.alura.AluraFake.domain.repository.UserRepository;
import br.com.alura.AluraFake.domain.service.AppUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AppUserDetailsServiceTest {

    @Mock
    private UserRepository userRepositoryMock;

    @InjectMocks
    private AppUserDetailsService appUserDetailsService;

    private User sampleUser;
    private String existingEmail;
    private String nonExistentEmail;
    private String hashedPassword;

    @BeforeEach
    void setUp() {
        existingEmail = "test@example.com";
        nonExistentEmail = "notfound@example.com";
        hashedPassword = "hashedPassword123";

        sampleUser = new User();
        sampleUser.setEmail(existingEmail);
        sampleUser.setPassword(hashedPassword);
        sampleUser.setRole(Role.INSTRUCTOR);
    }

    @Test
    @DisplayName("loadUserByUsername: When the user exists, it must return the correct UserDetails.")
    void loadUserByUsername_whenUserExists_shouldReturnUserDetails() {
        // Arrange
        when(userRepositoryMock.findByEmail(existingEmail)).thenReturn(Optional.of(sampleUser));

        // Act
        UserDetails userDetails = appUserDetailsService.loadUserByUsername(existingEmail);

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(existingEmail);
        assertThat(userDetails.getPassword()).isEqualTo(hashedPassword);

        // Verifica as authorities/roles
        assertThat(userDetails.getAuthorities())
                .isNotNull()
                .hasSize(1)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_" + Role.INSTRUCTOR.name());

        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();

        verify(userRepositoryMock).findByEmail(existingEmail);
    }

    @Test
    @DisplayName("loadUserByUsername: When the user does not exist, it should throw UsernameNotFoundException")
    void loadUserByUsername_whenUserNotFound_shouldThrowUsernameNotFoundException() {
        // Arrange
        when(userRepositoryMock.findByEmail(nonExistentEmail)).thenReturn(Optional.empty());

        String expectedMessage = "User with email '" + nonExistentEmail + "' not found.";

        // Act & Assert
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            appUserDetailsService.loadUserByUsername(nonExistentEmail);
        });

        assertThat(exception.getMessage()).isEqualTo(expectedMessage);

        verify(userRepositoryMock).findByEmail(nonExistentEmail);
    }

    @Test
    @DisplayName("loadUserByUsername: When the user exists but the Role is null, it should throw a NullPointerException (in the current implementation)")
    void loadUserByUsername_whenUserExistsButRoleIsNull_shouldThrowNullPointerException() {
        // Arrange
        sampleUser.setRole(null);
        when(userRepositoryMock.findByEmail(existingEmail)).thenReturn(Optional.of(sampleUser));

        // Act & Assert
        assertThatThrownBy(() -> appUserDetailsService.loadUserByUsername(existingEmail))
                .isInstanceOf(NullPointerException.class);

        verify(userRepositoryMock).findByEmail(existingEmail);
    }

}
