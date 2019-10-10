package io.onedev.server.git.command;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import io.onedev.commons.utils.command.Commandline;
import io.onedev.commons.utils.command.ExecuteResult;
import io.onedev.commons.utils.command.LineConsumer;

public class UploadCommand extends GitCommand<Void> {

	private static final Logger logger = LoggerFactory.getLogger(UploadCommand.class);
	
	private InputStream input;
	
	private OutputStream output;
	
	public UploadCommand(File gitDir, Map<String, String> environments) {
		super(gitDir, environments);
	}
	
	public UploadCommand input(InputStream input) {
		this.input = input;
		return this;
	}
	
	public UploadCommand output(OutputStream output) {
		this.output = output;
		return this;
	}
	
	@Override
	public Void call() {
		Preconditions.checkNotNull(input);
		Preconditions.checkNotNull(output);
		
		Commandline cmd = cmd();
		cmd.addArgs("upload-pack", "--stateless-rpc", ".");
		
		AtomicBoolean toleratedErrors = new AtomicBoolean(false);
		ExecuteResult result = cmd.execute(output, new LineConsumer() {

			@Override
			public void consume(String line) {
				// This error may happen during a normal shallow fetch/clone 
				if (line.contains("remote end hung up unexpectedly")) {
					toleratedErrors.set(true);
					logger.debug(line);
				} else {
					logger.error(line);
				}
			}
			
		}, input);
		
		if (result.getReturnCode() != 0 && !toleratedErrors.get())
			throw result.buildException();
		return null;
	}

}
