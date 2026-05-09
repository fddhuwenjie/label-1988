package com.tare.validator;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.springframework.scheduling.support.CronExpression;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = CronExpressionValidator.CronValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface CronExpressionValidator {
    
    String message() default "Cron表达式格式不正确";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    boolean allowEmpty() default true;
    
    class CronValidator implements ConstraintValidator<CronExpressionValidator, String> {
        
        private boolean allowEmpty;
        
        @Override
        public void initialize(CronExpressionValidator constraintAnnotation) {
            this.allowEmpty = constraintAnnotation.allowEmpty();
        }
        
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null || value.trim().isEmpty()) {
                return allowEmpty;
            }
            
            try {
                CronExpression.parse(value);
                return true;
            } catch (IllegalArgumentException e) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        "Cron表达式格式不正确: " + e.getMessage()
                ).addConstraintViolation();
                return false;
            }
        }
    }
}
