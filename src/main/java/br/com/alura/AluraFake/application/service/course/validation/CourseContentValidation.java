package br.com.alura.AluraFake.application.service.course.validation;

import br.com.alura.AluraFake.domain.enumeration.Type;
import br.com.alura.AluraFake.domain.model.Course;
import br.com.alura.AluraFake.domain.model.Task;
import br.com.alura.AluraFake.globalHandler.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CourseContentValidation implements CourseContentValidationInterface {
    @Override
    public void validateType(Course course) {

        if (course.getTasks() == null || course.getTasks().isEmpty()) {
            throw new BusinessRuleException("The course must contain activities to be published.");
        }

        Set<Type> presentTaskTypes = course.getTasks().stream()
                .map(Task::getTypeTask)
                .collect(Collectors.toSet());

        Set<Type> requiredTaskTypes = EnumSet.of(Type.OPEN_TEXT, Type.SINGLE_CHOICE, Type.MULTIPLE_CHOICE);

        if (!presentTaskTypes.containsAll(requiredTaskTypes)) {
            requiredTaskTypes.removeAll(presentTaskTypes);
            throw new BusinessRuleException(
                    "To publish the course, at least one activity of each of the following types is necessary: " +
                            requiredTaskTypes.stream().map(Enum::name).collect(Collectors.joining(", "))
            );
        }
    }

    @Override
    public void validateTaskOrderIsContinuous(Course course) {
        List<Task> tasks = course.getTasks();
        if (tasks == null || tasks.isEmpty()) {
            throw new BusinessRuleException("The course cannot be published without activities.");
        }

        tasks.sort(Comparator.comparingInt(Task::getOrder));

        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getOrder() != (i + 1)) {
                throw new BusinessRuleException(String.format(
                        "The order of the course activities is not continuous. Found order %d at position %d, expected %d.",
                        tasks.get(i).getOrder(), i + 1, i + 1));
            }
        }
    }
}
