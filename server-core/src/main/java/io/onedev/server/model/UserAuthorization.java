package io.onedev.server.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import io.onedev.server.security.permission.ProjectPrivilege;
import io.onedev.server.util.facade.UserAuthorizationFacade;

@Entity
@Table(
		indexes={@Index(columnList="o_user_id"), @Index(columnList="o_project_id")},
		uniqueConstraints={@UniqueConstraint(columnNames={"o_user_id", "o_project_id"})
})
public class UserAuthorization extends AbstractEntity {

	private static final long serialVersionUID = 1L;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(nullable=false)
	private Project project;
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(nullable=false)
	private User user;
	
	@Column(nullable=false)
	private ProjectPrivilege privilege = ProjectPrivilege.CODE_READ;
	
	public Project getProject() {
		return project;
	}

	public void setProject(Project project) {
		this.project = project;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public ProjectPrivilege getPrivilege() {
		return privilege;
	}

	public void setPrivilege(ProjectPrivilege privilege) {
		this.privilege = privilege;
	}

	public UserAuthorizationFacade getFacade() {
		return new UserAuthorizationFacade(this);
	}
	
}
