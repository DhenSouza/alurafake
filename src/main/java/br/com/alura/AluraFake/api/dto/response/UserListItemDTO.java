package br.com.alura.AluraFake.api.dto.response;

import br.com.alura.AluraFake.domain.enumeration.Role;
import br.com.alura.AluraFake.domain.model.User;

import java.io.Serializable;

public class UserListItemDTO implements Serializable {

    private String name;
    private String email;
    private Role role;

    public UserListItemDTO(User user) {
        this.name = user.getName();
        this.email = user.getEmail();
        this.role = user.getRole();
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

}
