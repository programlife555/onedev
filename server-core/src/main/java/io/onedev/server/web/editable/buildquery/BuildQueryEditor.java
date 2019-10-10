package io.onedev.server.web.editable.buildquery;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.ConversionException;

import io.onedev.server.model.Project;
import io.onedev.server.web.behavior.BuildQueryBehavior;
import io.onedev.server.web.behavior.OnTypingDoneBehavior;
import io.onedev.server.web.editable.PropertyDescriptor;
import io.onedev.server.web.editable.PropertyEditor;
import io.onedev.server.web.page.project.ProjectPage;

@SuppressWarnings("serial")
public class BuildQueryEditor extends PropertyEditor<String> {
	
	private final boolean noLoginSupport;
	
	private TextField<String> input;
	
	public BuildQueryEditor(String id, PropertyDescriptor propertyDescriptor, IModel<String> propertyModel, 
			boolean noLoginSupport) {
		super(id, propertyDescriptor, propertyModel);
		this.noLoginSupport = noLoginSupport;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
    	
    	input = new TextField<String>("input", getModel());
        input.add(new BuildQueryBehavior(new AbstractReadOnlyModel<Project>() {

			@Override
			public Project getObject() {
				return ((ProjectPage) getPage()).getProject();
			}
    		
    	}, noLoginSupport));
        
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
	protected String convertInputToValue() throws ConversionException {
		return input.getConvertedInput();
	}

}
