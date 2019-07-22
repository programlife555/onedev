package io.onedev.server.web.page.project.blob.render.renderers.folder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.CallbackParameter;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;

import io.onedev.server.OneDev;
import io.onedev.server.git.Blob;
import io.onedev.server.git.BlobIdent;
import io.onedev.server.util.userident.UserIdent;
import io.onedev.server.web.behavior.AbstractPostAjaxBehavior;
import io.onedev.server.web.component.link.ViewStateAwareAjaxLink;
import io.onedev.server.web.component.markdown.MarkdownViewer;
import io.onedev.server.web.component.user.detail.UserDetailPanel;
import io.onedev.server.web.page.project.blob.ProjectBlobPage;
import io.onedev.server.web.page.project.blob.render.BlobRenderContext;

@SuppressWarnings("serial")
public class FolderViewPanel extends Panel {

	private static final String USER_DETAIL_ID = "userDetail";
	
	private final BlobRenderContext context;
	
	private final IModel<List<BlobIdent>> childrenModel = new LoadableDetachableModel<List<BlobIdent>>() {

		@Override
		protected List<BlobIdent> load() {
			Repository repository = context.getProject().getRepository();			
			try (RevWalk revWalk = new RevWalk(repository)) {
				RevTree revTree = revWalk.parseCommit(getCommitId()).getTree();
				TreeWalk treeWalk;
				if (context.getBlobIdent().path != null) {
					treeWalk = Preconditions.checkNotNull(
							TreeWalk.forPath(repository, context.getBlobIdent().path, revTree));
					treeWalk.enterSubtree();
				} else {
					treeWalk = new TreeWalk(repository);
					treeWalk.addTree(revTree);
				}
				List<BlobIdent> children = new ArrayList<>();
				while (treeWalk.next())
					children.add(new BlobIdent(context.getBlobIdent().revision, treeWalk.getPathString(), 
							treeWalk.getRawMode(0)));
				for (int i=0; i<children.size(); i++) {
					BlobIdent child = children.get(i);
					while (child.isTree()) {
						treeWalk = TreeWalk.forPath(repository, child.path, revTree);
						Preconditions.checkNotNull(treeWalk);
						treeWalk.enterSubtree();
						if (treeWalk.next()) {
							BlobIdent grandChild = new BlobIdent(context.getBlobIdent().revision, 
									treeWalk.getPathString(), treeWalk.getRawMode(0));
							if (treeWalk.next() || !grandChild.isTree()) 
								break;
							else
								child = grandChild;
						} else {
							break;
						}
					}
					children.set(i, child);
				}
				
				Collections.sort(children);
				return children;
			} catch (IOException e) {
				throw new RuntimeException(e);
			} 
		}
		
	};
	
	private final IModel<BlobIdent> readmeModel = new LoadableDetachableModel<BlobIdent>() {

		@Override
		protected BlobIdent load() {
			for (BlobIdent blobIdent: childrenModel.getObject()) {
				if (blobIdent.isFile() && blobIdent.getName().equalsIgnoreCase("readme.md"))
					return blobIdent;
			}
			return null;
		}
		
	};
	
	private AbstractDefaultAjaxBehavior userDetailBehavior;
	
	public FolderViewPanel(String id, BlobRenderContext context) {
		super(id);

		this.context = context;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();

		WebMarkupContainer parent = new WebMarkupContainer("parent") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(context.getBlobIdent().path != null);
			}
			
		};
		
		final BlobIdent parentIdent;
		if (context.getBlobIdent().path == null) {
			parentIdent = null;
		} else if (context.getBlobIdent().path.indexOf('/') != -1) {
			parentIdent = new BlobIdent(
					context.getBlobIdent().revision, 
					StringUtils.substringBeforeLast(context.getBlobIdent().path, "/"), 
					FileMode.TREE.getBits());
		} else {
			parentIdent = new BlobIdent(context.getBlobIdent().revision, null, FileMode.TREE.getBits());
		}
		parent.add(new ViewStateAwareAjaxLink<Void>("link") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				context.onSelect(target, parentIdent, null);
			}

			@Override
			protected void onComponentTag(ComponentTag tag) {
				super.onComponentTag(tag);
				
				ProjectBlobPage.State state = new ProjectBlobPage.State(parentIdent);
				PageParameters params = ProjectBlobPage.paramsOf(context.getProject(), state); 
				tag.put("href", urlFor(ProjectBlobPage.class, params));
			}
			
		});
		add(parent);
		
		add(new ListView<BlobIdent>("children", childrenModel) {

			@Override
			protected void populateItem(ListItem<BlobIdent> item) {
				final BlobIdent blobIdent = item.getModelObject();
				
				WebMarkupContainer pathIcon = new WebMarkupContainer("pathIcon");
				String iconClass;
				if (blobIdent.isTree())
					iconClass = "fa fa-folder-o";
				else if (blobIdent.isGitLink()) 
					iconClass = "fa fa-ext fa-folder-submodule-o";
				else if (blobIdent.isSymbolLink()) 
					iconClass = "fa fa-ext fa-folder-symbol-link-o";
				else  
					iconClass = "fa fa-file-text-o";
				pathIcon.add(AttributeModifier.append("class", iconClass));
				
				item.add(pathIcon);
				
				AjaxLink<Void> pathLink = new ViewStateAwareAjaxLink<Void>("pathLink") {

					@Override
					protected void onComponentTag(ComponentTag tag) {
						super.onComponentTag(tag);
						
						ProjectBlobPage.State state = new ProjectBlobPage.State(blobIdent);
						PageParameters params = ProjectBlobPage.paramsOf(context.getProject(), state); 
						tag.put("href", urlFor(ProjectBlobPage.class, params));
					}

					@Override
					public void onClick(AjaxRequestTarget target) {
						context.onSelect(target, blobIdent, null);
					}
					
				}; 
				
				if (context.getBlobIdent().path != null)
					pathLink.add(new Label("label", blobIdent.path.substring(context.getBlobIdent().path.length()+1)));
				else
					pathLink.add(new Label("label", blobIdent.path));
				item.add(pathLink);
				
				if (item.getIndex() == 0)
					item.add(new Label("lastCommit", "Loading last commit info..."));
				else
					item.add(new Label("lastCommit"));
			}
			
		});
		
		WebMarkupContainer readmeContainer = new WebMarkupContainer("readme") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				
				setVisible(readmeModel.getObject() != null);
			}
			
		};
		readmeContainer.add(new Label("title", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				return readmeModel.getObject().getName();
			}
			
		}));
		
		readmeContainer.add(new MarkdownViewer("body", new LoadableDetachableModel<String>() {

			@Override
			protected String load() {
				Blob blob = context.getProject().getBlob(readmeModel.getObject(), true);
				Blob.Text text = blob.getText();
				if (text != null)
					return text.getContent();
				else
					return "This seems like a binary file!";
			}
			
		}, null) {

			@Override
			protected Object getRenderContext() {
				return context;
			}

		});
		
		add(readmeContainer);
		
		add(new WebMarkupContainer(USER_DETAIL_ID).setOutputMarkupId(true));
		add(userDetailBehavior = new AbstractPostAjaxBehavior() {
			
			@Override
			protected void respond(AjaxRequestTarget target) {
				String jsonOfUserIdent = RequestCycle.get().getRequest().getPostParameters().getParameterValue("userIdent").toString();
				try {
					UserIdent userIdent = OneDev.getInstance(ObjectMapper.class).readValue(jsonOfUserIdent, UserIdent.class);
					Component userDetail = new UserDetailPanel(USER_DETAIL_ID, userIdent);
					userDetail.setOutputMarkupId(true);
					replace(userDetail);
					target.add(userDetail);
					target.appendJavaScript("onedev.server.folderView.onUserDetailAvailable();");
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
			
		});
		
		setOutputMarkupId(true);
	}
	
	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		
		response.render(JavaScriptHeaderItem.forReference(new FolderViewResourceReference()));

		PageParameters params = LastCommitsResource.paramsOf(context.getProject(), 
				context.getBlobIdent().revision, context.getBlobIdent().path); 
		String lastCommitsUrl = urlFor(new LastCommitsResourceReference(), params).toString();
		CharSequence callback = userDetailBehavior.getCallbackFunction(CallbackParameter.explicit("userIdent"));
		String script = String.format("onedev.server.folderView.onDomReady('%s', '%s', %s)", getMarkupId(), lastCommitsUrl, callback); 
		response.render(OnDomReadyHeaderItem.forScript(script));
	}

	private ObjectId getCommitId() {
		return context.getCommit();
	}

	@Override
	protected void onDetach() {
		childrenModel.detach();
		readmeModel.detach();		
		
		super.onDetach();
	}

}
