package io.onedev.server.entitymanager;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.eclipse.jgit.lib.ObjectId;

import io.onedev.server.model.Build;
import io.onedev.server.model.Build.Status;
import io.onedev.server.model.Project;
import io.onedev.server.model.User;
import io.onedev.server.persistence.dao.EntityManager;
import io.onedev.server.search.entity.EntityCriteria;
import io.onedev.server.search.entity.EntityQuery;

public interface BuildManager extends EntityManager<Build> {
	
    @Nullable
    Build find(Project project, long number);
    
	Collection<Build> query(Project project, ObjectId commitId, @Nullable String jobName, Map<String, List<String>> params); 
	
	Collection<Build> query(Project project, ObjectId commitId, @Nullable String jobName); 
	
	Collection<Build> query(Project project, ObjectId commitId); 
	
	Map<ObjectId, Map<String, Status>> queryStatus(Project project, Collection<ObjectId> commitIds);
	
	void create(Build build);
	
	Collection<Build> queryUnfinished();

	List<Build> query(Project project, String term, int count);
	
	List<Build> queryAfter(Project project, Long afterBuildId, int count);

	List<Build> query(Project project, User user, EntityQuery<Build> buildQuery, int firstResult, int maxResults);
	
	int count(Project project, User user, EntityCriteria<Build> buildCriteria);

}
