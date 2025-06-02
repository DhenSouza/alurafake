package br.com.alura.AluraFake.user.unitTest.controller;

import br.com.alura.AluraFake.api.controller.UserController;
import br.com.alura.AluraFake.api.dto.request.NewUserDTO;
import br.com.alura.AluraFake.api.dto.response.UserListItemDTO;
import br.com.alura.AluraFake.application.interfaces.UserServiceInterface;
import br.com.alura.AluraFake.domain.enumeration.Role;
import br.com.alura.AluraFake.domain.model.User;
import br.com.alura.AluraFake.domain.repository.UserRepository;
import br.com.alura.AluraFake.domain.service.AppUserDetailsService;
import br.com.alura.AluraFake.globalHandler.BusinessRuleException;
import br.com.alura.AluraFake.globalHandler.GlobalExceptionHandler;
import br.com.alura.AluraFake.globalHandler.dto.ProblemType;
import br.com.alura.AluraFake.security.JwtAuthenticationFilter;
import br.com.alura.AluraFake.security.JwtTokenUtil;
import br.com.alura.AluraFake.security.SecurityConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(UserController.class)
@Import({GlobalExceptionHandler.class, SecurityConfiguration.class, JwtTokenUtil.class, JwtAuthenticationFilter.class, AppUserDetailsService.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserServiceInterface userServiceMock;

    @MockBean
    private UserRepository userRepositoryMock;

    @Captor
    private ArgumentCaptor<NewUserDTO> newUserDTOCaptor;

    private final String errorBaseUri = "https://api.seusite.com/erros";
    private NewUserDTO validNewUserDTO;
    private User sampleUser;

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("POST /user/new: Should return 400 Bad Request when email is blank")
    void newUser_whenEmailIsBlank_shouldReturnBadRequest() throws Exception {
        NewUserDTO newUserDTO = new NewUserDTO();
        newUserDTO.setEmail("");
        newUserDTO.setName("Caio Bugorin");
        newUserDTO.setRole(Role.STUDENT);

        mockMvc.perform(post("/user/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUserDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].name").value("email"))
                .andExpect(jsonPath("$.fields[0].userMessage").isNotEmpty());
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("POST /user/new: Should return 400 Bad Request when email is invalid")
    void newUser_whenEmailIsInvalid_shouldReturnBadRequest() throws Exception {
        NewUserDTO newUserDTO = new NewUserDTO();
        newUserDTO.setEmail("caio");
        newUserDTO.setName("Caio Bugorin");
        newUserDTO.setRole(Role.STUDENT);

        mockMvc.perform(post("/user/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUserDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].name").value("email"))
                .andExpect(jsonPath("$.fields[0].userMessage").isNotEmpty());
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("POST /user/new: Should return 400 Bad Request when email already exists")
    void newUser_whenEmailAlreadyExists_shouldReturnBadRequest() throws Exception {
        // Arrange
        NewUserDTO newUserDTO = new NewUserDTO();
        newUserDTO.setName("Caio Bugorin");
        newUserDTO.setEmail("caio.bugorin@alura.com.br");
        newUserDTO.setPassword("123456");
        newUserDTO.setRole(Role.INSTRUCTOR);

        String expectedErrorMessageFromService = "A user is already registered with the email: " + newUserDTO.getEmail();

        when(userServiceMock.createUser(any(NewUserDTO.class)))
                .thenThrow(new BusinessRuleException(expectedErrorMessageFromService));

        // Act
        ResultActions resultActions = mockMvc.perform(post("/user/new")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newUserDTO)));

        // Assert
        String expectedTypeUri = errorBaseUri + ProblemType.INVALID_OPERATION.getPath();
        String expectedTitle = ProblemType.INVALID_OPERATION.getTitle();
        String expectedUserMessage = expectedErrorMessageFromService;

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.type").value(expectedTypeUri))
                .andExpect(jsonPath("$.title").value(expectedTitle))
                .andExpect(jsonPath("$.detail").value(expectedErrorMessageFromService))
                .andExpect(jsonPath("$.userMessage").value(expectedUserMessage))
                .andExpect(jsonPath("$.instance").value("/user/new"))
                .andExpect(jsonPath("$.fields").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("POST /user/new: Deve criar novo usuário com DTO válido e retornar Status 201 Created com Location e corpo")
    void newUser_whenRequestIsValid_shouldReturnCreatedWithLocationAndBody() throws Exception {
        // Arrange
        NewUserDTO newUserDTO = new NewUserDTO(
                "Caio Bugorin Valido",
                "caio.valido@alura.com.br",
                Role.INSTRUCTOR,
                "123456"
        );

        User userReturnedByService = new User(
                newUserDTO.getName(),
                newUserDTO.getEmail(),
                newUserDTO.getRole(),
                "hashedPasswordValue"
        );
        userReturnedByService.setId(123L);

        when(userServiceMock.createUser(any(NewUserDTO.class))).thenReturn(userReturnedByService);

        // Act
        MvcResult result = mockMvc.perform(post("/user/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUserDTO)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(header().string(HttpHeaders.LOCATION, "http://localhost/user/new/" + userReturnedByService.getId())) // Ajuste o path base se necessário
                .andReturn();

        // Assert
        verify(userServiceMock).createUser(newUserDTOCaptor.capture());
        NewUserDTO capturedDTO = newUserDTOCaptor.getValue();
        assertThat(capturedDTO.getEmail()).isEqualTo(newUserDTO.getEmail());
        assertThat(capturedDTO.getName()).isEqualTo(newUserDTO.getName());
        assertThat(capturedDTO.getPassword()).isEqualTo("123456");

        UserListItemDTO responseDto = objectMapper.readValue(result.getResponse().getContentAsString(), UserListItemDTO.class);
        assertThat(responseDto.getEmail()).isEqualTo(userReturnedByService.getEmail());
        assertThat(responseDto.getName()).isEqualTo(userReturnedByService.getName());
        assertThat(responseDto.getRole()).isEqualTo(userReturnedByService.getRole());
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("GET /user/all: Should list users and return 200 OK with correct user list")
    void listAllUsers_whenUsersExist_shouldReturnOkWithUserList() throws Exception {
        // Arrange
        User userEntity1 = new User("User 1", "user1@test.com", Role.STUDENT, "hashedPassword1");
        userEntity1.setId(1L);
        User userEntity2 = new User("User 2", "user2@test.com", Role.STUDENT, "hashedPassword2");
        userEntity2.setId(2L);

        UserListItemDTO dto1 = new UserListItemDTO(userEntity1);
        UserListItemDTO dto2 = new UserListItemDTO(userEntity2);
        List<UserListItemDTO> expectedDtoListFromService = Arrays.asList(dto1, dto2);

        when(userServiceMock.listAllUsers()).thenReturn(expectedDtoListFromService);

        // Act
        ResultActions resultActions = mockMvc.perform(get("/user/all")
                .accept(MediaType.APPLICATION_JSON));

        // Assert
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("User 1"))
                .andExpect(jsonPath("$[0].email").value("user1@test.com"))
                .andExpect(jsonPath("$[0].role").value(Role.STUDENT.name()))
                .andExpect(jsonPath("$[1].name").value("User 2"))
                .andExpect(jsonPath("$[1].email").value("user2@test.com"));

        // Verify
        verify(userServiceMock, times(1)).listAllUsers();
    }

}
