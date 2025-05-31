package br.com.alura.AluraFake.api.controller;

import br.com.alura.AluraFake.api.dto.request.NewUserDTO;
import br.com.alura.AluraFake.api.dto.response.UserListItemDTO;
import br.com.alura.AluraFake.application.interfaces.UserServiceInterface;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        this.userService.createUser(newUser);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/all")
    public ResponseEntity<List<UserListItemDTO>> listAllUsers() {
        return ResponseEntity.status(HttpStatus.OK).body(userService.listAllUsers());
    }

}
