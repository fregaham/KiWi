package rrs.action;


import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


import kiwi.api.content.ContentItemService;
import kiwi.api.triplestore.TripleStore;
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
import org.jboss.seam.log.Log;

/**
 * Backing bean for the experiment template perspective. For a template Experiment, set up list of experiment template pages.
 * 
 * @author Marek Schmidt
 *
 */
@Name("rrs.experimentTemplateAction")
@Scope(ScopeType.PAGE)
public class ExperimentTemplateAction implements Serializable {
	
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
			
	private List<ContentItem> selectedTemplatePages;
	private List<ContentItem> availableTemplatePages;
	
	public void setAvailableTemplatePages(List<ContentItem> pages) {
		availableTemplatePages = pages;
	}
	
	public void setSelectedTemplatePages(List<ContentItem> pages) {
		selectedTemplatePages = pages;
	}
	
	public List<ContentItem> getSelectedTemplatePages() throws NamespaceResolvingException {
		
		if (selectedTemplatePages == null) {
		
			selectedTemplatePages = new LinkedList<ContentItem>();

			// There should be only one first page, but we'll just go through
			// all of them...
			for (KiWiTriple triple : currentContentItem.getResource().listOutgoing("<" + exp + "firstTemplatePage" + ">")) {
				KiWiResource firstPage = (KiWiResource) triple.getObject();

				// the pages are linked by the exp:nextPage, it should be a
				// linear list,
				// but we don't count on it, do a transitive closure

				LinkedList<KiWiResource> todo = new LinkedList<KiWiResource>();
				Set<KiWiResource> done = new HashSet<KiWiResource>();

				todo.add(firstPage);
				done.add(firstPage);
				selectedTemplatePages.add(firstPage.getContentItem());

				while (!todo.isEmpty()) {
					KiWiResource page = todo.pop();
					for (KiWiTriple nextTriple : page.listOutgoing("<" + exp + "nextTemplatePage" + ">")) {
						KiWiResource nextPage = (KiWiResource) nextTriple.getObject();
						if (!done.contains(nextPage)) {
							todo.add(nextPage);
							done.add(nextPage);
							selectedTemplatePages.add(nextPage.getContentItem());
						}
					}
				}
			}
		}
		
		return selectedTemplatePages;
	}
	
	public void saveTemplatePages() throws NamespaceResolvingException {
		// Remove the fistPage triples...
		for (KiWiTriple triple : currentContentItem.getResource().listOutgoing("<" + exp + "firstTemplatePage" + ">")) {
			tripleStore.removeTriple(triple);
		}
		
		// remove the exp:nextPage triples in the selected template pages
		for (int i = 0; i < selectedTemplatePages.size(); ++i) {
			KiWiResource pageTemplate = selectedTemplatePages.get(i).getResource();
			for (KiWiTriple triple : pageTemplate.listOutgoing("<" + exp + "nextTemplatePage" + ">")) {
				tripleStore.removeTriple(triple);
			}
		}
		
		if (selectedTemplatePages.size() > 0) {
			ContentItem firstPage = selectedTemplatePages.get(0);
			
			KiWiUriResource expFirstPage = tripleStore.createUriResource(exp + "firstTemplatePage");
			KiWiUriResource expNextPage = tripleStore.createUriResource(exp + "nextTemplatePage");
			
			tripleStore.createTriple(currentContentItem.getResource(), expFirstPage, firstPage.getResource());
			
			for (int i = 0; i < selectedTemplatePages.size() - 1; ++i) {
				KiWiResource pageFrom = selectedTemplatePages.get(i).getResource();
				KiWiResource pageTo = selectedTemplatePages.get(i + 1).getResource();
				
				tripleStore.createTriple(pageFrom, expNextPage, pageTo);
			}
		}
		
		availableTemplatePages = null;
		selectedTemplatePages = null;		
	}

	/**
	 * Available pages are ExperimentTemplatePages that are not selected yet.
	 * @return
	 * @throws NamespaceResolvingException
	 */
	public List<ContentItem> getAvailableTemplatePages() throws NamespaceResolvingException {
		if (availableTemplatePages == null) {
			
			List<ContentItem> selected = getSelectedTemplatePages();
			
			availableTemplatePages = new LinkedList<ContentItem>();
			ContentItem experimentTemplatePage = contentItemService.getContentItemByUri(exp + "ExperimentTemplatePage");
			for (KiWiTriple pageTriple : experimentTemplatePage.getResource().listIncoming("rdf:type")) {
				ContentItem page = pageTriple.getSubject().getContentItem();
				if (!selected.contains(page)) {
					availableTemplatePages.add(page);
				}
			}
		}
		
		return availableTemplatePages;
	}
}
