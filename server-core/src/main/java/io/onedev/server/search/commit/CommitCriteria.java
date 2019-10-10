package io.onedev.server.search.commit;

import java.io.Serializable;

import org.eclipse.jgit.lib.PersonIdent;

import io.onedev.commons.utils.match.WildcardUtils;
import io.onedev.server.event.RefUpdated;
import io.onedev.server.git.command.RevListCommand;
import io.onedev.server.model.Project;
import io.onedev.server.model.User;

public abstract class CommitCriteria implements Serializable {

	private static final long serialVersionUID = 1L;

	protected boolean matches(String value, PersonIdent person) {
		String formatted = String.format("%s <%s>", person.getName(), person.getEmailAddress());
		return WildcardUtils.matchString(value, formatted);
	}
	
	public abstract boolean needsLogin();
	
	public abstract void fill(Project project, RevListCommand command);
	
	public abstract boolean matches(RefUpdated event, User user);
	
}
