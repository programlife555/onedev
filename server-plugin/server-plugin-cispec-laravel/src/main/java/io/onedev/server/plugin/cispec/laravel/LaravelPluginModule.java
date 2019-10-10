package io.onedev.server.plugin.cispec.laravel;

import io.onedev.commons.launcher.loader.AbstractPluginModule;
import io.onedev.server.ci.DefaultCISpecProvider;

/**
 * NOTE: Do not forget to rename moduleClass property defined in the pom if you've renamed this class.
 *
 */
public class LaravelPluginModule extends AbstractPluginModule {

	@Override
	protected void configure() {
		super.configure();
		
		// put your guice bindings here
		contribute(DefaultCISpecProvider.class, DefaultLaravelCISpecProvider.class);
	}

}
