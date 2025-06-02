package br.com.alura.AluraFake.infra;
import br.com.alura.AluraFake.api.dto.request.NewUserDTO;
import br.com.alura.AluraFake.api.dto.response.UserListItemDTO;
import br.com.alura.AluraFake.application.interfaces.UserServiceInterface;
import br.com.alura.AluraFake.domain.enumeration.Role;
import br.com.alura.AluraFake.domain.model.Course;
import br.com.alura.AluraFake.domain.model.User;
import br.com.alura.AluraFake.domain.repository.CourseRepository;
import br.com.alura.AluraFake.domain.repository.UserRepository;
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
import org.springframework.test.util.ReflectionTestUtils; // Para setar campos privados

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {


    @Mock
    private UserRepository userRepositoryMock;
    @Mock
    private CourseRepository courseRepositoryMock;
    @Mock
    private PasswordEncoder passwordEncoderMock; // <<< ADICIONE ESTE MOCK

    @Mock
    private UserServiceInterface userServiceMock;

    @InjectMocks
    private DataSeeder dataSeeder;

    @Captor
    private ArgumentCaptor<User> userArgumentCaptor;
    @Captor
    private ArgumentCaptor<Course> courseArgumentCaptor;

    private User pauloInstructor;

    @BeforeEach
    void setUp() {
        pauloInstructor = new User("Paulo", "paulo@alura.com.br", Role.INSTRUCTOR, "hashedPasswordForPaulo");
        pauloInstructor.setId(2L);
    }


    @Test
    @DisplayName("It should not do anything if the profile is 'dev' but there are already users present.")
    void run_whenProfileIsDevAndUsersAlreadyExist_shouldDoNothing() throws Exception {
        // Arrange
        ReflectionTestUtils.setField(dataSeeder, "activeProfile", "dev");

        when(userRepositoryMock.count()).thenReturn(1L);

        // Act
        dataSeeder.run();

        // Assert
        verify(userRepositoryMock, times(1)).count();


        verify(userServiceMock, never()).createUser(any(NewUserDTO.class));
        verify(courseRepositoryMock, never()).save(any(Course.class));
    }

    @Test
    @DisplayName("Should create users (with hashed passwords) and a default course if the profile is 'dev' and there are no users.")
    void run_whenProfileIsDevAndNoUsersExist_shouldCreateDefaultData() throws Exception {
        // Arrange
        ReflectionTestUtils.setField(dataSeeder, "activeProfile", "dev");
        when(userRepositoryMock.count()).thenReturn(0L);

        String plainPasswordCaio = "senha123";
        String hashedPasswordCaio = "hashed_senha123_para_caio";
        String plainPasswordPaulo = "senha321";
        String hashedPasswordPaulo = "hashed_senha321_para_paulo";

        when(passwordEncoderMock.encode(plainPasswordCaio)).thenReturn(hashedPasswordCaio);
        when(passwordEncoderMock.encode(plainPasswordPaulo)).thenReturn(hashedPasswordPaulo);

        ArgumentCaptor<List<User>> userListCaptor = ArgumentCaptor.forClass(List.class);
        when(userRepositoryMock.saveAll(userListCaptor.capture())).thenReturn(Collections.emptyList());

        when(courseRepositoryMock.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        dataSeeder.run();

        // Assert
        verify(userRepositoryMock).count();
        verify(passwordEncoderMock).encode(plainPasswordCaio);
        verify(passwordEncoderMock).encode(plainPasswordPaulo);
        verify(userRepositoryMock).saveAll(anyList());

        List<User> savedUsers = userListCaptor.getValue();
        assertThat(savedUsers).hasSize(2);

        User caioSalvo = savedUsers.stream()
                .filter(u -> "caio@alura.com.br".equals(u.getEmail())).findFirst().orElseThrow();
        User pauloSalvo = savedUsers.stream()
                .filter(u -> "paulo@alura.com.br".equals(u.getEmail())).findFirst().orElseThrow();

        assertThat(caioSalvo.getName()).isEqualTo("Caio");
        assertThat(caioSalvo.getPassword()).isEqualTo(hashedPasswordCaio);
        assertThat(caioSalvo.getRole()).isEqualTo(Role.STUDENT);

        assertThat(pauloSalvo.getName()).isEqualTo("Paulo");
        assertThat(pauloSalvo.getPassword()).isEqualTo(hashedPasswordPaulo);
        assertThat(pauloSalvo.getRole()).isEqualTo(Role.INSTRUCTOR);

        verify(courseRepositoryMock).save(courseArgumentCaptor.capture());
        Course savedCourse = courseArgumentCaptor.getValue();

        assertThat(savedCourse.getTitle()).isEqualTo("Java");
        assertThat(savedCourse.getDescription()).isEqualTo("Aprenda Java com Alura");

        assertThat(savedCourse.getInstructor()).isNotNull();
        assertThat(savedCourse.getInstructor().getEmail()).isEqualTo(pauloSalvo.getEmail());
    }
}
