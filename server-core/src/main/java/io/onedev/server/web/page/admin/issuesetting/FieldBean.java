package io.onedev.server.web.page.admin.issuesetting;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import io.onedev.server.util.inputspec.InputSpec;
import io.onedev.server.web.editable.annotation.Editable;

@Editable
public class FieldBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private InputSpec field;

	private boolean promptUponIssueOpen = true;
	
	private boolean displayInIssueList = true;

	@Editable(name="Type", order=100)
	@NotNull(message="may not be empty")
	public InputSpec getField() {
		return field;
	}

	public void setField(InputSpec field) {
		this.field = field;
	}

	@Editable(order=200, description="If checked, this field will be prompted for user input by default when issue is opened")
	public boolean isPromptUponIssueOpen() {
		return promptUponIssueOpen;
	}

	public void setPromptUponIssueOpen(boolean promptUponIssueOpen) {
		this.promptUponIssueOpen = promptUponIssueOpen;
	}

	@Editable(order=300, description="If checked, this field will be displayed in issue list by default")
	public boolean isDisplayInIssueList() {
		return displayInIssueList;
	}

	public void setDisplayInIssueList(boolean displayInIssueList) {
		this.displayInIssueList = displayInIssueList;
	}

}
