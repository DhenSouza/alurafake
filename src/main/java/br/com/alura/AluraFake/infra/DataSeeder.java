package br.com.alura.AluraFake.infra;

import br.com.alura.AluraFake.domain.enumeration.Role;
import br.com.alura.AluraFake.domain.model.Course;
import br.com.alura.AluraFake.domain.model.User;
import br.com.alura.AluraFake.domain.repository.CourseRepository;
import br.com.alura.AluraFake.domain.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class DataSeeder implements CommandLineRunner {

    @Value("${spring.profiles.active:}")
    private String activeProfile;

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      CourseRepository courseRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!"dev".equals(activeProfile) && !"test".equals(activeProfile)) {
            return;
        }

        if (userRepository.count() == 0) {
            System.out.println(">>> Populando banco de dados com usuários e cursos iniciais (dev profile)...");

            User caio = new User("Caio", "caio@alura.com.br", Role.STUDENT,
                    passwordEncoder.encode("senha123"));

            User paulo = new User("Paulo", "paulo@alura.com.br", Role.INSTRUCTOR,
                    passwordEncoder.encode("senha321"));

            userRepository.saveAll(Arrays.asList(caio, paulo));

            Course cursoJava = new Course("Java", "Aprenda Java com Alura", paulo);

            courseRepository.save(cursoJava);
            System.out.println(">>> Dados iniciais populados.");
        }
    }
}