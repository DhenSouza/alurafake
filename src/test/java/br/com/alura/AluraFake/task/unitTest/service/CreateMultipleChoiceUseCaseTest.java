package br.com.alura.AluraFake.task.unitTest.service;

import br.com.alura.AluraFake.api.dto.request.ChoiceOptionRequest;
import br.com.alura.AluraFake.api.dto.request.MultipleChoiceTaskCreationRequest;
import br.com.alura.AluraFake.application.service.task.CourseTaskService;
import br.com.alura.AluraFake.application.usecase.task.CreateMultipleChoiceUseCase;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateMultipleChoiceUseCaseTest {

    @Mock
    private CourseTaskService courseTaskServiceMock;

    @InjectMocks
    private CreateMultipleChoiceUseCase createMultipleChoiceUseCase;

    @Captor
    private ArgumentCaptor<Task> taskArgumentCaptor;

    @Test
    @DisplayName("getType should return Type.MULTIPLE_CHOICE")
    void getType_shouldReturnMultipleChoice() {
        // Act
        Type actualType = createMultipleChoiceUseCase.getType();

        // Assert
        assertThat(actualType).isEqualTo(Type.MULTIPLE_CHOICE);
    }

    @Test
    @DisplayName("execute with valid MultipleChoice request should build Task with options and call CourseTaskService correctly")
    void execute_givenValidMultipleChoiceRequest_shouldBuildTaskWithOptionsAndCallServiceCorrectly() {
        // Arrange
        Long courseId = 10L;
        String statement = "Quais são linguagens de programação orientadas a objetos?";
        Integer order = 1;
        List<ChoiceOptionRequest> requestOptions = List.of(
                new ChoiceOptionRequest("Java", true),
                new ChoiceOptionRequest("Python", true),
                new ChoiceOptionRequest("Assembly", false),
                new ChoiceOptionRequest("C++", true)
        );

        MultipleChoiceTaskCreationRequest request = new MultipleChoiceTaskCreationRequest(
                courseId, statement, order, Type.MULTIPLE_CHOICE, requestOptions
        );

        // Act
        createMultipleChoiceUseCase.execute(request);

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

        for (ChoiceOptionRequest expectedDtoOption : requestOptions) {
            TaskOption actualEntityOption = capturedTask.getOptions().stream()
                    .filter(to -> to.getOptionText().equals(expectedDtoOption.option()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Mapped option not found: " + expectedDtoOption.option()));

            assertThat(actualEntityOption.getOptionText()).isEqualTo(expectedDtoOption.option());
            assertThat(actualEntityOption.getIsCorrect()).isEqualTo(expectedDtoOption.isCorrect());
        }

        long correctOptionsInTask = capturedTask.getOptions().stream().filter(TaskOption::getIsCorrect).count();
        assertThat(correctOptionsInTask).isEqualTo(3);
    }

    @Test
    @DisplayName("execute when CourseTaskService throws exception for MultipleChoice should propagate the exception")
    void execute_whenServiceThrowsExceptionForMultipleChoice_shouldPropagateException() {
        // Arrange
        Long courseId = 11L;
        String statement = "Outra pergunta de múltipla escolha.";
        Integer order = 2;
        List<ChoiceOptionRequest> requestOptions = List.of(
                new ChoiceOptionRequest("Correta 1", true),
                new ChoiceOptionRequest("Correta 2", true),
                new ChoiceOptionRequest("Incorreta 1", false)
        );
        MultipleChoiceTaskCreationRequest request = new MultipleChoiceTaskCreationRequest(
                courseId, statement, order, Type.MULTIPLE_CHOICE, requestOptions
        );

        String serviceErrorMessage = "Erro simulado ao salvar tarefa de múltipla escolha";
        RuntimeException simulatedServiceException = new RuntimeException(serviceErrorMessage);

        doThrow(simulatedServiceException).when(courseTaskServiceMock).addTaskToCourseAtPosition(
                anyLong(),
                any(Task.class),
                anyInt()
        );

        // Act & Assert
        RuntimeException thrownException = assertThrows(RuntimeException.class, () -> {
            createMultipleChoiceUseCase.execute(request);
        });

        assertThat(thrownException.getMessage()).isEqualTo(serviceErrorMessage);

        verify(courseTaskServiceMock, times(1)).addTaskToCourseAtPosition(
                eq(courseId),
                any(Task.class),
                eq(order)
        );
    }
}
