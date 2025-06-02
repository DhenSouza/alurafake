package br.com.alura.AluraFake.infra;

import br.com.alura.AluraFake.domain.enumeration.Role;
import br.com.alura.AluraFake.domain.enumeration.Status;
import br.com.alura.AluraFake.domain.enumeration.Type;
import br.com.alura.AluraFake.domain.model.Course;
import br.com.alura.AluraFake.domain.model.Task;
import br.com.alura.AluraFake.domain.model.User;

import br.com.alura.AluraFake.domain.repository.CourseRepository;
import br.com.alura.AluraFake.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(SpringExtension.class)
class CourseRepositoryTest {

    @Container
    private static final MySQLContainer<?> mysqlContainer =
            new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                    .withDatabaseName("alurafake_testdb_course")
                    .withUsername("testuser_course")
                    .withPassword("testpass_course");
    @Autowired
    private UserRepository userRepository;

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mysqlContainer::getUsername);
        registry.add("spring.datasource.password", mysqlContainer::getPassword);
        registry.add("spring.datasource.driverClassName", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
    }

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User instructor;

    @BeforeEach
    void setUp() {
        instructor = new User("Prof. Pardal", "pardal@example.com", Role.INSTRUCTOR, "password");
        entityManager.persist(instructor);
        entityManager.flush();
    }

    @Test
    @DisplayName("save: It should persist a new course with correct details.")
    void save_shouldPersistNewCourseWithCorrectDetails() {
        // Arrange
        Course newCourse = new Course("Curso de Spring Boot", "Fundamentos e API REST", instructor);

        // Act
        Course savedCourse = courseRepository.save(newCourse);
        entityManager.flush();
        entityManager.clear();

        // Assert
        assertThat(savedCourse).isNotNull();
        assertThat(savedCourse.getId()).isNotNull();

        Optional<Course> foundCourseOpt = courseRepository.findById(savedCourse.getId());
        assertThat(foundCourseOpt).isPresent();
        Course foundCourse = foundCourseOpt.get();

        assertThat(foundCourse.getTitle()).isEqualTo("Curso de Spring Boot");
        assertThat(foundCourse.getDescription()).isEqualTo("Fundamentos e API REST");
        assertThat(foundCourse.getInstructor().getId()).isEqualTo(instructor.getId());
        assertThat(foundCourse.getStatus()).isEqualTo(Status.BUILDING);
        assertThat(foundCourse.getCreatedAt()).isNotNull();
        assertThat(foundCourse.getPublishedAt()).isNull();
        assertThat(foundCourse.getTasks()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Save: Should continue the course with cascading tasks.")
    void save_courseWithTasks_shouldPersistCourseAndCascadeTasks() {
        // Arrange
        Course newCourse = new Course("Curso com Tarefas", "Testando cascata", instructor);
        Task task1 = Task.builder().statement("Tarefa 1").order(1).typeTask(Type.OPEN_TEXT).build();
        Task task2 = Task.builder().statement("Tarefa 2").order(2).typeTask(Type.SINGLE_CHOICE).build();

        newCourse.addTask(task1);
        newCourse.addTask(task2);

        // Act
        Course savedCourse = courseRepository.save(newCourse);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Optional<Course> foundCourseOpt = courseRepository.findById(savedCourse.getId());
        assertThat(foundCourseOpt).isPresent();
        Course foundCourse = foundCourseOpt.get();

        assertThat(foundCourse.getTasks()).hasSize(2);
        assertThat(foundCourse.getTasks().get(0).getStatement()).isEqualTo("Tarefa 1");
        assertThat(foundCourse.getTasks().get(0).getCourse().getId()).isEqualTo(foundCourse.getId());
        assertThat(foundCourse.getTasks().get(1).getStatement()).isEqualTo("Tarefa 2");
    }


    @Test
    @DisplayName("findById: Should return course when ID exists")
    void findById_whenCourseExists_shouldReturnCourse() {
        // Arrange
        Course course = new Course("Curso Existente", "Para ser encontrado", instructor);
        Course persistedCourse = entityManager.persistAndFlush(course);

        // Act
        Optional<Course> foundCourseOpt = courseRepository.findById(persistedCourse.getId());

        // Assert
        assertThat(foundCourseOpt).isPresent();
        assertThat(foundCourseOpt.get().getTitle()).isEqualTo("Curso Existente");
    }

    @Test
    @DisplayName("findById: Should return an empty Optional when ID does not exist")
    void findById_whenCourseDoesNotExist_shouldReturnEmptyOptional() {
        // Arrange
        Long nonExistentId = 999L;

        // Act
        Optional<Course> foundCourseOpt = courseRepository.findById(nonExistentId);

        // Assert
        assertThat(foundCourseOpt).isEmpty();
    }

    @Test
    @DisplayName("findAll: It should return all the persisted courses.")
    void findAll_shouldReturnAllPersistedCourses() {
        // Arrange
        Course course1 = new Course("Curso A", "Desc A", instructor);
        Course course2 = new Course("Curso B", "Desc B", instructor);
        entityManager.persist(course1);
        entityManager.persist(course2);
        entityManager.flush();

        // Act
        List<Course> courses = courseRepository.findAll();

        // Assert
        assertThat(courses).hasSize(2);
        assertThat(courses).extracting(Course::getTitle).containsExactlyInAnyOrder("Curso A", "Curso B");
    }

    @Test
    @DisplayName("findAll: Should return an empty list if no course exists")
    void findAll_whenNoCourses_shouldReturnEmptyList() {
        // Arrange
        userRepository.deleteAllInBatch();
        courseRepository.deleteAllInBatch();


        // Act
        List<Course> courses = courseRepository.findAll();

        // Assert
        assertThat(courses).isEmpty();
    }

    @Test
    @DisplayName("Delete: It must remove the course")
    void delete_shouldRemoveCourse() {
        // Arrange
        Course course = new Course("Curso para Deletar", "A ser removido", instructor);
        Course persistedCourse = entityManager.persistAndFlush(course);
        Long courseId = persistedCourse.getId();

        // Act
        courseRepository.deleteById(courseId);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Optional<Course> deletedCourseOpt = courseRepository.findById(courseId);
        assertThat(deletedCourseOpt).isEmpty();
    }

    @Test
    @DisplayName("update: Must modify an existing course")
    void update_shouldModifyExistingCourse() {
        // Arrange
        Course course = new Course("Título Original", "Descrição Original", instructor);
        Course persistedCourse = entityManager.persistAndFlush(course);
        entityManager.detach(persistedCourse);

        // Act
        Course courseToUpdate = courseRepository.findById(persistedCourse.getId()).orElseThrow();
        courseToUpdate.setTitle("Título Atualizado");
        courseToUpdate.setStatus(Status.PUBLISHED);
        courseToUpdate.setPublishedAt(LocalDateTime.now());
        courseRepository.save(courseToUpdate);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Optional<Course> updatedCourseOpt = courseRepository.findById(persistedCourse.getId());
        assertThat(updatedCourseOpt).isPresent();
        Course updatedCourse = updatedCourseOpt.get();
        assertThat(updatedCourse.getTitle()).isEqualTo("Título Atualizado");
        assertThat(updatedCourse.getStatus()).isEqualTo(Status.PUBLISHED);
        assertThat(updatedCourse.getPublishedAt()).isNotNull();
    }
}
