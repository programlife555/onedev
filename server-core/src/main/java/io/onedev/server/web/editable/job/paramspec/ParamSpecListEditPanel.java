package io.onedev.server.web.editable.job.paramspec;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.HeadersToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NoRecordsToolbar;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.ConversionException;

import io.onedev.commons.utils.StringUtils;
import io.onedev.server.util.inputspec.InputSpec;
import io.onedev.server.web.behavior.sortable.SortBehavior;
import io.onedev.server.web.behavior.sortable.SortPosition;
import io.onedev.server.web.component.modal.ModalLink;
import io.onedev.server.web.component.modal.ModalPanel;
import io.onedev.server.web.editable.EditableUtils;
import io.onedev.server.web.editable.ErrorContext;
import io.onedev.server.web.editable.PathElement;
import io.onedev.server.web.editable.PropertyDescriptor;
import io.onedev.server.web.editable.PropertyEditor;
import io.onedev.server.web.editable.PropertyUpdating;

@SuppressWarnings("serial")
class ParamSpecListEditPanel extends PropertyEditor<List<Serializable>> {

	private final List<InputSpec> params;
	
	public ParamSpecListEditPanel(String id, PropertyDescriptor propertyDescriptor, IModel<List<Serializable>> model) {
		super(id, propertyDescriptor, model);
		
		params = new ArrayList<>();
		for (Serializable each: model.getObject()) {
			params.add((InputSpec) each);
		}
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		add(new ModalLink("addNew") {

			@Override
			protected Component newContent(String id, ModalPanel modal) {
				return new ParamSpecEditPanel(id, params, -1) {

					@Override
					protected void onCancel(AjaxRequestTarget target) {
						modal.close();
					}

					@Override
					protected void onSave(AjaxRequestTarget target) {
						markFormDirty(target);
						modal.close();
						onPropertyUpdating(target);
						target.add(ParamSpecListEditPanel.this);
					}

				};
			}
			
		});
		
		List<IColumn<InputSpec, Void>> columns = new ArrayList<>();
		
		columns.add(new AbstractColumn<InputSpec, Void>(Model.of("")) {

			@Override
			public void populateItem(Item<ICellPopulator<InputSpec>> cellItem, String componentId, IModel<InputSpec> rowModel) {
				cellItem.add(new Label(componentId, "<span class=\"drag-indicator fa fa-reorder\"></span>").setEscapeModelStrings(false));
			}
			
			@Override
			public String getCssClass() {
				return "minimum actions";
			}
			
		});		
		
		columns.add(new AbstractColumn<InputSpec, Void>(Model.of("Name")) {

			@Override
			public void populateItem(Item<ICellPopulator<InputSpec>> cellItem, String componentId, IModel<InputSpec> rowModel) {
				Fragment fragment = new Fragment(componentId, "nameColumnFrag", ParamSpecListEditPanel.this);
				ModalLink link = new ModalLink("link") {

					@Override
					protected Component newContent(String id, ModalPanel modal) {
						return new ParamSpecEditPanel(id, params, cellItem.findParent(Item.class).getIndex()) {

							@Override
							protected void onCancel(AjaxRequestTarget target) {
								modal.close();
							}

							@Override
							protected void onSave(AjaxRequestTarget target) {
								markFormDirty(target);
								modal.close();
								onPropertyUpdating(target);
								target.add(ParamSpecListEditPanel.this);
							}

						};
					}
					
				};
				link.add(new Label("label", rowModel.getObject().getName()));
				fragment.add(link);
				
				cellItem.add(fragment);
			}
		});		
		
		columns.add(new AbstractColumn<InputSpec, Void>(Model.of("Type")) {

			@Override
			public void populateItem(Item<ICellPopulator<InputSpec>> cellItem, String componentId, IModel<InputSpec> rowModel) {
				cellItem.add(new Label(componentId, EditableUtils.getDisplayName(rowModel.getObject().getClass())));
			}
		});		
		
		columns.add(new AbstractColumn<InputSpec, Void>(Model.of("Allow Empty")) {

			@Override
			public void populateItem(Item<ICellPopulator<InputSpec>> cellItem, String componentId, IModel<InputSpec> rowModel) {
				cellItem.add(new Label(componentId, StringUtils.describe(rowModel.getObject().isAllowEmpty())));
			}
			
		});		
		
		columns.add(new AbstractColumn<InputSpec, Void>(Model.of("")) {

			@Override
			public void populateItem(Item<ICellPopulator<InputSpec>> cellItem, String componentId, IModel<InputSpec> rowModel) {
				Fragment fragment = new Fragment(componentId, "actionColumnFrag", ParamSpecListEditPanel.this);
				fragment.add(new AjaxLink<Void>("delete") {

					@Override
					public void onClick(AjaxRequestTarget target) {
						markFormDirty(target);
						params.remove(rowModel.getObject());
						onPropertyUpdating(target);
						target.add(ParamSpecListEditPanel.this);
					}
					
				});
				cellItem.add(fragment);
			}

			@Override
			public String getCssClass() {
				return "actions";
			}
			
		});		
		
		IDataProvider<InputSpec> dataProvider = new ListDataProvider<InputSpec>() {

			@Override
			protected List<InputSpec> getData() {
				return params;			
			}

		};
		
		DataTable<InputSpec, Void> dataTable;
		add(dataTable = new DataTable<InputSpec, Void>("paramSpecs", columns, dataProvider, Integer.MAX_VALUE));
		dataTable.addTopToolbar(new HeadersToolbar<Void>(dataTable, null));
		dataTable.addBottomToolbar(new NoRecordsToolbar(dataTable));
		
		dataTable.add(new SortBehavior() {

			@Override
			protected void onSort(AjaxRequestTarget target, SortPosition from, SortPosition to) {
				int fromIndex = from.getItemIndex();
				int toIndex = to.getItemIndex();
				if (fromIndex < toIndex) {
					for (int i=0; i<toIndex-fromIndex; i++) 
						Collections.swap(params, fromIndex+i, fromIndex+i+1);
				} else {
					for (int i=0; i<fromIndex-toIndex; i++) 
						Collections.swap(params, fromIndex-i, fromIndex-i-1);
				}
				onPropertyUpdating(target);
				target.add(ParamSpecListEditPanel.this);
			}
			
		}.sortable("tbody"));
	}

	@Override
	public void onEvent(IEvent<?> event) {
		super.onEvent(event);
		
		if (event.getPayload() instanceof PropertyUpdating) {
			event.stop();
			onPropertyUpdating(((PropertyUpdating)event.getPayload()).getHandler());
		}		
	}

	@Override
	protected List<Serializable> convertInputToValue() throws ConversionException {
		List<Serializable> value = new ArrayList<>();
		for (InputSpec each: params)
			value.add(each);
		return value;
	}

	@Override
	public ErrorContext getErrorContext(PathElement element) {
		return null;
	}
	
	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new ParamSpecCssResourceReference()));
	}
	
}
