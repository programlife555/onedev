package io.onedev.server.web.editable.job.param;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.validator.constraints.NotEmpty;

import io.onedev.server.model.support.Secret;
import io.onedev.server.web.editable.annotation.ChoiceProvider;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.page.project.blob.ProjectBlobPage;
import io.onedev.server.web.util.WicketUtils;

@Editable
public class SecretEditBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private String secret;

	@Editable
	@ChoiceProvider("getSecretChoices")
	@NotEmpty
	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}
	
	@SuppressWarnings("unused")
	private static List<String> getSecretChoices() {
		List<String> secretNames = new ArrayList<>();
		ProjectBlobPage page = (ProjectBlobPage) WicketUtils.getPage();
		for (Secret secret: page.getProject().getSecrets()) {
			if (!secretNames.contains(secret.getName()) && secret.isAuthorized(page.getProject(), page.getCommit()))
				secretNames.add(secret.getName());
		}
		return secretNames;
	}
	
}
