package io.onedev.server.web.behavior;

import static io.onedev.server.util.IssueConstants.FIELD_COMMENT;
import static io.onedev.server.util.IssueConstants.FIELD_COMMENT_COUNT;
import static io.onedev.server.util.IssueConstants.FIELD_DESCRIPTION;
import static io.onedev.server.util.IssueConstants.FIELD_MILESTONE;
import static io.onedev.server.util.IssueConstants.FIELD_NUMBER;
import static io.onedev.server.util.IssueConstants.FIELD_STATE;
import static io.onedev.server.util.IssueConstants.FIELD_SUBMIT_DATE;
import static io.onedev.server.util.IssueConstants.FIELD_TITLE;
import static io.onedev.server.util.IssueConstants.FIELD_UPDATE_DATE;
import static io.onedev.server.util.IssueConstants.FIELD_VOTE_COUNT;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import io.onedev.commons.codeassist.FenceAware;
import io.onedev.commons.codeassist.InputSuggestion;
import io.onedev.commons.codeassist.grammar.LexerRuleRefElementSpec;
import io.onedev.commons.codeassist.parser.Element;
import io.onedev.commons.codeassist.parser.ParseExpect;
import io.onedev.commons.codeassist.parser.TerminalExpect;
import io.onedev.server.OneDev;
import io.onedev.server.OneException;
import io.onedev.server.entitymanager.GroupManager;
import io.onedev.server.entitymanager.SettingManager;
import io.onedev.server.model.Project;
import io.onedev.server.model.support.administration.GlobalIssueSetting;
import io.onedev.server.model.support.issue.fieldspec.BooleanField;
import io.onedev.server.model.support.issue.fieldspec.BuildChoiceField;
import io.onedev.server.model.support.issue.fieldspec.ChoiceField;
import io.onedev.server.model.support.issue.fieldspec.DateField;
import io.onedev.server.model.support.issue.fieldspec.FieldSpec;
import io.onedev.server.model.support.issue.fieldspec.GroupChoiceField;
import io.onedev.server.model.support.issue.fieldspec.IssueChoiceField;
import io.onedev.server.model.support.issue.fieldspec.NumberField;
import io.onedev.server.model.support.issue.fieldspec.PullRequestChoiceField;
import io.onedev.server.model.support.issue.fieldspec.TextField;
import io.onedev.server.model.support.issue.fieldspec.UserChoiceField;
import io.onedev.server.search.entity.issue.IssueQuery;
import io.onedev.server.search.entity.issue.IssueQueryLexer;
import io.onedev.server.search.entity.issue.IssueQueryParser;
import io.onedev.server.util.ComponentContext;
import io.onedev.server.util.DateUtils;
import io.onedev.server.util.IssueConstants;
import io.onedev.server.util.SecurityUtils;
import io.onedev.server.web.behavior.inputassist.ANTLRAssistBehavior;
import io.onedev.server.web.util.SuggestionUtils;

@SuppressWarnings("serial")
public class IssueQueryBehavior extends ANTLRAssistBehavior {

	private final IModel<Project> projectModel;
	
	public IssueQueryBehavior(IModel<Project> projectModel) {
		super(IssueQueryParser.class, "query", false);
		this.projectModel = projectModel;
	}

	@Override
	public void detach(Component component) {
		super.detach(component);
		projectModel.detach();
	}
	
	private Project getProject() {
		return projectModel.getObject();
	}

	@Override
	protected List<InputSuggestion> suggest(TerminalExpect terminalExpect) {
		GlobalIssueSetting issueSetting = OneDev.getInstance(SettingManager.class).getIssueSetting();
		
		if (terminalExpect.getElementSpec() instanceof LexerRuleRefElementSpec) {
			LexerRuleRefElementSpec spec = (LexerRuleRefElementSpec) terminalExpect.getElementSpec();
			if (spec.getRuleName().equals("Quoted")) {
				return new FenceAware(codeAssist.getGrammar(), '"', '"') {

					@Override
					protected List<InputSuggestion> match(String matchWith) {
						Project project = getProject();
						if ("criteriaField".equals(spec.getLabel())) {
							List<String> candidates = new ArrayList<>(IssueConstants.QUERY_FIELDS);
							for (FieldSpec field: issueSetting.getFieldSpecs())
								candidates.add(field.getName());
							return SuggestionUtils.suggest(candidates, matchWith);
						} else if ("orderField".equals(spec.getLabel())) {
							List<String> candidates = new ArrayList<>(IssueConstants.ORDER_FIELDS.keySet());
							for (FieldSpec field: issueSetting.getFieldSpecs()) {
								if (field instanceof NumberField || field instanceof ChoiceField || field instanceof DateField) 
									candidates.add(field.getName());
							}
							return SuggestionUtils.suggest(candidates, matchWith);
						} else if ("revisionValue".equals(spec.getLabel())) {
							String revisionType = terminalExpect.getState()
									.findMatchedElementsByLabel("revisionType", true).iterator().next().getMatchedText();
							if (project != null) {
								switch (revisionType) {
								case "branch":
									return SuggestionUtils.suggestBranches(project, matchWith);
								case "tag":
									return SuggestionUtils.suggestTags(project, matchWith);
								case "build":
									return SuggestionUtils.suggestBuilds(project, matchWith);
								}
							}
						} else if ("criteriaValue".equals(spec.getLabel())) {
							List<Element> fieldElements = terminalExpect.getState().findMatchedElementsByLabel("criteriaField", true);
							List<Element> operatorElements = terminalExpect.getState().findMatchedElementsByLabel("operator", true);
							Preconditions.checkState(operatorElements.size() == 1);
							String operatorName = StringUtils.normalizeSpace(operatorElements.get(0).getMatchedText());
							int operator = IssueQuery.getOperator(operatorName);							
							if (fieldElements.isEmpty()) {
								if (operator == IssueQueryLexer.SubmittedBy)
									return SuggestionUtils.suggestUsers(matchWith);
								else if (project != null)
									return SuggestionUtils.suggestBuilds(project, matchWith);
							} else {
								String fieldName = IssueQuery.getValue(fieldElements.get(0).getMatchedText());
								
								try {
									IssueQuery.checkField(fieldName, operator);
									FieldSpec fieldSpec = issueSetting.getFieldSpec(fieldName);
									if (fieldSpec instanceof DateField || fieldName.equals(FIELD_SUBMIT_DATE) 
											|| fieldName.equals(FIELD_UPDATE_DATE)) {
										List<InputSuggestion> suggestions = SuggestionUtils.suggest(DateUtils.RELAX_DATE_EXAMPLES, matchWith);
										return !suggestions.isEmpty()? suggestions: null;
									} else if (fieldSpec instanceof UserChoiceField) {
										return SuggestionUtils.suggestUsers(matchWith);
									} else if (fieldSpec instanceof IssueChoiceField) {
										if (project != null)
											return SuggestionUtils.suggestIssues(project, matchWith);
									} else if (fieldSpec instanceof BuildChoiceField) {
										if (project != null)
											return SuggestionUtils.suggestBuilds(project, matchWith);
									} else if (fieldSpec instanceof PullRequestChoiceField) {
										if (project != null)
											return SuggestionUtils.suggestPullRequests(project, matchWith);
									} else if (fieldSpec instanceof BooleanField) {
										return SuggestionUtils.suggest(Lists.newArrayList("true", "false"), matchWith);
									} else if (fieldSpec instanceof GroupChoiceField) {
										List<String> candidates = OneDev.getInstance(GroupManager.class).query().stream().map(it->it.getName()).collect(Collectors.toList());
										return SuggestionUtils.suggest(candidates, matchWith);
									} else if (fieldName.equals(FIELD_STATE)) {
										List<String> candidates = issueSetting.getStateSpecs().stream().map(it->it.getName()).collect(Collectors.toList());
										return SuggestionUtils.suggest(candidates, matchWith);
									} else if (fieldSpec instanceof ChoiceField) {
										ComponentContext.push(new ComponentContext(getComponent()));
										try {
											List<String> candidates = new ArrayList<>(((ChoiceField)fieldSpec).getChoiceProvider().getChoices(true).keySet());
											return SuggestionUtils.suggest(candidates, matchWith);
										} finally {
											ComponentContext.pop();
										}			
									} else if (fieldName.equals(FIELD_MILESTONE)) {
										if (project != null) {
											List<String> candidates = project.getMilestones().stream().map(it->it.getName()).collect(Collectors.toList());
											return SuggestionUtils.suggest(candidates, matchWith);
										}
									} else if (fieldName.equals(FIELD_TITLE) || fieldName.equals(FIELD_DESCRIPTION) 
											|| fieldName.equals(FIELD_COMMENT) || fieldName.equals(FIELD_VOTE_COUNT) 
											|| fieldName.equals(FIELD_COMMENT_COUNT) || fieldName.equals(FIELD_NUMBER) 
											|| fieldSpec instanceof NumberField || fieldSpec instanceof TextField) {
										return null;
									}
								} catch (OneException ex) {
								}
							}
						}
						return new ArrayList<>();
					}
					
					@Override
					protected String getFencingDescription() {
						return "quote as literal value";
					}
					
				}.suggest(terminalExpect);
			}
		} 
		return null;
	}
	
	@Override
	protected Optional<String> describe(ParseExpect parseExpect, String suggestedLiteral) {
		if (suggestedLiteral.equals("mine")) {
			if (SecurityUtils.getUser() != null)
				return Optional.of("issues relevant to me");
			else
				return null;
		} 

		if ((suggestedLiteral.equals("is me") || suggestedLiteral.equals("is not me")) && SecurityUtils.getUser() == null)
			return null;	
		
		if (suggestedLiteral.equals("open"))
			return Optional.of("issues with state in open category");
		else if (suggestedLiteral.equals("closed"))
			return Optional.of("issues with state in closed category");
		
		parseExpect = parseExpect.findExpectByLabel("operator");
		if (parseExpect != null) {
			List<Element> fieldElements = parseExpect.getState().findMatchedElementsByLabel("criteriaField", false);
			if (!fieldElements.isEmpty()) {
				String fieldName = IssueQuery.getValue(fieldElements.iterator().next().getMatchedText());
				try {
					IssueQuery.checkField(fieldName, IssueQuery.getOperator(suggestedLiteral));
				} catch (OneException e) {
					return null;
				}
			}
		}
		return super.describe(parseExpect, suggestedLiteral);
	}

}
