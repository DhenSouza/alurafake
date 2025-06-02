package br.com.alura.AluraFake.api.controller;

import br.com.alura.AluraFake.api.dto.request.NewUserDTO;
import br.com.alura.AluraFake.api.dto.response.UserListItemDTO;
import br.com.alura.AluraFake.application.interfaces.UserServiceInterface;
import br.com.alura.AluraFake.domain.model.Course;
import br.com.alura.AluraFake.domain.model.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserServiceInterface userService;


    public UserController(UserServiceInterface userService) {
        this.userService = userService;
    }

    @PostMapping("/new")
    public ResponseEntity<?> newStudent(@RequestBody @Valid NewUserDTO newUser) {
        User createdUser = this.userService.createUser(newUser);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdUser.getId())
                .toUri();

        return ResponseEntity.created(location).body(new UserListItemDTO(createdUser));
    }

    @GetMapping("/all")
    public ResponseEntity<List<UserListItemDTO>> listAllUsers() {
        return ResponseEntity.ok(userService.listAllUsers());
    }

}
