package br.com.alura.AluraFake.application.service.course;

import br.com.alura.AluraFake.api.dto.request.NewCourseDTO;
import br.com.alura.AluraFake.domain.enumeration.Status;
import br.com.alura.AluraFake.domain.model.Course;
import br.com.alura.AluraFake.domain.model.User;
import br.com.alura.AluraFake.domain.repository.CourseRepository;
import br.com.alura.AluraFake.domain.repository.UserRepository;
import br.com.alura.AluraFake.exceptionhandler.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class CreateNewCourseService implements CreateNewCourseServiceInterface {

    private CourseRepository courseRepository;

    private UserRepository userRepository;

    public CreateNewCourseService(CourseRepository courseRepository, UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
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

        return courseRepository.save(course);
    }
}
