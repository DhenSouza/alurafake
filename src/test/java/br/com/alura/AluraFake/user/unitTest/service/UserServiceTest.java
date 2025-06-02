package br.com.alura.AluraFake.user.unitTest.service;

import br.com.alura.AluraFake.api.dto.request.NewUserDTO;
import br.com.alura.AluraFake.api.dto.response.UserListItemDTO;
import br.com.alura.AluraFake.application.service.user.UserService;
import br.com.alura.AluraFake.domain.enumeration.Role;
import br.com.alura.AluraFake.domain.model.User;
import br.com.alura.AluraFake.domain.repository.UserRepository;
import br.com.alura.AluraFake.globalHandler.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepositoryMock;

    @Mock
    private PasswordEncoder passwordEncoderMock;

    @InjectMocks
    private UserService userService;

    @Captor
    private ArgumentCaptor<User> userArgumentCaptor;

    private NewUserDTO newUserDTO;
    private User userFromDTO;
    private String plainPassword;
    private String hashedPassword;

    @BeforeEach
    void setUp() {
        plainPassword = "password123";
        hashedPassword = "hashedPassword123";

        newUserDTO = new NewUserDTO();
        newUserDTO.setName("Test User");
        newUserDTO.setEmail("test@example.com");
        newUserDTO.setPassword(plainPassword);
        newUserDTO.setRole(Role.STUDENT);

        userFromDTO = new User(newUserDTO.getName(), newUserDTO.getEmail(), newUserDTO.getRole(), newUserDTO.getPassword());
    }

    @Test
    @DisplayName("createUser: Must create and save user with hashed password when email does not exist")
    void createUser_whenEmailDoesNotExist_shouldSaveUserWithHashedPassword() {
        // Arrange
        when(userRepositoryMock.existsByEmail(newUserDTO.getEmail())).thenReturn(false);
        when(passwordEncoderMock.encode(plainPassword)).thenReturn(hashedPassword);

        User savedUser = new User(newUserDTO.getName(), newUserDTO.getEmail(), newUserDTO.getRole(), hashedPassword);
        savedUser.setId(4L);
        when(userRepositoryMock.save(any(User.class))).thenReturn(savedUser);

        // Act
        User result = userService.createUser(newUserDTO);

        // Assert
        verify(userRepositoryMock).existsByEmail(newUserDTO.getEmail());

        verify(passwordEncoderMock).encode(plainPassword);

        verify(userRepositoryMock).save(userArgumentCaptor.capture());
        User userToSave = userArgumentCaptor.getValue();

        assertThat(userToSave.getName()).isEqualTo(newUserDTO.getName());
        assertThat(userToSave.getEmail()).isEqualTo(newUserDTO.getEmail());
        assertThat(userToSave.getRole()).isEqualTo(newUserDTO.getRole());
        assertThat(userToSave.getPassword()).isEqualTo(hashedPassword);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(4L);
        assertThat(result.getPassword()).isEqualTo(hashedPassword);
    }

    @Test
    @DisplayName("createUser: It should throw BusinessRuleException when email already exists")
    void createUser_whenEmailAlreadyExists_shouldThrowBusinessRuleException() {
        // Arrange
        when(userRepositoryMock.existsByEmail(newUserDTO.getEmail())).thenReturn(true);

        String expectedMessage = "A user is already registered with the email: " + newUserDTO.getEmail();

        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(newUserDTO))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage(expectedMessage);

        verify(passwordEncoderMock, never()).encode(anyString());
        verify(userRepositoryMock, never()).save(any(User.class));
    }

    @Test
    @DisplayName("listAllUsers: Should return a list of UserListItemDTO when users exist")
    void listAllUsers_whenUsersExist_shouldReturnListOfUserListItemDTO() {
        // Arrange
        User user1 = new User("Alice", "alice@example.com", Role.INSTRUCTOR, "hashedPass1"); user1.setId(1L);
        User user2 = new User("Bob", "bob@example.com", Role.STUDENT, "hashedPass2"); user2.setId(2L);
        List<User> usersFromDb = List.of(user1, user2);

        when(userRepositoryMock.findAll()).thenReturn(usersFromDb);

        // Act
        List<UserListItemDTO> result = userService.listAllUsers();

        // Assert
        verify(userRepositoryMock).findAll();
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);

        assertThat(result.get(0).getName()).isEqualTo("Alice");
        assertThat(result.get(0).getEmail()).isEqualTo("alice@example.com");
        assertThat(result.get(0).getRole()).isEqualTo(Role.INSTRUCTOR);

        assertThat(result.get(1).getName()).isEqualTo("Bob");
        assertThat(result.get(1).getEmail()).isEqualTo("bob@example.com");
        assertThat(result.get(1).getRole()).isEqualTo(Role.STUDENT);
    }

    @Test
    @DisplayName("listAllUsers: It should return an empty list when there are no users.")
    void listAllUsers_whenNoUsersExist_shouldReturnEmptyList() {
        // Arrange
        when(userRepositoryMock.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<UserListItemDTO> result = userService.listAllUsers();

        // Assert
        verify(userRepositoryMock).findAll();
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("listAllUsers: Should propagate exception if repository fails")
    void listAllUsers_whenRepositoryThrowsException_shouldPropagateException() {
        // Arrange
        String errorMessage = "Error accessing the database";
        when(userRepositoryMock.findAll()).thenThrow(new RuntimeException(errorMessage));

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            userService.listAllUsers();
        });

        assertThat(thrown.getMessage()).isEqualTo(errorMessage);
        verify(userRepositoryMock).findAll();
    }
}