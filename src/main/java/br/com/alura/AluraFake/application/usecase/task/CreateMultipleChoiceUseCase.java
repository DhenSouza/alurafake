package br.com.alura.AluraFake.application.usecase.task;

import br.com.alura.AluraFake.api.dto.request.MultipleChoiceTaskCreationRequest;
import br.com.alura.AluraFake.application.service.task.CourseTaskService;
import br.com.alura.AluraFake.domain.enumeration.Type;
import org.springframework.stereotype.Service;

@Service
public class CreateMultipleChoiceUseCase extends AbstractCreateChoiceTaskUseCase<MultipleChoiceTaskCreationRequest> {

    public CreateMultipleChoiceUseCase(CourseTaskService courseTaskService) {
        super(courseTaskService);
    }

    @Override
    public Type getType() {
        return Type.MULTIPLE_CHOICE;
    }
}
