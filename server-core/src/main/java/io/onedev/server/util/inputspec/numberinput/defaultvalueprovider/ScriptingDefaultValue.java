package io.onedev.server.util.inputspec.numberinput.defaultvalueprovider;

import org.hibernate.validator.constraints.NotEmpty;

import io.onedev.server.util.GroovyUtils;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.Multiline;
import io.onedev.server.web.editable.annotation.OmitName;
import io.onedev.server.web.editable.annotation.Script;

@Editable(order=400, name="Evaluate script to get default value")
public class ScriptingDefaultValue implements DefaultValueProvider {

	private static final long serialVersionUID = 1L;

	private String script;

	@Editable(description="Groovy script to be evaluated. It should return a <i>Integer</i> value. "
			+ "Check <a href='$docRoot/Scripting' target='_blank'>scripting help</a> for details")
	@NotEmpty
	@Script(Script.GROOVY)
	@OmitName
	@Multiline
	public String getScript() {
		return script;
	}

	public void setScript(String script) {
		this.script = script;
	}

	@Override
	public int getDefaultValue() {
		return (int) GroovyUtils.evalScript(getScript());
	}

}
