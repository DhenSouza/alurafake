package br.com.alura.AluraFake.application.usecase.task;

import br.com.alura.AluraFake.api.dto.request.SingleChoiceTaskCreationRequest;
import br.com.alura.AluraFake.application.interfaces.CreateTaskUseCase;
import br.com.alura.AluraFake.application.service.task.CourseTaskService;
import br.com.alura.AluraFake.domain.enumeration.Type;
import br.com.alura.AluraFake.domain.model.Task;
import br.com.alura.AluraFake.domain.model.TaskOption;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CreateSingleChoiceUseCase extends AbstractCreateChoiceTaskUseCase<SingleChoiceTaskCreationRequest> {

    public CreateSingleChoiceUseCase(CourseTaskService courseTaskService) {
        super(courseTaskService);
    }

    @Override
    public Type getType() {
        return Type.SINGLE_CHOICE;
    }
}
