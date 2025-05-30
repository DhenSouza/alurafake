package br.com.alura.AluraFake.application.service.task;

import br.com.alura.AluraFake.domain.model.Course;
import br.com.alura.AluraFake.domain.model.Task;
import br.com.alura.AluraFake.domain.repository.CourseRepository;
import br.com.alura.AluraFake.domain.repository.TaskRepository;
import br.com.alura.AluraFake.exceptionhandler.InvalidCourseTaskOperationException;
import br.com.alura.AluraFake.exceptionhandler.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CourseTaskService {

    private final CourseRepository courseRepository;
    private final TaskRepository taskRepository;

    public CourseTaskService(CourseRepository courseRepository, TaskRepository taskRepository) {
        this.courseRepository = courseRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional
    public Course addTaskToCourseAtPosition(Long courseId, Task newTask, int position) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Curso não encontrado com ID: " + courseId));

        List<Task> existingTasks = course.getTasks();

        if (position < 1 || position > existingTasks.size() + 1) {
            throw new InvalidCourseTaskOperationException(
                    "Ordem de tarefa inválida. A ordem deve ser contínua e estar entre 1 e " + (existingTasks.size() + 1) + "."
            );
        }

        for (Task existingTask : existingTasks) {
            if (existingTask.getOrder() >= position) {
                existingTask.setOrder(existingTask.getOrder() + 1);
            }
        }

        newTask.setOrder(position);
        newTask.setCourse(course);

        course.addTask(newTask);

        taskRepository.save(newTask);
        return course;
    }

    @Transactional
    public Course removeTaskFromCourse(Long courseId, Long taskId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Curso não encontrado com ID: " + courseId));

        Task taskToRemove = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada com ID: " + taskId));

        if (taskToRemove.getCourse() == null || !taskToRemove.getCourse().getId().equals(courseId)) {
            String mensagemDeErroFormatada = String.format(
                    "A tarefa com ID %d não pertence ao curso com ID: %d",
                    taskToRemove.getId(),
                    courseId
            );
            throw new InvalidCourseTaskOperationException(mensagemDeErroFormatada);
        }

        course.removeTask(taskToRemove);

        int currentOrder = 1;
        List<Task> remainingTasks = course.getTasks();
        for (Task task : remainingTasks) {
            task.setOrder(currentOrder++);
        }

        return course;
    }
}
