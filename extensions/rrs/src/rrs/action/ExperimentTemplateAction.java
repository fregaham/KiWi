package rrs.action;


import java.io.IOException;
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

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Nodes;
import nu.xom.ParsingException;
import nu.xom.Serializer;
import nu.xom.ValidityException;

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
	
	private String prefix;
	
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
	
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}
	
	public String getPrefix() {
		return prefix;
	}
	
	/**
	 * Clone the text content of a template and set it to the new item
	 * @param template
	 * @param item
	 */
	private void clone(ContentItem template, ContentItem item) {
		contentItemService.updateTextContentItem(item, template.getTextContent().copyXmlDocument());
	}
	
	/**
	 * Create an experiment from the currentContentItem template
	 * @throws NamespaceResolvingException 
	 */
	public void createExperiment() throws NamespaceResolvingException {
		ContentItem experiment = contentItemService.createContentItem();
		experiment.addType(tripleStore.createUriResource(exp + "Experiment"));
		clone(currentContentItem, experiment);
		contentItemService.updateTitle(experiment, prefix + ": " + currentContentItem.getTitle());
		contentItemService.saveContentItem(experiment);
		
		tripleStore.createTriple(experiment.getResource(), tripleStore.createUriResource(exp + "experimentTemplate"), currentContentItem.getResource());
		
		KiWiUriResource expFirstPage = tripleStore.createUriResource(exp + "firstPage");
		KiWiUriResource expNextPage = tripleStore.createUriResource(exp + "nextPage");
		
		List<ContentItem> experimentPages = new LinkedList<ContentItem> ();
		List<ContentItem> templatePages = getSelectedTemplatePages(); 
		for (ContentItem template : templatePages) {
			ContentItem page = contentItemService.createContentItem();
			page.addType(tripleStore.createUriResource(exp + "ExperimentPage"));
			clone(template, page);
			
			contentItemService.updateTitle(page, prefix + ": " + template.getTitle());
			
			contentItemService.saveContentItem(page);
			
			experimentPages.add(page);
		}
		
		if (experimentPages.size() > 0) {
			tripleStore.createTriple(experiment.getResource(), expFirstPage, experimentPages.get(0).getResource());
		}
		
		for (int i = 0; i < experimentPages.size() - 1; ++i) {
			KiWiResource pageFrom = experimentPages.get(i).getResource();
			KiWiResource pageTo = experimentPages.get(i + 1).getResource();
			
			tripleStore.createTriple(pageFrom, expNextPage, pageTo);
		}
	}
	
	/**
	 * Get the list of experiments created from this template
	 * @return
	 * @throws NamespaceResolvingException
	 */
	public List<ContentItem> getExperiments() throws NamespaceResolvingException {
		List<ContentItem> ret = new LinkedList<ContentItem> ();
		for (KiWiTriple triple : currentContentItem.getResource().listIncoming("<" + exp + "experimentTemplate" +">")) {
			ret.add(triple.getSubject().getContentItem());
		}
		
		return ret;
	}
	
	private String fileUrl;
	
	public void setFileUrl(String fileUrl) {
		this.fileUrl = fileUrl;
	}
	
	public String getFileUrl() {
		return this.fileUrl;
	}
	
	public void loadExperimentTemplateFromXmlFile() throws ValidityException, ParsingException, IOException, NamespaceResolvingException {
		Builder parser = new Builder();
		Document document = parser.build(fileUrl);

		List<ContentItem> pages = new LinkedList<ContentItem>();
		
		Nodes nodes = document.query("//doc");
		for (int i = 0; i < nodes.size(); ++i) {
			Element doc = (Element)nodes.get(i);
			
			// Let it fail if the format is wrong... 
			String title = doc.getFirstChildElement("title").getValue();
			
			Nodes divs = doc.query("text/div");
			Element div = (Element)divs.get(0);
			
			String xml = div.toXML();
			
			ContentItem page = contentItemService.createContentItem();
			page.addType(tripleStore.createUriResource(exp + "ExperimentTemplatePage"));
			contentItemService.updateTextContentItem(page, xml);
			contentItemService.updateTitle(page, title);
			contentItemService.saveContentItem(page);
			
			pages.add(page);
		}
		
		setSelectedTemplatePages(pages);
		saveTemplatePages();
	}
}