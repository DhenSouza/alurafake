package br.com.alura.AluraFake.api.dto.response;

import br.com.alura.AluraFake.domain.model.Course;
import br.com.alura.AluraFake.domain.enumeration.Status;

import java.io.Serializable;

public class CourseListItemDTO implements Serializable {

    private Long id;
    private String title;
    private String description;
    private Status status;

    public CourseListItemDTO(Course course) {
        this.id = course.getId();
        this.title = course.getTitle();
        this.description = course.getDescription();
        this.status = course.getStatus();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Status getStatus() {
        return status;
    }
}
