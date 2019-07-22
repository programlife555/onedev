package io.onedev.server.web.page.project.blob.render.renderers.failsafe;

import org.apache.wicket.request.resource.CssResourceReference;

public class FailsafeResourceReference extends CssResourceReference {

	private static final long serialVersionUID = 1L;

	public FailsafeResourceReference() {
		super(FailsafeResourceReference.class, "failsafe.css");
	}

}
