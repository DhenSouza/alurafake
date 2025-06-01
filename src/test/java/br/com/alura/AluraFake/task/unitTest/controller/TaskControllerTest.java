package br.com.alura.AluraFake.task.unitTest.controller;

import br.com.alura.AluraFake.api.controller.TaskController;
import br.com.alura.AluraFake.api.dto.request.*;
import br.com.alura.AluraFake.application.factory.TaskUseCaseFactory;
import br.com.alura.AluraFake.application.interfaces.CreateTaskUseCase;
import br.com.alura.AluraFake.domain.enumeration.Type;
import br.com.alura.AluraFake.domain.repository.UserRepository;
import br.com.alura.AluraFake.domain.service.AppUserDetailsService;
import br.com.alura.AluraFake.globalHandler.GlobalExceptionHandler;
import br.com.alura.AluraFake.globalHandler.OptionalInvalidException;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.doNothing;
import static org.mockito.BDDMockito.doThrow;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(TaskController.class)
@Import({GlobalExceptionHandler.class, SecurityConfiguration.class, JwtTokenUtil.class, JwtAuthenticationFilter.class, AppUserDetailsService.class})
public class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskUseCaseFactory useCaseFactory;

    @MockBean
    private CreateTaskUseCase createTaskUseCaseMock;

    @MockBean
    private UserRepository userRepositoryMock;

    private final ArgumentCaptor<TaskCreationRequest> captor = ArgumentCaptor.forClass(TaskCreationRequest.class);

    private final String errorBaseUri = "https://api.seusite.com/erros";

    private final String expectedDetailMessageFromConstructorException =
            "Two or more correct alternatives and at least one incorrect alternative are required for Multiple Choice.";

    private final String onlyOneCorrectAllowed =
            "Only one correct alternative is allowed for Single Choice.";

    @BeforeEach
    void setUp() {
        when(useCaseFactory.getUseCase(any(Type.class)))
                .thenReturn(createTaskUseCaseMock);
    }

    private List<ChoiceOptionRequest> defaultSingleChoiceOptionsValid() {
        return List.of(
                new ChoiceOptionRequest("Option A (Correct)", true),
                new ChoiceOptionRequest("Option B (Incorrect)", false),
                new ChoiceOptionRequest("Option C (Incorrect)", false)
        );
    }

    private List<ChoiceOptionRequest> defaultMultipleChoiceOptionsValid() {
        return List.of(
                new ChoiceOptionRequest("Multiple Option A (Correct)", true),
                new ChoiceOptionRequest("Multiple Option B (Correct)", true),
                new ChoiceOptionRequest("Multiple Option C (Incorrect)", false)
        );
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("POST /task/new with valid OPEN_TEXT payload returns 200 OK and success=true")
    void createNewTask_whenValidOpenTextRequest_shouldReturnOk() throws Exception {
        // Arrange
        OpenTextTaskCreationRequest request = new OpenTextTaskCreationRequest(
                1L, "Describe the Strategy pattern.", 1, Type.OPEN_TEXT
        );
        doNothing().when(createTaskUseCaseMock).execute(any(TaskCreationRequest.class));

        // Act & Assert
        mockMvc.perform(post("/task/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(useCaseFactory).getUseCase(eq(Type.OPEN_TEXT));
        verify(createTaskUseCaseMock).execute(captor.capture());

        OpenTextTaskCreationRequest captured = (OpenTextTaskCreationRequest) captor.getValue();
        assertThat(captured.courseId()).isEqualTo(1L);
        assertThat(captured.statement()).isEqualTo("Describe the Strategy pattern.");
        assertThat(captured.type()).isEqualTo(Type.OPEN_TEXT);
    }

    @ParameterizedTest
    @MethodSource("invalidOpenTextRequests")
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("POST /task/new with invalid OPEN_TEXT payload returns 400 Bad Request")
    void createNewTask_whenInvalidOpenTextRequest_shouldReturnBadRequest(OpenTextTaskCreationRequest invalid) throws Exception {
        mockMvc.perform(post("/task/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.type").value(errorBaseUri + ProblemType.DEFAULT_USER_MESSAGE_VALIDATION.getPath()))
                .andExpect(jsonPath("$.fields[0].name").exists())
                .andExpect(jsonPath("$.fields[0].userMessage").exists());
    }

    static Stream<OpenTextTaskCreationRequest> invalidOpenTextRequests() {
        return Stream.of(
                new OpenTextTaskCreationRequest(1L, "", 1, Type.OPEN_TEXT),
                new OpenTextTaskCreationRequest(1L, "Valid statement", 0, Type.OPEN_TEXT),
                new OpenTextTaskCreationRequest(null, "Valid statement", 1, Type.OPEN_TEXT)
        );
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("POST /task/new when resource not found during OPEN_TEXT creation returns 404 Not Found")
    void createNewTask_whenResourceNotFoundOpenText_shouldReturnNotFound() throws Exception {
        OpenTextTaskCreationRequest request = new OpenTextTaskCreationRequest(
                20L, "Test exception", 1, Type.OPEN_TEXT
        );
        String detail = "Specific resource not found during task creation.";
        doThrow(new ResourceNotFoundException(detail))
                .when(createTaskUseCaseMock).execute(any(TaskCreationRequest.class));

        mockMvc.perform(post("/task/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.type").value(errorBaseUri + ProblemType.RESOURCE_NOT_FOUND.getPath()))
                .andExpect(jsonPath("$.detail").value(detail));
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("POST /task/new when OptionalInvalidException thrown returns 400 Bad Request")
    void createNewTask_whenOptionalInvalidException_shouldReturnBadRequest() throws Exception {
        OpenTextTaskCreationRequest request = new OpenTextTaskCreationRequest(
                1L, "Valid statement", 1, Type.OPEN_TEXT
        );
        String message = "OptionalInvalidException rule violated.";
        doThrow(new OptionalInvalidException(message))
                .when(createTaskUseCaseMock).execute(any(TaskCreationRequest.class));

        mockMvc.perform(post("/task/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(message));
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("POST /task/new when unexpected RuntimeException thrown returns 500 Internal Server Error")
    void createNewTask_whenUnhandledException_shouldReturnInternalError() throws Exception {
        OpenTextTaskCreationRequest request = new OpenTextTaskCreationRequest(
                1L, "Anything", 1, Type.OPEN_TEXT
        );
        doThrow(new RuntimeException("boom"))
                .when(createTaskUseCaseMock).execute(any(TaskCreationRequest.class));

        mockMvc.perform(post("/task/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("POST /task/new with OPEN_TEXT when user is STUDENT returns 403 Forbidden")
    @WithMockUser(username = "student@example.com", roles = {"STUDENT"})
    void createNewTask_whenUserIsStudentOpenText_shouldReturnForbidden() throws Exception {
        OpenTextTaskCreationRequest request = new OpenTextTaskCreationRequest(
                1L, "Student creation attempt", 1, Type.OPEN_TEXT
        );

        mockMvc.perform(post("/task/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(createTaskUseCaseMock, never()).execute(any(TaskCreationRequest.class));
        verify(useCaseFactory, never()).getUseCase(any(Type.class));
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("POST /task/new with valid SINGLE_CHOICE payload returns 200 OK and success=true")
    void createNewTask_whenValidSingleChoiceRequest_shouldReturnOk() throws Exception {
        // Arrange
        SingleChoiceTaskCreationRequest singleChoiceRequest = new SingleChoiceTaskCreationRequest(
                2L,
                "What is the capital of France?",
                1,
                Type.SINGLE_CHOICE,
                defaultSingleChoiceOptionsValid()
        );

        doNothing().when(createTaskUseCaseMock).execute(any(TaskCreationRequest.class));

        // Act & Assert
        mockMvc.perform(post("/task/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(singleChoiceRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(useCaseFactory).getUseCase(eq(Type.SINGLE_CHOICE));
        verify(createTaskUseCaseMock).execute(captor.capture());

        SingleChoiceTaskCreationRequest captured = (SingleChoiceTaskCreationRequest) captor.getValue();
        assertThat(captured.courseId()).isEqualTo(2L);
        assertThat(captured.statement()).isEqualTo("What is the capital of France?");
        assertThat(captured.order()).isEqualTo(1);
        assertThat(captured.type()).isEqualTo(Type.SINGLE_CHOICE);

        assertThat(captured.options()).hasSize(3);
        assertThat(captured.options().get(0).option()).isEqualTo("Option A (Correct)");
        assertThat(captured.options().get(0).isCorrect()).isTrue();
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("POST /task/new with SINGLE_CHOICE and zero correct options returns 400 Bad Request")
    void createNewTask_whenSingleChoiceHasZeroCorrectOptions_shouldReturnBadRequest() throws Exception {
        String invalidJsonPayload = """
            {
                "courseId": 3,
                "statement": "Which option is correct here (none)?",
                "order": 1,
                "type": "SINGLE_CHOICE",
                "options": [
                    {"option": "Option X", "isCorrect": false},
                    {"option": "Option Y", "isCorrect": false},
                    {"option": "Option Z", "isCorrect": false}
                ]
            }
            """;

        String expectedTypeUri = errorBaseUri + ProblemType.INVALID_DATA.getPath();

        mockMvc.perform(post("/task/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.type").value(expectedTypeUri))
                .andExpect(jsonPath("$.title").value(ProblemType.INVALID_DATA.getTitle()))
                .andExpect(jsonPath("$.detail").value(this.onlyOneCorrectAllowed))
                .andExpect(jsonPath("$.userMessage").value(ProblemType.INVALID_DATA.getMessage()))
                .andExpect(jsonPath("$.instance").value("/task/new"));
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("POST /task/new with SINGLE_CHOICE and fewer than three options returns 400 Bad Request")
    void createNewTask_whenSingleChoiceHasLessThanThreeOptions_shouldReturnBadRequest() throws Exception {
        String invalidJsonPayload = """
            {
                "courseId": 3,
                "statement": "Which option is correct here (none)?",
                "order": 1,
                "type": "SINGLE_CHOICE",
                "options": [
                    {"option": "Option X", "isCorrect": false}
                ]
            }
            """;

        String expectedTypeUri = errorBaseUri + ProblemType.INVALID_DATA.getPath();

        mockMvc.perform(post("/task/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.type").value(expectedTypeUri))
                .andExpect(jsonPath("$.title").value(ProblemType.INVALID_DATA.getTitle()))
                .andExpect(jsonPath("$.detail").value(this.onlyOneCorrectAllowed))
                .andExpect(jsonPath("$.userMessage").value(ProblemType.INVALID_DATA.getMessage()))
                .andExpect(jsonPath("$.instance").value("/task/new"));
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("POST /task/new with SINGLE_CHOICE and non-existent courseId returns 404 Not Found")
    void createNewTask_whenSingleChoiceAndCourseIdNotFound_shouldReturnNotFound() throws Exception {
        long nonExistentCourseId = 999L;
        SingleChoiceTaskCreationRequest requestForNonExistentCourse = new SingleChoiceTaskCreationRequest(
                nonExistentCourseId,
                "Statement for a non-existent course (single choice)",
                1,
                Type.SINGLE_CHOICE,
                defaultSingleChoiceOptionsValid()
        );

        String expectedExceptionDetailMessage = "Curso não encontrado com ID: " + nonExistentCourseId;
        ResourceNotFoundException thrownException = new ResourceNotFoundException(expectedExceptionDetailMessage);

        doThrow(thrownException)
                .when(createTaskUseCaseMock).execute(any(TaskCreationRequest.class));

        String expectedTypeUri = errorBaseUri + ProblemType.RESOURCE_NOT_FOUND.getPath();

        mockMvc.perform(post("/task/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestForNonExistentCourse)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.type").value(expectedTypeUri))
                .andExpect(jsonPath("$.title").value(ProblemType.RESOURCE_NOT_FOUND.getTitle()))
                .andExpect(jsonPath("$.detail").value(expectedExceptionDetailMessage))
                .andExpect(jsonPath("$.userMessage").value(ProblemType.RESOURCE_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.instance").value("/task/new"));
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("POST /task/new with SINGLE_CHOICE when use case throws RuntimeException returns 500 Internal Server Error")
    void createNewTask_whenSingleChoiceUseCaseThrowsRuntimeException_shouldReturnInternalServerError() throws Exception {
        SingleChoiceTaskCreationRequest validSingleChoiceRequest = new SingleChoiceTaskCreationRequest(
                3L,
                "Valid statement for 500 error test (single choice)",
                1,
                Type.SINGLE_CHOICE,
                defaultSingleChoiceOptionsValid()
        );

        RuntimeException unexpectedException = new RuntimeException("boom - unexpected server error for single choice");
        doThrow(unexpectedException)
                .when(createTaskUseCaseMock).execute(any(TaskCreationRequest.class));

        String expectedTypeUri = errorBaseUri + ProblemType.UNEXPECTED_ERROR.getPath();

        mockMvc.perform(post("/task/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validSingleChoiceRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(HttpStatus.INTERNAL_SERVER_ERROR.value()))
                .andExpect(jsonPath("$.type").value(expectedTypeUri))
                .andExpect(jsonPath("$.title").value(ProblemType.UNEXPECTED_ERROR.getTitle()))
                .andExpect(jsonPath("$.detail").value(ProblemType.UNEXPECTED_ERROR.getMessage()))
                .andExpect(jsonPath("$.userMessage").value(ProblemType.UNEXPECTED_ERROR.getMessage()))
                .andExpect(jsonPath("$.instance").value("/task/new"));
    }

    @Test
    @DisplayName("POST /task/new with SINGLE_CHOICE when user is STUDENT returns 403 Forbidden")
    @WithMockUser(username = "student.curious@example.com", roles = {"STUDENT"})
    void createNewTask_whenSingleChoiceAndUserIsStudent_shouldReturnForbidden() throws Exception {
        SingleChoiceTaskCreationRequest singleChoiceRequestPayload = new SingleChoiceTaskCreationRequest(
                2L,
                "What is the capital of France? (Student attempt)",
                1,
                Type.SINGLE_CHOICE,
                defaultSingleChoiceOptionsValid()
        );

        mockMvc.perform(post("/task/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(singleChoiceRequestPayload)))
                .andExpect(status().isForbidden());

        verify(createTaskUseCaseMock, never()).execute(any(TaskCreationRequest.class));
        verify(useCaseFactory, never()).getUseCase(any(Type.class));
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("POST /task/new with valid MULTIPLE_CHOICE payload returns 200 OK and success=true")
    void createNewTask_whenValidMultipleChoiceRequest_shouldReturnOk() throws Exception {
        MultipleChoiceTaskCreationRequest multipleChoiceRequest = new MultipleChoiceTaskCreationRequest(
                10L,
                "Select all server-side programming languages:",
                1,
                Type.MULTIPLE_CHOICE,
                defaultMultipleChoiceOptionsValid()
        );

        doNothing().when(createTaskUseCaseMock).execute(any(TaskCreationRequest.class));

        mockMvc.perform(post("/task/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(multipleChoiceRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(useCaseFactory).getUseCase(eq(Type.MULTIPLE_CHOICE));
        verify(createTaskUseCaseMock).execute(captor.capture());

        MultipleChoiceTaskCreationRequest captured = (MultipleChoiceTaskCreationRequest) captor.getValue();
        assertThat(captured.courseId()).isEqualTo(10L);
        assertThat(captured.statement()).isEqualTo("Select all server-side programming languages:");
        assertThat(captured.type()).isEqualTo(Type.MULTIPLE_CHOICE);
        assertThat(captured.options()).hasSize(3);
        assertThat(captured.options().stream().filter(ChoiceOptionRequest::isCorrect).count()).isEqualTo(2);
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("POST /task/new with MULTIPLE_CHOICE and not enough correct options returns 400 Bad Request")
    void createNewTask_whenMultipleChoiceHasNotEnoughCorrectOptions_shouldReturnBadRequest() throws Exception {
        String invalidJsonPayload = """
            {
                "courseId": 11,
                "statement": "Which is the only correct option?",
                "order": 1,
                "type": "MULTIPLE_CHOICE",
                "options": [
                    {"option": "Multiple Option A (Correct)", "isCorrect": true},
                    {"option": "Multiple Option B (Incorrect)", "isCorrect": false},
                    {"option": "Multiple Option C (Incorrect)", "isCorrect": false}
                ]
            }
            """;

        String expectedTypeUri = errorBaseUri + ProblemType.INVALID_DATA.getPath();

        mockMvc.perform(post("/task/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.type").value(expectedTypeUri))
                .andExpect(jsonPath("$.title").value(ProblemType.INVALID_DATA.getTitle()))
                .andExpect(jsonPath("$.detail").value(this.expectedDetailMessageFromConstructorException))
                .andExpect(jsonPath("$.userMessage").value(ProblemType.INVALID_DATA.getMessage()))
                .andExpect(jsonPath("$.instance").value("/task/new"));
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("POST /task/new with MULTIPLE_CHOICE and no incorrect option returns 400 Bad Request")
    void createNewTask_whenMultipleChoiceHasNoIncorrectOption_shouldReturnBadRequest() throws Exception {
        String invalidJsonPayloadAllCorrect = """
            {
                "courseId": 12,
                "statement": "Are all options correct?",
                "order": 1,
                "type": "MULTIPLE_CHOICE",
                "options": [
                    {"option": "Multiple Option A (Correct)", "isCorrect": true},
                    {"option": "Multiple Option B (Correct)", "isCorrect": true},
                    {"option": "Multiple Option C (Correct)", "isCorrect": true}
                ]
            }
            """;

        String expectedTypeUri = errorBaseUri + ProblemType.INVALID_DATA.getPath();

        mockMvc.perform(post("/task/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJsonPayloadAllCorrect))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.type").value(expectedTypeUri))
                .andExpect(jsonPath("$.title").value(ProblemType.INVALID_DATA.getTitle()))
                .andExpect(jsonPath("$.detail").value(this.expectedDetailMessageFromConstructorException))
                .andExpect(jsonPath("$.userMessage").value(ProblemType.INVALID_DATA.getMessage()))
                .andExpect(jsonPath("$.instance").value("/task/new"));
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("POST /task/new with MULTIPLE_CHOICE and fewer than three options returns 400 Bad Request (Bean Validation)")
    void createNewTask_whenMultipleChoiceHasTooFewOptions_shouldReturnBadRequest_BeanValidation() throws Exception {
        String invalidJsonPayloadFewOptions = """
            {
                "courseId": 13,
                "statement": "Too few options and all correct",
                "order": 1,
                "type": "MULTIPLE_CHOICE",
                "options": [
                    {"option": "Option A (Correct)", "isCorrect": true},
                    {"option": "Option B (Correct)", "isCorrect": true}
                ]
            }
            """;

        String expectedTypeUri = errorBaseUri + ProblemType.INVALID_DATA.getPath();

        mockMvc.perform(post("/task/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJsonPayloadFewOptions))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.type").value(expectedTypeUri))
                .andExpect(jsonPath("$.title").value(ProblemType.INVALID_DATA.getTitle()))
                .andExpect(jsonPath("$.detail").value(this.expectedDetailMessageFromConstructorException))
                .andExpect(jsonPath("$.userMessage").value(ProblemType.INVALID_DATA.getMessage()))
                .andExpect(jsonPath("$.instance").value("/task/new"))
                .andExpect(jsonPath("$.fields").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("POST /task/new with MULTIPLE_CHOICE and non-existent course returns 404 Not Found")
    void createNewTask_whenMultipleChoiceAndCourseNotFound_shouldReturnNotFound() throws Exception {
        long nonExistentCourseId = 998L;
        MultipleChoiceTaskCreationRequest requestWithNonExistentCourse = new MultipleChoiceTaskCreationRequest(
                nonExistentCourseId,
                "Statement for a non-existent course (multiple choice)",
                1,
                Type.MULTIPLE_CHOICE,
                defaultMultipleChoiceOptionsValid()
        );

        String expectedExceptionDetailMessage = "Curso não encontrado com ID: " + nonExistentCourseId;
        ResourceNotFoundException thrownException = new ResourceNotFoundException(expectedExceptionDetailMessage);

        doThrow(thrownException)
                .when(createTaskUseCaseMock).execute(any(TaskCreationRequest.class));

        String expectedTypeUri = errorBaseUri + ProblemType.RESOURCE_NOT_FOUND.getPath();

        mockMvc.perform(post("/task/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithNonExistentCourse)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.type").value(expectedTypeUri))
                .andExpect(jsonPath("$.title").value(ProblemType.RESOURCE_NOT_FOUND.getTitle()))
                .andExpect(jsonPath("$.detail").value(expectedExceptionDetailMessage))
                .andExpect(jsonPath("$.userMessage").value(ProblemType.RESOURCE_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.instance").value("/task/new"));
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("POST /task/new with MULTIPLE_CHOICE when use case throws RuntimeException returns 500 Internal Server Error")
    void createNewTask_whenMultipleChoiceUseCaseThrowsRuntimeException_shouldReturnInternalServerError() throws Exception {
        MultipleChoiceTaskCreationRequest validRequest = new MultipleChoiceTaskCreationRequest(
                14L,
                "Valid statement for 500 error test (multiple choice)",
                1,
                Type.MULTIPLE_CHOICE,
                defaultMultipleChoiceOptionsValid()
        );

        RuntimeException unexpectedException = new RuntimeException("Simulated internal error for multiple choice");
        doThrow(unexpectedException)
                .when(createTaskUseCaseMock).execute(any(TaskCreationRequest.class));

        String expectedTypeUri = errorBaseUri + ProblemType.UNEXPECTED_ERROR.getPath();

        mockMvc.perform(post("/task/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(HttpStatus.INTERNAL_SERVER_ERROR.value()))
                .andExpect(jsonPath("$.type").value(expectedTypeUri))
                .andExpect(jsonPath("$.title").value(ProblemType.UNEXPECTED_ERROR.getTitle()))
                .andExpect(jsonPath("$.detail").value(ProblemType.UNEXPECTED_ERROR.getMessage()))
                .andExpect(jsonPath("$.userMessage").value(ProblemType.UNEXPECTED_ERROR.getMessage()))
                .andExpect(jsonPath("$.instance").value("/task/new"));
    }

    @Test
    @DisplayName("POST /task/new with MULTIPLE_CHOICE when user is STUDENT returns 403 Forbidden")
    @WithMockUser(username = "student.curious@example.com", roles = {"STUDENT"})
    void createNewTask_whenMultipleChoiceAndUserIsStudent_shouldReturnForbidden() throws Exception {
        MultipleChoiceTaskCreationRequest multipleChoiceRequestPayload = new MultipleChoiceTaskCreationRequest(
                10L,
                "Which backend frameworks? (Student attempt)",
                1,
                Type.MULTIPLE_CHOICE,
                defaultMultipleChoiceOptionsValid()
        );

        mockMvc.perform(post("/task/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(multipleChoiceRequestPayload)))
                .andExpect(status().isForbidden());

        verify(createTaskUseCaseMock, never()).execute(any(TaskCreationRequest.class));
    }
}
