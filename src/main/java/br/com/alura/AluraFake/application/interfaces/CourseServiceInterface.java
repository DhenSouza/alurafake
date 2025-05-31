package br.com.alura.AluraFake.application.interfaces;

import br.com.alura.AluraFake.api.dto.request.NewCourseDTO;
import br.com.alura.AluraFake.api.dto.response.CourseListItemDTO;
import br.com.alura.AluraFake.domain.model.Course;

import java.util.List;

public interface CourseServiceInterface {

    Course createNewCourse(NewCourseDTO courseDTO);
    List<CourseListItemDTO> listAllCourses();
    Course publish(Long courseId);

}
