package br.com.alura.AluraFake.application.service.course.validation;

import br.com.alura.AluraFake.domain.enumeration.Type;
import br.com.alura.AluraFake.domain.model.Course;
import br.com.alura.AluraFake.domain.model.Task;
import br.com.alura.AluraFake.exceptionhandler.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CourseContentValidation implements CourseContentValidationInterface {
    @Override
    public void validateType(Course course) {

        if (course.getTasks() == null || course.getTasks().isEmpty()) {
            throw new BusinessRuleException("O curso deve conter atividades para ser publicado.");
        }

        Set<Type> presentTaskTypes = course.getTasks().stream()
                .map(Task::getTypeTask)
                .collect(Collectors.toSet());

        Set<Type> requiredTaskTypes = EnumSet.of(Type.OPEN_TEXT, Type.SINGLE_CHOICE, Type.MULTIPLE_CHOICE);

        if (!presentTaskTypes.containsAll(requiredTaskTypes)) {
            requiredTaskTypes.removeAll(presentTaskTypes);
            throw new BusinessRuleException(
                    "Para publicar o curso, é necessário ao menos uma atividade de cada um dos seguintes tipos: " +
                            requiredTaskTypes.stream().map(Enum::name).collect(Collectors.joining(", "))
            );
        }
    }
}
