package io.onedev.server.web.util;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.wicket.AttributeModifier;

@SuppressWarnings("serial")
public class ConfirmOnClick extends AttributeModifier {

	public ConfirmOnClick(String message) {
		super("onclick", String.format("return confirm('%s');", StringEscapeUtils.escapeEcmaScript(message)));
	}

}
