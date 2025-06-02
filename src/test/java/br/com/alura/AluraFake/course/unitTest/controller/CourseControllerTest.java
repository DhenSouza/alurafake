package br.com.alura.AluraFake.course.unitTest.controller;

import br.com.alura.AluraFake.api.controller.CourseController;
import br.com.alura.AluraFake.api.dto.request.NewCourseDTO;
import br.com.alura.AluraFake.api.dto.response.CourseListItemDTO;
import br.com.alura.AluraFake.application.interfaces.CourseServiceInterface;
import br.com.alura.AluraFake.domain.enumeration.Role;
import br.com.alura.AluraFake.domain.enumeration.Status;
import br.com.alura.AluraFake.domain.model.Course;
import br.com.alura.AluraFake.domain.model.User;
import br.com.alura.AluraFake.domain.repository.UserRepository;
import br.com.alura.AluraFake.domain.service.AppUserDetailsService;
import br.com.alura.AluraFake.globalHandler.BusinessRuleException;
import br.com.alura.AluraFake.globalHandler.EntityNotFoundException;
import br.com.alura.AluraFake.globalHandler.GlobalExceptionHandler;
import br.com.alura.AluraFake.globalHandler.ResourceNotFoundException;
import br.com.alura.AluraFake.globalHandler.dto.ProblemType;
import br.com.alura.AluraFake.security.JwtAuthenticationFilter;
import br.com.alura.AluraFake.security.JwtTokenUtil;
import br.com.alura.AluraFake.security.SecurityConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(CourseController.class)
@Import({GlobalExceptionHandler.class, SecurityConfiguration.class, JwtTokenUtil.class, JwtAuthenticationFilter.class, AppUserDetailsService.class})
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CourseServiceInterface courseServiceMock;

    @MockBean
    private UserRepository userRepositoryMock;

    @Captor
    private ArgumentCaptor<NewCourseDTO> newCourseDTOCaptor;

    private final String errorBaseUri = "https://api.seusite.com/erros";

    private NewCourseDTO validNewCourseDTO;
    private Course createdCourse;
    private User instructor;

    @BeforeEach
    void setUp() {
        validNewCourseDTO = new NewCourseDTO();
        validNewCourseDTO.setTitle("Curso de Java Avançado");
        validNewCourseDTO.setDescription("Aprenda tópicos avançados");
        validNewCourseDTO.setEmailInstructor("instructor@example.com");

        instructor = new User("Taina", "Taina@email.com", Role.INSTRUCTOR, "senha123");

        createdCourse = new Course("Curso de Java Avançado", "Aprenda tópicos avançados", instructor);
        createdCourse.setId(1L);
        createdCourse.setStatus(Status.BUILDING);
        createdCourse.setPublishedAt(LocalDateTime.now());
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("POST /course/new: Should create course with valid DTO and return 201 Created")
    void createCourse_withValidDTO_shouldReturnCreated() throws Exception {
        // Arrange
        when(courseServiceMock.createNewCourse(any(NewCourseDTO.class))).thenReturn(createdCourse);

        // Act
        ResultActions resultActions = mockMvc.perform(post("/course/new")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validNewCourseDTO)));

        // Assert
        resultActions.andExpect(status().isOk());

        verify(courseServiceMock).createNewCourse(newCourseDTOCaptor.capture());
        assertThat(newCourseDTOCaptor.getValue().getTitle()).isEqualTo(validNewCourseDTO.getTitle());
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("POST /course/new: Should return 400 Bad Request when DTO is invalid")
    void createCourse_withInvalidDTO_shouldReturnBadRequest() throws Exception {
        // Arrange
        NewCourseDTO invalidDto = new NewCourseDTO();
        invalidDto.setTitle(null);
        invalidDto.setDescription("Desc");
        invalidDto.setEmailInstructor("invalid-email");

        // Act
        ResultActions resultActions = mockMvc.perform(post("/course/new")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)));

        // Assert
        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.type").value(errorBaseUri + ProblemType.DEFAULT_USER_MESSAGE_VALIDATION.getPath()))
                .andExpect(jsonPath("$.title").value(ProblemType.DEFAULT_USER_MESSAGE_VALIDATION.getTitle()))
                .andExpect(jsonPath("$.fields[0].name").exists());

        verify(courseServiceMock, never()).createNewCourse(any());
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("POST /course/new: Should return 404 Not Found when instructor is not found by service")
    void createCourse_whenInstructorNotFoundByService_shouldReturnNotFound() throws Exception {
        // Arrange
        String errorMessage = "Instrutor não encontrado com e-mail: " + validNewCourseDTO.getEmailInstructor();
        when(courseServiceMock.createNewCourse(any(NewCourseDTO.class)))
                .thenThrow(new EntityNotFoundException(errorMessage));

        // Act
        ResultActions resultActions = mockMvc.perform(post("/course/new")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validNewCourseDTO)));

        // Assert
        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.type").value(errorBaseUri + ProblemType.RESOURCE_NOT_FOUND.getPath()))
                .andExpect(jsonPath("$.title").value(ProblemType.RESOURCE_NOT_FOUND.getTitle()))
                .andExpect(jsonPath("$.detail").value(errorMessage));
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("GET /course/all: Should list courses and return 200 OK with detailed course list")
    void listAllCourses_whenCoursesExist_shouldReturnOkWithDetailedCourseList() throws Exception {
        // Arrange
        User instructor = new User("Nome Instrutor", "instrutor@example.com", Role.INSTRUCTOR, "password");

        Course course1 = new Course("Curso de Java Completo", "Aprenda Java do básico ao avançado", instructor);
        course1.setId(1L);
        course1.setStatus(Status.PUBLISHED);

        Course course2 = new Course("Spring Boot Essentials", "Domine o Spring Boot para APIs REST", instructor);
        course2.setId(2L);
        course2.setStatus(Status.BUILDING);

        CourseListItemDTO dto1 = new CourseListItemDTO(course1);
        CourseListItemDTO dto2 = new CourseListItemDTO(course2);
        List<CourseListItemDTO> expectedCourseDtoList = List.of(dto1, dto2);

        when(courseServiceMock.listAllCourses()).thenReturn(expectedCourseDtoList);

        // Act
        ResultActions resultActions = mockMvc.perform(get("/course/all")
                .accept(MediaType.APPLICATION_JSON));

        // Assert
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(expectedCourseDtoList.size()))
                .andExpect(jsonPath("$[0].id").value(dto1.getId()))
                .andExpect(jsonPath("$[0].title").value(dto1.getTitle()))
                .andExpect(jsonPath("$[0].description").value(dto1.getDescription()))
                .andExpect(jsonPath("$[0].status").value(dto1.getStatus().toString()))
                .andExpect(jsonPath("$[1].id").value(dto2.getId()))
                .andExpect(jsonPath("$[1].title").value(dto2.getTitle()))
                .andExpect(jsonPath("$[1].description").value(dto2.getDescription()))
                .andExpect(jsonPath("$[1].status").value(dto2.getStatus().toString()));

        verify(courseServiceMock).listAllCourses();
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("GET /course/all: Should return 200 OK with empty list when no courses exist")
    void listAllCourses_whenNoCoursesExist_shouldReturnOkWithEmptyList() throws Exception {
        // Arrange
        when(courseServiceMock.listAllCourses()).thenReturn(Collections.emptyList());

        // Act
        ResultActions resultActions = mockMvc.perform(get("/course/all")
                .accept(MediaType.APPLICATION_JSON));

        // Assert
        resultActions
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(courseServiceMock).listAllCourses();
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("POST /course/{id}/publish: Should publish course and return 201 Created")
    void publishCourse_withValidIdAndConditions_shouldReturnCreated() throws Exception {
        // Arrange
        Long courseIdToPublish = 1L;
        createdCourse.setStatus(Status.PUBLISHED);
        createdCourse.setPublishedAt(LocalDateTime.now());
        when(courseServiceMock.publish(courseIdToPublish)).thenReturn(createdCourse);

        // Act
        ResultActions resultActions = mockMvc.perform(post("/course/{id}/publish", courseIdToPublish));

        // Assert
        resultActions.andExpect(status().isCreated());
        verify(courseServiceMock).publish(courseIdToPublish);
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("POST /course/{id}/publish: Should return 404 Not Found if course to publish is not found")
    void publishCourse_whenCourseIdNotFound_shouldReturnNotFound() throws Exception {
        // Arrange
        Long nonExistentCourseId = 999L;
        String errorMessage = "Curso não encontrado com ID: " + nonExistentCourseId;
        when(courseServiceMock.publish(nonExistentCourseId)).thenThrow(new ResourceNotFoundException(errorMessage));

        // Act
        ResultActions resultActions = mockMvc.perform(post("/course/{id}/publish", nonExistentCourseId));

        // Assert
        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.type").value(errorBaseUri + ProblemType.RESOURCE_NOT_FOUND.getPath()))
                .andExpect(jsonPath("$.title").value(ProblemType.RESOURCE_NOT_FOUND.getTitle()))
                .andExpect(jsonPath("$.detail").value(errorMessage));
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("POST /course/{id}/publish: Should return 400 Bad Request if business rule is violated (e.g., status is not BUILDING)")
    void publishCourse_whenBusinessRuleViolated_shouldReturnBadRequest() throws Exception {
        // Arrange
        Long courseId = 1L;
        String courseTitleForTest = "Already Published Course for Test";

        createdCourse.setTitle(courseTitleForTest);
        createdCourse.setStatus(Status.PUBLISHED);

        String expectedDetailMessage = String.format(
                "O curso '%s' não pode ser publicado pois seu status é '%s'. Apenas cursos em 'BUILDING' são permitidos.",
                createdCourse.getTitle(),
                createdCourse.getStatus()
        );

        when(courseServiceMock.publish(courseId))
                .thenThrow(new BusinessRuleException(expectedDetailMessage));

        // Act
        ResultActions resultActions = mockMvc.perform(post("/course/{id}/publish", courseId)
                .contentType(MediaType.APPLICATION_JSON));

        // Assert
        String expectedTypeUri = errorBaseUri + ProblemType.INVALID_OPERATION.getPath();
        String expectedTitle = ProblemType.INVALID_OPERATION.getTitle();
        String expectedUserMessage = expectedDetailMessage;

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.type").value(expectedTypeUri))
                .andExpect(jsonPath("$.title").value(expectedTitle))
                .andExpect(jsonPath("$.detail").value(expectedDetailMessage))
                .andExpect(jsonPath("$.userMessage").value(expectedUserMessage))
                .andExpect(jsonPath("$.instance").value("/course/" + courseId + "/publish"))
                .andExpect(jsonPath("$.fields").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("POST /course/{id}/publish: Should return 500 Internal Server Error if unexpected error occurs in service")
    void publishCourse_whenServiceThrowsRuntimeException_shouldReturnInternalServerError() throws Exception {
        // Arrange
        Long courseId = 1L;
        when(courseServiceMock.publish(courseId)).thenThrow(new RuntimeException("Unexpected internal error"));

        // Act
        ResultActions resultActions = mockMvc.perform(post("/course/{id}/publish", courseId));

        // Assert
        resultActions
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(HttpStatus.INTERNAL_SERVER_ERROR.value()))
                .andExpect(jsonPath("$.type").value(errorBaseUri + ProblemType.UNEXPECTED_ERROR.getPath()))
                .andExpect(jsonPath("$.title").value(ProblemType.UNEXPECTED_ERROR.getTitle()));
    }

}
