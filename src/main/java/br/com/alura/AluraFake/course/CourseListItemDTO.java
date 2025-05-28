package br.com.alura.AluraFake.course;

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
