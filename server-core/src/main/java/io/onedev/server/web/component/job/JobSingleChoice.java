package io.onedev.server.web.component.job;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import io.onedev.server.web.component.select2.Select2Choice;
import io.onedev.server.web.component.stringchoice.StringChoiceProvider;
import io.onedev.server.web.page.project.ProjectPage;
import io.onedev.server.web.util.WicketUtils;

public class JobSingleChoice extends Select2Choice<String> {

	private static final long serialVersionUID = 1L;

	public JobSingleChoice(String id, IModel<String> model) {
		super(id, model, new StringChoiceProvider(new LoadableDetachableModel<Map<String, String>>() {

			private static final long serialVersionUID = 1L;

			@Override
			protected Map<String, String> load() {
				Map<String, String> choices = new LinkedHashMap<>();
				if (WicketUtils.getPage() instanceof ProjectPage) {
					ProjectPage projectPage = (ProjectPage) WicketUtils.getPage();
					List<String> jobNames = projectPage.getProject().getJobNames();
					for (String jobName: jobNames)
						choices.put(jobName, jobName);
				} 
				return choices;
			}
			
		}));
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		if (isRequired())
			getSettings().setPlaceholder("Choose job...");
		getSettings().setFormatResult("onedev.server.choiceFormatter.formatResult");
		getSettings().setFormatSelection("onedev.server.choiceFormatter.formatSelection");
		getSettings().setEscapeMarkup("onedev.server.choiceFormatter.escapeMarkup");
	}

}
