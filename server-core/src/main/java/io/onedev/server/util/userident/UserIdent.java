package io.onedev.server.util.userident;

import java.io.Serializable;

import javax.annotation.Nullable;

import org.eclipse.jgit.lib.PersonIdent;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import io.onedev.server.util.facade.UserFacade;

@JsonTypeInfo(property="@class", use = Id.CLASS)
public abstract class UserIdent implements Serializable {

	private static final long serialVersionUID = 1L;

	public abstract String getName();
	
	public static UserIdent of(@Nullable UserFacade user) {
		if (user != null)
			return new OrdinaryUserIdent(user.getDisplayName(), user.getEmail());
		else
			return new SystemUserIdent();
	}
	
	public static UserIdent of(@Nullable UserFacade user, @Nullable String userName) {
		if (user != null) {
			return new OrdinaryUserIdent(user.getDisplayName(), user.getEmail());
		} else if (userName != null) {
			return new RemovedUserIdent(userName);
		} else {
			return new SystemUserIdent();
		}
	}

	public static UserIdent of(PersonIdent person) {
		return new GitUserIdent(person.getName(), person.getEmailAddress());
	}
	
	public static UserIdent of(PersonIdent person, String gitRole) {
		return new GitUserIdent(person.getName(), person.getEmailAddress(), gitRole);
	}
	
}
