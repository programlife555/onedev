package io.onedev.server.util.markdown;

import org.apache.wicket.request.cycle.RequestCycle;
import org.jsoup.nodes.Document;

import io.onedev.server.model.Project;
import io.onedev.server.model.PullRequest;
import io.onedev.server.web.page.project.pullrequests.detail.activities.PullRequestActivitiesPage;

public class PullRequestProcessor extends PullRequestParser implements MarkdownProcessor {
	
	@Override
	public void process(Project project, Document document, Object context) {
		parseReferences(project, document);
	}

	@Override
	protected String toHtml(PullRequest request, String text) {
		CharSequence url = RequestCycle.get().urlFor(
				PullRequestActivitiesPage.class, PullRequestActivitiesPage.paramsOf(request, null)); 
		return String.format("<a href='%s' class='pull-request reference' data-reference=%d>%s</a>", url, request.getId(), text);
	}
	
}
