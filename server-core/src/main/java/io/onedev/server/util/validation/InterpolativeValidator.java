package io.onedev.server.util.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import io.onedev.server.web.editable.annotation.Interpolative;

public class InterpolativeValidator implements ConstraintValidator<Interpolative, Object> {
	
	private String message;
	
	@Override
	public void initialize(Interpolative constaintAnnotation) {
		message = constaintAnnotation.message();
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean isValid(Object value, ConstraintValidatorContext constraintContext) {
		if (value == null) 
			return true;
		
		if (!Interpolated.get()) {
			List<String> values = new ArrayList<>();
			if (value instanceof Collection)
				values.addAll((Collection<String>)value);
			else
				values.add((String) value);
			
			for (String each: values) {
				try {
					io.onedev.server.util.interpolative.Interpolative.fromString(each);					
				} catch (Exception e) {
					constraintContext.disableDefaultConstraintViolation();
					String message = this.message;
					if (message.length() == 0)
						message = "Malformed interpolative: please make sure '@' and '\\' is escaped appropriately";
					constraintContext.buildConstraintViolationWithTemplate(message).addConstraintViolation();
					return false;
				}
			}
		} 
		return true;
	}
}