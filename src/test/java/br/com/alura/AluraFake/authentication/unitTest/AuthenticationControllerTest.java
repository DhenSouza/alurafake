package br.com.alura.AluraFake.authentication.unitTest;

import br.com.alura.AluraFake.api.controller.AuthenticationController;
import br.com.alura.AluraFake.api.dto.request.LoginRequestDTO;
import br.com.alura.AluraFake.domain.repository.UserRepository;
import br.com.alura.AluraFake.domain.service.AppUserDetailsService;
import br.com.alura.AluraFake.globalHandler.GlobalExceptionHandler;
import br.com.alura.AluraFake.globalHandler.dto.ProblemType;
import br.com.alura.AluraFake.security.JwtTokenUtil;
import br.com.alura.AluraFake.security.SecurityConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User; // User do Spring Security
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ExtendWith(SpringExtension.class)
@WebMvcTest(AuthenticationController.class)
@Import({GlobalExceptionHandler.class, SecurityConfiguration.class, JwtTokenUtil.class, AppUserDetailsService.class})
public class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManagerMock;

    @MockBean
    private JwtTokenUtil jwtTokenUtilMock;

    @MockBean
    private UserRepository userRepositoryMock;

    private final String errorBaseUri = "https://api.seusite.com/erros"; // Do seu GlobalExceptionHandler

    private LoginRequestDTO validLoginRequest;
    private UserDetails mockUserDetails;
    private Authentication mockAuthentication;

    @BeforeEach
    void setUp() {
        validLoginRequest = new LoginRequestDTO("test@example.com", "password123");

        mockUserDetails = new User(
                "test@example.com",
                "hashedPassword",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        mockAuthentication = new UsernamePasswordAuthenticationToken(mockUserDetails, null, mockUserDetails.getAuthorities());
    }

    @Test
    @DisplayName("POST /auth/login: With valid credentials, should return Status 200 OK and JWT token")
    void createAuthenticationToken_withValidCredentials_shouldReturnOkWithJwt() throws Exception {
        // Arrange
        String expectedJwt = "mocked.jwt.token.string";

        when(authenticationManagerMock.authenticate(
                any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuthentication);

        when(jwtTokenUtilMock.generateToken(mockAuthentication)).thenReturn(expectedJwt);

        // Act
        ResultActions resultActions = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginRequest)));

        // Assert
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwtToken").value(expectedJwt));

        verify(authenticationManagerMock).authenticate(
                Mockito.argThat(token ->
                        token.getName().equals(validLoginRequest.getEmail()) &&
                                token.getCredentials().toString().equals(validLoginRequest.getPassword())
                )
        );
        verify(jwtTokenUtilMock).generateToken(mockAuthentication);
    }

// Dentro da sua classe AuthenticationControllerTest

    @Test
    @DisplayName("POST /auth/login: With invalid credentials, should return Status 401 Unauthorized")
    void createAuthenticationToken_withInvalidCredentials_shouldReturnUnauthorized() throws Exception {
        // Arrange
        LoginRequestDTO invalidLoginRequest = new LoginRequestDTO("wrong@example.com", "wrongpassword");

        String badCredentialsMessage = "Invalid email or password.";
        String serviceExceptionMessage = "Customize Toolbar…";

        when(authenticationManagerMock.authenticate(
                argThat(token ->
                        token.getName().equals(invalidLoginRequest.getEmail()) &&
                                token.getCredentials().toString().equals(invalidLoginRequest.getPassword())
                )
        )).thenThrow(new BadCredentialsException(serviceExceptionMessage));

        // Act
        ResultActions resultActions = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidLoginRequest)));

        // Assert
        String expectedTypeUri = errorBaseUri + ProblemType.AUTHENTICATION_ERROR.getPath();
        String expectedTitle = ProblemType.AUTHENTICATION_ERROR.getTitle();
        String expectedUserMessage = ProblemType.AUTHENTICATION_ERROR.getMessage();

        resultActions
                .andExpect(status().isUnauthorized()) // HTTP 401
                .andExpect(jsonPath("$.status").value(HttpStatus.UNAUTHORIZED.value()))
                .andExpect(jsonPath("$.type").value(expectedTypeUri))
                .andExpect(jsonPath("$.title").value(expectedTitle))
                .andExpect(jsonPath("$.detail").value(badCredentialsMessage))
                .andExpect(jsonPath("$.userMessage").value(expectedUserMessage))
                .andExpect(jsonPath("$.instance").value("/auth/login"));

        verify(jwtTokenUtilMock, never()).generateToken(any(Authentication.class));
    }

    @Test
    @DisplayName("POST /auth/login: With invalid LoginRequestDTO (Bean Validation), should return Status 400 Bad Request")
    void createAuthenticationToken_withInvalidLoginRequestDTO_shouldReturnBadRequest() throws Exception {
        // Arrange
        LoginRequestDTO dtoWithBlankEmail = new LoginRequestDTO("", "password123");

        // Act
        ResultActions resultActions = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dtoWithBlankEmail)));

        // Assert
        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.type").value(errorBaseUri + ProblemType.DEFAULT_USER_MESSAGE_VALIDATION.getPath()))
                .andExpect(jsonPath("$.title").value(ProblemType.DEFAULT_USER_MESSAGE_VALIDATION.getTitle()))
                .andExpect(jsonPath("$.fields[0].name").value("email"));

        verify(authenticationManagerMock, never()).authenticate(any());
        verify(jwtTokenUtilMock, never()).generateToken(any());
    }

    @Test
    @DisplayName("POST /auth/login: When token generation fails, should return Status 500 Internal Server Error")
    void createAuthenticationToken_whenTokenGenerationFails_shouldReturnInternalServerError() throws Exception {
        // Arrange
        String tokenGenerationErrorMessage = "Error during JWT generation";
        when(authenticationManagerMock.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuthentication);
        when(jwtTokenUtilMock.generateToken(mockAuthentication))
                .thenThrow(new RuntimeException(tokenGenerationErrorMessage));

        // Act
        ResultActions resultActions = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginRequest)));

        // Assert
        resultActions
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(HttpStatus.INTERNAL_SERVER_ERROR.value()))
                .andExpect(jsonPath("$.type").value(errorBaseUri + ProblemType.UNEXPECTED_ERROR.getPath()))
                .andExpect(jsonPath("$.title").value(ProblemType.UNEXPECTED_ERROR.getTitle()))
                .andExpect(jsonPath("$.detail").value(ProblemType.UNEXPECTED_ERROR.getMessage()))
                .andExpect(jsonPath("$.userMessage").value(ProblemType.UNEXPECTED_ERROR.getMessage()));
    }

}
