package br.com.alura.AluraFake.application.service.user;

import br.com.alura.AluraFake.api.dto.request.NewUserDTO;
import br.com.alura.AluraFake.domain.model.User;

public interface UserCreateInterface {
    User createUser(NewUserDTO newUser);
}
