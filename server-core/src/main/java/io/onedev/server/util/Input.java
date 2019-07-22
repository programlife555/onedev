package io.onedev.server.util;

import java.io.Serializable;
import java.util.List;

import javax.annotation.Nullable;
import javax.validation.ValidationException;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

import io.onedev.server.util.inputspec.InputSpec;
import io.onedev.server.util.inputspec.SecretInput;

public class Input implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private static final Logger logger = LoggerFactory.getLogger(Input.class);

	private final String name;
	
	@XStreamOmitField
	private final String type;
	
	private final List<String> values;
	
	public Input(String name, String type, List<String> values) {
		this.name = name;
		this.type = type;
		this.values = values;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public List<String> getValues() {
		return values;
	}
	
	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		} else if (other instanceof Input) {
			Input otherField = (Input) other;
			return new EqualsBuilder()
					.append(getName(), otherField.getName())
					.append(getType(), otherField.getType())
					.append(getValues(), otherField.getValues())
					.isEquals();
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37)
				.append(getName())
				.append(getType())
				.append(getValues())
				.toHashCode();
	}

	@Nullable
	public Object getTypedValue(@Nullable InputSpec inputSpec) {
		try {
			return inputSpec.convertToObject(getValues());
		} catch (ValidationException e) {
			String displayValue;
			if (type.equals(InputSpec.SECRET)) 
				displayValue = SecretInput.MASK;
			else 
				displayValue = "" + getValues();
			
			logger.error("Error converting field value (field: {}, value: {}, error: {})", 
					getName(), displayValue, e.getMessage());
			return null;
		}
	}
	
}
