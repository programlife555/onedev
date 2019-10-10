package io.onedev.server.event.issue;

import java.util.HashMap;
import java.util.Map;

import io.onedev.server.OneDev;
import io.onedev.server.entitymanager.GroupManager;
import io.onedev.server.entitymanager.UserManager;
import io.onedev.server.event.MarkdownAware;
import io.onedev.server.model.Group;
import io.onedev.server.model.Issue;
import io.onedev.server.model.User;
import io.onedev.server.model.support.issue.fieldspec.FieldSpec;
import io.onedev.server.util.Input;

public class IssueOpened extends IssueEvent implements MarkdownAware {

	public IssueOpened(Issue issue) {
		super(issue.getSubmitter(), issue.getSubmitDate(), issue);
	}

	@Override
	public String getMarkdown() {
		return getIssue().getDescription();
	}

	@Override
	public boolean affectsBoards() {
		return true;
	}

	@Override
	public Map<String, User> getNewUsers() {
		Map<String, User> newUsers = new HashMap<>();
		UserManager userManager = OneDev.getInstance(UserManager.class);
		for (Input field: getIssue().getFieldInputs().values()) {
			if (field.getType().equals(FieldSpec.USER)) {
				if (!field.getValues().isEmpty()) {
					User newUser = userManager.findByName(field.getValues().iterator().next());
					if (newUser != null)
						newUsers.put(field.getName(), newUser);
				}
			} 
		}
		return newUsers;
	}

	@Override
	public Map<String, Group> getNewGroups() {
		Map<String, Group> newGroups = new HashMap<>();
		GroupManager groupManager = OneDev.getInstance(GroupManager.class);
		for (Input field: getIssue().getFieldInputs().values()) {
			if (field.getType().equals(FieldSpec.GROUP)) {
				if (!field.getValues().isEmpty()) {
					Group newGroup = groupManager.find(field.getValues().iterator().next());
					if (newGroup != null)
						newGroups.put(field.getName(), newGroup);
				}
			} 
		}
		return newGroups;
	}

}