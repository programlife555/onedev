package io.onedev.server.web.page.project.issues.list;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import io.onedev.server.OneDev;
import io.onedev.server.entitymanager.IssueQuerySettingManager;
import io.onedev.server.entitymanager.ProjectManager;
import io.onedev.server.model.IssueQuerySetting;
import io.onedev.server.model.Project;
import io.onedev.server.model.support.QuerySetting;
import io.onedev.server.model.support.issue.IssueSetting;
import io.onedev.server.model.support.issue.NamedIssueQuery;
import io.onedev.server.search.entity.issue.IssueQuery;
import io.onedev.server.security.SecurityUtils;
import io.onedev.server.web.component.issue.list.IssueListPanel;
import io.onedev.server.web.component.modal.ModalPanel;
import io.onedev.server.web.page.project.issues.ProjectIssuesPage;
import io.onedev.server.web.page.project.savedquery.NamedQueriesBean;
import io.onedev.server.web.page.project.savedquery.SaveQueryPanel;
import io.onedev.server.web.page.project.savedquery.SavedQueriesPanel;
import io.onedev.server.web.util.PagingHistorySupport;
import io.onedev.server.web.util.QuerySaveSupport;

@SuppressWarnings("serial")
public class IssueListPage extends ProjectIssuesPage {

	private static final String PARAM_CURRENT_PAGE = "currentPage";
	
	private static final String PARAM_QUERY = "query";
	
	private String query;
	
	public IssueListPage(PageParameters params) {
		super(params);
		query = params.get(PARAM_QUERY).toOptionalString();
		if (query != null && query.length() == 0) {
			query = null;
			List<String> queries = new ArrayList<>();
			if (getProject().getIssueQuerySettingOfCurrentUser() != null) { 
				for (NamedIssueQuery namedQuery: getProject().getIssueQuerySettingOfCurrentUser().getUserQueries())
					queries.add(namedQuery.getQuery());
			}
			for (NamedIssueQuery namedQuery: getProject().getIssueSetting().getSavedQueries(true))
				queries.add(namedQuery.getQuery());
			for (String each: queries) {
				try {
					if (SecurityUtils.getUser() != null || !IssueQuery.parse(getProject(), each, true).needsLogin()) {  
						query = each;
						break;
					}
				} catch (Exception e) {
				}
			} 
		}
	}

	private IssueQuerySettingManager getIssueQuerySettingManager() {
		return OneDev.getInstance(IssueQuerySettingManager.class);		
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();

		SavedQueriesPanel<NamedIssueQuery> savedQueries;
		add(savedQueries = new SavedQueriesPanel<NamedIssueQuery>("side") {

			@Override
			protected NamedQueriesBean<NamedIssueQuery> newNamedQueriesBean() {
				return new NamedIssueQueriesBean();
			}

			@Override
			protected boolean needsLogin(NamedIssueQuery namedQuery) {
				return IssueQuery.parse(getProject(), namedQuery.getQuery(), true).needsLogin();
			}

			@Override
			protected Link<Void> newQueryLink(String componentId, NamedIssueQuery namedQuery) {
				return new BookmarkablePageLink<Void>(componentId, IssueListPage.class, 
						IssueListPage.paramsOf(getProject(), namedQuery.getQuery(), 0));
			}

			@Override
			protected QuerySetting<NamedIssueQuery> getQuerySetting() {
				return getProject().getIssueQuerySettingOfCurrentUser();
			}

			@Override
			protected ArrayList<NamedIssueQuery> getProjectQueries() {
				return (ArrayList<NamedIssueQuery>) getProject().getIssueSetting().getSavedQueries(false);
			}

			@Override
			protected void onSaveQuerySetting(QuerySetting<NamedIssueQuery> querySetting) {
				getIssueQuerySettingManager().save((IssueQuerySetting) querySetting);
			}

			@Override
			protected void onSaveProjectQueries(ArrayList<NamedIssueQuery> projectQueries) {
				getProject().getIssueSetting().setSavedQueries(projectQueries);
				OneDev.getInstance(ProjectManager.class).save(getProject());
			}

			@Override
			protected ArrayList<NamedIssueQuery> getDefaultProjectQueries() {
				return (ArrayList<NamedIssueQuery>) getGlobalIssueSetting().getDefaultQueries();
			}
			
		});
		
		PagingHistorySupport pagingHistorySupport = new PagingHistorySupport() {

			@Override
			public PageParameters newPageParameters(int currentPage) {
				PageParameters params = paramsOf(getProject(), query, 0);
				params.add(PARAM_CURRENT_PAGE, currentPage+1);
				return params;
			}
			
			@Override
			public int getCurrentPage() {
				return getPageParameters().get(PARAM_CURRENT_PAGE).toInt(1)-1;
			}
			
		};
		
		add(new IssueListPanel("main", getProject(), query) {

			@Override
			protected PagingHistorySupport getPagingHistorySupport() {
				return pagingHistorySupport;
			}

			@Override
			protected void onQueryUpdated(AjaxRequestTarget target, String query) {
				setResponsePage(IssueListPage.class, paramsOf(getProject(), query, 0));
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
										IssueQuerySetting setting = getProject().getIssueQuerySettingOfCurrentUser();
										NamedIssueQuery namedQuery = setting.getUserQuery(name);
										if (namedQuery == null) {
											namedQuery = new NamedIssueQuery(name, query);
											setting.getUserQueries().add(namedQuery);
										} else {
											namedQuery.setQuery(query);
										}
										getIssueQuerySettingManager().save(setting);
										target.add(savedQueries);
										close();
									}

									@Override
									protected void onSaveForAll(AjaxRequestTarget target, String name) {
										IssueSetting setting = getProject().getIssueSetting();
										if (setting.getSavedQueries(false) == null) {
											setting.setSavedQueries(new ArrayList<>(getGlobalIssueSetting().getDefaultQueries()));
										}
										NamedIssueQuery namedQuery = getProject().getIssueSetting().getSavedQuery(name);
										if (namedQuery == null) {
											namedQuery = new NamedIssueQuery(name, query);
											getProject().getIssueSetting().getSavedQueries(true).add(namedQuery);
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

		});
		
	}
	
	public static PageParameters paramsOf(Project project, @Nullable String query, int page) {
		PageParameters params = paramsOf(project);
		if (query != null)
			params.add(PARAM_QUERY, query);
		if (page != 0)
			params.add(PARAM_CURRENT_PAGE, page);
		return params;
	}
	
}
