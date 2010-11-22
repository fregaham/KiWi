package rrs.action;


import kiwi.api.content.ContentItemService;
import kiwi.api.triplestore.TripleStore;
import kiwi.exception.NamespaceResolvingException;
import kiwi.model.content.ContentItem;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;

@Name("rrs.descriptionAction")
@Scope(ScopeType.CONVERSATION)
public class DescriptionAction {
	
	@Logger 
	private Log log;
	
	@In(create=true)
	ContentItem currentContentItem;
	
	@In(create=true)
	ContentItemService contentItemService;
	
	@In(create=true)
	private TripleStore tripleStore;
	
	
	public void addDescription() throws NamespaceResolvingException {
		log.info("Adding descrpition to #0", currentContentItem.getKiwiIdentifier());
		
		String pub = "http://nlp.fit.vutbr.cz/2010/publications#";
		ContentItem ci = contentItemService.createContentItem();
		
		ci.addType(tripleStore.createUriResource(pub + "PublicationDescription"));
		ci.getResource().addOutgoingNode(tripleStore.createUriResource(pub+"aboutPublication"), currentContentItem.getResource());
		ci.setTitle(currentContentItem.getTitle());
	}
	
	public void removeDescription(String uri) {
		log.info("Removing descrpition to #0, #1", currentContentItem.getKiwiIdentifier(), uri);
		
		ContentItem item = contentItemService.getContentItemByUri(uri);
		if (item != null) {
			contentItemService.removeContentItem(item);
		}
	}
}
