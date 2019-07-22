package io.onedev.server.util.inputspec.userchoiceinput.choiceprovider;

import java.io.Serializable;
import java.util.List;

import io.onedev.server.util.facade.UserFacade;
import io.onedev.server.web.editable.annotation.Editable;

@Editable
public interface ChoiceProvider extends Serializable {
	
	List<UserFacade> getChoices(boolean allPossible);
	
}
