package io.onedev.server.entitymanager.impl;

import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.hibernate.criterion.Restrictions;

import io.onedev.server.entitymanager.IssueQuerySettingManager;
import io.onedev.server.model.IssueQuerySetting;
import io.onedev.server.model.Project;
import io.onedev.server.model.User;
import io.onedev.server.persistence.annotation.Sessional;
import io.onedev.server.persistence.annotation.Transactional;
import io.onedev.server.persistence.dao.AbstractEntityManager;
import io.onedev.server.persistence.dao.Dao;
import io.onedev.server.persistence.dao.EntityCriteria;

@Singleton
public class DefaultIssueQuerySettingManager extends AbstractEntityManager<IssueQuerySetting> 
		implements IssueQuerySettingManager {

	@Inject
	public DefaultIssueQuerySettingManager(Dao dao) {
		super(dao);
	}

	@Sessional
	@Override
	public IssueQuerySetting find(Project project, User user) {
		EntityCriteria<IssueQuerySetting> criteria = newCriteria();
		criteria.add(Restrictions.and(Restrictions.eq("project", project), Restrictions.eq("user", user)));
		return find(criteria);
	}

	@Transactional
	@Override
	public void save(IssueQuerySetting setting) {
		setting.getQueryWatchSupport().getUserQueryWatches().keySet().retainAll(
				setting.getUserQueries().stream().map(it->it.getName()).collect(Collectors.toSet()));
		setting.getQueryWatchSupport().getProjectQueryWatches().keySet().retainAll(
				setting.getProject().getIssueSetting().getSavedQueries(true).stream().map(it->it.getName()).collect(Collectors.toSet()));
		if (setting.getQueryWatchSupport().getProjectQueryWatches().isEmpty() && setting.getUserQueries().isEmpty()) {
			if (!setting.isNew())
				delete(setting);
		} else {
			super.save(setting);
		}
	}

}
