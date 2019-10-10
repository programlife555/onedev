package io.onedev.server.plugin.report.html;

import java.util.List;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.HeaderItem;

import io.onedev.server.web.page.base.BaseDependentCssResourceReference;
import io.onedev.server.web.page.base.BaseDependentResourceReference;

public class HtmlReportResourceReference extends BaseDependentResourceReference {

	private static final long serialVersionUID = 1L;

	public HtmlReportResourceReference() {
		super(HtmlReportResourceReference.class, "html-report.js");
	}

	@Override
	public List<HeaderItem> getDependencies() {
		List<HeaderItem> dependencies = super.getDependencies();
		dependencies.add(CssHeaderItem.forReference(new BaseDependentCssResourceReference(
				HtmlReportResourceReference.class, "html-report.css")));
		return dependencies;
	}

}
