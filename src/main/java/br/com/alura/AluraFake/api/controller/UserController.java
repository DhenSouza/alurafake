package br.com.alura.AluraFake.api.controller;

import br.com.alura.AluraFake.api.dto.request.NewUserDTO;
import br.com.alura.AluraFake.application.service.user.UserCreateInterface;
import br.com.alura.AluraFake.domain.model.User;
import br.com.alura.AluraFake.api.dto.response.UserListItemDTO;
import br.com.alura.AluraFake.domain.repository.UserRepository;
import br.com.alura.AluraFake.util.ErrorItemDTO;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserCreateInterface userCreate;

    private final UserRepository userRepository;

    public UserController(UserCreateInterface userCreate, UserRepository userRepository) {
        this.userCreate = userCreate;
        this.userRepository = userRepository;
    }

    @PostMapping("/new")
    public ResponseEntity<?> newStudent(@RequestBody @Valid NewUserDTO newUser) {
        this.userCreate.createUser(newUser);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/all")
    public List<UserListItemDTO> listAllUsers() {
        return userRepository.findAll().stream().map(UserListItemDTO::new).toList();
    }

}
