package br.com.alura.AluraFake.application.service.task;

import br.com.alura.AluraFake.api.dto.request.TaskRequest;
import br.com.alura.AluraFake.application.service.task.interfaces.CreateOpenTextTaskUseCaseInterface;
import br.com.alura.AluraFake.domain.repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateOpenTextTaskUseCase implements CreateOpenTextTaskUseCaseInterface {

    private final CourseRepository courseRepository;

    @Autowired
    public CreateOpenTextTaskUseCase(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @Transactional
    public void createOpenTextTask(TaskRequest taskRequest) {



    }
}
