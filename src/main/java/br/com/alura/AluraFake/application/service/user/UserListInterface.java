package br.com.alura.AluraFake.application.service.user;

import br.com.alura.AluraFake.api.dto.response.UserListItemDTO;

import java.util.List;

public interface UserListInterface {

    List<UserListItemDTO> listAllUsers();
}
