package br.com.alura.AluraFake.task.unitTest.service;

import br.com.alura.AluraFake.api.dto.request.ChoiceOptionRequest;
import br.com.alura.AluraFake.api.dto.request.SingleChoiceTaskCreationRequest;
import br.com.alura.AluraFake.application.service.task.CourseTaskService;
import br.com.alura.AluraFake.application.usecase.task.CreateSingleChoiceUseCase;
import br.com.alura.AluraFake.domain.enumeration.Type;
import br.com.alura.AluraFake.domain.model.Task;
import br.com.alura.AluraFake.domain.model.TaskOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateSingleChoiceUseCaseTest {

    @Mock
    private CourseTaskService courseTaskServiceMock;

    @InjectMocks
    private CreateSingleChoiceUseCase createSingleChoiceUseCase;

    @Captor
    private ArgumentCaptor<Task> taskArgumentCaptor;

    @Test
    @DisplayName("getType should return Type.SINGLE_CHOICE")
    void getType_shouldReturnSingleChoice() {
        // Act
        Type actualType = createSingleChoiceUseCase.getType();

        // Assert
        assertThat(actualType).isEqualTo(Type.SINGLE_CHOICE);
    }

    @Test
    @DisplayName("Executing with a valid SingleChoiceRequest must build a Task with options and call CourseTaskService correctly.")
    void execute_givenValidSingleChoiceRequest_shouldBuildTaskWithOptionsAndCallServiceCorrectly() {
        // Arrange
        Long courseId = 1L;
        String statement = "Qual é a capital da França?";
        Integer order = 2;
        List<ChoiceOptionRequest> requestOptions = List.of(
                new ChoiceOptionRequest("Paris", true),
                new ChoiceOptionRequest("Londres", false),
                new ChoiceOptionRequest("Berlim", false)
        );
        SingleChoiceTaskCreationRequest request = new SingleChoiceTaskCreationRequest(
                courseId, statement, order, Type.SINGLE_CHOICE, requestOptions
        );

        // Act
        createSingleChoiceUseCase.execute(request);

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

        assertThat(capturedTask.getOptions()).isNotNull();
        assertThat(capturedTask.getOptions()).hasSize(requestOptions.size());


        for (int i = 0; i < requestOptions.size(); i++) {
            ChoiceOptionRequest expectedDtoOption = requestOptions.get(i);
            TaskOption actualEntityOption = capturedTask.getOptions().get(i);

            assertThat(actualEntityOption.getOptionText()).isEqualTo(expectedDtoOption.option());
            assertThat(actualEntityOption.getIsCorrect()).isEqualTo(expectedDtoOption.isCorrect());
        }
    }

    @Test
    @DisplayName("execute when CourseTaskService throws an exception must propagate the exception")
    void execute_whenCourseTaskServiceThrowsException_shouldPropagateException() {
        // Arrange
        Long courseId = 2L;
        String statement = "Outra pergunta de escolha única.";
        Integer order = 1;
        List<ChoiceOptionRequest> requestOptions = List.of(
                new ChoiceOptionRequest("Sim", true),
                new ChoiceOptionRequest("Não", false)
        );
        SingleChoiceTaskCreationRequest request = new SingleChoiceTaskCreationRequest(
                courseId, statement, order, Type.SINGLE_CHOICE, requestOptions
        );

        String serviceErrorMessage = "Simulated error when trying to add task to the course";

        RuntimeException simulatedServiceException = new RuntimeException(serviceErrorMessage);

        doThrow(simulatedServiceException).when(courseTaskServiceMock).addTaskToCourseAtPosition(
                anyLong(),
                any(Task.class),
                anyInt()
        );

        // Act & Assert
        RuntimeException thrownException = assertThrows(RuntimeException.class, () -> {
            createSingleChoiceUseCase.execute(request);
        });

        assertThat(thrownException.getMessage()).isEqualTo(serviceErrorMessage);

        verify(courseTaskServiceMock, times(1)).addTaskToCourseAtPosition(
                eq(courseId),
                any(Task.class),
                eq(order)
        );
    }
}