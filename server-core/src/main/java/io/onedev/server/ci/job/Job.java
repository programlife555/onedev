package io.onedev.server.ci.job;

import static io.onedev.server.search.entity.EntityQuery.quote;
import static io.onedev.server.search.entity.build.BuildQuery.getRuleName;
import static io.onedev.server.search.entity.build.BuildQueryLexer.And;
import static io.onedev.server.search.entity.build.BuildQueryLexer.Is;
import static io.onedev.server.util.BuildConstants.FIELD_COMMIT;
import static io.onedev.server.util.BuildConstants.FIELD_JOB;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintValidatorContext;
import javax.validation.Valid;
import javax.validation.constraints.Size;

import org.apache.wicket.Component;
import org.eclipse.jgit.lib.ObjectId;
import org.hibernate.validator.constraints.NotEmpty;

import io.onedev.commons.codeassist.InputSuggestion;
import io.onedev.server.ci.job.param.JobParam;
import io.onedev.server.ci.job.paramspec.ParamSpec;
import io.onedev.server.ci.job.trigger.JobTrigger;
import io.onedev.server.event.ProjectEvent;
import io.onedev.server.model.Project;
import io.onedev.server.util.ComponentContext;
import io.onedev.server.util.EditContext;
import io.onedev.server.util.validation.Validatable;
import io.onedev.server.util.validation.annotation.ClassValidating;
import io.onedev.server.util.validation.annotation.GroupName;
import io.onedev.server.web.editable.annotation.Code;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.Horizontal;
import io.onedev.server.web.editable.annotation.Interpolative;
import io.onedev.server.web.editable.annotation.NameOfEmptyValue;
import io.onedev.server.web.editable.annotation.Patterns;
import io.onedev.server.web.editable.annotation.ShowCondition;
import io.onedev.server.web.page.project.blob.ProjectBlobPage;
import io.onedev.server.web.util.SuggestionUtils;
import io.onedev.server.web.util.WicketUtils;

@Editable
@Horizontal
@ClassValidating
public class Job implements Serializable, Validatable {

	private static final long serialVersionUID = 1L;
	
	public static final String SELECTION_PREFIX = "jobs/";
	
	private String name;
	
	private List<ParamSpec> paramSpecs = new ArrayList<>();
	
	private String image;
	
	private List<String> commands;
	
	private boolean retrieveSource = true;
	
	private List<SubmoduleCredential> submoduleCredentials = new ArrayList<>();
	
	private List<JobDependency> jobDependencies = new ArrayList<>();
	
	private List<ProjectDependency> projectDependencies = new ArrayList<>();
	
	private String artifacts;
	
	private List<JobReport> reports = new ArrayList<>();

	private List<JobTrigger> triggers = new ArrayList<>();
	
	private List<JobService> services = new ArrayList<>();
	
	private List<CacheSpec> caches = new ArrayList<>();

	private String cpuRequirement = "500m";
	
	private String memoryRequirement = "128m";
	
	private long timeout = 3600;
	
	private transient Map<String, ParamSpec> paramSpecMap;
	
	@Editable(order=100, description="Specify name of the job")
	@GroupName
	@NotEmpty
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Editable(order=110, description="Specify docker image of the job. "
			+ "<b>Note:</b> Type <tt>@</tt> to <a href='https://github.com/theonedev/onedev/wiki/Variable-Substitution'>insert variable</a>, use <tt>\\</tt> to escape normal occurrences of <tt>@</tt> or <tt>\\</tt>")
	@Interpolative(variableSuggester="suggestVariables")
	@NotEmpty
	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public static List<InputSuggestion> suggestVariables(String matchWith) {
		Component component = ComponentContext.get().getComponent();
		List<InputSuggestion> suggestions = new ArrayList<>();
		ProjectBlobPage page = (ProjectBlobPage) WicketUtils.getPage();
		JobAware jobAware = WicketUtils.findInnermost(component, JobAware.class);
		if (jobAware != null) {
			Job job = jobAware.getJob();
			if (job != null)
				suggestions.addAll(SuggestionUtils.suggestVariables(page.getProject(), page.getCommit(), job, matchWith));
		}
		return suggestions;
	}
	
	@Editable(order=120, description="Specify commands to execute in above image, with one command per line. "
			+ "For Windows based images, commands will be interpretated by cmd.exe, and for Unix/Linux "
			+ "based images, commands will be interpretated by shell. "
			+ "<b>Note:</b> Type <tt>@</tt> to <a href='https://github.com/theonedev/onedev/wiki/Variable-Substitution'>insert variable</a>, use <tt>\\</tt> to escape normal occurrences of <tt>@</tt> or <tt>\\</tt>")
	@Interpolative
	@Code(language = Code.SHELL, variableProvider="getVariables")
	@Size(min=1, message="may not be empty")
	public List<String> getCommands() {
		return commands;
	}

	public void setCommands(List<String> commands) {
		this.commands = commands;
	}
	
	@SuppressWarnings("unused")
	private static List<String> getVariables() {
		List<String> variables = new ArrayList<>();
		ProjectBlobPage page = (ProjectBlobPage) WicketUtils.getPage();
		Project project = page.getProject();
		ObjectId commitId = page.getCommit();
		Job job = ComponentContext.get().getComponent().findParent(JobAware.class).getJob();
		for (InputSuggestion suggestion: SuggestionUtils.suggestVariables(project, commitId, job, ""))  
			variables.add(suggestion.getContent());
		return variables;
	}
	
	@Editable(order=130, name="Parameter Specs", description="Define parameter specifications of the job")
	@Valid
	public List<ParamSpec> getParamSpecs() {
		return paramSpecs;
	}

	public void setParamSpecs(List<ParamSpec> paramSpecs) {
		this.paramSpecs = paramSpecs;
	}

	@Editable(order=500, description="Use triggers to run the job automatically under certain conditions")
	@Valid
	public List<JobTrigger> getTriggers() {
		return triggers;
	}

	public void setTriggers(List<JobTrigger> triggers) {
		this.triggers = triggers;
	}

	@Editable(order=9000, group="Source Retrieval", description="Check this to retrieve files stored in the repository into job workspace")
	public boolean isRetrieveSource() {
		return retrieveSource;
	}

	public void setRetrieveSource(boolean retrieveSource) {
		this.retrieveSource = retrieveSource;
	}

	@Editable(order=9100, group="Source Retrieval", description="For git submodules accessing via http/https, you will "
			+ "need to specify credentials here if required")
	@ShowCondition("isSubmoduleCredentialsVisible")
	@Valid
	public List<SubmoduleCredential> getSubmoduleCredentials() {
		return submoduleCredentials;
	}

	public void setSubmoduleCredentials(List<SubmoduleCredential> submoduleCredentials) {
		this.submoduleCredentials = submoduleCredentials;
	}
	
	@SuppressWarnings("unused")
	private static boolean isSubmoduleCredentialsVisible() {
		return (boolean) EditContext.get().getInputValue("retrieveSource");
	}

	@Editable(name="Job Dependencies", order=9110, group="Dependencies", description="Job dependencies determines the order and "
			+ "concurrency when run different jobs. You may also specify artifacts to retrieve from upstream jobs")
	@Valid
	public List<JobDependency> getJobDependencies() {
		return jobDependencies;
	}

	public void setJobDependencies(List<JobDependency> jobDependencies) {
		this.jobDependencies = jobDependencies;
	}

	@Editable(name="Project Dependencies", order=9113, group="Dependencies", description="Use project dependency to retrieve "
			+ "artifacts from other projects")
	@Valid
	public List<ProjectDependency> getProjectDependencies() {
		return projectDependencies;
	}

	public void setProjectDependencies(List<ProjectDependency> projectDependencies) {
		this.projectDependencies = projectDependencies;
	}

	@Editable(order=9115, group="Artifacts & Reports", description="Optionally specify files to publish as job artifacts. "
			+ "Artifact files are relative to OneDev workspace, and may use * or ? for pattern match. "
			+ "<b>Note:</b> Type <tt>@</tt> to <a href='https://github.com/theonedev/onedev/wiki/Variable-Substitution'>insert variable</a>, use <tt>\\</tt> to escape normal occurrences of <tt>@</tt> or <tt>\\</tt>")
	@Interpolative(variableSuggester="suggestVariables")
	@Patterns(interpolative = true)
	@NameOfEmptyValue("No artifacts")
	public String getArtifacts() {
		return artifacts;
	}

	public void setArtifacts(String artifacts) {
		this.artifacts = artifacts;
	}

	@Editable(order=9120, group="Artifacts & Reports", description="Add job reports here")
	@Valid
	public List<JobReport> getReports() {
		return reports;
	}

	public void setReports(List<JobReport> reports) {
		this.reports = reports;
	}

	@Editable(order=9200, name="CPU Requirement", group="Resource Requirements", description="Specify CPU requirement of the job. "
			+ "Refer to <a href='https://kubernetes.io/docs/concepts/configuration/manage-compute-resources-container/#meaning-of-cpu' target='_blank'>kubernetes documentation</a> for details. "
			+ "<b>Note:</b> Type <tt>@</tt> to <a href='https://github.com/theonedev/onedev/wiki/Variable-Substitution'>insert variable</a>, use <tt>\\</tt> to escape normal occurrences of <tt>@</tt> or <tt>\\</tt>")
	@Interpolative(variableSuggester="suggestVariables")
	@NotEmpty
	public String getCpuRequirement() {
		return cpuRequirement;
	}

	public void setCpuRequirement(String cpuRequirement) {
		this.cpuRequirement = cpuRequirement;
	}

	@Editable(order=9300, group="Resource Requirements", description="Specify memory requirement of the job. "
			+ "Refer to <a href='https://kubernetes.io/docs/concepts/configuration/manage-compute-resources-container/#meaning-of-memory' target='_blank'>kubernetes documentation</a> for details. "
			+ "<b>Note:</b> Type <tt>@</tt> to <a href='https://github.com/theonedev/onedev/wiki/Variable-Substitution'>insert variable</a>, use <tt>\\</tt> to escape normal occurrences of <tt>@</tt> or <tt>\\</tt>")
	@Interpolative(variableSuggester="suggestVariables")
	@NotEmpty
	public String getMemoryRequirement() {
		return memoryRequirement;
	}

	public void setMemoryRequirement(String memoryRequirement) {
		this.memoryRequirement = memoryRequirement;
	}

	@Editable(order=10000, group="More Settings", description="Optionally define services used by this job")
	@Valid
	public List<JobService> getServices() {
		return services;
	}

	public void setServices(List<JobService> services) {
		this.services = services;
	}

	@Editable(order=10100, group="More Settings", description="Cache specific paths to speed up job execution. For instance for node.js "
			+ "projects, you may cache the <tt>node_modules</tt> folder to avoid downloading node modules for "
			+ "subsequent job executions. Note that cache is considered as a best-effort approach and your "
			+ "build script should always consider that cache might not be available")
	@Valid
	public List<CacheSpec> getCaches() {
		return caches;
	}

	public void setCaches(List<CacheSpec> caches) {
		this.caches = caches;
	}

	@Editable(order=10500, group="More Settings", description="Specify timeout in seconds")
	public long getTimeout() {
		return timeout;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	public JobTrigger getMatchedTrigger(ProjectEvent event) {
		for (JobTrigger trigger: getTriggers()) {
			if (trigger.matches(event, this))
				return trigger;
		}
		return null;
	}

	@Override
	public boolean isValid(ConstraintValidatorContext context) {
		Set<String> keys = new HashSet<>();
		Set<String> paths = new HashSet<>();
		
		boolean isValid = true;
		for (CacheSpec cache: caches) {
			if (keys.contains(cache.getKey())) {
				isValid = false;
				context.buildConstraintViolationWithTemplate("Duplicate key: " + cache.getKey())
						.addPropertyNode("caches").addConstraintViolation();
			} else {
				keys.add(cache.getKey());
			}
			if (paths.contains(cache.getPath())) {
				isValid = false;
				context.buildConstraintViolationWithTemplate("Duplicate path: " + cache.getPath())
						.addPropertyNode("caches").addConstraintViolation();
			} else {
				paths.add(cache.getPath());
			}
		}

		Set<String> dependencyJobs = new HashSet<>();
		for (JobDependency dependency: jobDependencies) {
			if (dependencyJobs.contains(dependency.getJobName())) {
				isValid = false;
				context.buildConstraintViolationWithTemplate("Duplicate dependency: " + dependency.getJobName())
						.addPropertyNode("dependencies").addConstraintViolation();
			} else {
				dependencyJobs.add(dependency.getJobName());
			}
		}
		
		Set<String> paramSpecNames = new HashSet<>();
		for (ParamSpec paramSpec: paramSpecs) {
			if (paramSpecNames.contains(paramSpec.getName())) {
				isValid = false;
				context.buildConstraintViolationWithTemplate("Duplicate parameter spec: " + paramSpec.getName())
						.addPropertyNode("paramSpecs").addConstraintViolation();
			} else {
				paramSpecNames.add(paramSpec.getName());
			}
		}
		
		if (retrieveSource) {
			int index = 0;
			for (SubmoduleCredential credential: submoduleCredentials) {
				if (credential.getUrl() != null 
						&& !credential.getUrl().startsWith("http://") 
						&& !credential.getUrl().startsWith("https://")) {
					isValid = false;
					context.buildConstraintViolationWithTemplate("Can only provide credentials for submodules accessing via http/https")
							.addPropertyNode("submoduleCredentials").addPropertyNode("url")
								.inIterable().atIndex(index)
							.addConstraintViolation();
				}
				index++;
			}
		}
		
		if (!isValid)
			context.disableDefaultConstraintViolation();
		
		return isValid;
	}
	
	public Map<String, ParamSpec> getParamSpecMap() {
		if (paramSpecMap == null)
			paramSpecMap = JobParam.getParamSpecMap(paramSpecs);
		return paramSpecMap;
	}
	
	public static String getBuildQuery(ObjectId commitId, String jobName) {
		return "" 
				+ quote(FIELD_COMMIT) + " " + getRuleName(Is) + " " + quote(commitId.name()) 
				+ " " + getRuleName(And) + " "
				+ quote(FIELD_JOB) + " " + getRuleName(Is) + " " + quote(jobName);
	}
	
}
