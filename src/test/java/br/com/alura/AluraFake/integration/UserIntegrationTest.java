package br.com.alura.AluraFake.integration;

import br.com.alura.AluraFake.api.dto.request.LoginRequestDTO;
import br.com.alura.AluraFake.api.dto.request.NewUserDTO;
import br.com.alura.AluraFake.api.dto.response.LoginResponseDTO;
import br.com.alura.AluraFake.api.dto.response.UserListItemDTO;
import br.com.alura.AluraFake.application.service.user.UserService;
import br.com.alura.AluraFake.domain.enumeration.Role;
import br.com.alura.AluraFake.domain.model.User;
import br.com.alura.AluraFake.domain.repository.UserRepository;
import br.com.alura.AluraFake.globalHandler.dto.ProblemType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserIntegrationTest {

    @Container
    private static final MySQLContainer<?> mysqlContainer =
            new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                    .withDatabaseName("alurafake_it_user_db")
                    .withUsername("it_user_user")
                    .withPassword("it_user_pass");
    @Autowired
    private UserService userService;

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
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User instructorUser;

    private String studentJwtToken;
    private String instructorJwtToken;
    private final String errorBaseUri = "https://api.seusite.com/erros";


    @BeforeEach
    void setUp() throws Exception {

        User paulo = userRepository.findByEmail("paulo@alura.com.br")
                .orElseGet(() -> userRepository.save(new User("Paulo (Seeder)", "paulo@alura.com.br", Role.INSTRUCTOR, passwordEncoder.encode("senha321"))));
        instructorUser = paulo;

        User caio = userRepository.findByEmail("caio@alura.com.br")
                .orElseGet(() -> userRepository.save(new User("Caio (Seeder)", "caio@alura.com.br", Role.STUDENT, passwordEncoder.encode("senha123"))));

        userRepository.flush();

        LoginRequestDTO instructorLogin = new LoginRequestDTO("paulo@alura.com.br", "senha321");
        MvcResult instructorLoginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(instructorLogin)))
                .andExpect(status().isOk())
                .andReturn();
        instructorJwtToken = objectMapper.readValue(instructorLoginResult.getResponse().getContentAsString(), LoginResponseDTO.class).getJwtToken();

        LoginRequestDTO studentLogin = new LoginRequestDTO("caio@alura.com.br", "senha123");
        MvcResult studentLoginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(studentLogin)))
                .andExpect(status().isOk())
                .andReturn();
        studentJwtToken = objectMapper.readValue(studentLoginResult.getResponse().getContentAsString(), LoginResponseDTO.class).getJwtToken();
    }

    @Test
    @DisplayName("POST /user/new: Must create new user with valid DTO and return Status 201 Created and Location header")
    @Transactional
    void createUser_withValidDTO_shouldReturnCreatedWithLocationAndBody() throws Exception {
        // Arrange
        String plainPasswordForNewUser = "teste2";
        NewUserDTO newUserDTO = new NewUserDTO(
                "Novo Aluno",
                "novo.aluno@example.com",
                Role.STUDENT,
                plainPasswordForNewUser
        );

        // Act
        MvcResult result = mockMvc.perform(post("/user/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUserDTO)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andReturn();

        // Assert - Verificação no Banco
        Optional<User> createdUserOpt = userRepository.findByEmail("novo.aluno@example.com");
        assertThat(createdUserOpt).isPresent();
        User createdUserInDb = createdUserOpt.get();
        assertThat(createdUserInDb.getName()).isEqualTo("Novo Aluno");
        assertThat(createdUserInDb.getRole()).isEqualTo(Role.STUDENT);

        assertThat(passwordEncoder.matches(plainPasswordForNewUser, createdUserInDb.getPassword())).isTrue();
    }

    @Test
    @DisplayName("POST /user/new: Should create new user with EMPTY password and return Status 201 Created")
    @Transactional
    void createUser_withEmptyPassword_shouldReturnCreated() throws Exception {
        // Arrange
        NewUserDTO newUserDTO = new NewUserDTO(
                "Usuário Com Senha Vazia",
                "sem.senha.valida@example.com",
                Role.STUDENT,
                ""
        );

        // Act
        mockMvc.perform(post("/user/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUserDTO)))
                .andExpect(status().isCreated());

        // Assert
        Optional<User> createdUserOpt = userRepository.findByEmail("sem.senha.valida@example.com");
        assertThat(createdUserOpt).isPresent();
        assertThat(passwordEncoder.matches("", createdUserOpt.get().getPassword())).isTrue();
    }


    @ParameterizedTest
    @MethodSource("invalidNewUserDTOs")
    @DisplayName("POST /user/new: Must not create user with invalid DTO (Bean Validation) and return Status 400")
    void createUser_withInvalidDTO_shouldReturnBadRequest(NewUserDTO invalidDto, String expectedFailedField) throws Exception {
        // Act
        mockMvc.perform(post("/user/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(errorBaseUri + ProblemType.DEFAULT_USER_MESSAGE_VALIDATION.getPath()))
                .andExpect(jsonPath("$.title").value(ProblemType.DEFAULT_USER_MESSAGE_VALIDATION.getTitle()))
                .andExpect(jsonPath("$.fields[?(@.name == '%s')]", expectedFailedField).exists());
    }

    static Stream<Arguments> invalidNewUserDTOs() {
        return Stream.of(
                Arguments.of(new NewUserDTO(null, "email@valido.com",  Role.STUDENT, "123456"), "name"),
                Arguments.of(new NewUserDTO("No", "email@valido.com", Role.STUDENT, "123456"), "name"),
                Arguments.of(new NewUserDTO("Nome Válido", "", Role.STUDENT, "123456"), "email"),
                Arguments.of(new NewUserDTO("Nome Válido", "email-invalido", Role.STUDENT,"123456"), "email"),
                Arguments.of(new NewUserDTO("Nome Válido", "email@valido.com", Role.STUDENT, "12345"), "password"),
                Arguments.of(new NewUserDTO("Nome Válido", "email@valido.com", Role.STUDENT, "1234567"), "password"),
                Arguments.of(new NewUserDTO("Nome Válido", "email@valido.com", null, "123456"), "role")
        );
    }

    @Test
    @DisplayName("POST /user/new: Should return Status 400 if email already exists (BusinessRuleException)")
    @Transactional
    void createUser_whenEmailAlreadyExists_shouldReturnBadRequest() throws Exception {
        // Arrange
        NewUserDTO duplicateEmailDto = new NewUserDTO("Outro Caio", "caio@alura.com.br", Role.STUDENT, "abcdef");

        // Act
        MvcResult result = mockMvc.perform(post("/user/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateEmailDto)))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Assert
        String expectedErrorMessage = "A user is already registered with the email: " + duplicateEmailDto.getEmail();
        assertThat(result.getResponse().getContentAsString())
                .contains(ProblemType.INVALID_OPERATION.getPath())
                .contains(ProblemType.INVALID_OPERATION.getTitle())
                .contains(expectedErrorMessage);
    }

    @Test
    @DisplayName("GET /user/all: Authenticated user (STUDENT) should list users and return Status 200 OK")
    @Transactional(readOnly = true)
    void listAllUsers_whenAuthenticatedAsStudent_shouldReturnOk() throws Exception {
        // Act
        MvcResult result = mockMvc.perform(get("/user/all")
                        .header("Authorization", "Bearer " + instructorJwtToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Assert
        List<UserListItemDTO> returnedList = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<List<UserListItemDTO>>() {}
        );

        assertThat(returnedList.size()).isGreaterThanOrEqualTo(2);
        assertThat(returnedList).extracting(UserListItemDTO::getEmail).contains("caio@alura.com.br", "paulo@alura.com.br");
    }

    @Test
    @DisplayName("GET /user/all: Should return Status 403 Forbidden when not authenticated")
    void listAllUsers_whenNotAuthenticated_shouldReturnForbidden() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/user/all")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
