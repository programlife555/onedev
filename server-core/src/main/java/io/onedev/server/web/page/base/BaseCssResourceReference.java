package io.onedev.server.web.page.base;

import java.util.List;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.request.resource.CssResourceReference;

import de.agilecoders.wicket.core.Bootstrap;
import io.onedev.server.web.asset.fontext.FontExtResourceReference;

public class BaseCssResourceReference extends CssResourceReference {

	private static final long serialVersionUID = 1L;

	public BaseCssResourceReference() {
		super(BaseCssResourceReference.class, "base.css");
	}

	@Override
	public List<HeaderItem> getDependencies() {
		List<HeaderItem> dependencies = super.getDependencies();
		dependencies.add(CssHeaderItem.forReference(Bootstrap.getSettings().getCssResourceReference()));
		dependencies.add(CssHeaderItem.forReference(new FontExtResourceReference()));
		return dependencies;
	}

}
