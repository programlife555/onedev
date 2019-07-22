package io.onedev.server.web.page.my;

import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import io.onedev.server.model.User;
import io.onedev.server.web.component.user.avataredit.AvatarEditPanel;

@SuppressWarnings("serial")
public class MyAvatarPage extends MyPage {
	
	public MyAvatarPage(PageParameters params) {
		super(params);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		add(new AvatarEditPanel("content", new AbstractReadOnlyModel<User>() {

			@Override
			public User getObject() {
				return getLoginUser();
			}
			
		}));
	}
	
}
