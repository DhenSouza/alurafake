package br.com.alura.AluraFake.task.unitTest.factory;

import br.com.alura.AluraFake.api.dto.request.TaskCreationRequest;
import br.com.alura.AluraFake.application.factory.TaskUseCaseFactory;
import br.com.alura.AluraFake.application.interfaces.CreateTaskUseCase;
import br.com.alura.AluraFake.domain.enumeration.Type;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskUseCaseFactoryTest {

    @Mock
    private CreateTaskUseCase<TaskCreationRequest> mockOpenTextUseCase;
    @Mock
    private CreateTaskUseCase<TaskCreationRequest> mockSingleChoiceUseCase;
    @Mock
    private CreateTaskUseCase<TaskCreationRequest> mockAnotherOpenTextUseCase;

    @Test
    @DisplayName("Constructor: Should throw IllegalArgumentException when factory is built with an empty list")
    void constructor_withEmptyList_getUseCaseShouldThrowException() {
        // Arrange
        List<CreateTaskUseCase<?>> emptyUseCasesList = Collections.emptyList();
        TaskUseCaseFactory factory = new TaskUseCaseFactory(emptyUseCasesList);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            factory.getUseCase(Type.OPEN_TEXT);
        });
        assertThat(exception.getMessage()).isEqualTo("No Use Case found for the type of task: OPEN_TEXT");
    }

    @Test
    @DisplayName("Constructor: Should register and allow retrieval of multiple distinct use cases")
    void constructor_withMultipleDifferentUseCases_shouldRegisterAndAllowRetrieval() {
        // Arrange
        when(mockOpenTextUseCase.getType()).thenReturn(Type.OPEN_TEXT);
        when(mockSingleChoiceUseCase.getType()).thenReturn(Type.SINGLE_CHOICE);

        List<CreateTaskUseCase<?>> useCases = List.of(mockOpenTextUseCase, mockSingleChoiceUseCase);
        TaskUseCaseFactory factory = new TaskUseCaseFactory(useCases);

        // Act
        CreateTaskUseCase<?> retrievedOpenTextUseCase = factory.getUseCase(Type.OPEN_TEXT);
        CreateTaskUseCase<?> retrievedSingleChoiceUseCase = factory.getUseCase(Type.SINGLE_CHOICE);

        // Assert
        assertThat(retrievedOpenTextUseCase).isSameAs(mockOpenTextUseCase);
        assertThat(retrievedSingleChoiceUseCase).isSameAs(mockSingleChoiceUseCase);

        assertThatThrownBy(() -> factory.getUseCase(Type.MULTIPLE_CHOICE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No Use Case found for the type of task: MULTIPLE_CHOICE")
                .hasMessage("No Use Case found for the type of task: MULTIPLE_CHOICE");
    }

    @Test
    @DisplayName("Constructor: Should return the last registered use case when duplicate types exist")
    void constructor_withDuplicateUseCaseTypes_shouldReturnLastRegistered() {
        // Arrange
        when(mockOpenTextUseCase.getType()).thenReturn(Type.OPEN_TEXT);
        when(mockAnotherOpenTextUseCase.getType()).thenReturn(Type.OPEN_TEXT); // Same type

        List<CreateTaskUseCase<?>> useCases = List.of(mockOpenTextUseCase, mockAnotherOpenTextUseCase);
        TaskUseCaseFactory factory = new TaskUseCaseFactory(useCases);

        // Act
        CreateTaskUseCase<?> retrievedUseCase = factory.getUseCase(Type.OPEN_TEXT);

        // Assert
        assertThat(retrievedUseCase).isSameAs(mockAnotherOpenTextUseCase);
    }

    @Test
    @DisplayName("getUseCase: Should throw IllegalArgumentException if provided type is null")
    void getUseCase_whenTypeIsNull_shouldThrowIllegalArgumentException() {
        // Arrange
        TaskUseCaseFactory factory = new TaskUseCaseFactory(Collections.emptyList());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            factory.getUseCase(null);
        });

        assertThat(exception.getMessage()).isEqualTo("No Use Case found for the type of task: null");
    }

    @Test
    @DisplayName("getUseCase: Should throw IllegalArgumentException for an unregistered type")
    void getUseCase_forUnregisteredType_shouldThrowIllegalArgumentException() {
        // Arrange
        when(mockOpenTextUseCase.getType()).thenReturn(Type.OPEN_TEXT);
        List<CreateTaskUseCase<?>> useCases = List.of(mockOpenTextUseCase);
        TaskUseCaseFactory factory = new TaskUseCaseFactory(useCases);

        // Act & Assert
        Type unregisteredType = Type.SINGLE_CHOICE;
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            factory.getUseCase(unregisteredType);
        });

        assertThat(exception.getMessage()).isEqualTo("No Use Case found for the type of task: " + unregisteredType);
    }
}
