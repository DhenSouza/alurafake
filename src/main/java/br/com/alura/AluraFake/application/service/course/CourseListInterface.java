package br.com.alura.AluraFake.application.service.course;

import br.com.alura.AluraFake.api.dto.response.CourseListItemDTO;
import br.com.alura.AluraFake.domain.model.Course;

import java.util.List;

public interface CourseListInterface {
    List<CourseListItemDTO> listAllCourses();
}
