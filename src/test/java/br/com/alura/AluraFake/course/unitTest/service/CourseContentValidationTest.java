package br.com.alura.AluraFake.course.unitTest.service;

import br.com.alura.AluraFake.application.service.course.validation.CourseContentValidation;
import br.com.alura.AluraFake.domain.enumeration.Type;
import br.com.alura.AluraFake.domain.model.Course;
import br.com.alura.AluraFake.domain.model.Task;
import br.com.alura.AluraFake.domain.model.User;
import br.com.alura.AluraFake.globalHandler.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class CourseContentValidationTest {

    private CourseContentValidation courseContentValidation;
    private Course course;
    private User dummyInstructor; // Adicionado para o construtor do Course

    @BeforeEach
    void setUp() {
        courseContentValidation = new CourseContentValidation();
        dummyInstructor = new User();
        dummyInstructor.setRole(br.com.alura.AluraFake.domain.enumeration.Role.INSTRUCTOR);

        course = new Course("Curso de Teste", "Descrição do Curso de Teste", dummyInstructor);
        course.setId(1L);
    }

    private Task createTaskWithOrderAndType(Long id, int order, Type type) {
        Task task = Task.builder()
                .id(id)
                .statement("Statement para task " + id)
                .order(order)
                .typeTask(type)
                .course(this.course)
                .build();
        return task;
    }

    @Test
    @DisplayName("validateType: It must pass if the course contains all the necessary types of tasks.")
    void validateType_whenAllRequiredTaskTypesPresent_shouldNotThrowException() {
        // Arrange
        course.getTasks().add(createTaskWithOrderAndType(1L, 1, Type.OPEN_TEXT));
        course.getTasks().add(createTaskWithOrderAndType(2L, 2, Type.SINGLE_CHOICE));
        course.getTasks().add(createTaskWithOrderAndType(3L, 3, Type.MULTIPLE_CHOICE));
        course.getTasks().add(createTaskWithOrderAndType(4L, 4, Type.OPEN_TEXT));

        // Act & Assert
        assertDoesNotThrow(() -> courseContentValidation.validateType(course));
    }

    @Test
    @DisplayName("validateType: It should throw BusinessRuleException if the task list is null.")
    void validateType_whenTasksListIsNull_shouldThrowBusinessRuleException() {
        // Arrange
        course.setTasks(null);

        // Act & Assert
        assertThatThrownBy(() -> courseContentValidation.validateType(course))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("The course must contain activities to be published.");
    }

    @Test
    @DisplayName("validateType: It should throw BusinessRuleException if the task list is empty.")
    void validateType_whenTasksListIsEmpty_shouldThrowBusinessRuleException() {
        // Act & Assert
        assertThatThrownBy(() -> courseContentValidation.validateType(course))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("The course must contain activities to be published.");
    }

    @Test
    @DisplayName("validateType: Must throw BusinessRuleException if a required task type (MULTIPLE_CHOICE) is missing.")
    void validateType_whenOneRequiredTaskTypeIsMissing_shouldThrowBusinessRuleException() {
        // Arrange
        course.getTasks().add(createTaskWithOrderAndType(1L,1, Type.OPEN_TEXT));
        course.getTasks().add(createTaskWithOrderAndType(2L,2, Type.SINGLE_CHOICE));

        // Act & Assert
        assertThatThrownBy(() -> courseContentValidation.validateType(course))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("To publish the course, at least one activity of each of the following types is necessary: MULTIPLE_CHOICE");
    }

    @Test
    @DisplayName("It must throw BusinessRuleException if multiple task types are missing (SINGLE_CHOICE, MULTIPLE_CHOICE)")
    void validateType_whenMultipleRequiredTaskTypesAreMissing_shouldThrowBusinessRuleException() {
        // Arrange
        course.getTasks().add(createTaskWithOrderAndType(1L, 1, Type.OPEN_TEXT));

        // Act & Assert
        assertThatThrownBy(() -> courseContentValidation.validateType(course))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("To publish the course, at least one activity of each of the following types is necessary:")

                .hasMessageContaining(Type.SINGLE_CHOICE.name())
                .hasMessageContaining(Type.MULTIPLE_CHOICE.name());
    }

    @Test
    @DisplayName("validateTaskOrderIsContinuous: It should pass if the tasks are in continuous order (1, 2, 3)")
    void validateTaskOrderIsContinuous_whenTasksAreInContinuousOrder_shouldNotThrowException() {
        // Arrange
        course.getTasks().add(createTaskWithOrderAndType(1L, 1, Type.OPEN_TEXT));
        course.getTasks().add(createTaskWithOrderAndType(2L, 2, Type.SINGLE_CHOICE));
        course.getTasks().add(createTaskWithOrderAndType(3L, 3, Type.MULTIPLE_CHOICE));

        // Act & Assert
        assertDoesNotThrow(() -> courseContentValidation.validateTaskOrderIsContinuous(course));
    }

    @Test
    @DisplayName("validateTaskOrderIsContinuous: It should pass if the tasks are sorted to continuous order (e.g., 3, 1, 2 -> 1, 2, 3)")
    void validateTaskOrderIsContinuous_whenTasksAreUnorderedButContinuousAfterSort_shouldNotThrowException() {
        // Arrange
        course.getTasks().add(createTaskWithOrderAndType(1L, 3, Type.MULTIPLE_CHOICE));
        course.getTasks().add(createTaskWithOrderAndType(2L, 1, Type.OPEN_TEXT));
        course.getTasks().add(createTaskWithOrderAndType(3L, 2, Type.SINGLE_CHOICE));

        // Act & Assert
        assertDoesNotThrow(() -> courseContentValidation.validateTaskOrderIsContinuous(course));
    }

    @Test
    @DisplayName("validateTaskOrderIsContinuous: It should switch to a single task with order 1.")
    void validateTaskOrderIsContinuous_whenSingleTaskWithOrderOne_shouldNotThrowException() {
        // Arrange
        course.getTasks().add(createTaskWithOrderAndType(1L, 1, Type.OPEN_TEXT));

        // Act & Assert
        assertDoesNotThrow(() -> courseContentValidation.validateTaskOrderIsContinuous(course));
    }

    @Test
    @DisplayName("validateTaskOrderIsContinuous: Should throw BusinessRuleException if the task list is null")
    void validateTaskOrderIsContinuous_whenTasksListIsNull_shouldThrowBusinessRuleException() {
        // Arrange
        course.setTasks(null);

        // Act & Assert
        assertThatThrownBy(() -> courseContentValidation.validateTaskOrderIsContinuous(course))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("The course cannot be published without activities.");
    }

    @Test
    @DisplayName("validateTaskOrderIsContinuous: Should throw BusinessRuleException if the task list is empty")
    void validateTaskOrderIsContinuous_whenTasksListIsEmpty_shouldThrowBusinessRuleException() {
        // Arrange
        // Lista de tasks já está vazia devido ao construtor de Course

        // Act & Assert
        assertThatThrownBy(() -> courseContentValidation.validateTaskOrderIsContinuous(course))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("The course cannot be published without activities.");
    }

    @Test
    @DisplayName("validateTaskOrderIsContinuous: It should throw BusinessRuleException if the order does not start at 1 (e.g., 2, 3)")
    void validateTaskOrderIsContinuous_whenOrderDoesNotStartFromOne_shouldThrowBusinessRuleException() {
        // Arrange
        course.getTasks().add(createTaskWithOrderAndType(1L, 2, Type.OPEN_TEXT));
        course.getTasks().add(createTaskWithOrderAndType(2L, 3, Type.SINGLE_CHOICE));

        // Act & Assert
        assertThatThrownBy(() -> courseContentValidation.validateTaskOrderIsContinuous(course))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("The order of the course activities is not continuous. Found order 2 at position 1, expected 1.");
    }

    @Test
    @DisplayName("validateTaskOrderIsContinuous: It should throw BusinessRuleException if there is a gap in the order (e.g., 1, 3)")
    void validateTaskOrderIsContinuous_whenGapInOrder_shouldThrowBusinessRuleException() {
        // Arrange
        course.getTasks().add(createTaskWithOrderAndType(1L, 1, Type.OPEN_TEXT));
        course.getTasks().add(createTaskWithOrderAndType(2L, 3, Type.SINGLE_CHOICE));

        // Act & Assert
        assertThatThrownBy(() -> courseContentValidation.validateTaskOrderIsContinuous(course))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("The order of the course activities is not continuous. Found order 3 at position 2, expected 2.");
    }

    @Test
    @DisplayName("validateTaskOrderIsContinuous: It should throw a BusinessRuleException for duplicate orders that break the sequence.")
    void validateTaskOrderIsContinuous_whenDuplicateOrdersBreakSequence_shouldThrowBusinessRuleException() {
        // Arrange
        course.getTasks().add(createTaskWithOrderAndType(1L, 1, Type.OPEN_TEXT));
        course.getTasks().add(createTaskWithOrderAndType(2L, 2, Type.SINGLE_CHOICE));
        course.getTasks().add(createTaskWithOrderAndType(3L, 2, Type.MULTIPLE_CHOICE));
        course.getTasks().add(createTaskWithOrderAndType(4L, 3, Type.OPEN_TEXT));


        assertThatThrownBy(() -> courseContentValidation.validateTaskOrderIsContinuous(course))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("The order of the course activities is not continuous. Found order 2 at position 3, expected 3.");
    }
}
