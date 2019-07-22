package io.onedev.server.web.component.commit.graph;

import java.util.List;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.resource.CssResourceReference;

import io.onedev.server.web.asset.snapsvg.SnapSvgResourceReference;
import io.onedev.server.web.page.base.BaseDependentResourceReference;

public class CommitGraphResourceReference extends BaseDependentResourceReference {

	private static final long serialVersionUID = 1L;

	public CommitGraphResourceReference() {
		super(CommitGraphResourceReference.class, "commit-graph.js");
	}

	@Override
	public List<HeaderItem> getDependencies() {
		List<HeaderItem> dependencies = super.getDependencies();
		dependencies.add(JavaScriptHeaderItem.forReference(new SnapSvgResourceReference()));
		dependencies.add(CssHeaderItem.forReference(
				new CssResourceReference(CommitGraphResourceReference.class, "commit-graph.css")));
		return dependencies;
	}

}
