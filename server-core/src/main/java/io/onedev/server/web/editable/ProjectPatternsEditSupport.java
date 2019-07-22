package io.onedev.server.web.editable;

import java.lang.reflect.Method;
import java.util.List;

import org.apache.wicket.model.IModel;

import com.google.common.collect.Lists;

import io.onedev.commons.codeassist.InputSuggestion;
import io.onedev.commons.codeassist.parser.TerminalExpect;
import io.onedev.server.web.behavior.PatternSetAssistBehavior;
import io.onedev.server.web.behavior.inputassist.InputAssistBehavior;
import io.onedev.server.web.editable.annotation.ProjectPatterns;
import io.onedev.server.web.editable.string.StringPropertyEditor;
import io.onedev.server.web.editable.string.StringPropertyViewer;
import io.onedev.server.web.util.SuggestionUtils;

@SuppressWarnings("serial")
public class ProjectPatternsEditSupport implements EditSupport {

	@Override
	public PropertyContext<?> getEditContext(PropertyDescriptor descriptor) {
		Method propertyGetter = descriptor.getPropertyGetter();
        if (propertyGetter.getAnnotation(ProjectPatterns.class) != null) {
        	if (propertyGetter.getReturnType() == String.class) {
        		return new PropertyContext<String>(descriptor) {

    				@Override
    				public PropertyViewer renderForView(String componentId, IModel<String> model) {
    					return new StringPropertyViewer(componentId, descriptor, model.getObject());
    				}

    				@Override
    				public PropertyEditor<String> renderForEdit(String componentId, IModel<String> model) {
    		        	return new StringPropertyEditor(componentId, descriptor, model) {

    						@Override
    						protected InputAssistBehavior getInputAssistBehavior() {
    							return new PatternSetAssistBehavior() {

									@Override
									protected List<InputSuggestion> suggest(String matchWith) {
										return SuggestionUtils.suggestProjects(matchWith);
									}

									@Override
									protected List<String> getHints(TerminalExpect terminalExpect) {
										return Lists.newArrayList("Use * or ? for wildcard match");
									}
    								
    							};
    						}
    		        		
    		        	};
    				}
        			
        		};
        	} else {
	    		throw new RuntimeException("Annotation 'ProjectPatterns' should be applied to property "
	    				+ "of type 'String'");
        	}
        } else {
            return null;
        }
	}

	@Override
	public int getPriority() {
		return DEFAULT_PRIORITY;
	}
	
}
