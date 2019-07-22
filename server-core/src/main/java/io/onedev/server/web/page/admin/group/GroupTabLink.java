package io.onedev.server.web.page.admin.group;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;

import io.onedev.server.web.component.link.ViewStateAwarePageLink;

@SuppressWarnings("serial")
public class GroupTabLink extends Panel {

	private final GroupTab tab;
	
	public GroupTabLink(String id, GroupTab tab) {
		super(id);
		
		this.tab = tab;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		GroupPage page = (GroupPage) getPage();
		Link<Void> link = new ViewStateAwarePageLink<Void>("link", 
				tab.getMainPageClass(), GroupPage.paramsOf(page.getGroup()));
		link.add(new WebMarkupContainer("icon").add(AttributeAppender.append("class", tab.getIconClass())));
		link.add(new Label("label", tab.getTitleModel()));
		add(link);
	}

}
