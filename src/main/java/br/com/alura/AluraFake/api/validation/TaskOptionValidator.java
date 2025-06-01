package br.com.alura.AluraFake.api.validation;

import br.com.alura.AluraFake.api.dto.request.ChoiceOptionRequest;
import br.com.alura.AluraFake.globalHandler.OptionalInvalidException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TaskOptionValidator {
    public static void validateUniqueAndStatementComparison(String statement, List<ChoiceOptionRequest> options) {

        Set<String> uniqueOptions = new HashSet<>();
        for (ChoiceOptionRequest opt : options) {
            if (!uniqueOptions.add(opt.option().trim().toLowerCase())) {
                throw new OptionalInvalidException("The options cannot be the same as each other.");
            }
        }

        if (statement != null) {
            String normalizedStatement = statement.trim().toLowerCase();
            for (ChoiceOptionRequest opt : options) {
                if (opt.option() != null && opt.option().trim().toLowerCase().equals(normalizedStatement)) {
                    throw new OptionalInvalidException("The alternatives cannot be the same as the statement of the activity.");
                }
            }
        }
    }
}
