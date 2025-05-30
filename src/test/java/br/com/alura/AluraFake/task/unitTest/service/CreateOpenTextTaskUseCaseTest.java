package br.com.alura.AluraFake.task.unitTest.service;

import br.com.alura.AluraFake.api.dto.request.OpenTextTaskCreationRequest;
import br.com.alura.AluraFake.application.service.task.CourseTaskService;
import br.com.alura.AluraFake.application.usecase.task.CreateOpenTextTaskUseCase;
import br.com.alura.AluraFake.domain.enumeration.Type;
import br.com.alura.AluraFake.domain.model.Task;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreateOpenTextTaskUseCaseTest {

    @Mock // Cria um mock para CourseTaskService
    private CourseTaskService courseTaskServiceMock;

    @InjectMocks // Cria uma instância de CreateOpenTextTaskUseCase e injeta os mocks nela
    private CreateOpenTextTaskUseCase createOpenTextTaskUseCase;

    @Captor
    private ArgumentCaptor<Task> taskArgumentCaptor;

    @Test
    void getType_shouldReturnOpenText() {
        // Act
        Type type = createOpenTextTaskUseCase.getType();

        // Assert
        assertThat(type).isEqualTo(Type.OPEN_TEXT);
    }

    @Test
    @DisplayName("execute com request válido deve construir Task e chamar CourseTaskService corretamente")
    void execute_givenValidRequest_shouldBuildTaskAndCallServiceWithCorrectParameters() {
        // Arrange
        Long courseId = 1L;
        String statement = "Qual é o principal objetivo do TDD?";
        Integer order = 3;
        Type type = Type.OPEN_TEXT;
        OpenTextTaskCreationRequest request = new OpenTextTaskCreationRequest(courseId, statement, order, type);


        // Act
        createOpenTextTaskUseCase.execute(request);

        // Assert
        verify(courseTaskServiceMock, times(1)).addTaskToCourseAtPosition(
                eq(courseId),
                taskArgumentCaptor.capture(),
                eq(order)
        );

        Task capturedTask = taskArgumentCaptor.getValue();
        assertThat(capturedTask).isNotNull();
        assertThat(capturedTask.getStatement()).isEqualTo(statement);
        assertThat(capturedTask.getOrder()).isEqualTo(order);
    }

    @Test
    @DisplayName("execute quando CourseTaskService lança exceção deve propagar a exceção")
    void execute_whenServiceThrowsException_shouldPropagateException() {
        // Arrange
        Long courseId = 2L;
        String statement = "Outra pergunta de texto.";
        Integer order = 1;
        Type type = Type.OPEN_TEXT;
        OpenTextTaskCreationRequest request = new OpenTextTaskCreationRequest(courseId, statement, order, type);

        String errorMessage = "Erro simulado ao adicionar tarefa ao curso";
        RuntimeException simulatedException = new RuntimeException(errorMessage);


        doThrow(simulatedException).when(courseTaskServiceMock).addTaskToCourseAtPosition(
                anyLong(),   // Pode usar any() ou eq() para os argumentos
                any(Task.class),
                anyInt()
        );

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            createOpenTextTaskUseCase.execute(request);
        });

        assertThat(thrown.getMessage()).isEqualTo(errorMessage);

        verify(courseTaskServiceMock, times(1)).addTaskToCourseAtPosition(
                eq(courseId),
                any(Task.class),
                eq(order)
        );
    }
}
