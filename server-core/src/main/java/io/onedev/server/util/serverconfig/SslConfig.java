package io.onedev.server.util.serverconfig;

public interface SslConfig {
	int getPort();
	
	String getKeystorePath();
	
	String getKeystorePassword();
	
	String getKeystoreKeyPassword();
}
