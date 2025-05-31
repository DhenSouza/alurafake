package br.com.alura.AluraFake.application.service.course;

import br.com.alura.AluraFake.api.dto.request.NewCourseDTO;
import br.com.alura.AluraFake.domain.model.Course;

public interface CreateNewCourseServiceInterface {
    Course createNewCourse(NewCourseDTO courseDTO);
}
