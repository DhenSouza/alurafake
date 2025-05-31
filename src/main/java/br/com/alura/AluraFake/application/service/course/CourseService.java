package br.com.alura.AluraFake.application.service.course;

import br.com.alura.AluraFake.api.dto.request.NewCourseDTO;
import br.com.alura.AluraFake.api.dto.response.CourseListItemDTO;
import br.com.alura.AluraFake.application.interfaces.CourseServiceInterface;
import br.com.alura.AluraFake.application.service.course.validation.CourseContentValidation;
import br.com.alura.AluraFake.domain.enumeration.Status;
import br.com.alura.AluraFake.domain.model.Course;
import br.com.alura.AluraFake.domain.model.User;
import br.com.alura.AluraFake.domain.repository.CourseRepository;
import br.com.alura.AluraFake.domain.repository.UserRepository;
import br.com.alura.AluraFake.exceptionhandler.BusinessRuleException;
import br.com.alura.AluraFake.exceptionhandler.EntityNotFoundException;
import br.com.alura.AluraFake.exceptionhandler.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseService implements CourseServiceInterface {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    private final CourseContentValidation courseContentValidation;

    public CourseService(CourseRepository courseRepository, UserRepository userRepository, CourseContentValidation courseContentValidation) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.courseContentValidation = courseContentValidation;
    }

    @Override
    @Transactional
    public Course createNewCourse(NewCourseDTO courseDTO) {
        User author = userRepository
                .findByEmail(courseDTO.getEmailInstructor())
                .filter(User::isInstructor)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Instrutor não encontrado com e-mail: " + courseDTO.getEmailInstructor()
                        )
                );

        Course course = new Course(
                courseDTO.getTitle(),
                courseDTO.getDescription(),
                author
        );

        course.setPublishedAt(LocalDateTime.now());

        return courseRepository.save(course);
    }

    @Transactional(readOnly = true)
    public List<CourseListItemDTO> listAllCourses() {
        List<Course> coursesFromDb = courseRepository.findAll();

        return coursesFromDb.stream()
                .map(CourseListItemDTO::new)
                .collect(Collectors.toList());
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

        courseContentValidation.validateTaskOrderIsContinuous(course);

        course.setStatus(Status.PUBLISHED);

        course.setPublishedAt(LocalDateTime.now());
        courseRepository.save(course);

        return course;
    }
}
