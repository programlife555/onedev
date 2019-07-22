package io.onedev.server.web.page.admin.group;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.unbescape.html.HtmlEscape;

import io.onedev.server.model.Group;
import io.onedev.server.security.SecurityUtils;
import io.onedev.server.web.component.floating.AlignPlacement;
import io.onedev.server.web.component.floating.FloatingPanel;
import io.onedev.server.web.component.link.DropdownLink;
import io.onedev.server.web.component.sidebar.SideBar;
import io.onedev.server.web.component.tabbable.PageTab;
import io.onedev.server.web.component.tabbable.Tab;
import io.onedev.server.web.component.tabbable.Tabbable;
import io.onedev.server.web.model.EntityModel;
import io.onedev.server.web.page.admin.AdministrationPage;

@SuppressWarnings("serial")
public abstract class GroupPage extends AdministrationPage {
	
	private static final String PARAM_GROUP = "group";
	
	protected final IModel<Group> groupModel;
	
	public GroupPage(PageParameters params) {
		super(params);
		
		Long groupId = params.get(PARAM_GROUP).toLong();
		
		groupModel = new EntityModel<Group>(Group.class, groupId);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		add(new SideBar("groupSidebar", null) {

			@Override
			protected Component newHead(String componentId) {
				String content = "<i class='fa fa-group'></i> " + HtmlEscape.escapeHtml5(getGroup().getName()); 
				return new Label(componentId, content).setEscapeModelStrings(false);
			}

			@Override
			protected List<? extends Tab> newTabs() {
				return GroupPage.this.newTabs();
			}
			
		});
	}
	
	private List<? extends Tab> newTabs() {
		List<PageTab> tabs = new ArrayList<>();
		
		tabs.add(new GroupTab("Profile", "fa fa-fw fa-list-alt", GroupProfilePage.class));
		tabs.add(new GroupTab("Members", "fa fa-fw fa-user", GroupMembershipsPage.class));
		if (SecurityUtils.isAdministrator() && !getGroup().isAdministrator())
			tabs.add(new GroupTab("Authorized Projects", "fa fa-fw fa-ext fa-repo", GroupAuthorizationsPage.class));
		return tabs;
	}

	@Override
	protected Component newNavContext(String componentId) {
		Fragment fragment = new Fragment(componentId, "navContextFrag", this);
		DropdownLink link = new DropdownLink("dropdown", AlignPlacement.bottom(15)) {

			@Override
			protected void onInitialize(FloatingPanel dropdown) {
				super.onInitialize(dropdown);
				dropdown.add(AttributeAppender.append("class", "nav-context-dropdown user-nav-context-dropdown"));
			}

			@Override
			protected Component newContent(String id, FloatingPanel dropdown) {
				Fragment fragment = new Fragment(id, "navContextDropdownFrag", GroupPage.this);
				fragment.add(new Tabbable("menu", newTabs()));
				return fragment;
			}
			
		};
		link.add(new Label("name", getGroup().getName()));
		fragment.add(link);
		
		return fragment;
	}	
	
	@Override
	protected void onDetach() {
		groupModel.detach();
		
		super.onDetach();
	}
	
	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new GroupCssResourceReference()));
	}
	
	public Group getGroup() {
		return groupModel.getObject();
	}
	
	public static PageParameters paramsOf(Group group) {
		PageParameters params = new PageParameters();
		params.add(PARAM_GROUP, group.getId());
		return params;
	}

}
