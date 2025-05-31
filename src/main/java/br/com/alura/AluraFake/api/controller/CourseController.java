package br.com.alura.AluraFake.api.controller;

import br.com.alura.AluraFake.application.service.course.CourseListInterface;
import br.com.alura.AluraFake.application.service.course.CoursePublicationServiceInterface;
import br.com.alura.AluraFake.application.service.course.CreateNewCourseServiceInterface;
import br.com.alura.AluraFake.domain.model.Course;
import br.com.alura.AluraFake.api.dto.response.CourseListItemDTO;
import br.com.alura.AluraFake.domain.repository.CourseRepository;
import br.com.alura.AluraFake.api.dto.request.NewCourseDTO;
import br.com.alura.AluraFake.domain.model.User;
import br.com.alura.AluraFake.domain.repository.UserRepository;
import br.com.alura.AluraFake.util.ErrorItemDTO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/course")
public class CourseController {

    private final CreateNewCourseServiceInterface createNewCourseService;
    private final CoursePublicationServiceInterface publishService;
    private final CourseListInterface courseListService;

    @Autowired
    public CourseController(CoursePublicationServiceInterface publishService,
                            CreateNewCourseServiceInterface createNewCourseService,
                            CourseListInterface courseListService) {
        this.publishService = publishService;
        this.createNewCourseService = createNewCourseService;
        this.courseListService = courseListService;
    }

    @PostMapping("/new")
    public ResponseEntity<?> createCourse(@Valid @RequestBody NewCourseDTO newCourse) {
        this.createNewCourseService.createNewCourse(newCourse);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/all")
    public ResponseEntity<List<CourseListItemDTO>> listAllCourses() {
        return ResponseEntity.ok(courseListService.listAllCourses());
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<?> createCourse(@PathVariable("id") Long id) {
        this.publishService.publish(id);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}
