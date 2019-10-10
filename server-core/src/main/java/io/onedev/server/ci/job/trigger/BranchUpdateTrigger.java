package io.onedev.server.ci.job.trigger;

import java.util.Collection;
import java.util.List;

import org.eclipse.jgit.lib.ObjectId;

import io.onedev.commons.codeassist.InputSuggestion;
import io.onedev.commons.utils.match.Matcher;
import io.onedev.commons.utils.match.PathMatcher;
import io.onedev.server.ci.job.Job;
import io.onedev.server.event.ProjectEvent;
import io.onedev.server.event.RefUpdated;
import io.onedev.server.git.GitUtils;
import io.onedev.server.model.Project;
import io.onedev.server.util.patternset.PatternSet;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.NameOfEmptyValue;
import io.onedev.server.web.editable.annotation.Patterns;
import io.onedev.server.web.util.SuggestionUtils;

@Editable(order=100, name="When update branches")
public class BranchUpdateTrigger extends JobTrigger {

	private static final long serialVersionUID = 1L;

	private String branches;
	
	private String paths;
	
	@Editable(name="Branches", order=100, 
			description="Optionally specify space-separated branches to check. Use * or ? for wildcard match. "
					+ "Leave empty to match all branches")
	@Patterns(suggester = "suggestBranches")
	@NameOfEmptyValue("Any branch")
	public String getBranches() {
		return branches;
	}

	public void setBranches(String branches) {
		this.branches = branches;
	}

	@SuppressWarnings("unused")
	private static List<InputSuggestion> suggestBranches(String matchWith) {
		return SuggestionUtils.suggestBranches(Project.get(), matchWith);
	}
	
	@Editable(name="Touched Files", order=200, 
			description="Optionally specify space-separated files to check. Use * or ? for wildcard match. "
					+ "Leave empty to match all files")
	@Patterns(suggester = "getPathSuggestions")
	@NameOfEmptyValue("Any file")
	public String getPaths() {
		return paths;
	}

	public void setPaths(String paths) {
		this.paths = paths;
	}

	@SuppressWarnings("unused")
	private static List<InputSuggestion> getPathSuggestions(String matchWith) {
		return SuggestionUtils.suggestBlobs(Project.get(), matchWith);
	}

	private boolean touchedFile(RefUpdated refUpdated) {
		if (getPaths() != null) {
			if (refUpdated.getOldCommitId().equals(ObjectId.zeroId())) {
				return true;
			} else if (refUpdated.getNewCommitId().equals(ObjectId.zeroId())) {
				return false;
			} else {
				Collection<String> changedFiles = GitUtils.getChangedFiles(refUpdated.getProject().getRepository(), 
						refUpdated.getOldCommitId(), refUpdated.getNewCommitId());
				PatternSet patternSet = PatternSet.fromString(getPaths());
				Matcher matcher = new PathMatcher();
				for (String changedFile: changedFiles) {
					if (patternSet.matches(matcher, changedFile))
						return true;
				}
				return false;
			}
		} else {
			return true;
		}
	}
	
	@Override
	public boolean matches(ProjectEvent event, Job job) {
		if (event instanceof RefUpdated) {
			RefUpdated refUpdated = (RefUpdated) event;
			String branch = GitUtils.ref2branch(refUpdated.getRefName());
			if (branch != null) {
				if ((getBranches() == null || PatternSet.fromString(getBranches()).matches(new PathMatcher(), branch)) 
						&& touchedFile(refUpdated)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public String getDescription() {
		String description;
		if (getBranches() != null && getPaths() != null)
			description = String.format("When update branches '%s' and touch files '%s'", getBranches(), getPaths());
		else if (getBranches() != null)
			description = String.format("When update branches '%s'", getBranches());
		else if (getPaths() != null)
			description = String.format("When touch files '%s'", getBranches());
		else
			description = "When update branches";
		return description;
	}

}
