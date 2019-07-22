package io.onedev.server.search.entity.issue;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import io.onedev.server.model.Issue;
import io.onedev.server.model.Project;
import io.onedev.server.model.User;
import io.onedev.server.util.IssueConstants;

public class VoteCountCriteria extends IssueCriteria {

	private static final long serialVersionUID = 1L;

	private final int operator;
	
	private final int value;
	
	public VoteCountCriteria(int value, int operator) {
		this.operator = operator;
		this.value = value;
	}

	@Override
	public Predicate getPredicate(Project project, Root<Issue> root, CriteriaBuilder builder, User user) {
		Path<Integer> attribute = root.get(IssueConstants.ATTR_VOTE_COUNT);
		if (operator == IssueQueryLexer.Is)
			return builder.equal(attribute, value);
		else if (operator == IssueQueryLexer.IsGreaterThan)
			return builder.greaterThan(attribute, value);
		else
			return builder.lessThan(attribute, value);
	}

	@Override
	public boolean matches(Issue issue, User user) {
		if (operator == IssueQueryLexer.Is)
			return issue.getVoteCount() == value;
		else if (operator == IssueQueryLexer.IsGreaterThan)
			return issue.getVoteCount() > value;
		else
			return issue.getVoteCount() < value;
	}

	@Override
	public boolean needsLogin() {
		return false;
	}

	@Override
	public String toString() {
		return IssueQuery.quote(IssueConstants.FIELD_VOTE_COUNT) + " " + IssueQuery.getRuleName(operator) + " " + IssueQuery.quote(String.valueOf(value));
	}

}
