package br.com.alura.AluraFake.task.unitTest.controller;

import br.com.alura.AluraFake.api.controller.TaskController;
import br.com.alura.AluraFake.api.dto.request.ChoiceOptionRequest;
import br.com.alura.AluraFake.api.dto.request.OpenTextTaskCreationRequest;
import br.com.alura.AluraFake.api.dto.request.SingleChoiceTaskCreationRequest;
import br.com.alura.AluraFake.api.dto.request.TaskCreationRequest;
import br.com.alura.AluraFake.application.factory.TaskUseCaseFactory;
import br.com.alura.AluraFake.application.interfaces.CreateTaskUseCase;
import br.com.alura.AluraFake.domain.enumeration.Type;
import br.com.alura.AluraFake.exceptionhandler.GlobalExceptionHandler;
import br.com.alura.AluraFake.exceptionhandler.OptionalInvalidException;
import br.com.alura.AluraFake.exceptionhandler.ResourceNotFoundException;
import br.com.alura.AluraFake.exceptionhandler.dto.ProblemType;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(TaskController.class)
@Import(GlobalExceptionHandler.class)
public class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskUseCaseFactory useCaseFactory;

    @MockBean
    private CreateTaskUseCase createTaskUseCaseMock;

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

    private List<ChoiceOptionRequest> defaultSingleChoiceOptionsInvalidSingleChoice() {
        return List.of(
                new ChoiceOptionRequest("Opção A", true),
                new ChoiceOptionRequest("Opção B", false)
        );
    }

    private List<ChoiceOptionRequest> multipleChoiceOptions_NoIncorrect() {
        return List.of(
                new ChoiceOptionRequest("Opção Múltipla A (Correta)", true),
                new ChoiceOptionRequest("Opção Múltipla B (Correta)", true),
                new ChoiceOptionRequest("Opção Múltipla C (Correta)", true)
        );
    }

    private List<ChoiceOptionRequest> multipleChoiceOptions_TooFewOptions() {
        return List.of(
                new ChoiceOptionRequest("Opção Múltipla A (Correta)", true),
                new ChoiceOptionRequest("Opção Múltipla B (Correta)", true)
        );
    }

    @Test
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

        OpenTextTaskCreationRequest captured = (OpenTextTaskCreationRequest) captor.getValue();
        assertThat(captured.courseId()).isEqualTo(1L);
        assertThat(captured.statement()).isEqualTo("Descreva o padrão Strategy.");
        assertThat(captured.type()).isEqualTo(Type.OPEN_TEXT);
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
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
                .andExpect(status().isOk())                                // verifica 200 OK
                .andExpect(jsonPath("$.success").value(true));            // verifica success=true

        // Verify
        verify(useCaseFactory).getUseCase(eq(Type.SINGLE_CHOICE));
        verify(createTaskUseCaseMock).execute(captor.capture());

        SingleChoiceTaskCreationRequest captured =
                (SingleChoiceTaskCreationRequest) captor.getValue();

        assertThat(captured.courseId()).isEqualTo(2L);
        assertThat(captured.statement()).isEqualTo("Qual é a capital da França?");
        assertThat(captured.order()).isEqualTo(1);
        assertThat(captured.type()).isEqualTo(Type.SINGLE_CHOICE);

        assertThat(captured.options()).hasSize(3);
        assertThat(captured.options().get(0).option()).isEqualTo("Opção A (Correta)");
        assertThat(captured.options().get(0).isCorrect()).isTrue();
    }

    @Test
    @DisplayName("POST /task/new com SINGLE_CHOICE e nenhuma opção correta retorna 400 Bad Request")
    void createNewTask_whenSingleChoiceHasNoCorrectOption_shouldReturnBadRequest() throws Exception {
        // Arrange
        SingleChoiceTaskCreationRequest invalidSingleChoiceRequest = new SingleChoiceTaskCreationRequest(
                null,
                "Qual opção está correta aqui?",
                1,
                Type.SINGLE_CHOICE,
                defaultSingleChoiceOptionsInvalidSingleChoice()
        );

        String expectedDetailMessageFromException = "Um ou mais campos estão inválidos. Faça o preenchimento correto e tente novamente.";

        // Act
        ResultActions resultActions = mockMvc.perform(post("/task/new")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidSingleChoiceRequest)));

        String expectedTypeUri = errorBaseUri + ProblemType.DEFAULT_USER_MESSAGE_VALIDATION.getPath();

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.type").value(expectedTypeUri))
                .andExpect(jsonPath("$.title").value(ProblemType.DEFAULT_USER_MESSAGE_VALIDATION.getTitle()))
                .andExpect(jsonPath("$.detail").value(expectedDetailMessageFromException))
                .andExpect(jsonPath("$.userMessage").value(ProblemType.DEFAULT_USER_MESSAGE_VALIDATION.getMessage()))
                .andExpect(jsonPath("$.instance").value("/task/new"));
    }

    @Test
    @DisplayName("POST /task/new com SINGLE_CHOICE e nenhuma opção correta retorna 404 Bad Request")
    void createNewTask_whenSingleChoiceHasNoCorrectOption_shouldReturnNotFound() throws Exception {
        // Arrange
        long nonExistentCourseId = 999L;

        SingleChoiceTaskCreationRequest requestWithNonExistentCourse = new SingleChoiceTaskCreationRequest(
                nonExistentCourseId,
                "Qual opção está correta aqui?",
                1,
                Type.SINGLE_CHOICE,
                defaultSingleChoiceOptionsInvalidSingleChoice()
        );

        String expectedExceptionDetailMessage = "Curso não encontrado com ID: " + nonExistentCourseId;
        ResourceNotFoundException thrownException = new ResourceNotFoundException(expectedExceptionDetailMessage);

        doThrow(thrownException)
                .when(createTaskUseCaseMock).execute(any(SingleChoiceTaskCreationRequest.class));

        // Act
        ResultActions resultActions = mockMvc.perform(post("/task/new")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestWithNonExistentCourse)));

        // Assert
        String expectedTypeUri = errorBaseUri + ProblemType.RESOURCE_NOT_FOUND.getPath();

        resultActions
                .andExpect(status().isNotFound()) // HTTP 404
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.type").value(expectedTypeUri))
                .andExpect(jsonPath("$.title").value(ProblemType.RESOURCE_NOT_FOUND.getTitle()))
                .andExpect(jsonPath("$.detail").value(expectedExceptionDetailMessage)) // 'detail' vem de ex.getMessage()
                .andExpect(jsonPath("$.userMessage").value(ProblemType.RESOURCE_NOT_FOUND.getMessage())) // 'userMessage' vem do ProblemType
                .andExpect(jsonPath("$.instance").value("/task/new"));
    }

    @Test
    @DisplayName("POST /task/new quando UseCase lança RuntimeException inesperada retorna 500 Internal Server Error")
    void createNewTask_whenUseCaseThrowsUnhandledRuntimeException_shouldReturnInternalServerError() throws Exception {
        // Arrange
        SingleChoiceTaskCreationRequest validRequest = new SingleChoiceTaskCreationRequest(
                3L,
                "Enunciado válido para teste de erro 500",
                1,
                Type.SINGLE_CHOICE,
                defaultSingleChoiceOptionsInvalidSingleChoice()
        );

        RuntimeException unexpectedException = new RuntimeException("boom - erro inesperado no servidor");
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
}
