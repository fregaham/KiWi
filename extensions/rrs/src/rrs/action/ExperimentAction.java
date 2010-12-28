package rrs.action;


import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


import kiwi.api.content.ContentItemService;
import kiwi.api.triplestore.TripleStore;
import kiwi.context.CurrentApplicationFactory;
import kiwi.context.CurrentContentItemFactory;
import kiwi.exception.NamespaceResolvingException;
import kiwi.model.content.ContentItem;
import kiwi.model.kbase.KiWiResource;
import kiwi.model.kbase.KiWiTriple;
import kiwi.model.kbase.KiWiUriResource;
import kiwi.model.user.User;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.jboss.seam.log.Log;

/**
 * 
 * @author Marek Schmidt
 *
 */
@Name("rrs.experimentAction")
@Scope(ScopeType.CONVERSATION)
public class ExperimentAction implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2027211278763405599L;

	// private static final String pub = "http://nlp.fit.vutbr.cz/2010/publications#";
	private static final String exp = "http://nlp.fit.vutbr.cz/2010/experiment#";
	
	@Logger 
	private Log log;
	
	@In(create=true)
	ContentItem currentContentItem;
	
	@In(create=true)
	ContentItemService contentItemService;
	
	@In(create=true)
	User currentUser;
	
	@In(create=true)
	private TripleStore tripleStore;
	
	@In(create=true)
	private CurrentContentItemFactory currentContentItemFactory;
	
	@In(create=true)
	private CurrentApplicationFactory currentApplicationFactory;
	
	public void next() throws NamespaceResolvingException {
		
		ContentItem next = null;
		for (KiWiTriple triple : currentContentItem.getResource().listOutgoing("<" + exp + "nextPage" + ">")) {
			next = ((KiWiResource)triple.getObject()).getContentItem();
			break;
		}
		
		if (next != null) {
			currentContentItemFactory.setCurrentItemKiWiId(next.getKiwiIdentifier());
			currentContentItemFactory.refresh();
			
			Events.instance().raiseEvent("refreshEditor");
		}
	}
	
	public void previous() throws NamespaceResolvingException {
		ContentItem next = null;
		for (KiWiTriple triple : currentContentItem.getResource().listIncoming("<" + exp + "nextPage" + ">")) {
			next = ((KiWiResource)triple.getSubject()).getContentItem();
			break;
		}
		
		if (next != null) {
			currentContentItemFactory.setCurrentItemKiWiId(next.getKiwiIdentifier());
			currentContentItemFactory.refresh();
			
			Events.instance().raiseEvent("refreshEditor");
		}
	}
	
	public List<ContentItem> getExperiments() throws NamespaceResolvingException {
		List<ContentItem> experiments = new LinkedList<ContentItem>();
		ContentItem experimentType = contentItemService.getContentItemByUri(exp + "Experiment");
		for (KiWiTriple triple : experimentType.getResource().listIncoming("rdf:type")) {
			ContentItem page = triple.getSubject().getContentItem();
			experiments.add(page);
		}
		
		return experiments;
	}
	
	public String startExperiment(ContentItem item) throws NamespaceResolvingException {
		ContentItem first = null;
		for (KiWiTriple triple : item.getResource().listOutgoing("<" + exp + "firstPage" + ">")) {
			first = ((KiWiResource)triple.getObject()).getContentItem();
			break;
		}
		
		if (first != null) {
			currentContentItemFactory.setCurrentItemKiWiId(first.getKiwiIdentifier());
			currentContentItemFactory.refresh();
			
			Events.instance().raiseEvent("refreshEditor");
		}
		
		return "/rrs/experiment.xhtml";
	}
}
