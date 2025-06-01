package br.com.alura.AluraFake.task.unitTest.service;

import br.com.alura.AluraFake.application.service.task.CourseTaskService;
import br.com.alura.AluraFake.domain.enumeration.Status;
import br.com.alura.AluraFake.domain.model.Course;
import br.com.alura.AluraFake.domain.model.Task;
import br.com.alura.AluraFake.domain.repository.CourseRepository;
import br.com.alura.AluraFake.domain.repository.TaskRepository;
import br.com.alura.AluraFake.globalHandler.InvalidCourseTaskOperationException;
import br.com.alura.AluraFake.globalHandler.ResourceNotFoundException;
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
        course.setStatus(Status.BUILDING);
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
        newTask.setStatement("New Task");
    }

    @Test
    @DisplayName("addTaskToCourseAtPosition: Should add task successfully when course is empty and position is one")
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
    @DisplayName("addTaskToCourseAtPosition: Should reorder and add task when inserting into middle of existing tasks")
    void addTaskToCourseAtPosition_whenAddingToMiddleOfExistingTasks_shouldReorderAndAddTask() {
        // Arrange
        course.getTasks().add(task1);
        Task task3 = new Task();
        task3.setId(104L);
        task3.setOrder(2);
        task3.setCourse(course);
        course.getTasks().add(task3);

        when(courseRepositoryMock.findById(1L)).thenReturn(Optional.of(course));
        when(taskRepositoryMock.save(any(Task.class))).thenReturn(newTask);

        // Act
        Course updatedCourse = courseTaskService.addTaskToCourseAtPosition(1L, newTask, 2);

        // Assert
        verify(taskRepositoryMock).save(taskArgumentCaptor.capture());
        Task savedNewTask = taskArgumentCaptor.getValue();

        assertThat(savedNewTask.getOrder()).isEqualTo(2);
        assertThat(task1.getOrder()).isEqualTo(1);
        assertThat(task3.getOrder()).isEqualTo(3);

        assertThat(updatedCourse.getTasks())
                .containsExactlyInAnyOrder(task1, savedNewTask, task3);
        assertThat(updatedCourse.getTasks())
                .extracting(Task::getOrder)
                .containsExactly(1, 3, 2);
    }

    @Test
    @DisplayName("addTaskToCourseAtPosition: Should throw ResourceNotFoundException when course not found")
    void addTaskToCourseAtPosition_whenCourseNotFound_shouldThrowResourceNotFoundException() {
        // Arrange
        when(courseRepositoryMock.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                courseTaskService.addTaskToCourseAtPosition(99L, newTask, 1)
        );
        assertThat(exception.getMessage()).isEqualTo("Course not found with ID: 99");
        verify(taskRepositoryMock, never()).save(any());
    }

    @Test
    @DisplayName("addTaskToCourseAtPosition: Should throw InvalidCourseTaskOperationException when position is too high")
    void addTaskToCourseAtPosition_whenPositionIsTooHigh_shouldThrowInvalidCourseTaskOperationException() {
        // Arrange
        course.getTasks().add(task1);
        when(courseRepositoryMock.findById(1L)).thenReturn(Optional.of(course));

        // Act & Assert
        InvalidCourseTaskOperationException exception = assertThrows(InvalidCourseTaskOperationException.class, () ->
                courseTaskService.addTaskToCourseAtPosition(1L, newTask, 3)
        );
        assertThat(exception.getMessage()).isEqualTo("Invalid task order. The order must be continuous and between 1 and 2.");
    }

    @Test
    @DisplayName("removeTaskFromCourse: Should reorder remaining tasks when removing a middle task")
    void removeTaskFromCourse_whenRemovingMiddleTask_shouldReorderRemainingTasks() {
        // Arrange
        course.getTasks().addAll(List.of(task1, task2, newTask));
        newTask.setOrder(3);
        newTask.setCourse(course);
        task1.setOrder(1);
        task2.setOrder(2);

        when(courseRepositoryMock.findById(1L)).thenReturn(Optional.of(course));
        when(taskRepositoryMock.findById(task2.getId())).thenReturn(Optional.of(task2));

        // Act
        Course updatedCourse = courseTaskService.removeTaskFromCourse(1L, task2.getId());

        // Assert
        assertThat(updatedCourse.getTasks()).doesNotContain(task2);
        assertThat(updatedCourse.getTasks()).hasSize(2);

        Task remainingTask1 = updatedCourse.getTasks()
                .stream().filter(t -> t.getId().equals(task1.getId())).findFirst().orElseThrow();
        Task remainingNewTask = updatedCourse.getTasks()
                .stream().filter(t -> t.getId().equals(newTask.getId())).findFirst().orElseThrow();

        assertThat(remainingTask1.getOrder()).isEqualTo(1);
        assertThat(remainingNewTask.getOrder()).isEqualTo(2);
    }

    @Test
    @DisplayName("removeTaskFromCourse: Should result in empty task list when removing the only task")
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
    @DisplayName("removeTaskFromCourse: Should throw ResourceNotFoundException when course not found")
    void removeTaskFromCourse_whenCourseNotFound_shouldThrowResourceNotFoundException() {
        // Arrange
        when(courseRepositoryMock.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                courseTaskService.removeTaskFromCourse(99L, 101L)
        );
        assertThat(exception.getMessage()).isEqualTo("Course not found with ID: 99");
    }

    @Test
    @DisplayName("removeTaskFromCourse: Should throw ResourceNotFoundException when task not found")
    void removeTaskFromCourse_whenTaskNotFound_shouldThrowResourceNotFoundException() {
        // Arrange
        when(courseRepositoryMock.findById(1L)).thenReturn(Optional.of(course));
        when(taskRepositoryMock.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                courseTaskService.removeTaskFromCourse(1L, 999L)
        );
        assertThat(exception.getMessage()).isEqualTo("Task not found with ID: 999");
    }

    @Test
    @DisplayName("removeTaskFromCourse: Should throw InvalidCourseTaskOperationException when task does not belong to course")
    void removeTaskFromCourse_whenTaskDoesNotBelongToCourse_shouldThrowInvalidCourseTaskOperationException() {
        // Arrange
        Course otherCourse = new Course();
        otherCourse.setId(2L);
        task1.setCourse(otherCourse);

        when(courseRepositoryMock.findById(1L)).thenReturn(Optional.of(course));
        when(taskRepositoryMock.findById(task1.getId())).thenReturn(Optional.of(task1));

        // Act & Assert
        InvalidCourseTaskOperationException exception = assertThrows(InvalidCourseTaskOperationException.class, () ->
                courseTaskService.removeTaskFromCourse(1L, task1.getId())
        );
        assertThat(exception.getMessage())
                .isEqualTo("The task with ID " + task1.getId() + " does not belong to the course with ID: 1");
    }

    @Test
    @DisplayName("addTaskToCourseAtPosition: Should throw InvalidCourseTaskOperationException when new task statement already exists in course")
    void addTaskToCourseAtPosition_whenNewTaskStatementAlreadyExistsInCourse_shouldThrowInvalidCourseTaskOperationException() {
        // Arrange
        String existingStatement = "What is the color of the sky?";
        task1.setStatement(existingStatement);
        course.getTasks().add(task1);

        when(courseRepositoryMock.findById(1L)).thenReturn(Optional.of(course));

        newTask.setStatement("  WHAT IS the COLOR OF THE SKY?  ");
        String expectedMessage = "The course already has a question with the statement: '" + newTask.getStatement() + "'";

        // Act & Assert
        InvalidCourseTaskOperationException exception = assertThrows(InvalidCourseTaskOperationException.class, () ->
                courseTaskService.addTaskToCourseAtPosition(1L, newTask, 2)
        );
        assertThat(exception.getMessage()).isEqualTo(expectedMessage);

        verify(taskRepositoryMock, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("addTaskToCourseAtPosition: Should throw InvalidCourseTaskOperationException when course status is not BUILDING")
    void addTaskToCourseAtPosition_whenCourseStatusIsNotBuilding_shouldThrowInvalidCourseTaskOperationException() {
        // Arrange
        Long courseId = 1L;
        course.setStatus(Status.PUBLISHED);
        when(courseRepositoryMock.findById(courseId)).thenReturn(Optional.of(course));

        int position = 1;
        String expectedErrorMessage = String.format(
                "It is not possible to add tasks to the course '%s' because its status is '%s'. Only courses with the status 'BUILDING' can be modified.",
                course.getTitle(),
                course.getStatus()
        );

        // Act & Assert
        InvalidCourseTaskOperationException exception = assertThrows(InvalidCourseTaskOperationException.class, () ->
                courseTaskService.addTaskToCourseAtPosition(courseId, newTask, position)
        );
        assertThat(exception.getMessage()).isEqualTo(expectedErrorMessage);

        verify(taskRepositoryMock, never()).save(any(Task.class));
    }
}
