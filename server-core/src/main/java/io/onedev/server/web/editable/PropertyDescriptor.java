package io.onedev.server.web.editable;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.hibernate.validator.constraints.NotEmpty;

import com.google.common.collect.Sets;

import io.onedev.commons.utils.BeanUtils;
import io.onedev.commons.utils.HtmlUtils;
import io.onedev.commons.utils.ReflectionUtils;
import io.onedev.commons.utils.StringUtils;
import io.onedev.server.OneDev;
import io.onedev.server.util.OneContext;
import io.onedev.server.web.editable.annotation.NameOfEmptyValue;
import io.onedev.server.web.editable.annotation.ShowCondition;

public class PropertyDescriptor implements Serializable {

	private static final long serialVersionUID = 1L;

	private final Class<?> beanClass;
	
	private final String propertyName;
	
	private boolean propertyExcluded;
	
	private Set<String> dependencyPropertyNames = new HashSet<>();
	
	private transient Method propertyGetter;
	
	private transient Method propertySetter;
	
	public PropertyDescriptor(Class<?> beanClass, String propertyName) {
		this.beanClass = beanClass;
		this.propertyName = propertyName;
	}
	
	public PropertyDescriptor(Method propertyGetter) {
		this.beanClass = propertyGetter.getDeclaringClass();
		this.propertyName = BeanUtils.getPropertyName(propertyGetter);
	}
	
	public PropertyDescriptor(PropertyDescriptor propertyDescriptor) {
		this.beanClass = propertyDescriptor.getBeanClass();
		this.propertyName = propertyDescriptor.getPropertyName();
		this.propertyExcluded = propertyDescriptor.isPropertyExcluded();
	}
	
	public Class<?> getBeanClass() {
		return beanClass;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public Method getPropertyGetter() {
		if (propertyGetter == null)
			propertyGetter = BeanUtils.getGetter(beanClass, propertyName);
		return propertyGetter;
	}
	
	public Method getPropertySetter() {
		if (propertySetter == null) 
			propertySetter = BeanUtils.getSetter(getPropertyGetter());
		return propertySetter;
	}

	public boolean isPropertyExcluded() {
		return propertyExcluded;
	}

	public void setPropertyExcluded(boolean propertyExcluded) {
		this.propertyExcluded = propertyExcluded;
	}

	public void copyProperty(Object fromBean, Object toBean) {
		setPropertyValue(toBean, getPropertyValue(fromBean));
	}

	public Object getPropertyValue(Object bean) {
		try {
			return getPropertyGetter().invoke(bean);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	public void setPropertyValue(Object bean, Object propertyValue) {
		try {
			getPropertySetter().invoke(bean, propertyValue);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	public Class<?> getPropertyClass() {
		return getPropertyGetter().getReturnType();
	}

	public boolean isPropertyRequired() {
		return getPropertyGetter().getReturnType().isPrimitive()
				|| getPropertyGetter().getAnnotation(NotNull.class) != null 
				|| getPropertyGetter().getAnnotation(NotEmpty.class) != null
				|| getPropertyGetter().getAnnotation(Size.class) != null && getPropertyGetter().getAnnotation(Size.class).min()>=1;
	}
	
	@Nullable
	public String getNameOfEmptyValue() {
		NameOfEmptyValue annotation = getPropertyGetter().getAnnotation(NameOfEmptyValue.class);
		if (annotation != null)
			return annotation.value();
		else
			return null;
	}

	public boolean isPropertyVisible(OneContext oneContext, BeanDescriptor beanDescriptor) {
		return isPropertyVisible(oneContext, beanDescriptor, Sets.newHashSet());
	}
	
	private boolean isPropertyVisible(OneContext oneContext, BeanDescriptor beanDescriptor, Set<String> checkedPropertyNames) {
		if (!checkedPropertyNames.add(getPropertyName()))
			return false;
		
		Set<String> prevDependencyPropertyNames = new HashSet<>(getDependencyPropertyNames());
		OneContext.push(oneContext);
		try {
			/* 
			 * Sometimes, the dependency may include properties introduced while evaluating available choices 
			 * of a choice input. We clear it temporarily here in order to make visibility of the property 
			 * consistent of BeanViewer, Issue.isFieldVisible, or Build.isParamVisible 
			 */
			getDependencyPropertyNames().clear();
			ShowCondition showCondition = getPropertyGetter().getAnnotation(ShowCondition.class);
			if (showCondition != null && !(boolean)ReflectionUtils.invokeStaticMethod(getBeanClass(), showCondition.value()))
				return false;
			for (String dependencyPropertyName: getDependencyPropertyNames()) {
				Set<String> copyOfCheckedPropertyNames = new HashSet<>(checkedPropertyNames);
				if (!beanDescriptor.getProperty(dependencyPropertyName).isPropertyVisible(oneContext, beanDescriptor, copyOfCheckedPropertyNames))
					return false;
			}
			return true;
		} finally {
			getDependencyPropertyNames().addAll(prevDependencyPropertyNames);
			OneContext.pop();
		}
	}
	
	public Set<String> getDependencyPropertyNames() {
		return dependencyPropertyNames;
	}
	
	public void setDependencyPropertyNames(Set<String> dependencyPropertyNames) {
		this.dependencyPropertyNames = dependencyPropertyNames;
	}
	
	public String getDisplayName() {
		return EditableUtils.getDisplayName(getPropertyGetter());
	}
	
	public String getDisplayName(Component component) {
		String displayName = getDisplayName();
		return Application.get().getResourceSettings().getLocalizer().getString(displayName, component, displayName);
	}
	
	public String getDescription() {
		return EditableUtils.getDescription(getPropertyGetter());
	}
	
	public String getDescription(Component component) {
		String description = getDescription();
		if (description != null) {
			OneContext.push(new OneContext(component));
			try {
				description = Application.get().getResourceSettings().getLocalizer().getString(description, component, description);
				description = StringUtils.replace(description, "$docRoot", OneDev.getInstance().getDocRoot());
				return HtmlUtils.clean(description).body().html();
			} finally {
				OneContext.pop();
			}
		} else {
			return null;
		}
	}
}
