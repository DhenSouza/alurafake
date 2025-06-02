package br.com.alura.AluraFake.domain.repository;

import br.com.alura.AluraFake.domain.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long>{

}
