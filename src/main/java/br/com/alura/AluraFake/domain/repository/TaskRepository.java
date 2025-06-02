package br.com.alura.AluraFake.domain.repository;

import br.com.alura.AluraFake.domain.model.Task;
import br.com.alura.AluraFake.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {
}
