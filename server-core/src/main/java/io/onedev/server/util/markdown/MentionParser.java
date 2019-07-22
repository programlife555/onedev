package io.onedev.server.util.markdown;

import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;

import com.google.common.collect.ImmutableSet;

import io.onedev.commons.utils.HtmlUtils;
import io.onedev.commons.utils.TextNodeVisitor;
import io.onedev.server.OneDev;
import io.onedev.server.entitymanager.UserManager;
import io.onedev.server.model.User;

public class MentionParser {
	
	private static final Collection<String> IGNORED_TAGS = ImmutableSet.of("pre", "code", "a");
	
	private static final Pattern PATTERN = Pattern.compile("(^|\\s+)@(\\S+)($|\\s+)");

	public Collection<User> parseMentions(String rendered) {
		return parseMentions(Jsoup.parseBodyFragment(rendered));		
	}
	
	public Collection<User> parseMentions(Document document) {
		Collection<User> mentions = new HashSet<>();
		
		TextNodeVisitor visitor = new TextNodeVisitor() {
			
			@Override
			protected boolean isApplicable(TextNode node) {
				if (HtmlUtils.hasAncestor(node, IGNORED_TAGS))
					return false;
				
				return node.getWholeText().contains("@"); // fast scan here, do pattern match later
			}
		};
		
		NodeTraversor tranversor = new NodeTraversor(visitor);
		tranversor.traverse(document);
		
		UserManager userManager = OneDev.getInstance(UserManager.class);
		
		for (TextNode node : visitor.getMatchedNodes()) {
			Matcher matcher = PATTERN.matcher(node.getWholeText());
			while (matcher.find()) {
				String userName = matcher.group(2);
				String userTag;
				User user = userManager.findByName(userName);
				if (user != null) {
					mentions.add(user);
					userTag = toHtml(user);
				} else {
					userTag = "@" + userName;
				}
				HtmlUtils.appendReplacement(matcher, node, matcher.group(1) + userTag + matcher.group(3));
			}
			HtmlUtils.appendTail(matcher, node);
		}

		return mentions;
	}

	protected String toHtml(User user) {
		return "@" + user.getName();
	}
	
}
