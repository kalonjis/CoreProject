package be.steby.CoreProject.bll.common.validators;

import java.util.ArrayList;
import java.util.List;

public record ValidatorResult(
        boolean isValid,
        List<String> errors
) {
    /**
     * Successful validation
     */
    public static ValidatorResult valid(){
        return new ValidatorResult(true, List.of());
    }

    /**
     * Failed validation
     */
    public static ValidatorResult invalid(String error){
        return new ValidatorResult(false, List.of(error));
    }

    /**
     * Failed validation
     */
    public static ValidatorResult invalid(List<String> errors){
        return new ValidatorResult(false, errors);
    }

    /**
     *  Combines several validation results
     */
    public static ValidatorResult combine(ValidatorResult... results){
        List<String> allErrors = new ArrayList<>();
        boolean allValid = true;

        for (ValidatorResult result: results){
            if (!result.isValid){
                allErrors.addAll(result.errors());
                allValid = false;
            }
        }
        return new ValidatorResult(allValid, allErrors);
    }
}
