package br.com.alura.AluraFake.course.unitTest.service;

import br.com.alura.AluraFake.api.dto.request.NewCourseDTO;
import br.com.alura.AluraFake.api.dto.response.CourseListItemDTO;
import br.com.alura.AluraFake.application.service.course.CourseService;
import br.com.alura.AluraFake.application.service.course.validation.CourseContentValidation;
import br.com.alura.AluraFake.domain.enumeration.Role;
import br.com.alura.AluraFake.domain.enumeration.Status;
import br.com.alura.AluraFake.domain.model.Course;
import br.com.alura.AluraFake.domain.model.User;
import br.com.alura.AluraFake.domain.repository.CourseRepository;
import br.com.alura.AluraFake.domain.repository.UserRepository;
import br.com.alura.AluraFake.globalHandler.BusinessRuleException;
import br.com.alura.AluraFake.globalHandler.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepositoryMock;
    @Mock
    private UserRepository userRepositoryMock;
    @Mock
    private CourseContentValidation courseContentValidationMock;

    @InjectMocks
    private CourseService courseService;

    @Captor
    private ArgumentCaptor<Course> courseArgumentCaptor;

    private NewCourseDTO newCourseDTO;
    private User instructor;
    private Course existingCourse;

    @BeforeEach
    void setUp() {
        instructor = new User("Instrutor Teste", "instrutor@teste.com", Role.INSTRUCTOR, "senha");
        instructor.setId(10L);

        newCourseDTO = new NewCourseDTO();
        newCourseDTO.setTitle("Curso de Testes em Java");
        newCourseDTO.setDescription("Aprenda a testar com JUnit e Mockito");
        newCourseDTO.setEmailInstructor("instrutor@teste.com");

        existingCourse = new Course("Curso Existente", "Descrição", instructor);
        existingCourse.setId(1L);
        existingCourse.setStatus(Status.BUILDING);
    }

    @Test
    @DisplayName("createNewCourse: with valid DTO and valid instructor, it must save and return the course")
    void createNewCourse_withValidDtoAndInstructor_shouldSaveAndReturnCourse() {
        when(userRepositoryMock.findByEmail("instrutor@teste.com"))
                .thenReturn(Optional.of(instructor));
        when(courseRepositoryMock.save(any(Course.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Course result = courseService.createNewCourse(newCourseDTO);

        // Assert
        verify(userRepositoryMock).findByEmail("instrutor@teste.com");
        verify(courseRepositoryMock).save(courseArgumentCaptor.capture());

        Course savedCourse = courseArgumentCaptor.getValue();
        assertThat(savedCourse.getTitle()).isEqualTo("Curso de Testes em Java");
        assertThat(savedCourse.getDescription()).isEqualTo("Aprenda a testar com JUnit e Mockito");
        assertThat(savedCourse.getInstructor()).isEqualTo(instructor);
        assertThat(savedCourse.getStatus()).isEqualTo(Status.BUILDING);
        assertThat(savedCourse.getPublishedAt()).isNotNull();

        assertThat(result).isEqualTo(savedCourse);

    }

    @Test
    @DisplayName("createNewCourse: when instructor not found, must throw EntityNotFoundException")
    void createNewCourse_whenInstructorNotFound_shouldThrowEntityNotFoundException() {
        // Arrange
        when(userRepositoryMock.findByEmail(newCourseDTO.getEmailInstructor())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> courseService.createNewCourse(newCourseDTO))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Instructor not found with email: " + newCourseDTO.getEmailInstructor());

        verify(courseRepositoryMock, never()).save(any());
    }

    @Test
    @DisplayName("publishCourse: with a valid course in BUILDING and validations OK, it should publish")
    void publishCourse_withValidBuildingCourseAndValidations_shouldPublishCourse() {
        // Arrange
        Long courseId = 1L;
        existingCourse.setStatus(Status.BUILDING);

        when(courseRepositoryMock.findById(courseId)).thenReturn(Optional.of(existingCourse));

        doNothing().when(courseContentValidationMock).validateType(existingCourse);
        doNothing().when(courseContentValidationMock).validateTaskOrderIsContinuous(existingCourse);

        when(courseRepositoryMock.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));


        // Act
        Course publishedCourse = courseService.publish(courseId);

        // Assert
        verify(courseRepositoryMock).findById(courseId);
        verify(courseContentValidationMock).validateType(existingCourse);
        verify(courseContentValidationMock).validateTaskOrderIsContinuous(existingCourse);
        verify(courseRepositoryMock).save(courseArgumentCaptor.capture());

        Course savedCourse = courseArgumentCaptor.getValue();
        assertThat(savedCourse.getStatus()).isEqualTo(Status.PUBLISHED);
        assertThat(savedCourse.getPublishedAt()).isNotNull();
        assertThat(savedCourse.getPublishedAt()).isAfter(existingCourse.getCreatedAt());

        assertThat(publishedCourse).isEqualTo(savedCourse);
    }

    @Test
    @DisplayName("publishCourse: when the course status is not BUILDING, it should throw a BusinessRuleException")
    void publishCourse_whenCourseStatusIsNotBuilding_shouldThrowBusinessRuleException() {
        // Arrange
        Long courseId = 1L;
        existingCourse.setStatus(Status.PUBLISHED);
        existingCourse.setTitle("Curso Já Publicado");

        when(courseRepositoryMock.findById(courseId)).thenReturn(Optional.of(existingCourse));

        String expectedMessage = String.format(
                "The course '%s' cannot be published because its status is '%s'. Only courses in 'BUILDING' are allowed.",
                existingCourse.getTitle(),
                existingCourse.getStatus());

        // Act & Assert
        assertThatThrownBy(() -> courseService.publish(courseId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage(expectedMessage);

        verify(courseContentValidationMock, never()).validateType(any());
        verify(courseContentValidationMock, never()).validateTaskOrderIsContinuous(any());
        verify(courseRepositoryMock, never()).save(any());
    }

    @Test
    @DisplayName("publishCourse: when content type validation fails, it should throw BusinessRuleException.")
    void publishCourse_whenContentTypeValidationFails_shouldThrowBusinessRuleException() {
        // Arrange
        Long courseId = 1L;
        existingCourse.setStatus(Status.BUILDING);
        String validationErrorMessage = "The course does not contain all the necessary types of tasks.";

        when(courseRepositoryMock.findById(courseId)).thenReturn(Optional.of(existingCourse));
        doThrow(new BusinessRuleException(validationErrorMessage))
                .when(courseContentValidationMock).validateType(existingCourse);

        // Act & Assert
        assertThatThrownBy(() -> courseService.publish(courseId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage(validationErrorMessage);

        verify(courseContentValidationMock).validateType(existingCourse);
        verify(courseContentValidationMock, never()).validateTaskOrderIsContinuous(any());
        verify(courseRepositoryMock, never()).save(any());
    }

    @Test
    @DisplayName("publishCourse: when the validation of the continuous order of tasks fails, it should throw BusinessRuleException")
    void publishCourse_whenTaskOrderIsNotContinuous_shouldThrowBusinessRuleException() {
        // Arrange
        Long courseId = 4L;
        existingCourse.setStatus(Status.BUILDING);
        existingCourse.setId(courseId);

        if (existingCourse.getTitle() == null) {
            existingCourse.setTitle("Curso para Teste de Ordem");
        }


        String validationErrorMessage = String.format(
                "The order of the course activities is not continuous. Order X found in position Y, expected Z."
        );

        when(courseRepositoryMock.findById(courseId)).thenReturn(Optional.of(existingCourse));

        doNothing().when(courseContentValidationMock).validateType(existingCourse);

        doThrow(new BusinessRuleException(validationErrorMessage))
                .when(courseContentValidationMock).validateTaskOrderIsContinuous(existingCourse);

        // Act & Assert
        assertThatThrownBy(() -> courseService.publish(courseId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage(validationErrorMessage);

        verify(courseRepositoryMock).findById(courseId);
        verify(courseContentValidationMock).validateType(existingCourse);
        verify(courseContentValidationMock).validateTaskOrderIsContinuous(existingCourse);

        verify(courseRepositoryMock, never()).save(any(Course.class));

        assertThat(existingCourse.getStatus()).isEqualTo(Status.BUILDING);
        assertThat(existingCourse.getPublishedAt()).isNull();
    }

    @Test
    @DisplayName("listAllCourses: Should return a list of CourseListItemDTO when courses exist in the repository")
    void listAllCourses_whenCoursesExist_shouldReturnListOfCourseListItemDTO() {
        // Arrange
        User instructor = new User("Instrutor Teste", "instrutor@example.com", Role.INSTRUCTOR, "password");

        Course course1 = new Course("Curso de Java", "Fundamentos de Java", instructor);
        course1.setId(1L);
        course1.setStatus(Status.PUBLISHED);
        course1.setDescription("Descrição completa do curso de Java.");

        Course course2 = new Course("Curso de Spring", "Spring Boot do zero", instructor);
        course2.setId(2L);
        course2.setStatus(Status.BUILDING);
        course2.setDescription("Descrição completa do curso de Spring.");

        List<Course> coursesFromDb = List.of(course1, course2);

        when(courseRepositoryMock.findAll()).thenReturn(coursesFromDb);

        // Act
        List<CourseListItemDTO> resultDtoList = courseService.listAllCourses();

        // Assert
        verify(courseRepositoryMock).findAll();

        assertThat(resultDtoList).isNotNull();
        assertThat(resultDtoList).hasSize(2);

        CourseListItemDTO resultDto1 = resultDtoList.get(0);
        assertThat(resultDto1.getId()).isEqualTo(course1.getId());
        assertThat(resultDto1.getTitle()).isEqualTo(course1.getTitle());
        assertThat(resultDto1.getDescription()).isEqualTo(course1.getDescription());
        assertThat(resultDto1.getStatus()).isEqualTo(course1.getStatus());

        CourseListItemDTO resultDto2 = resultDtoList.get(1);
        assertThat(resultDto2.getId()).isEqualTo(course2.getId());
        assertThat(resultDto2.getTitle()).isEqualTo(course2.getTitle());
        assertThat(resultDto2.getDescription()).isEqualTo(course2.getDescription());
        assertThat(resultDto2.getStatus()).isEqualTo(course2.getStatus());
    }

    @Test
    @DisplayName("listAllCourses: Should return an empty list when there are no courses in the repository")
    void listAllCourses_whenNoCoursesExist_shouldReturnEmptyList() {
        // Arrange
        when(courseRepositoryMock.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<CourseListItemDTO> resultDtoList = courseService.listAllCourses();

        // Assert
        verify(courseRepositoryMock).findAll();

        assertThat(resultDtoList).isNotNull();
        assertThat(resultDtoList).isEmpty();
    }

    @Test
    @DisplayName("listAllCourses: Should propagate exception if the repository fails")
    void listAllCourses_whenRepositoryThrowsException_shouldPropagateException() {
        // Arrange
        String errorMessage = "Erro simulado ao acessar o banco de dados";
        when(courseRepositoryMock.findAll()).thenThrow(new RuntimeException(errorMessage));

        // Act & Assert
        RuntimeException thrownException = assertThrows(RuntimeException.class, () -> {
            courseService.listAllCourses();
        });

        assertThat(thrownException.getMessage()).isEqualTo(errorMessage);

        verify(courseRepositoryMock).findAll();
    }
}
