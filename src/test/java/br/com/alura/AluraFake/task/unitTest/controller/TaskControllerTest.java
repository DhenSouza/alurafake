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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(TaskController.class)
@Import(GlobalExceptionHandler.class) // <<< ADICIONADO PARA CARREGAR SEU HANDLER GLOBAL
public class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskUseCaseFactory useCaseFactory;

    @MockBean
    private CreateTaskUseCase createTaskUseCaseMock;


    @Captor
    private ArgumentCaptor<TaskCreationRequest> taskCreationRequestCaptor;

    private final String errorBaseUri = "https://api.seusite.com/erros";


    @BeforeEach
    void setUp() {
        when(useCaseFactory.getUseCase(any(Type.class))).thenReturn(createTaskUseCaseMock);
    }


    @Test
    @DisplayName("POST /task/new com payload válido retorna 200 OK")
    void createNewTask_whenValidOpenTextRequest_shouldReturnOk() throws Exception {
        // Arrange
        OpenTextTaskCreationRequest openTextRequest = new OpenTextTaskCreationRequest(
                1L,
                "Descreva o padrão Strategy.",
                1,
                Type.OPEN_TEXT
        );

        doNothing().when(createTaskUseCaseMock).execute(any(TaskCreationRequest.class));

        mockMvc.perform(post("/task/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(openTextRequest)))
                .andExpect(status().isOk());

        verify(useCaseFactory).getUseCase(eq(Type.OPEN_TEXT));
        verify(createTaskUseCaseMock).execute(taskCreationRequestCaptor.capture());

        assertThat(taskCreationRequestCaptor.getValue()).isInstanceOf(OpenTextTaskCreationRequest.class);
        OpenTextTaskCreationRequest capturedRequest = (OpenTextTaskCreationRequest) taskCreationRequestCaptor.getValue();
        assertThat(capturedRequest.courseId()).isEqualTo(1L);
        assertThat(capturedRequest.statement()).isEqualTo("Descreva o padrão Strategy.");
        assertThat(capturedRequest.type()).isEqualTo(Type.OPEN_TEXT);
    }

    @Test
    void createNewTask_whenRequestIsInvalid_shouldReturnBadRequest() throws Exception {
        // Arrange
        OpenTextTaskCreationRequest invalidRequest = new OpenTextTaskCreationRequest(
                1L,
                "Statement válido",
                0,
                Type.OPEN_TEXT
        );

        mockMvc.perform(post("/task/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())

                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.type").value(errorBaseUri + ProblemType.DEFAULT_USER_MESSAGE_VALIDATION.getPath()))
                .andExpect(jsonPath("$.title").value(ProblemType.DEFAULT_USER_MESSAGE_VALIDATION.getTitle()))
                .andExpect(jsonPath("$.userMessage").value(ProblemType.DEFAULT_USER_MESSAGE_VALIDATION.getMessage()))
                .andExpect(jsonPath("$.fields[0].name").value("order")) // Verifica o campo específico
                .andExpect(jsonPath("$.fields[0].userMessage").exists()); // Verifica se a mensagem do campo existe
    }

    @Test
    void createNewTask_whenUseCaseThrowsResourceNotFoundException_shouldReturnNotFound() throws Exception {
        // Arrange
        OpenTextTaskCreationRequest validRequest = new OpenTextTaskCreationRequest(
                20L, "Statement válido para teste de exceção", 1, Type.OPEN_TEXT
        );

        String expectedExceptionDetailMessage = "Recurso específico não encontrado durante a criação da tarefa.";
        ResourceNotFoundException thrownException = new ResourceNotFoundException(expectedExceptionDetailMessage);

        doThrow(thrownException)
                .when(createTaskUseCaseMock).execute(any(TaskCreationRequest.class));

        // Act
        ResultActions resultActions = mockMvc.perform(post("/task/new")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)));

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
    void createNewTask_whenUseCaseThrowsOptionalInvalidException_shouldReturnBadRequest() throws Exception {
        // Arrange
        OpenTextTaskCreationRequest validRequest = new OpenTextTaskCreationRequest(
                1L, "Statement válido", 1, Type.OPEN_TEXT
        );
        String expectedErrorMessage = "Uma regra de negócio específica para OptionalInvalidException foi violada.";
        OptionalInvalidException thrownException = new OptionalInvalidException(expectedErrorMessage);

        doThrow(thrownException)
                .when(createTaskUseCaseMock).execute(any(TaskCreationRequest.class));

        // Act
        ResultActions resultActions = mockMvc.perform(post("/task/new")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)));

        // Assert
        String expectedTypeUri = errorBaseUri + ProblemType.MESSAGE_NOT_READABLE.getPath();

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.type").value(expectedTypeUri))
                .andExpect(jsonPath("$.title").value(ProblemType.MESSAGE_NOT_READABLE.getTitle()))
                .andExpect(jsonPath("$.detail").value(expectedErrorMessage))
                .andExpect(jsonPath("$.userMessage").value(ProblemType.MESSAGE_NOT_READABLE.getMessage()))
                .andExpect(jsonPath("$.instance").value("/task/new"));
    }
}
