package kiwi.wiki.action;

import kiwi.api.config.ConfigurationService;
import kiwi.api.content.ContentItemService;
import kiwi.context.CurrentContentItemFactory;
import kiwi.model.content.ContentItem;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

@Name("deleteAction")
@Scope(ScopeType.EVENT)
public class DeleteAction {
	@In(create = true)
	ContentItemService contentItemService;
	
	@In
	ContentItem currentContentItem;
	
	@In(create=true)
	private CurrentContentItemFactory currentContentItemFactory;
	
	@In(create = true)
	private ConfigurationService configurationService;
	
	public void delete() {
		if (currentContentItem != null) {
			contentItemService.removeContentItem(currentContentItem);
		
			ContentItem startPage = contentItemService.getContentItemByUri(configurationService.getStartPage());
			
			if (startPage != null) {
				currentContentItemFactory.setCurrentItemKiWiId(startPage.getKiwiIdentifier());
				currentContentItemFactory.refresh();
			}
		}
	}
}
