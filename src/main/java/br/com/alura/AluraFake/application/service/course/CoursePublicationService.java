package br.com.alura.AluraFake.application.service.course;

import br.com.alura.AluraFake.application.service.course.validation.CourseContentValidation;
import br.com.alura.AluraFake.domain.enumeration.Status;
import br.com.alura.AluraFake.domain.model.Course;
import br.com.alura.AluraFake.domain.model.Task;
import br.com.alura.AluraFake.domain.repository.CourseRepository;
import br.com.alura.AluraFake.exceptionhandler.BusinessRuleException;
import br.com.alura.AluraFake.exceptionhandler.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class CoursePublicationService implements CoursePublicationServiceInterface {

    private final CourseRepository courseRepository;

    private final CourseContentValidation courseContentValidation;

    public CoursePublicationService(CourseRepository courseRepository, CourseContentValidation courseContentValidation) {
        this.courseRepository = courseRepository;
        this.courseContentValidation = courseContentValidation;
    }

    @Transactional
    public Course publish(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Curso não encontrado com ID: " + courseId));

        if (course.getStatus() != Status.BUILDING) {
            throw new BusinessRuleException(String.format(
                    "O curso '%s' não pode ser publicado pois seu status é '%s'. Apenas cursos em 'BUILDING' são permitidos.",
                    course.getTitle(),
                    course.getStatus()));
        }

        courseContentValidation.validateType(course);

        validateTaskOrderIsContinuous(course);

        course.setStatus(Status.PUBLISHED);

        course.setPublishedAt(LocalDateTime.now());
        courseRepository.save(course);

        return course;
    }

    private void validateTaskOrderIsContinuous(Course course) {
        List<Task> tasks = course.getTasks();
        if (tasks == null || tasks.isEmpty()) {
            throw new BusinessRuleException("O curso não pode ser publicado sem atividades.");
        }

        tasks.sort(Comparator.comparingInt(Task::getOrder));

        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getOrder() != (i + 1)) {
                throw new BusinessRuleException(String.format(
                        "A ordem das atividades do curso não é contínua. Encontrada ordem %d na posição %d, esperado %d.",
                        tasks.get(i).getOrder(), i + 1, i + 1));
            }
        }
    }
}
