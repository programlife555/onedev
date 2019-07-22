package io.onedev.server.web.page.project.blob.render.renderers.image;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.image.Image;

import io.onedev.server.web.download.RawBlobDownloadResource;
import io.onedev.server.web.download.RawBlobDownloadResourceReference;
import io.onedev.server.web.page.project.blob.render.BlobRenderContext;
import io.onedev.server.web.page.project.blob.render.view.BlobViewPanel;

@SuppressWarnings("serial")
public class ImageViewPanel extends BlobViewPanel {

	public ImageViewPanel(String id, BlobRenderContext context) {
		super(id, context);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		add(new Image("img", new RawBlobDownloadResourceReference(), 
				RawBlobDownloadResource.paramsOf(context.getProject(), context.getBlobIdent())));
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new ImageViewResourceReference()));
	}

	@Override
	protected boolean isEditSupported() {
		return false;
	}

	@Override
	protected boolean isViewPlainSupported() {
		return false;
	}

}
