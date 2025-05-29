package br.com.alura.AluraFake.application.factory;
import br.com.alura.AluraFake.api.dto.request.TaskCreationRequest;
import br.com.alura.AluraFake.application.interfaces.CreateTaskUseCase;
import br.com.alura.AluraFake.domain.enumeration.Type;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TaskUseCaseFactory {

    private final Map<Type, CreateTaskUseCase<? extends TaskCreationRequest>> useCaseMap;

    public TaskUseCaseFactory(List<CreateTaskUseCase<?>> useCases) {
        useCaseMap = new EnumMap<>(Type.class);
        for (CreateTaskUseCase<?> useCase : useCases) {
            useCaseMap.put(useCase.getType(), useCase);
        }
    }

    @SuppressWarnings("unchecked")
    public <R extends TaskCreationRequest> CreateTaskUseCase<R> getUseCase(Type type) {
        return (CreateTaskUseCase<R>) Optional.ofNullable(useCaseMap.get(type))
                .orElseThrow(() -> new IllegalArgumentException("Nenhum Use Case encontrado para o tipo de tarefa: " + type));
    }
}
