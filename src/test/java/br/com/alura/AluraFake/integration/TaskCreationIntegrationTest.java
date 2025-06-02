package br.com.alura.AluraFake.integration;

import br.com.alura.AluraFake.api.dto.request.*;
import br.com.alura.AluraFake.api.dto.response.LoginResponseDTO;
import br.com.alura.AluraFake.domain.enumeration.Status;
import br.com.alura.AluraFake.domain.enumeration.Type;
import br.com.alura.AluraFake.domain.model.Course;
import br.com.alura.AluraFake.domain.model.Task;
import br.com.alura.AluraFake.domain.model.TaskOption;
import br.com.alura.AluraFake.domain.repository.CourseRepository;
import br.com.alura.AluraFake.domain.repository.TaskRepository;
import br.com.alura.AluraFake.domain.repository.UserRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskCreationIntegrationTest {

    @Container
    private static final MySQLContainer<?> mysqlContainer =
            new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                    .withDatabaseName("alurafake_it_db")
                    .withUsername("it_user")
                    .withPassword("it_pass");

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
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    private String instructorJwtToken;
    private Course existingCourse;

    @BeforeEach
    void setUp() throws Exception {
        LoginRequestDTO loginRequest = new LoginRequestDTO("paulo@alura.com.br", "senha321");
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String loginResponse = loginResult.getResponse().getContentAsString();
        LoginResponseDTO loginResponseDTO = objectMapper.readValue(loginResponse, LoginResponseDTO.class);
        instructorJwtToken = loginResponseDTO.getJwtToken();
        assertThat(instructorJwtToken).isNotBlank();

        List<Course> courses = courseRepository.findAll();
        existingCourse = courses.stream()
                .filter(c -> "Java".equals(c.getTitle()) && "paulo@alura.com.br".equals(c.getInstructor().getEmail()))
                .findFirst()
                .orElseGet(() -> {
                    System.out.println("Curso 'Java' do seeder não encontrado, criando um novo para o teste.");
                    return userRepository.findByEmail("paulo@alura.com.br").map(instr -> {
                        Course newSetupCourse = new Course("Java Test Course", "Setup Course", instr);
                        return courseRepository.save(newSetupCourse);
                    }).orElseThrow(() -> new RuntimeException("Instrutor do seeder não encontrado para criar curso de setup"));
                });
        assertThat(existingCourse).isNotNull();
        assertThat(existingCourse.getId()).isNotNull();
    }

    private List<ChoiceOptionRequest> createSampleValidMultipleChoiceOptions() {
        return List.of(
                new ChoiceOptionRequest("Opção Múltipla Correta Alfa", true),
                new ChoiceOptionRequest("Opção Múltipla Correta Beta", true),
                new ChoiceOptionRequest("Opção Múltipla Incorreta Gama", false)
        );
    }

    @Test
    @Transactional
    @DisplayName("POST /task/new: Should successfully create a SINGLE_CHOICE task for an existing course and authenticated instructor")
    void createNewTask_whenSingleChoiceRequestIsValid_shouldCreateTaskAndReturnSuccess() throws Exception {
        // Arrange
        String taskStatement = "Qual é a capital da França?";
        int taskOrder = 1;

        List<ChoiceOptionRequest> options = List.of(
                new ChoiceOptionRequest("Paris", true),
                new ChoiceOptionRequest("Londres", false),
                new ChoiceOptionRequest("Roma", false)
        );

        SingleChoiceTaskCreationRequest taskRequest = new SingleChoiceTaskCreationRequest(
                existingCourse.getId(),
                taskStatement,
                taskOrder,
                Type.SINGLE_CHOICE,
                options
        );

        // Act
        ResultActions resultActions = mockMvc.perform(post("/task/new")
                .header("Authorization", "Bearer " + instructorJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(taskRequest)));

        // Assert
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));


        Course courseFromDbAfterTaskCreation = courseRepository.findById(existingCourse.getId())
                .orElseThrow(() -> new AssertionError("Course not found in the database after creating the task. ID: " + existingCourse.getId()));

        List<Task> tasksInDbForCourse = courseFromDbAfterTaskCreation.getTasks();

        assertThat(tasksInDbForCourse).hasSize(1);
        Task createdTask = tasksInDbForCourse.get(0);

        assertThat(createdTask.getStatement()).isEqualTo(taskStatement);
        assertThat(createdTask.getTypeTask()).isEqualTo(Type.SINGLE_CHOICE);
        assertThat(createdTask.getOrder()).isEqualTo(taskOrder);
        assertThat(createdTask.getCourse().getId()).isEqualTo(existingCourse.getId());

        assertThat(createdTask.getOptions()).hasSize(options.size());

        TaskOption correctOption = createdTask.getOptions().stream()
                .filter(TaskOption::getIsCorrect)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No correct option found in persisted task"));
        assertThat(correctOption.getOptionText()).isEqualTo("Paris");

        long incorrectOptionsCount = createdTask.getOptions().stream()
                .filter(opt -> !opt.getIsCorrect())
                .count();
        assertThat(incorrectOptionsCount).isEqualTo(2);
    }

    @Test
    @DisplayName("POST /task/new: Should successfully create an OPEN_TEXT task for an existing course and authenticated instructor.")
    @Transactional
    void createNewTask_whenValidOpenTextRequest_shouldCreateTaskAndReturnSuccess() throws Exception {
        // Arrange
        assertThat(existingCourse.getStatus()).isEqualTo(Status.BUILDING);

        String taskStatement = "Descreva os benefícios da programação funcional.";
        int taskOrder = existingCourse.getTasks().size() + 1;

        OpenTextTaskCreationRequest taskRequest = new OpenTextTaskCreationRequest(
                existingCourse.getId(),
                taskStatement,
                taskOrder,
                Type.OPEN_TEXT
        );

        // Act
        ResultActions resultActions = mockMvc.perform(post("/task/new")
                .header("Authorization", "Bearer " + instructorJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(taskRequest)));

        // Assert
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Course courseFromDb = courseRepository.findById(existingCourse.getId()).orElseThrow();
        List<Task> tasksInDbForCourse = courseFromDb.getTasks();

        Task createdTask = tasksInDbForCourse.stream()
                .filter(t -> taskStatement.equals(t.getStatement()) && t.getTypeTask() == Type.OPEN_TEXT)
                .findFirst()
                .orElseThrow(() -> new AssertionError("OPEN_TEXT task not found in the database with the expected statement."));

        assertThat(createdTask.getOrder()).isEqualTo(taskOrder);
        assertThat(createdTask.getCourse().getId()).isEqualTo(existingCourse.getId());
        assertThat(createdTask.getOptions()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("POST /task/new: Should successfully create a MULTIPLE CHOICE task, respecting its option rules")
    @Transactional
    void createNewTask_whenValidMultipleChoiceRequest_shouldCreateTaskAndPersistWithOptionsCorrectly() throws Exception {
        // Arrange
        assertThat(existingCourse.getStatus()).isEqualTo(Status.BUILDING);

        String taskStatement = "Quais das seguintes são cores primárias (luz)?";
        int taskOrder = existingCourse.getTasks().isEmpty() ? 1 : existingCourse.getTasks().size() + 1;

        List<ChoiceOptionRequest> multipleChoiceOptions = createSampleValidMultipleChoiceOptions();

        MultipleChoiceTaskCreationRequest taskRequest = new MultipleChoiceTaskCreationRequest(
                existingCourse.getId(),
                taskStatement,
                taskOrder,
                Type.MULTIPLE_CHOICE,
                multipleChoiceOptions
        );

        // Act
        ResultActions resultActions = mockMvc.perform(post("/task/new")
                .header("Authorization", "Bearer " + instructorJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(taskRequest)));

        // Assert
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Course courseFromDb = courseRepository.findById(existingCourse.getId())
                .orElseThrow(() -> new AssertionError("Course not found in the bank. ID: " + existingCourse.getId()));

        List<Task> tasksInDbForCourse = courseFromDb.getTasks();

        // Encontra a tarefa recém-criada pelo statement e tipo
        Task createdTask = tasksInDbForCourse.stream()
                .filter(t -> taskStatement.equals(t.getStatement()) && t.getTypeTask() == Type.MULTIPLE_CHOICE)
                .findFirst()
                .orElseThrow(() -> new AssertionError("MULTIPLE_CHOICE task not found in database with expected statement"));

        assertThat(createdTask.getOrder()).isEqualTo(taskOrder);
        assertThat(createdTask.getCourse().getId()).isEqualTo(existingCourse.getId());

        assertThat(createdTask.getOptions()).isNotNull();
        assertThat(createdTask.getOptions()).hasSize(multipleChoiceOptions.size());

        long correctOptionsCountInDb = createdTask.getOptions().stream()
                .filter(TaskOption::getIsCorrect)
                .count();
        long incorrectOptionsCountInDb = createdTask.getOptions().stream()
                .filter(opt -> !opt.getIsCorrect())
                .count();

        assertThat(correctOptionsCountInDb).as("Must have at least 2 correct options").isGreaterThanOrEqualTo(2);
        assertThat(incorrectOptionsCountInDb).as("There must be at least 1 incorrect option").isGreaterThanOrEqualTo(1);

        assertThat(createdTask.getOptions())
                .anyMatch(opt -> "Opção Múltipla Correta Alfa".equals(opt.getOptionText()) && opt.getIsCorrect());
        assertThat(createdTask.getOptions())
                .anyMatch(opt -> "Opção Múltipla Incorreta Gama".equals(opt.getOptionText()) && !opt.getIsCorrect());
    }
}
