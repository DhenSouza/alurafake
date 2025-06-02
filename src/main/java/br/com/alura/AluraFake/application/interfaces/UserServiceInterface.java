package br.com.alura.AluraFake.application.interfaces;

import br.com.alura.AluraFake.api.dto.request.NewUserDTO;
import br.com.alura.AluraFake.api.dto.response.UserListItemDTO;
import br.com.alura.AluraFake.domain.model.User;

import java.util.List;

public interface UserServiceInterface {
    User createUser(NewUserDTO newUser);
    List<UserListItemDTO> listAllUsers();
}
