package io.onedev.server.model.support.administration.jobexecutor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.Size;

import io.onedev.commons.utils.match.Matcher;
import io.onedev.commons.utils.match.PathMatcher;
import io.onedev.server.ci.job.JobService;
import io.onedev.server.util.patternset.PatternSet;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.NameOfEmptyValue;
import io.onedev.server.web.editable.annotation.Patterns;

@Editable
public class ServiceLocator implements Serializable {

	private static final long serialVersionUID = 1L;

	private String serviceNames;
	
	private String serviceImages;
	
	private List<NodeSelectorEntry> nodeSelector = new ArrayList<>();
	
	@Editable(order=100, name="Applicable Names", description="Optionally specify space-separated "
			+ "service names applicable for this locator. Use * or ? for wildcard match. Leave empty to "
			+ "match all")
	@Patterns
	@NameOfEmptyValue("All")
	public String getServiceNames() {
		return serviceNames;
	}

	public void setServiceNames(String serviceNames) {
		this.serviceNames = serviceNames;
	}
	
	@Editable(order=200, name="Applicable Images", description="Optionally specify space-separated "
			+ "service images applicable for this locator. Use * or ? for wildcard match. Leave "
			+ "empty to match all")
	@Patterns
	@NameOfEmptyValue("All")
	public String getServiceImages() {
		return serviceImages;
	}

	public void setServiceImages(String serviceImages) {
		this.serviceImages = serviceImages;
	}

	@Editable(order=300, description="Specify node selector of this locator")
	@Size(min=1, message="At least one entry should be specified")
	public List<NodeSelectorEntry> getNodeSelector() {
		return nodeSelector;
	}

	public void setNodeSelector(List<NodeSelectorEntry> nodeSelector) {
		this.nodeSelector = nodeSelector;
	}
	
	public final boolean isApplicable(JobService service) {
		Matcher matcher = new PathMatcher();
		return (getServiceNames() == null || PatternSet.fromString(getServiceNames()).matches(matcher, service.getName()))
				&& (getServiceImages() == null || PatternSet.fromString(getServiceImages()).matches(matcher, service.getImage()));
	}
	
}