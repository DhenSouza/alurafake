package br.com.alura.AluraFake.task.unitTest.controller;

import br.com.alura.AluraFake.api.controller.TaskController;
import br.com.alura.AluraFake.api.dto.request.OpenTextTaskCreationRequest;
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

    @Test
    @DisplayName("POST /task/new com payload válido retorna 200 OK e success=true")
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
    @DisplayName("POST /task/new com payload inválido retorna 400 Bad Request")
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
    @DisplayName("POST /task/new quando recurso não existe retorna 404 Not Found")
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
    @DisplayName("POST /task/new quando regra de negócio viola OptionalInvalidException retorna 400 Bad Request")
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
    @DisplayName("POST /task/new quando exceção desconhecida retorna 500 Internal Server Error")
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
}
