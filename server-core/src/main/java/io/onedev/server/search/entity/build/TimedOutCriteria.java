package io.onedev.server.search.entity.build;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import io.onedev.server.model.Build;
import io.onedev.server.model.Project;
import io.onedev.server.model.User;
import io.onedev.server.search.entity.EntityCriteria;
import io.onedev.server.util.BuildConstants;

public class TimedOutCriteria extends EntityCriteria<Build> {

	private static final long serialVersionUID = 1L;

	@Override
	public Predicate getPredicate(Project project, Root<Build> root, CriteriaBuilder builder, User user) {
		Path<?> attribute = root.get(BuildConstants.ATTR_STATUS);
		return builder.equal(attribute, Build.Status.TIMED_OUT);
	}

	@Override
	public boolean matches(Build build, User user) {
		return build.getStatus() == Build.Status.TIMED_OUT;
	}

	@Override
	public boolean needsLogin() {
		return false;
	}

	@Override
	public String toString() {
		return BuildQuery.getRuleName(BuildQueryLexer.TimedOut);
	}

}
