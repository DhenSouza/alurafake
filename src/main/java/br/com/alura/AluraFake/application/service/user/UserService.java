package br.com.alura.AluraFake.application.service.user;

import br.com.alura.AluraFake.api.dto.request.NewUserDTO;
import br.com.alura.AluraFake.api.dto.response.UserListItemDTO;
import br.com.alura.AluraFake.application.interfaces.UserServiceInterface;
import br.com.alura.AluraFake.domain.model.User;
import br.com.alura.AluraFake.domain.repository.UserRepository;
import br.com.alura.AluraFake.exceptionhandler.BusinessRuleException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService implements UserServiceInterface {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User createUser(NewUserDTO newUser) {
        if(userRepository.existsByEmail(newUser.getEmail())) {
            throw new BusinessRuleException("Já existe um usuário cadastrado com o e-mail: " + newUser.getEmail());
        }
        User user = newUser.toModel();

        return userRepository.save(user);
    }

    @Override
    public List<UserListItemDTO> listAllUsers() {
        return userRepository.findAll().stream().map(UserListItemDTO::new).toList();
    }
}
