package io.onedev.server.search.entity.issue;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import io.onedev.server.OneDev;
import io.onedev.server.entitymanager.SettingManager;
import io.onedev.server.model.Issue;
import io.onedev.server.model.Project;
import io.onedev.server.model.User;
import io.onedev.server.model.support.issue.StateSpec;

public class ClosedCriteria extends IssueCriteria {

	private static final long serialVersionUID = 1L;

	private IssueCriteria getCriteria(Project project) {
		return OneDev.getInstance(SettingManager.class).getIssueSetting().getCategoryCriteria(StateSpec.Category.CLOSED);
	}
	
	@Override
	public Predicate getPredicate(Project project, Root<Issue> root, CriteriaBuilder builder, User user) {
		return getCriteria(project).getPredicate(project, root, builder, user);
	}

	@Override
	public boolean matches(Issue issue, User user) {
		return getCriteria(issue.getProject()).matches(issue, user);
	}

	@Override
	public boolean needsLogin() {
		return false;
	}

	@Override
	public String toString() {
		return IssueQuery.getRuleName(IssueQueryLexer.Closed);
	}

}
