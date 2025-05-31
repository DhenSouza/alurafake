package br.com.alura.AluraFake.security;

import br.com.alura.AluraFake.domain.enumeration.Role;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                // Configura a política de criação de sessão para STATELESS se usar tokens (ex: JWT)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // Endpoints de criação e publicação restritos a INSTRUCTOR
                        .requestMatchers(HttpMethod.POST, "/course/new").hasRole(Role.INSTRUCTOR.name())
                        .requestMatchers(HttpMethod.POST, "/course/{id}/publish").hasRole(Role.INSTRUCTOR.name())
                        .requestMatchers(HttpMethod.POST, "/task/new").hasRole(Role.INSTRUCTOR.name())


                        // Endpoints de listagem (ex: /course/all) acessíveis por qualquer usuário AUTENTICADO
                        .requestMatchers(HttpMethod.GET, "/course/all").authenticated()
                        .requestMatchers(HttpMethod.GET, "/user/all").authenticated()
                        .requestMatchers(HttpMethod.POST, "/user/new").authenticated()

                        // Qualquer outra requisição deve ser autenticada (regra geral)
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults());
        // Para JWT, você adicionaria um filtro customizado aqui com .addFilterBefore(...)

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
