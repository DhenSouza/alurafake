package br.com.alura.AluraFake.infra;

import br.com.alura.AluraFake.domain.enumeration.Role;
import br.com.alura.AluraFake.domain.enumeration.Status;
import br.com.alura.AluraFake.domain.enumeration.Type;
import br.com.alura.AluraFake.domain.model.Course;
import br.com.alura.AluraFake.domain.model.Task;
import br.com.alura.AluraFake.domain.model.TaskOption;
import br.com.alura.AluraFake.domain.model.User;
import br.com.alura.AluraFake.domain.repository.CourseRepository;
import br.com.alura.AluraFake.domain.repository.TaskRepository;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(SpringExtension.class)
class TaskRepositoryTest {

    @Container
    private static final MySQLContainer<?> mysqlContainer =
            new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                    .withDatabaseName("alurafake_testdb_task")
                    .withUsername("testuser_task")
                    .withPassword("testpass_task");

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
    private TaskRepository taskRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Course persistedCourse;
    private User instructor;

    @BeforeEach
    void setUp() {
        // Limpar em cascata ou na ordem correta de dependência se necessário
        // taskRepository.deleteAllInBatch();
        // courseRepository.deleteAllInBatch();
        // userRepository.deleteAllInBatch();

        instructor = new User("Prof. Task", "proftask@example.com", Role.INSTRUCTOR, "password");
        entityManager.persist(instructor);

        persistedCourse = new Course("Curso para Tarefas", "Descrição do Curso", instructor);
        persistedCourse.setStatus(Status.BUILDING);
        entityManager.persist(persistedCourse);
        entityManager.flush();
    }

    @Test
    @DisplayName("save: It should persist a new Task with correct details.")
    void save_shouldPersistNewTaskWithCorrectDetails() {
        // Arrange
        Task newTask = Task.builder()
                .statement("Qual o primeiro princípio do SOLID?")
                .order(1)
                .course(persistedCourse)
                .typeTask(Type.OPEN_TEXT)
                .build();

        // Act
        Task savedTask = taskRepository.save(newTask);
        entityManager.flush();
        entityManager.clear();

        // Assert
        assertThat(savedTask).isNotNull();
        assertThat(savedTask.getId()).isNotNull();

        Optional<Task> foundTaskOpt = taskRepository.findById(savedTask.getId());
        assertThat(foundTaskOpt).isPresent();
        Task foundTask = foundTaskOpt.get();

        assertThat(foundTask.getStatement()).isEqualTo("Qual o primeiro princípio do SOLID?");
        assertThat(foundTask.getOrder()).isEqualTo(1);
        assertThat(foundTask.getTypeTask()).isEqualTo(Type.OPEN_TEXT);
        assertThat(foundTask.getCourse().getId()).isEqualTo(persistedCourse.getId());
        assertThat(foundTask.getOptions()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Save: Task should persist with cascading TaskOptions")
    void save_taskWithOptions_shouldPersistTaskAndCascadeOptions() {
        // Arrange
        Task newTask = Task.builder()
                .statement("Qual das opções é uma linguagem de marcação?")
                .order(1)
                .course(persistedCourse)
                .typeTask(Type.SINGLE_CHOICE)
                .build();

        TaskOption option1 = TaskOption.builder().optionText("Java").isCorrect(false).build();
        TaskOption option2 = TaskOption.builder().optionText("HTML").isCorrect(true).build();

        newTask.addOption(option1);
        newTask.addOption(option2);

        // Act
        Task savedTask = taskRepository.save(newTask);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Optional<Task> foundTaskOpt = taskRepository.findById(savedTask.getId());
        assertThat(foundTaskOpt).isPresent();
        Task foundTask = foundTaskOpt.get();

        assertThat(foundTask.getOptions()).hasSize(2);
        assertThat(foundTask.getOptions()).extracting(TaskOption::getOptionText)
                .containsExactlyInAnyOrder("Java", "HTML");
        assertThat(foundTask.getOptions().get(1).getTask().getId()).isEqualTo(foundTask.getId());
    }

    @Test
    @DisplayName("findById: Should return Task when ID exists")
    void findById_whenTaskExists_shouldReturnTask() {
        // Arrange
        Task task = Task.builder()
                .statement("Tarefa Existente")
                .order(1)
                .course(persistedCourse)
                .typeTask(Type.OPEN_TEXT)
                .build();
        Task persistedTask = entityManager.persistAndFlush(task);

        // Act
        Optional<Task> foundTaskOpt = taskRepository.findById(persistedTask.getId());

        // Assert
        assertThat(foundTaskOpt).isPresent();
        assertThat(foundTaskOpt.get().getStatement()).isEqualTo("Tarefa Existente");
    }

    @Test
    @DisplayName("findById: It should return an empty Optional when the Task ID does not exist.")
    void findById_whenTaskDoesNotExist_shouldReturnEmptyOptional() {
        // Act
        Optional<Task> foundTaskOpt = taskRepository.findById(999L);

        // Assert
        assertThat(foundTaskOpt).isEmpty();
    }
}
