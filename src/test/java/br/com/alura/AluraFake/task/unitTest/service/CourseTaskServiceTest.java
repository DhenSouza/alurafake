package br.com.alura.AluraFake.task.unitTest.service;

import br.com.alura.AluraFake.application.service.task.CourseTaskService;
import br.com.alura.AluraFake.domain.model.Course;
import br.com.alura.AluraFake.domain.model.Task;
import br.com.alura.AluraFake.domain.repository.CourseRepository;
import br.com.alura.AluraFake.domain.repository.TaskRepository;
import br.com.alura.AluraFake.exceptionhandler.InvalidCourseTaskOperationException;
import br.com.alura.AluraFake.exceptionhandler.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseTaskServiceTest {

    @Mock
    private CourseRepository courseRepositoryMock;

    @Mock
    private TaskRepository taskRepositoryMock;

    @InjectMocks
    private CourseTaskService courseTaskService;

    @Captor
    private ArgumentCaptor<Task> taskArgumentCaptor;

    private Course course;
    private Task task1, task2, newTask;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setId(1L);
        course.setTasks(new ArrayList<>());

        task1 = new Task();
        task1.setId(101L);
        task1.setOrder(1);
        task1.setCourse(course);

        task2 = new Task();
        task2.setId(102L);
        task2.setOrder(2);
        task2.setCourse(course);

        newTask = new Task();
        newTask.setId(103L);
        newTask.setStatement("Nova Tarefa");
    }

    @Test
    @DisplayName("addTaskToCourseAtPosition: Adicionar tarefa em curso vazio na posição 1")
    void addTaskToCourseAtPosition_whenCourseIsEmptyAndPositionIsOne_shouldAddTaskSuccessfully() {
        // Arrange
        when(courseRepositoryMock.findById(1L)).thenReturn(Optional.of(course));
        when(taskRepositoryMock.save(any(Task.class))).thenReturn(newTask);

        // Act
        Course updatedCourse = courseTaskService.addTaskToCourseAtPosition(1L, newTask, 1);

        // Assert
        verify(courseRepositoryMock).findById(1L);
        verify(taskRepositoryMock).save(taskArgumentCaptor.capture());

        Task savedTask = taskArgumentCaptor.getValue();
        assertThat(savedTask.getOrder()).isEqualTo(1);
        assertThat(savedTask.getCourse()).isEqualTo(course);
        assertThat(updatedCourse.getTasks()).containsExactly(savedTask);
    }

    @Test
    @DisplayName("addTaskToCourseAtPosition: Adicionar tarefa no meio de curso com tarefas existentes")
    void addTaskToCourseAtPosition_whenAddingToMiddleOfExistingTasks_shouldReorderAndAddTask() {
        // Arrange
        // Ordem 1
        course.getTasks().add(task1);
        // Será ordem 2 original
        Task task3 = new Task(); task3.setId(104L); task3.setOrder(2); task3.setCourse(course);
        course.getTasks().add(task3);

        when(courseRepositoryMock.findById(1L)).thenReturn(Optional.of(course));
        when(taskRepositoryMock.save(any(Task.class))).thenReturn(newTask);

        // Act
        Course updatedCourse = courseTaskService.addTaskToCourseAtPosition(1L, newTask, 2);

        // Assert
        verify(taskRepositoryMock).save(taskArgumentCaptor.capture());
        Task savedNewTask = taskArgumentCaptor.getValue();

        // newTask fica na posição 2
        assertThat(savedNewTask.getOrder()).isEqualTo(2);
        // task1 mantém a posição 1
        assertThat(task1.getOrder()).isEqualTo(1);
        // task3 (originalmente 2) é movida para 3
        assertThat(task3.getOrder()).isEqualTo(3);

        assertThat(updatedCourse.getTasks()).containsExactlyInAnyOrder(task1, savedNewTask, task3);
        assertThat(updatedCourse.getTasks()).extracting(Task::getOrder).containsExactly(1, 3, 2);
    }

    @Test
    @DisplayName("addTaskToCourseAtPosition: Deve lançar ResourceNotFoundException se curso não encontrado")
    void addTaskToCourseAtPosition_whenCourseNotFound_shouldThrowResourceNotFoundException() {
        // Arrange
        when(courseRepositoryMock.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            courseTaskService.addTaskToCourseAtPosition(99L, newTask, 1);
        });
        assertThat(exception.getMessage()).isEqualTo("Curso não encontrado com ID: 99");
        verify(taskRepositoryMock, never()).save(any());
    }

    @Test
    @DisplayName("addTaskToCourseAtPosition: Deve lançar InvalidCourseTaskOperationException se posição for inválida (muito alta)")
    void addTaskToCourseAtPosition_whenPositionIsTooHigh_shouldThrowInvalidCourseTaskOperationException() {
        // Arrange
        course.getTasks().add(task1);
        when(courseRepositoryMock.findById(1L)).thenReturn(Optional.of(course));

        // Act & Assert: Posição 3 é inválida (permitido seria 1 ou 2)
        InvalidCourseTaskOperationException exception = assertThrows(InvalidCourseTaskOperationException.class, () -> {
            courseTaskService.addTaskToCourseAtPosition(1L, newTask, 3);
        });
        assertThat(exception.getMessage()).isEqualTo("Ordem de tarefa inválida. A ordem deve ser contínua e estar entre 1 e 2.");
    }

    @Test
    @DisplayName("removeTaskFromCourse: Remover tarefa do meio e reordenar restantes")
    void removeTaskFromCourse_whenRemovingMiddleTask_shouldReorderRemainingTasks() {
        // Arrange
        course.getTasks().addAll(List.of(task1, task2, newTask));
        newTask.setOrder(3); newTask.setCourse(course);
        task1.setOrder(1); task2.setOrder(2);

        when(courseRepositoryMock.findById(1L)).thenReturn(Optional.of(course));
        when(taskRepositoryMock.findById(task2.getId())).thenReturn(Optional.of(task2));

        // Act
        Course updatedCourse = courseTaskService.removeTaskFromCourse(1L, task2.getId());

        // Assert
        assertThat(updatedCourse.getTasks()).doesNotContain(task2);
        assertThat(updatedCourse.getTasks()).hasSize(2);

        // Verifica reordenamento
        Task remainingTask1 = updatedCourse.getTasks().stream().filter(t -> t.getId().equals(task1.getId())).findFirst().orElseThrow();
        Task remainingNewTask = updatedCourse.getTasks().stream().filter(t -> t.getId().equals(newTask.getId())).findFirst().orElseThrow();

        assertThat(remainingTask1.getOrder()).isEqualTo(1);
        assertThat(remainingNewTask.getOrder()).isEqualTo(2);
    }

    @Test
    @DisplayName("removeTaskFromCourse: Remover única tarefa do curso")
    void removeTaskFromCourse_whenRemovingOnlyTask_shouldResultInEmptyTaskList() {
        // Arrange
        course.getTasks().add(task1);
        when(courseRepositoryMock.findById(1L)).thenReturn(Optional.of(course));
        when(taskRepositoryMock.findById(task1.getId())).thenReturn(Optional.of(task1));

        // Act
        Course updatedCourse = courseTaskService.removeTaskFromCourse(1L, task1.getId());

        // Assert
        assertThat(updatedCourse.getTasks()).isEmpty();
    }

    @Test
    @DisplayName("removeTaskFromCourse: Deve lançar ResourceNotFoundException se curso não for encontrado")
    void removeTaskFromCourse_whenCourseNotFound_shouldThrowResourceNotFoundException() {
        // Arrange
        when(courseRepositoryMock.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            courseTaskService.removeTaskFromCourse(99L, 101L);
        });
        assertThat(exception.getMessage()).isEqualTo("Curso não encontrado com ID: 99");
    }

    @Test
    @DisplayName("removeTaskFromCourse: Deve lançar ResourceNotFoundException se tarefa não for encontrada")
    void removeTaskFromCourse_whenTaskNotFound_shouldThrowResourceNotFoundException() {
        // Arrange
        when(courseRepositoryMock.findById(1L)).thenReturn(Optional.of(course));
        when(taskRepositoryMock.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            courseTaskService.removeTaskFromCourse(1L, 999L);
        });
        assertThat(exception.getMessage()).isEqualTo("Tarefa não encontrada com ID: 999");
    }

    @Test
    @DisplayName("removeTaskFromCourse: Deve lançar InvalidCourseTaskOperationException se tarefa não pertence ao curso")
    void removeTaskFromCourse_whenTaskDoesNotBelongToCourse_shouldThrowInvalidCourseTaskOperationException() {
        // Arrange
        Course otherCourse = new Course(); otherCourse.setId(2L);
        task1.setCourse(otherCourse);

        when(courseRepositoryMock.findById(1L)).thenReturn(Optional.of(course));
        when(taskRepositoryMock.findById(task1.getId())).thenReturn(Optional.of(task1));

        // Act & Assert
        InvalidCourseTaskOperationException exception = assertThrows(InvalidCourseTaskOperationException.class, () -> {
            courseTaskService.removeTaskFromCourse(1L, task1.getId());
        });
        assertThat(exception.getMessage()).isEqualTo("A tarefa com ID " + task1.getId() + " não pertence ao curso com ID: 1");
    }

}
