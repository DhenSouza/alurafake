package br.com.alura.AluraFake.course.unitTest;

import br.com.alura.AluraFake.api.controller.CourseController;
import br.com.alura.AluraFake.api.dto.request.NewCourseDTO;
import br.com.alura.AluraFake.api.dto.response.CourseListItemDTO;
import br.com.alura.AluraFake.application.service.course.CourseListInterface;
import br.com.alura.AluraFake.application.service.course.CoursePublicationServiceInterface;
import br.com.alura.AluraFake.application.service.course.CreateNewCourseServiceInterface;
import br.com.alura.AluraFake.domain.enumeration.Role;
import br.com.alura.AluraFake.domain.enumeration.Status;
import br.com.alura.AluraFake.domain.model.Course;
import br.com.alura.AluraFake.domain.model.User;
import br.com.alura.AluraFake.exceptionhandler.BusinessRuleException;
import br.com.alura.AluraFake.exceptionhandler.EntityNotFoundException;
import br.com.alura.AluraFake.exceptionhandler.GlobalExceptionHandler;
import br.com.alura.AluraFake.exceptionhandler.ResourceNotFoundException;
import br.com.alura.AluraFake.exceptionhandler.dto.ProblemType;
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
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(CourseController.class)
@Import(GlobalExceptionHandler.class)
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateNewCourseServiceInterface createNewCourseServiceMock;

    @MockBean
    private CoursePublicationServiceInterface publishServiceMock;

    @MockBean
    private CourseListInterface courseListServiceMock;

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
    @DisplayName("POST /course/new: Deve criar curso com DTO válido e retornar Status 200 Created")
    void createCourse_withValidDTO_shouldReturnCreated() throws Exception {
        // Arrange
        when(createNewCourseServiceMock.createNewCourse(any(NewCourseDTO.class))).thenReturn(createdCourse);

        // Act
        ResultActions resultActions = mockMvc.perform(post("/course/new")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validNewCourseDTO)));

        // Assert
        resultActions.andExpect(status().isOk());

        verify(createNewCourseServiceMock).createNewCourse(newCourseDTOCaptor.capture());
        assertThat(newCourseDTOCaptor.getValue().getTitle()).isEqualTo(validNewCourseDTO.getTitle());
    }

    @Test
    @DisplayName("POST /course/new: Não deve criar curso com DTO inválido e retornar Status 400 Bad Request")
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

        verify(createNewCourseServiceMock, never()).createNewCourse(any());
    }

    @Test
    @DisplayName("POST /course/new: Deve retornar Status 404 se instrutor não for encontrado pelo serviço")
    void createCourse_whenInstructorNotFoundByService_shouldReturnNotFound() throws Exception {
        // Arrange
        String errorMessage = "Instrutor não encontrado com e-mail: " + validNewCourseDTO.getEmailInstructor();
        when(createNewCourseServiceMock.createNewCourse(any(NewCourseDTO.class)))
                .thenThrow(new EntityNotFoundException(errorMessage));

        // Act
        ResultActions resultActions = mockMvc.perform(post("/course/new")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validNewCourseDTO)));

        // Assert
        // Seu GlobalExceptionHandler.handleEntityNotFound usa ProblemType.RESOURCE_NOT_FOUND
        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.type").value(errorBaseUri + ProblemType.RESOURCE_NOT_FOUND.getPath()))
                .andExpect(jsonPath("$.title").value(ProblemType.RESOURCE_NOT_FOUND.getTitle()))
                .andExpect(jsonPath("$.detail").value(errorMessage));
    }

    @Test
    @DisplayName("GET /course/all: Deve listar cursos e retornar Status 200 OK com lista")
    void listAllCourses_whenCoursesExist_shouldReturnOkWithCourseList() throws Exception {
        // Arrange
        Course createCourseBuildingStatus = new Course("Curso 1", "Aprenda tópicos avançados", instructor);
        createCourseBuildingStatus.setId(2L);
        createCourseBuildingStatus.setStatus(Status.BUILDING);
        createCourseBuildingStatus.setPublishedAt(LocalDateTime.now());

        Course createCoursePublishedStatus = new Course("Curso 2", "Aprenda tópicos avançados", instructor);
        createCourseBuildingStatus.setId(2L);
        createCourseBuildingStatus.setStatus(Status.BUILDING);
        createCourseBuildingStatus.setPublishedAt(LocalDateTime.now());

        CourseListItemDTO dto1 = new CourseListItemDTO(createCourseBuildingStatus);
        CourseListItemDTO dto2 = new CourseListItemDTO(createCoursePublishedStatus);

        List<CourseListItemDTO> courseList = List.of(dto1, dto2);
        when(courseListServiceMock.listAllCourses()).thenReturn(courseList);

        // Act
        ResultActions resultActions = mockMvc.perform(get("/course/all")
                .accept(MediaType.APPLICATION_JSON));

        // Assert
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Curso 1"))
                .andExpect(jsonPath("$[1].title").value("Curso 2"));

        verify(courseListServiceMock).listAllCourses();
    }

    @Test
    @DisplayName("GET /course/all: Deve retornar Status 200 OK com lista vazia se não houver cursos")
    void listAllCourses_whenNoCoursesExist_shouldReturnOkWithEmptyList() throws Exception {
        // Arrange
        when(courseListServiceMock.listAllCourses()).thenReturn(Collections.emptyList());

        // Act
        ResultActions resultActions = mockMvc.perform(get("/course/all")
                .accept(MediaType.APPLICATION_JSON));

        // Assert
        resultActions
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(courseListServiceMock).listAllCourses();
    }

    @Test
    @DisplayName("POST /course/{id}/publish: Deve publicar curso com sucesso e retornar Status 201 Created")
    void publishCourse_withValidIdAndConditions_shouldReturnCreated() throws Exception {
        // Arrange
        Long courseIdToPublish = 1L;
        // O método publishService.publish(id) retorna o Course atualizado
        createdCourse.setStatus(Status.PUBLISHED);
        createdCourse.setPublishedAt(LocalDateTime.now());
        when(publishServiceMock.publish(courseIdToPublish)).thenReturn(createdCourse);

        // Act
        ResultActions resultActions = mockMvc.perform(post("/course/{id}/publish", courseIdToPublish));

        // Assert
        resultActions.andExpect(status().isCreated());
        verify(publishServiceMock).publish(courseIdToPublish);
    }

    @Test
    @DisplayName("POST /course/{id}/publish: Deve retornar Status 404 se curso a ser publicado não for encontrado")
    void publishCourse_whenCourseIdNotFound_shouldReturnNotFound() throws Exception {
        // Arrange
        Long nonExistentCourseId = 999L;
        String errorMessage = "Curso não encontrado com ID: " + nonExistentCourseId;
        when(publishServiceMock.publish(nonExistentCourseId)).thenThrow(new ResourceNotFoundException(errorMessage));

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
    @DisplayName("POST /course/{id}/publish: Deve retornar Status 400 se regra de negócio for violada (ex: status não é BUILDING)")
    void publishCourse_whenBusinessRuleViolated_shouldReturnBadRequest() throws Exception {
        // Arrange
        Long courseId = 1L;
        String errorMessage = "O curso 'Curso Teste' não pode ser publicado pois seu status é 'PUBLISHED'. Apenas cursos em 'BUILDING' são permitidos.";
        when(publishServiceMock.publish(courseId)).thenThrow(new BusinessRuleException(errorMessage));

        // Act
        ResultActions resultActions = mockMvc.perform(post("/course/{id}/publish", courseId));

        // Assert
        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.type").value(errorBaseUri + ProblemType.INVALID_OPERATION.getPath()))
                .andExpect(jsonPath("$.title").value(ProblemType.INVALID_OPERATION.getTitle()))
                .andExpect(jsonPath("$.detail").value(errorMessage));
    }

    @Test
    @DisplayName("POST /course/{id}/publish: Deve retornar Status 500 se ocorrer erro inesperado no serviço")
    void publishCourse_whenServiceThrowsRuntimeException_shouldReturnInternalServerError() throws Exception {
        // Arrange
        Long courseId = 1L;
        when(publishServiceMock.publish(courseId)).thenThrow(new RuntimeException("Erro interno inesperado"));

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