package io.onedev.server.ci.job.param;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class SpecifiedValues implements ValuesProvider {

	private static final long serialVersionUID = 1L;

	public static final String DISPLAY_NAME = "Use specified values";
	
	public static final String SECRET_DISPLAY_NAME = "Use specified secrets";

	private List<List<String>> values = new ArrayList<>();
	
	@Override
	public List<List<String>> getValues() {
		return values;
	}

	public void setValues(List<List<String>> values) {
		this.values = values;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof SpecifiedValues)) 
			return false;
		if (this == other)
			return true;
		SpecifiedValues otherSpecifiedValues = (SpecifiedValues) other;
		return new EqualsBuilder()
			.append(values, otherSpecifiedValues.values)
			.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37)
			.append(values)
			.toHashCode();
	}

}
