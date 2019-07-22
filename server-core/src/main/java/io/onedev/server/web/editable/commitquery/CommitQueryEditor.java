package io.onedev.server.web.editable.commitquery;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.ConversionException;

import io.onedev.server.model.Project;
import io.onedev.server.web.behavior.CommitQueryBehavior;
import io.onedev.server.web.behavior.OnTypingDoneBehavior;
import io.onedev.server.web.editable.ErrorContext;
import io.onedev.server.web.editable.PathElement;
import io.onedev.server.web.editable.PropertyDescriptor;
import io.onedev.server.web.editable.PropertyEditor;
import io.onedev.server.web.page.project.ProjectPage;

@SuppressWarnings("serial")
public class CommitQueryEditor extends PropertyEditor<String> {
	
	private TextField<String> input;
	
	public CommitQueryEditor(String id, PropertyDescriptor propertyDescriptor, IModel<String> propertyModel) {
		super(id, propertyDescriptor, propertyModel);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
    	
    	input = new TextField<String>("input", getModel());
        input.add(new CommitQueryBehavior(new AbstractReadOnlyModel<Project>() {

			@Override
			public Project getObject() {
				return ((ProjectPage) getPage()).getProject();
			}
    		
    	}));
        
		input.setLabel(Model.of(getDescriptor().getDisplayName(this)));
        
        add(input);
		input.add(new OnTypingDoneBehavior() {

			@Override
			protected void onTypingDone(AjaxRequestTarget target) {
				onPropertyUpdating(target);
			}
			
		});
	}

	@Override
	public ErrorContext getErrorContext(PathElement element) {
		return null;
	}

	@Override
	protected String convertInputToValue() throws ConversionException {
		return input.getConvertedInput();
	}

}
