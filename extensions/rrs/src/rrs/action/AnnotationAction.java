package rrs.action;

import static kiwi.model.kbase.KiWiQueryLanguage.SPARQL;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import javax.persistence.Query;

import kiwi.api.content.ContentItemService;
import kiwi.api.entity.KiWiEntityManager;
import kiwi.api.fragment.FragmentFacade;
import kiwi.api.fragment.FragmentService;
import kiwi.api.tagging.TaggingService;
import kiwi.api.triplestore.TripleStore;
import kiwi.exception.NamespaceResolvingException;
import kiwi.model.Constants;
import kiwi.model.content.ContentItem;
import kiwi.model.kbase.KiWiLiteral;
import kiwi.model.kbase.KiWiNode;
import kiwi.model.kbase.KiWiResource;
import kiwi.model.kbase.KiWiTriple;
import kiwi.model.kbase.KiWiUriResource;
import kiwi.model.ontology.KiWiProperty;
import kiwi.model.tagging.Tag;
import kiwi.model.user.User;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;


@Name("rrs.annotationAction")
@Scope(ScopeType.CONVERSATION)
public class AnnotationAction {
	
	private static final String pub = "http://nlp.fit.vutbr.cz/2010/publications#";
	
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
	private FragmentService fragmentService;
	
	@In(create=true)
	private TaggingService taggingService;	
	
	public List<Tag> getTags() {
		List<Tag> ret = new LinkedList<Tag>();
		ret.addAll(taggingService.getTaggings(currentContentItem));
		
		for (FragmentFacade ff : fragmentService.getContentItemFragments(currentContentItem, FragmentFacade.class)) {
			ret.addAll(taggingService.getTaggings(ff.getDelegate()));
		}
		
		return ret;
	}
	
	private String tag;
	
	public void setTag(String tag) {
		this.tag = tag;
	}
	
	public String getTag() {
		return this.tag;
	}
	
	public void addTag() {
		ContentItem taggingResource = taggingService.parseTag(this.tag);
		taggingService.createTagging(taggingResource.getTitle(), currentContentItem, taggingResource, currentUser);
	}
	
	
	/**
	 * Lists structured tags that are made of the specified tag. e.g. for tag "SVM", tags "(use, SVM)", "(combination, SVM, CRF)", etc. are listed  
	 * @param tag 
	 * @return
	 * @throws NamespaceResolvingException 
	 */
	public List<ContentItem> listRelatedStructuredTags(ContentItem tag) throws NamespaceResolvingException {
		
		Set<ContentItem> done = new HashSet<ContentItem> ();
		List<ContentItem> ret = new LinkedList<ContentItem> ();
		Queue<ContentItem> todo = new LinkedList<ContentItem>();
		todo.add(tag);
		ret.add(tag);
		done.add(tag);

		while (!todo.isEmpty()) {
			ContentItem ci = todo.poll();
			
			List<KiWiTriple> firstTriples = ci.getResource().listIncoming("rdf:first");
			List<KiWiTriple> restTriples = ci.getResource().listIncoming("rdf:rest");
			
			for (KiWiTriple triple : firstTriples) {
				ContentItem next = triple.getSubject().getContentItem();
				if (!done.contains(next)) {
					ret.add(next);
					done.add(next);
					todo.add(next);
				}
			}
			
			for (KiWiTriple triple : restTriples) {
				ContentItem next = triple.getSubject().getContentItem();
				if (!done.contains(next)) {
					ret.add(next);
					done.add(next);
					todo.add(next);
				}
			}
		}
		
		return ret;
	}
	
	/**
	 * List content items tagged with a tag. 
	 * @param tag
	 * @return Content items tagged with tag
	 * @throws NamespaceResolvingException
	 */
	public List<ContentItem> listTaggedItems(ContentItem tag) throws NamespaceResolvingException {
		List<KiWiTriple> taggedWithTagTriples = tag.getResource().listIncoming("hgtags:taggedWithTag");
		
		List<ContentItem> ret = new LinkedList<ContentItem> ();
		
		for (KiWiTriple triple : taggedWithTagTriples) {
			ret.add(triple.getSubject().getContentItem());
		}
		
		return ret;
	}
}
