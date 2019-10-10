package org.server.plugin.cispec.django;

import io.onedev.commons.launcher.loader.AbstractPluginModule;
import io.onedev.server.ci.DefaultCISpecProvider;

/**
 * NOTE: Do not forget to rename moduleClass property defined in the pom if you've renamed this class.
 *
 */
public class DjangoModule extends AbstractPluginModule {

	@Override
	protected void configure() {
		super.configure();
		
		// put your guice bindings here
		contribute(DefaultCISpecProvider.class, DefaultDjangoCISpecProvider.class);
	}

}
