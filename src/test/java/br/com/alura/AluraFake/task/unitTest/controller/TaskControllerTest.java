package br.com.alura.AluraFake.task.unitTest.controller;

import br.com.alura.AluraFake.api.controller.TaskController;
import br.com.alura.AluraFake.api.dto.request.*;
import br.com.alura.AluraFake.application.factory.TaskUseCaseFactory;
import br.com.alura.AluraFake.application.interfaces.CreateTaskUseCase;
import br.com.alura.AluraFake.domain.enumeration.Type;
import br.com.alura.AluraFake.domain.repository.UserRepository;
import br.com.alura.AluraFake.domain.service.AppUserDetailsService;
import br.com.alura.AluraFake.exceptionhandler.GlobalExceptionHandler;
import br.com.alura.AluraFake.exceptionhandler.OptionalInvalidException;
import br.com.alura.AluraFake.exceptionhandler.ResourceNotFoundException;
import br.com.alura.AluraFake.exceptionhandler.dto.ProblemType;
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
import org.springframework.security.test.context.support.WithMockUser;

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

    @BeforeEach
    void setUp() {
        when(useCaseFactory.getUseCase(any(Type.class)))
                .thenReturn(createTaskUseCaseMock);
    }

    private List<ChoiceOptionRequest> defaultSingleChoiceOptionsValid() {
        return List.of(
                new ChoiceOptionRequest("Opção A (Correta)", true),
                new ChoiceOptionRequest("Opção B (Incorreta)", false),
                new ChoiceOptionRequest("Opção C (Incorreta)", false)
        );
    }


    private List<ChoiceOptionRequest> defaultMultipleChoiceOptionsValid() {
        return List.of(
                new ChoiceOptionRequest("Opção Múltipla A (Correta)", true),
                new ChoiceOptionRequest("Opção Múltipla B (Correta)", true),
                new ChoiceOptionRequest("Opção Múltipla C (Incorreta)", false)
        );
    }


    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("POST /task/new com payload válido retorna 200 OK e success=true com o tipo OPEN_TEXT")
    void createNewTask_whenValidOpenTextRequest_shouldReturnOk() throws Exception {
        // Arrange
        OpenTextTaskCreationRequest request = new OpenTextTaskCreationRequest(
                1L, "Descreva o padrão Strategy.", 1, Type.OPEN_TEXT
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

        OpenTextTaskCreationRequest openTextCaptured = (OpenTextTaskCreationRequest) captor.getValue();
        assertThat(openTextCaptured.courseId()).isEqualTo(1L);
        assertThat(openTextCaptured.statement()).isEqualTo("Descreva o padrão Strategy.");
        assertThat(openTextCaptured.type()).isEqualTo(Type.OPEN_TEXT);
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("POST /task/new com payload invalido com o tipo OPEN_TEXT 400 Bad Request")
    void createNewTask_whenInvalidRequest_shouldReturnBadRequest(OpenTextTaskCreationRequest invalid) throws Exception {
        mockMvc.perform(post("/task/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.type").value(errorBaseUri + ProblemType.DEFAULT_USER_MESSAGE_VALIDATION.getPath()))
                .andExpect(jsonPath("$.fields[0].name").exists())
                .andExpect(jsonPath("$.fields[0].userMessage").exists());
    }

    static Stream<OpenTextTaskCreationRequest> invalidRequests() {
        return Stream.of(
                new OpenTextTaskCreationRequest(1L, "", 1, Type.OPEN_TEXT),
                new OpenTextTaskCreationRequest(1L, "Enunciado OK", 0, Type.OPEN_TEXT),
                new OpenTextTaskCreationRequest(null, "Enunciado OK", 1, Type.OPEN_TEXT)
        );
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("POST /task/new com payload invalido com o tipo OPEN_TEXT 404 Not Found")
    void createNewTask_whenResourceNotFound_shouldReturnNotFound() throws Exception {
        OpenTextTaskCreationRequest request = new OpenTextTaskCreationRequest(
                20L, "Teste exceção", 1, Type.OPEN_TEXT
        );
        String detail = "Recurso específico não encontrado durante a criação da tarefa.";
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
    @DisplayName("POST /task/new com payload invalido com o tipo OPEN_TEXT retorna 400 Bad Request")
    void createNewTask_whenOptionalInvalidException_shouldReturnBadRequest() throws Exception {
        OpenTextTaskCreationRequest request = new OpenTextTaskCreationRequest(
                1L, "Statement válido", 1, Type.OPEN_TEXT
        );
        String message = "Regra de OptionalInvalidException violada.";
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
    @DisplayName("POST /task/new com OPEN_TEXT quando UseCase lança RuntimeException inesperada retorna 500 Internal Server Error\"")
    void createNewTask_whenUnhandledException_shouldReturnInternalError() throws Exception {
        OpenTextTaskCreationRequest request = new OpenTextTaskCreationRequest(
                1L, "Qualquer coisa", 1, Type.OPEN_TEXT
        );
        doThrow(new RuntimeException("boom"))
                .when(createTaskUseCaseMock).execute(any(TaskCreationRequest.class));

        mockMvc.perform(post("/task/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("POST /task/new com OPEN_TEXT quando usuário é STUDENT retorna 403 Forbidden")
    @WithMockUser(username = "aluno@example.com", roles = {"STUDENT"})
    void createNewTask_whenUserIsStudent_shouldReturnForbidden() throws Exception {
        // Arrange
        OpenTextTaskCreationRequest request = new OpenTextTaskCreationRequest(
                1L,
                "Tentativa de criação por aluno.",
                1,
                Type.OPEN_TEXT
        );

        // Act
        ResultActions resultActions = mockMvc.perform(post("/task/new")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Assert
        resultActions
                .andExpect(status().isForbidden());

        verify(createTaskUseCaseMock, never()).execute(any(TaskCreationRequest.class));
        verify(useCaseFactory, never()).getUseCase(any(Type.class));
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("POST /task/new com payload válido retorna 200 OK e success=true com SINGLE_CHOICE")
    void createNewTask_whenValidSingleChoiceRequest_shouldReturnOk() throws Exception {
        // Arrange
        SingleChoiceTaskCreationRequest singleChoiceRequest = new SingleChoiceTaskCreationRequest(
                2L,
                "Qual é a capital da França?",
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

        // Verify
        verify(useCaseFactory).getUseCase(eq(Type.SINGLE_CHOICE));
        verify(createTaskUseCaseMock).execute(captor.capture());

        SingleChoiceTaskCreationRequest singleChoiceCaptured =
                (SingleChoiceTaskCreationRequest) captor.getValue();

        assertThat(singleChoiceCaptured.courseId()).isEqualTo(2L);
        assertThat(singleChoiceCaptured.statement()).isEqualTo("Qual é a capital da França?");
        assertThat(singleChoiceCaptured.order()).isEqualTo(1);
        assertThat(singleChoiceCaptured.type()).isEqualTo(Type.SINGLE_CHOICE);

        assertThat(singleChoiceCaptured.options()).hasSize(3);
        assertThat(singleChoiceCaptured.options().get(0).option()).isEqualTo("Opção A (Correta)");
        assertThat(singleChoiceCaptured.options().get(0).isCorrect()).isTrue();
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("POST /task/new com SINGLE_CHOICE e NENHUMA opção correta (regra de negócio) retorna 400 Bad Request")
    void createNewTask_whenSingleChoiceHasZeroCorrectOptions_RuleViolation_shouldReturnBadRequest() throws Exception {
        // Arrange
        String invalidJsonPayload = """
        {
            "courseId": 3,
            "statement": "Qual opção está correta aqui (nenhuma)?",
            "order": 1,
            "type": "SINGLE_CHOICE",
            "options": [
                {"option": "Opção X", "isCorrect": false},
                {"option": "Opção Y", "isCorrect": false},
                {"option": "Opção Z", "isCorrect": false}
            ]
        }
        """;

        String expectedDetailMessageFromConstructorException = "Uma e apenas uma alternativa correta é permitida para Single Choice.";

        // Act
        ResultActions resultActions = mockMvc.perform(post("/task/new")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJsonPayload));

        // Assert
        String expectedTypeUri = errorBaseUri + ProblemType.INVALID_DATA.getPath();

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.type").value(expectedTypeUri))
                .andExpect(jsonPath("$.title").value(ProblemType.INVALID_DATA.getTitle()))
                .andExpect(jsonPath("$.detail").value(expectedDetailMessageFromConstructorException))
                .andExpect(jsonPath("$.userMessage").value(ProblemType.INVALID_DATA.getMessage()))
                .andExpect(jsonPath("$.instance").value("/task/new"));
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("POST /task/new com SINGLE_CHOICE e com menos de 2 escolhas (regra de negócio) retorna 400 Bad Request")
    void createNewTask_whenSingleChoiceHasLessThanThree_RuleViolation_shouldReturnBadRequest() throws Exception {
        // Arrange
        String invalidJsonPayload = """
        {
            "courseId": 3,
            "statement": "Qual opção está correta aqui (nenhuma)?",
            "order": 1,
            "type": "SINGLE_CHOICE",
            "options": [
                {"option": "Opção X", "isCorrect": false}
            ]
        }
        """;

        String expectedDetailMessageFromConstructorException = "Uma e apenas uma alternativa correta é permitida para Single Choice.";

        // Act
        ResultActions resultActions = mockMvc.perform(post("/task/new")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJsonPayload));

        // Assert
        String expectedTypeUri = errorBaseUri + ProblemType.INVALID_DATA.getPath();

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.type").value(expectedTypeUri))
                .andExpect(jsonPath("$.title").value(ProblemType.INVALID_DATA.getTitle()))
                .andExpect(jsonPath("$.detail").value(expectedDetailMessageFromConstructorException))
                .andExpect(jsonPath("$.userMessage").value(ProblemType.INVALID_DATA.getMessage()))
                .andExpect(jsonPath("$.instance").value("/task/new"));
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("POST /task/new com SINGLE_CHOICE e courseId inexistente (UseCase lança Exceção) retorna 404 Not Found")
    void createNewTask_whenSingleChoiceAndCourseIdNotFound_shouldReturnNotFound() throws Exception {
        // Arrange
        long nonExistentCourseId = 999L;

        SingleChoiceTaskCreationRequest requestForNonExistentCourse = new SingleChoiceTaskCreationRequest(
                nonExistentCourseId,
                "Enunciado para um curso que não existe (single choice)",
                1,
                Type.SINGLE_CHOICE,
                defaultSingleChoiceOptionsValid()
        );

        String expectedExceptionDetailMessage = "Curso não encontrado com ID: " + nonExistentCourseId;

        ResourceNotFoundException thrownException = new ResourceNotFoundException(expectedExceptionDetailMessage);

        doThrow(thrownException)
                .when(createTaskUseCaseMock).execute(any(TaskCreationRequest.class));

        // Act
        ResultActions resultActions = mockMvc.perform(post("/task/new")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestForNonExistentCourse)));

        // Assert
        String expectedTypeUri = errorBaseUri + ProblemType.RESOURCE_NOT_FOUND.getPath();
        resultActions
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
    @DisplayName("POST /task/new com SINGLE_CHOICE quando UseCase lança RuntimeException inesperada retorna 500 Internal Server Error")
    void createNewTask_whenSingleChoiceAndUseCaseThrowsUnhandledRuntimeException_shouldReturnInternalServerError() throws Exception {
        // Arrange
        SingleChoiceTaskCreationRequest validSingleChoiceRequest = new SingleChoiceTaskCreationRequest(
                3L,
                "Enunciado válido para teste de erro 500 com SingleChoice",
                1,
                Type.SINGLE_CHOICE,
                defaultSingleChoiceOptionsValid()
        );

        RuntimeException unexpectedException = new RuntimeException("boom - erro inesperado no servidor para single choice");
        doThrow(unexpectedException)
                .when(createTaskUseCaseMock).execute(any(TaskCreationRequest.class));

        // Act
        ResultActions resultActions = mockMvc.perform(post("/task/new")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validSingleChoiceRequest)));

        // Assert
        String expectedTypeUri = errorBaseUri + ProblemType.UNEXPECTED_ERROR.getPath();

        resultActions
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(HttpStatus.INTERNAL_SERVER_ERROR.value()))
                .andExpect(jsonPath("$.type").value(expectedTypeUri))
                .andExpect(jsonPath("$.title").value(ProblemType.UNEXPECTED_ERROR.getTitle()))
                .andExpect(jsonPath("$.detail").value(ProblemType.UNEXPECTED_ERROR.getMessage()))
                .andExpect(jsonPath("$.userMessage").value(ProblemType.UNEXPECTED_ERROR.getMessage()))
                .andExpect(jsonPath("$.instance").value("/task/new"));
    }

    @Test
    @DisplayName("POST /task/new com SINGLE_CHOICE quando usuário é STUDENT retorna 403 Forbidden")
    @WithMockUser(username = "aluno.teste@example.com", roles = {"STUDENT"})
    void createNewTask_whenSingleChoiceRequestAndUserIsStudent_shouldReturnForbidden() throws Exception {
        // Arrange
        SingleChoiceTaskCreationRequest singleChoiceRequestPayload = new SingleChoiceTaskCreationRequest(
                2L,
                "Qual é a capital da França? (Tentativa por Aluno)",
                1,
                Type.SINGLE_CHOICE,
                defaultSingleChoiceOptionsValid()
        );
        // Act
        ResultActions resultActions = mockMvc.perform(post("/task/new")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(singleChoiceRequestPayload)));

        // Assert
        resultActions
                .andExpect(status().isForbidden());

        verify(createTaskUseCaseMock, never()).execute(any(TaskCreationRequest.class));
        verify(useCaseFactory, never()).getUseCase(any(Type.class));
    }


    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("POST /task/new com payload válido para MULTIPLE_CHOICE retorna 200 OK e success=true")
    void createNewTask_whenValidMultipleChoiceRequest_shouldReturnOk() throws Exception {
        // Arrange
        MultipleChoiceTaskCreationRequest multipleChoiceRequest = new MultipleChoiceTaskCreationRequest(
                10L,
                "Selecione todas as linguagens de programação server-side:",
                1,
                Type.MULTIPLE_CHOICE,
                defaultMultipleChoiceOptionsValid()
        );

        doNothing().when(createTaskUseCaseMock).execute(any(TaskCreationRequest.class));

        // Act & Assert
        mockMvc.perform(post("/task/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(multipleChoiceRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(useCaseFactory).getUseCase(eq(Type.MULTIPLE_CHOICE));
        verify(createTaskUseCaseMock).execute(captor.capture());

        MultipleChoiceTaskCreationRequest multipleChoiceCaptured = (MultipleChoiceTaskCreationRequest) captor.getValue();
        assertThat(multipleChoiceCaptured.courseId()).isEqualTo(10L);
        assertThat(multipleChoiceCaptured.statement()).isEqualTo("Selecione todas as linguagens de programação server-side:");
        assertThat(multipleChoiceCaptured.type()).isEqualTo(Type.MULTIPLE_CHOICE);
        assertThat(multipleChoiceCaptured.options()).hasSize(3);
        assertThat(multipleChoiceCaptured.options().stream().filter(ChoiceOptionRequest::isCorrect).count()).isEqualTo(2);
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("POST /task/new com MULTIPLE_CHOICE e menos de duas opções corretas retorna 400 Bad Request")
    void createNewTask_whenMultipleChoiceHasNotEnoughCorrectOptions_shouldReturnBadRequest() throws Exception {
        // Arrange
        String invalidJsonPayload = """
        {
            "courseId": 11,
            "statement": "Qual é a única opção correta?",
            "order": 1,
            "type": "MULTIPLE_CHOICE",
            "options": [
                {"option": "Opção Múltipla A (Correta)", "isCorrect": true},
                {"option": "Opção Múltipla B (Incorreta)", "isCorrect": false},
                {"option": "Opção Múltipla C (Incorreta)", "isCorrect": false}
            ]
        }
        """;

        String expectedDetailMessageFromConstructorException = "São necessárias duas ou mais alternativas corretas e ao menos uma alternativa incorreta para Multiple Choice.";

        // Act
        ResultActions resultActions = mockMvc.perform(post("/task/new")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJsonPayload)); // <<< Envie a String JSON diretamente

        // Assert
        String expectedTypeUri = errorBaseUri + ProblemType.INVALID_DATA.getPath();

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.type").value(expectedTypeUri))
                .andExpect(jsonPath("$.title").value(ProblemType.INVALID_DATA.getTitle()))
                .andExpect(jsonPath("$.detail").value(expectedDetailMessageFromConstructorException))
                .andExpect(jsonPath("$.userMessage").value(ProblemType.INVALID_DATA.getMessage()))
                .andExpect(jsonPath("$.instance").value("/task/new"));
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("POST /task/new com MULTIPLE_CHOICE e nenhuma opção incorreta retorna 400 Bad Request")
    void createNewTask_whenMultipleChoiceHasNoIncorrectOption_shouldReturnBadRequest() throws Exception {
        // Arrange
        String invalidJsonPayload_AllCorrect = """
        {
            "courseId": 12,
            "statement": "Todas são corretas?",
            "order": 1,
            "type": "MULTIPLE_CHOICE",
            "options": [
                {"option": "Opção Múltipla A (Correta)", "isCorrect": true},
                {"option": "Opção Múltipla B (Correta)", "isCorrect": true},
                {"option": "Opção Múltipla C (Correta)", "isCorrect": true}
            ]
        }
        """;

        String expectedDetailMessageFromConstructorException = "São necessárias duas ou mais alternativas corretas e ao menos uma alternativa incorreta para Multiple Choice.";

        // Act
        ResultActions resultActions = mockMvc.perform(post("/task/new")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJsonPayload_AllCorrect));

        // Assert
        String expectedTypeUri = errorBaseUri + ProblemType.INVALID_DATA.getPath();

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.type").value(expectedTypeUri))
                .andExpect(jsonPath("$.title").value(ProblemType.INVALID_DATA.getTitle()))
                .andExpect(jsonPath("$.detail").value(expectedDetailMessageFromConstructorException))
                .andExpect(jsonPath("$.userMessage").value(ProblemType.INVALID_DATA.getMessage()))
                .andExpect(jsonPath("$.instance").value("/task/new"));
    }


    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("POST /task/new com MULTIPLE_CHOICE e poucas opções (< 3) retorna 400 Bad Request (Bean Validation)")
    void createNewTask_whenMultipleChoiceHasTooFewOptions_shouldReturnBadRequest_BeanValidation() throws Exception {
        // Arrange
        String invalidJsonPayload_AllCorrectInTwoOptions = """
        {
            "courseId": 13,
            "statement": "Poucas opções e todas corretas",
            "order": 1,
            "type": "MULTIPLE_CHOICE",
            "options": [
                {"option": "Opção A (Correta)", "isCorrect": true},
                {"option": "Opção B (Correta)", "isCorrect": true}
            ]
        }
        """;

        String expectedDetailMessageFromConstructorException = "São necessárias duas ou mais alternativas corretas e ao menos uma alternativa incorreta para Multiple Choice.";

        // Act
        ResultActions resultActions = mockMvc.perform(post("/task/new")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJsonPayload_AllCorrectInTwoOptions));

        // Assert
        String expectedTypeUri = errorBaseUri + ProblemType.INVALID_DATA.getPath();

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.type").value(expectedTypeUri))
                .andExpect(jsonPath("$.title").value(ProblemType.INVALID_DATA.getTitle()))
                .andExpect(jsonPath("$.detail").value(expectedDetailMessageFromConstructorException))
                .andExpect(jsonPath("$.userMessage").value(ProblemType.INVALID_DATA.getMessage()))
                .andExpect(jsonPath("$.instance").value("/task/new"))
                .andExpect(jsonPath("$.fields").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("POST /task/new com MULTIPLE_CHOICE quando CourseId não existe retorna 404 Not Found")
    void createNewTask_whenMultipleChoiceAndCourseNotFound_shouldReturnNotFound() throws Exception {
        // Arrange
        long nonExistentCourseId = 998L;
        MultipleChoiceTaskCreationRequest requestWithNonExistentCourse = new MultipleChoiceTaskCreationRequest(
                nonExistentCourseId,
                "Enunciado para um curso que não existe (múltipla escolha)",
                1,
                Type.MULTIPLE_CHOICE,
                defaultMultipleChoiceOptionsValid()
        );

        String expectedExceptionDetailMessage = "Curso não encontrado com ID: " + nonExistentCourseId;
        ResourceNotFoundException thrownException = new ResourceNotFoundException(expectedExceptionDetailMessage);

        doThrow(thrownException)
                .when(createTaskUseCaseMock).execute(any(TaskCreationRequest.class));

        // Act
        ResultActions resultActions = mockMvc.perform(post("/task/new")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestWithNonExistentCourse)));

        // Assert
        String expectedTypeUri = errorBaseUri + ProblemType.RESOURCE_NOT_FOUND.getPath();
        resultActions
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
    @DisplayName("POST /task/new com MULTIPLE_CHOICE quando UseCase lança RuntimeException retorna 500 Internal Server Error")
    void createNewTask_whenMultipleChoiceAndUseCaseThrowsUnhandledRuntimeException_shouldReturnInternalServerError() throws Exception {
        // Arrange
        MultipleChoiceTaskCreationRequest validRequest = new MultipleChoiceTaskCreationRequest(
                14L,
                "Enunciado válido para teste de erro 500 (múltipla escolha)",
                1,
                Type.MULTIPLE_CHOICE,
                defaultMultipleChoiceOptionsValid()
        );

        RuntimeException unexpectedException = new RuntimeException("Erro interno simulado para múltipla escolha");
        doThrow(unexpectedException)
                .when(createTaskUseCaseMock).execute(any(TaskCreationRequest.class));

        // Act
        ResultActions resultActions = mockMvc.perform(post("/task/new")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)));

        // Assert
        String expectedTypeUri = errorBaseUri + ProblemType.UNEXPECTED_ERROR.getPath();
        resultActions
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(HttpStatus.INTERNAL_SERVER_ERROR.value()))
                .andExpect(jsonPath("$.type").value(expectedTypeUri))
                .andExpect(jsonPath("$.title").value(ProblemType.UNEXPECTED_ERROR.getTitle()))
                .andExpect(jsonPath("$.detail").value(ProblemType.UNEXPECTED_ERROR.getMessage()))
                .andExpect(jsonPath("$.userMessage").value(ProblemType.UNEXPECTED_ERROR.getMessage()))
                .andExpect(jsonPath("$.instance").value("/task/new"));
    }

    @Test
    @DisplayName("POST /task/new com MULTIPLE_CHOICE quando usuário é STUDENT retorna 403 Forbidden")
    @WithMockUser(username = "estudante.curioso@example.com", roles = {"STUDENT"}) // <<< SIMULA UM USUÁRIO COM ROLE STUDENT
    void createNewTask_whenMultipleChoiceRequestAndUserIsStudent_shouldReturnForbidden() throws Exception {
        // Arrange
        MultipleChoiceTaskCreationRequest multipleChoiceRequestPayload = new MultipleChoiceTaskCreationRequest(
                10L,
                "Quais são frameworks backend? (Tentativa por Aluno)",
                1,
                Type.MULTIPLE_CHOICE,
                defaultMultipleChoiceOptionsValid()
        );

        // Act
        ResultActions resultActions = mockMvc.perform(post("/task/new")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(multipleChoiceRequestPayload)));

        // Assert
        resultActions
                .andExpect(status().isForbidden());
        verify(createTaskUseCaseMock, never()).execute(any(TaskCreationRequest.class));
    }
}
