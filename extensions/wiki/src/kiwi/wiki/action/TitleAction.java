package kiwi.wiki.action;

import kiwi.api.content.ContentItemService;
import kiwi.context.CurrentContentItemFactory;
import kiwi.model.content.ContentItem;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Synchronized;

@Scope(ScopeType.PAGE)
@Name("titleAction")
@Synchronized
public class TitleAction {

	private String title;
	
	@In(create=true)
	ContentItem currentContentItem;
	
	@In(create=true)
	ContentItemService contentItemService;
	
	
	@Create
	public void init() {
		title = currentContentItem.getTitle();
	}
	
	public void save() {
		contentItemService.updateTitle(currentContentItem, title);
	}
	
	public void cancel() {
		init();
	}
	
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	
}
