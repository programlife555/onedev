package io.onedev.server.entitymanager;

import java.util.List;

import javax.annotation.Nullable;

import io.onedev.server.model.Project;
import io.onedev.server.model.PullRequestUpdate;
import io.onedev.server.persistence.dao.EntityManager;

public interface PullRequestUpdateManager extends EntityManager<PullRequestUpdate> {
	
	/**
	 * @param update
	 * 			update to be saved
	 * @param independent
	 * 			whether or not this update is an independent update. An independent update is 
	 * 			not created as result of other actions such as open and merge
	 */
	void save(PullRequestUpdate update, boolean independent);
	
	List<PullRequestUpdate> queryAfter(Project project, @Nullable Long afterUpdateId, int count);
}
