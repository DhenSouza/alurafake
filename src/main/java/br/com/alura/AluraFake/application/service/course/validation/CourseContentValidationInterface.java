package br.com.alura.AluraFake.application.service.course.validation;

import br.com.alura.AluraFake.domain.model.Course;

public interface CourseContentValidationInterface {
    void validateType(Course course);
}
