package br.com.alura.AluraFake.api.validation;

import br.com.alura.AluraFake.api.dto.request.ChoiceOptionRequest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TaskOptionValidator {
    public static void validateUniqueAndStatementComparison(String statement, List<ChoiceOptionRequest> options) {

        Set<String> uniqueOptions = new HashSet<>();
        for (ChoiceOptionRequest opt : options) {
            if (!uniqueOptions.add(opt.option().trim().toLowerCase())) {
                throw new IllegalArgumentException("As alternativas não podem ser iguais entre si.");
            }
        }

        if (statement != null) {
            String normalizedStatement = statement.trim().toLowerCase();
            for (ChoiceOptionRequest opt : options) {
                if (opt.option() != null && opt.option().trim().toLowerCase().equals(normalizedStatement)) {
                    throw new IllegalArgumentException("As alternativas não podem ser iguais ao enunciado da atividade.");
                }
            }
        }
    }
}
