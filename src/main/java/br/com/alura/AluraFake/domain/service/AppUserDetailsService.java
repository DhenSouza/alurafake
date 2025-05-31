package br.com.alura.AluraFake.domain.service;

import br.com.alura.AluraFake.domain.model.User;
import br.com.alura.AluraFake.domain.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
public class AppUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional(readOnly = true) // Boa prática para métodos de leitura
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User applicationUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário com e-mail '" + email + "' não encontrado."));

        Set<GrantedAuthority> authorities = new HashSet<>();
        // Adiciona o prefixo "ROLE_" para compatibilidade com hasRole()
        authorities.add(new SimpleGrantedAuthority("ROLE_" + applicationUser.getRole().name()));
        // Se o usuário tivesse uma lista de roles, você iteraria e adicionaria todas.

        return new org.springframework.security.core.userdetails.User(
                applicationUser.getEmail(),
                applicationUser.getPassword(),
                authorities);
    }
}
