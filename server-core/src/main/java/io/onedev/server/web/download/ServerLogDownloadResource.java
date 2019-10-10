package io.onedev.server.web.download;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.shiro.authz.UnauthorizedException;
import org.apache.tika.mime.MimeTypes;
import org.apache.wicket.request.resource.AbstractResource;

import io.onedev.commons.launcher.bootstrap.Bootstrap;
import io.onedev.commons.utils.FileUtils;
import io.onedev.server.util.SecurityUtils;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;

public class ServerLogDownloadResource extends AbstractResource {

	private static final long serialVersionUID = 1L;

	@Override
	protected ResourceResponse newResourceResponse(Attributes attributes) {
		if (!SecurityUtils.isAdministrator()) 
			throw new UnauthorizedException();

		ResourceResponse response = new ResourceResponse();
		response.setContentType(MimeTypes.OCTET_STREAM);
		
		response.disableCaching();
		
		try {
			response.setFileName(URLEncoder.encode("server-log.txt", Charsets.UTF_8.name()));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		response.setWriteCallback(new WriteCallback() {

			@Override
			public void writeData(Attributes attributes) throws IOException {
				String content = Joiner.on("\n").join(readServerLog());
				attributes.getResponse().getOutputStream().write(content.getBytes(Charsets.UTF_8));
			}				
		});

		return response;
	}

	public static List<String> readServerLog() {
		File logFile = new File(Bootstrap.installDir, "logs/server.log");
    	List<String> lines = new ArrayList<>();
    	int index = logFile.getParentFile().list().length;
    	try {
			File logDir = logFile.getParentFile();
			for (int i=index; i>=1; i--) {
				File rollFile = new File(logDir, logFile.getName() + "." + i);
				if (rollFile.exists())
					lines.addAll((FileUtils.readLines(rollFile, Charsets.UTF_8)));
			}
			lines.addAll((FileUtils.readLines(logFile, Charsets.UTF_8)));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
    	return lines;
	}
	
}
