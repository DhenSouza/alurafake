package br.com.alura.AluraFake.api.controller;

import br.com.alura.AluraFake.application.interfaces.CourseServiceInterface;
import br.com.alura.AluraFake.api.dto.response.CourseListItemDTO;
import br.com.alura.AluraFake.api.dto.request.NewCourseDTO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/course")
public class CourseController {

    private final CourseServiceInterface courseService;

    @Autowired
    public CourseController(CourseServiceInterface courseService) {
        this.courseService = courseService;
    }

    @PostMapping("/new")
    public ResponseEntity<?> createCourse(@Valid @RequestBody NewCourseDTO newCourse) {
        this.courseService.createNewCourse(newCourse);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/all")
    public ResponseEntity<List<CourseListItemDTO>> listAllCourses() {
        return ResponseEntity.ok(courseService.listAllCourses());
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<?> createCourse(@PathVariable("id") Long id) {
        this.courseService.publish(id);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}
