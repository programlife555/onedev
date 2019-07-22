package io.onedev.server.search.entity.build;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.From;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import io.onedev.server.model.Build;
import io.onedev.server.model.BuildDependence;
import io.onedev.server.model.Project;
import io.onedev.server.model.User;
import io.onedev.server.search.entity.EntityCriteria;
import io.onedev.server.util.BuildConstants;

public class DependsOnCriteria extends EntityCriteria<Build> {

	private static final long serialVersionUID = 1L;

	private Build value;
	
	public DependsOnCriteria(Build value) {
		this.value = value;
	}

	@Override
	public Predicate getPredicate(Project project, Root<Build> root, CriteriaBuilder builder, User user) {
		From<?, ?> join = root.join(BuildConstants.ATTR_DEPENDENCIES, JoinType.LEFT);
		return builder.equal(join.get(BuildDependence.ATTR_DEPENDENCY), value);
	}

	@Override
	public boolean matches(Build build, User user) {
		for (BuildDependence dependence: value.getDependents()) {
			if (dependence.getDependent().equals(build))
				return true;
		}
		return false;
	}

	@Override
	public boolean needsLogin() {
		return false;
	}

	@Override
	public String toString() {
		return BuildQuery.getRuleName(BuildQueryLexer.DependsOn) + " " + BuildQuery.quote("#" + value.getNumber());
	}

}
