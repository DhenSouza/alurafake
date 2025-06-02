package br.com.alura.AluraFake.integration;

import br.com.alura.AluraFake.api.dto.request.LoginRequestDTO;
import br.com.alura.AluraFake.api.dto.request.NewCourseDTO;
import br.com.alura.AluraFake.api.dto.response.CourseListItemDTO;
import br.com.alura.AluraFake.api.dto.response.LoginResponseDTO;
import br.com.alura.AluraFake.application.service.course.CourseService;
import br.com.alura.AluraFake.domain.enumeration.Status;
import br.com.alura.AluraFake.domain.enumeration.Type;
import br.com.alura.AluraFake.domain.model.Course;
import br.com.alura.AluraFake.domain.model.Task;
import br.com.alura.AluraFake.domain.model.TaskOption;
import br.com.alura.AluraFake.domain.model.User;
import br.com.alura.AluraFake.domain.repository.CourseRepository;
import br.com.alura.AluraFake.domain.repository.TaskRepository;
import br.com.alura.AluraFake.domain.repository.UserRepository;
import br.com.alura.AluraFake.globalHandler.dto.ProblemType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CourseIntegrationTest {

    @Container
    private static final MySQLContainer<?> mysqlContainer =
            new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                    .withDatabaseName("alurafake_it_course_db")
                    .withUsername("it_course_user")
                    .withPassword("it_course_pass");

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
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private CourseService courseService;

    private String instructorJwtToken;
    private String studentJwtToken;
    private User instructorUser;
    private Course createdCourse;
    private Task createdTask;
    private final String errorBaseUri = "https://api.seusite.com/erros";

    @BeforeEach
    void setUp() throws Exception {

        // 2. Faça o login para obter os tokens JWT
        LoginRequestDTO instructorLogin = new LoginRequestDTO("paulo@alura.com.br", "senha321");
        MvcResult instructorLoginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(instructorLogin)))
                .andExpect(status().isOk())
                .andReturn();
        LoginResponseDTO instructorLoginResponse = objectMapper.readValue(
                instructorLoginResult.getResponse().getContentAsString(), LoginResponseDTO.class);
        instructorJwtToken = instructorLoginResponse.getJwtToken();

        LoginRequestDTO studentLogin = new LoginRequestDTO("caio@alura.com.br", "senha123");
        MvcResult studentLoginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(studentLogin)))
                .andExpect(status().isOk())
                .andReturn();
        LoginResponseDTO studentLoginResponse = objectMapper.readValue(
                studentLoginResult.getResponse().getContentAsString(), LoginResponseDTO.class);
        studentJwtToken = studentLoginResponse.getJwtToken();

        instructorUser = userRepository.findByEmail("paulo@alura.com.br")
                .orElseThrow(() -> new IllegalStateException("Usuário instrutor 'paulo@alura.com.br' não encontrado. Verifique o DataSeeder."));

        taskRepository.deleteAllInBatch();
        courseRepository.deleteAllInBatch();
    }

    private Task createTaskForTest(Course course, int order, Type type, String statement) {
        return Task.builder()
                .course(course)
                .order(order)
                .typeTask(type)
                .statement(statement)
                .options(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("POST /course/new: INSTRUCTOR must create course with valid DTO and return Status 200 OK")
    @Transactional
    void createCourse_byInstructorWithValidDTO_shouldReturnOk() throws Exception {
        // Arrange
        NewCourseDTO newCourseDto = new NewCourseDTO();
        newCourseDto.setTitle("Novo Curso de Integração por Teste");
        newCourseDto.setDescription("Descrição detalhada do novo curso.");
        newCourseDto.setEmailInstructor(instructorUser.getEmail());

        // Act
        MvcResult result = mockMvc.perform(post("/course/new")
                        .header("Authorization", "Bearer " + instructorJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCourseDto)))
                .andExpect(status().isOk())
                .andReturn();


        // Assert
        List<Course> courses = courseRepository.findAll();
        assertThat(courses).anyMatch(course ->
                        "Novo Curso de Integração por Teste".equals(course.getTitle()) &&
                                instructorUser.getEmail().equals(course.getInstructor().getEmail()) &&
                                Status.BUILDING.equals(course.getStatus())
        );
    }

    @Test
    @DisplayName("POST /course/new: STUDENT should NOT create course and return Status 403 Forbidden")
    void createCourse_byStudent_shouldReturnForbidden() throws Exception {
        // Arrange
        NewCourseDTO newCourseDto = new NewCourseDTO();
        newCourseDto.setTitle("Tentativa de Curso por Aluno");
        newCourseDto.setDescription("Descrição.");
        newCourseDto.setEmailInstructor("paulo@alura.com.br");

        // Act & Assert
        mockMvc.perform(post("/course/new")
                        .header("Authorization", "Bearer " + studentJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCourseDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /course/all: Authenticated user (STUDENT) should list courses and return Status 200 OK")
    @Transactional
    void listAllCourses_whenAuthenticatedAsStudent_shouldReturnOkWithCourseList() throws Exception {
        // Arrange
        Course courseJava = courseRepository.findAll().stream()
                .filter(c -> "Java".equals(c.getTitle())).findFirst()
                .orElseGet(() -> courseRepository.save(new Course("Java", "Curso de Java do Seeder", instructorUser)));


        // Act
        MvcResult result = mockMvc.perform(get("/course/all")
                        .header("Authorization", "Bearer " + studentJwtToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Assert
        List<CourseListItemDTO> returnedList = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<List<CourseListItemDTO>>() {}
        );
        assertThat(returnedList).isNotEmpty();
        assertThat(returnedList).extracting(CourseListItemDTO::getTitle).contains("Java");
    }

    @Test
    @DisplayName("POST /course/{id}/publish: INSTRUCTOR must publish course in BUILDING with valid content and return Status 201 Created")
    @Transactional
    void publishCourse_byInstructorForBuildingCourseWithValidContent_shouldReturnCreated() throws Exception {
        // Arrange
        Course courseToPublish = new Course("Curso Completo para Publicar", "Conteúdo validado.", instructorUser);
        courseToPublish.setStatus(Status.BUILDING);

        courseRepository.saveAndFlush(courseToPublish);

        Task task1 = createTaskForTest(courseToPublish, 1, Type.OPEN_TEXT, "Enunciado Open Text");
        Task task2 = createTaskForTest(courseToPublish, 2, Type.SINGLE_CHOICE, "Enunciado Single Choice");

        TaskOption scOption1 = TaskOption.builder().optionText("SC Op1").isCorrect(true).task(task2).build();
        task2.getOptions().add(scOption1);


        Task task3 = createTaskForTest(courseToPublish, 3, Type.MULTIPLE_CHOICE, "Enunciado Multiple Choice");
        TaskOption mcOption1 = TaskOption.builder().optionText("MC Op1").isCorrect(true).task(task3).build();
        TaskOption mcOption2 = TaskOption.builder().optionText("MC Op2").isCorrect(true).task(task3).build();
        TaskOption mcOption3 = TaskOption.builder().optionText("MC Op3").isCorrect(false).task(task3).build();
        task3.getOptions().add(mcOption1);
        task3.getOptions().add(mcOption2);
        task3.getOptions().add(mcOption3);

        courseToPublish.addTask(task1);
        courseToPublish.addTask(task2);
        courseToPublish.addTask(task3);

        courseRepository.saveAndFlush(courseToPublish);
        Long courseIdToPublish = courseToPublish.getId();

        // Act
        ResultActions resultActions = mockMvc.perform(post("/course/{id}/publish", courseIdToPublish)
                .header("Authorization", "Bearer " + instructorJwtToken));

        // Assert
        resultActions.andExpect(status().isCreated());

        Optional<Course> publishedCourseOpt = courseRepository.findById(courseIdToPublish);
        assertThat(publishedCourseOpt).isPresent();
        Course publishedCourse = publishedCourseOpt.get();
        assertThat(publishedCourse.getStatus()).isEqualTo(Status.PUBLISHED);
        assertThat(publishedCourse.getPublishedAt()).isNotNull();
        assertThat(publishedCourse.getTasks()).hasSize(3);
    }

    @Test
    @DisplayName("POST /course/{id}/publish: Should return Status 400 if business rule is violated (e.g., status is not BUILDING)")
    @WithMockUser(username = "paulo@alura.com.br", roles = "INSTRUCTOR")
    @Transactional
    void publishCourse_whenBusinessRuleViolated_shouldReturnBadRequest() throws Exception {
        // Arrange
        String courseTitleForTest = "Curso Já Publicado Para Teste";

        Course courseParaTeste = new Course(courseTitleForTest, "Não pode republicar.", instructorUser);
        courseParaTeste.setStatus(Status.PUBLISHED);
        courseParaTeste.setPublishedAt(LocalDateTime.now().minusDays(1));

        Course persistedCourse = courseRepository.saveAndFlush(courseParaTeste);
        Long courseId = persistedCourse.getId();

        String expectedDetailMessage = String.format(
                "The course '%s' cannot be published because its status is '%s'. Only courses in 'BUILDING' are allowed.",
                persistedCourse.getTitle(),
                persistedCourse.getStatus()
        );

        // Act
        ResultActions resultActions = mockMvc.perform(post("/course/{id}/publish", courseId)
                .header("Authorization", "Bearer " + instructorJwtToken)
                .contentType(MediaType.APPLICATION_JSON));

        // Assert
        String expectedTypeUri = errorBaseUri + ProblemType.INVALID_OPERATION.getPath();
        String expectedTitle = ProblemType.INVALID_OPERATION.getTitle();
        String expectedUserMessage = expectedDetailMessage;


        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.type").value(expectedTypeUri))
                .andExpect(jsonPath("$.title").value(expectedTitle))
                .andExpect(jsonPath("$.detail").value(expectedDetailMessage))
                .andExpect(jsonPath("$.userMessage").value(expectedUserMessage))
                .andExpect(jsonPath("$.instance").value("/course/" + courseId + "/publish"))
                .andExpect(jsonPath("$.fields").doesNotExist());
    }
}
