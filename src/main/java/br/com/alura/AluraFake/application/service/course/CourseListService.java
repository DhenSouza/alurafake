package br.com.alura.AluraFake.application.service.course;

import br.com.alura.AluraFake.api.dto.response.CourseListItemDTO;
import br.com.alura.AluraFake.domain.model.Course;
import br.com.alura.AluraFake.domain.repository.CourseRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseListService implements CourseListInterface{

    private final CourseRepository courseRepository;

    public CourseListService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    public List<CourseListItemDTO> listAllCourses() {
        List<Course> coursesFromDb = courseRepository.findAll();

        return coursesFromDb.stream()
                .map(CourseListItemDTO::new)
                .collect(Collectors.toList());
    }
}
