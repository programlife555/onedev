package io.onedev.server.web.page.project.commits;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import io.onedev.server.OneDev;
import io.onedev.server.entitymanager.CommitQuerySettingManager;
import io.onedev.server.entitymanager.ProjectManager;
import io.onedev.server.model.CommitQuerySetting;
import io.onedev.server.model.Project;
import io.onedev.server.model.support.NamedCommitQuery;
import io.onedev.server.model.support.QuerySetting;
import io.onedev.server.search.commit.CommitQuery;
import io.onedev.server.security.SecurityUtils;
import io.onedev.server.web.component.commit.list.CommitListPanel;
import io.onedev.server.web.component.modal.ModalPanel;
import io.onedev.server.web.page.project.ProjectPage;
import io.onedev.server.web.page.project.savedquery.NamedQueriesBean;
import io.onedev.server.web.page.project.savedquery.SaveQueryPanel;
import io.onedev.server.web.page.project.savedquery.SavedQueriesPanel;
import io.onedev.server.web.util.QuerySaveSupport;

@SuppressWarnings("serial")
public class ProjectCommitsPage extends ProjectPage {

	private static final String PARAM_COMPARE_WITH = "compareWith";
	
	private static final String PARAM_COMMIT_QUERY = "commitQuery";
	
	private String query;
	
	private String compareWith;
	
	public ProjectCommitsPage(PageParameters params) {
		super(params);
		
		compareWith = params.get(PARAM_COMPARE_WITH).toString();

		query = params.get(PARAM_COMMIT_QUERY).toString();
		if (query != null && query.length() == 0) {
			query = null;
			List<String> queries = new ArrayList<>();
			if (getProject().getCommitQuerySettingOfCurrentUser() != null) { 
				for (NamedCommitQuery namedQuery: getProject().getCommitQuerySettingOfCurrentUser().getUserQueries())
					queries.add(namedQuery.getQuery());
			}
			for (NamedCommitQuery namedQuery: getProject().getSavedCommitQueries())
				queries.add(namedQuery.getQuery());
			for (String each: queries) {
				try {
					if (SecurityUtils.getUser() != null || !CommitQuery.parse(getProject(), each).needsLogin()) {  
						query = each;
						break;
					}
				} catch (Exception e) {
				}
			} 
		}
	}

	private CommitQuerySettingManager getCommitQuerySettingManager() {
		return OneDev.getInstance(CommitQuerySettingManager.class);
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();

		SavedQueriesPanel<NamedCommitQuery> savedQueries;
		add(savedQueries = new SavedQueriesPanel<NamedCommitQuery>("side") {

			@Override
			protected NamedQueriesBean<NamedCommitQuery> newNamedQueriesBean() {
				return new NamedCommitQueriesBean();
			}

			@Override
			protected boolean needsLogin(NamedCommitQuery namedQuery) {
				return CommitQuery.parse(getProject(), namedQuery.getQuery()).needsLogin();
			}

			@Override
			protected Link<Void> newQueryLink(String componentId, NamedCommitQuery namedQuery) {
				query = namedQuery.getQuery();
				return new BookmarkablePageLink<Void>(componentId, ProjectCommitsPage.class, 
						ProjectCommitsPage.paramsOf(getProject(), query, compareWith));
			}

			@Override
			protected QuerySetting<NamedCommitQuery> getQuerySetting() {
				return getProject().getCommitQuerySettingOfCurrentUser();
			}

			@Override
			protected ArrayList<NamedCommitQuery> getProjectQueries() {
				return getProject().getSavedCommitQueries();
			}

			@Override
			protected void onSaveQuerySetting(QuerySetting<NamedCommitQuery> querySetting) {
				getCommitQuerySettingManager().save((CommitQuerySetting) querySetting);
			}

			@Override
			protected void onSaveProjectQueries(ArrayList<NamedCommitQuery> projectQueries) {
				getProject().setSavedCommitQueries(projectQueries);
				OneDev.getInstance(ProjectManager.class).save(getProject());
			}

		});
		
		add(new CommitListPanel("main", getProject(), query) {

			@Override
			protected void onQueryUpdated(AjaxRequestTarget target, String query) {
				setResponsePage(ProjectCommitsPage.class, paramsOf(getProject(), query, compareWith));
			}

			@Override
			protected QuerySaveSupport getQuerySaveSupport() {
				return new QuerySaveSupport() {

					@Override
					public void onSaveQuery(AjaxRequestTarget target, String query) {
						new ModalPanel(target)  {

							@Override
							protected Component newContent(String id) {
								return new SaveQueryPanel(id) {

									@Override
									protected void onSaveForMine(AjaxRequestTarget target, String name) {
										CommitQuerySetting setting = getProject().getCommitQuerySettingOfCurrentUser();
										NamedCommitQuery namedQuery = setting.getUserQuery(name);
										if (namedQuery == null) {
											namedQuery = new NamedCommitQuery(name, query);
											setting.getUserQueries().add(namedQuery);
										} else {
											namedQuery.setQuery(query);
										}
										getCommitQuerySettingManager().save(setting);
										target.add(savedQueries);
										close();
									}

									@Override
									protected void onSaveForAll(AjaxRequestTarget target, String name) {
										NamedCommitQuery namedQuery = getProject().getSavedCommitQuery(name);
										if (namedQuery == null) {
											namedQuery = new NamedCommitQuery(name, query);
											getProject().getSavedCommitQueries().add(namedQuery);
										} else {
											namedQuery.setQuery(query);
										}
										OneDev.getInstance(ProjectManager.class).save(getProject());
										target.add(savedQueries);
										close();
									}

									@Override
									protected void onCancel(AjaxRequestTarget target) {
										close();
									}
									
								};
							}
							
						};
					}

					@Override
					public boolean isSavedQueriesVisible() {
						savedQueries.configure();
						return savedQueries.isVisible();
					}

				};
			}

			@Override
			protected String getCompareWith() {
				return compareWith;
			}

		});
	}
	
	public static PageParameters paramsOf(Project project, @Nullable String query, @Nullable String compareWith) {
		PageParameters params = paramsOf(project);
		if (compareWith != null)
			params.set(PARAM_COMPARE_WITH, compareWith);
		if (query != null)
			params.set(PARAM_COMMIT_QUERY, query);
		return params;
	}
	
	@Override
	protected String getRobotsMeta() {
		return "noindex,nofollow";
	}

	@Override
	protected boolean isPermitted() {
		return SecurityUtils.canReadCode(getProject().getFacade());
	}
	
}