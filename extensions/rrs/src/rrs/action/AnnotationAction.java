package rrs.action;

import static kiwi.model.kbase.KiWiQueryLanguage.SPARQL;

import java.util.LinkedList;
import java.util.List;

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
// import kiwi.wiki.action.EditorInterface;

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
	
	/*
	public static class PropertyUI {
		private KiWiTriple triple;
		private String label;
		
		public void setTriple(KiWiTriple triple) {
			this.triple = triple;
		}
		
		public KiWiTriple getTriple() {
			return triple;
		}

		public void setLabel(String label) {
			this.label = label;
		}

		public String getLabel() {
			return label;
		}
		
		public boolean isDatatype() {
			return getTriple().getProperty().hasType(Constants.NS_OWL + "DatatypeProperty"); 
		}
		
		public boolean isObject() {
			return getTriple().getProperty().hasType(Constants.NS_OWL + "ObjectProperty");			
		}
		
		public void setSuggestedObject(String kiwiid) {
			if (isObject()) {
				ContentItemService contentItemService = (ContentItemService) Component.getInstance("contentItemService");
				ContentItem item = contentItemService.getContentItemByKiwiId(kiwiid);
				if (item != null) {
					getTriple().setObject(item.getResource());
				}
			}
		}
		
		public void clear() {
			getTriple().setObject(null);
		}
	};*/
	
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
	
	/*@In
	private KiWiEntityManager kiwiEntityManager;
	
	
	private ContentItem taggedItem;
	private ContentItem annotationType;
	private List<PropertyUI> properties;*/
	
/*	@In(value="editorAction", required=false)
	private EditorInterface editorAction;*/
	
	// The currently edited fragment in the editor
	//private KiWiResource taggedResource;
	// annotation of the taggedResource.
	// private ContentItem fragmentAnnotation;
	
	/*
	public void setAnnotationType(ContentItem type) {
		this.annotationType = type;
		//taggedItem = null;
		//properties = null;
		log.info("Annotation type: #0", this.annotationType);
	}*/
	
	/*
	public void updateType () {
		taggedItem = null;
		properties = null;
	}*/
	
	/*
	public ContentItem getAnnotationType() {
		log.info("get Annotation type: #0", annotationType);
		
		return annotationType;
	}*/
	
	/*public List<ContentItem> getAnnotationTypes() throws NamespaceResolvingException {
		List<ContentItem> ret = new LinkedList<ContentItem>();
				
		ContentItem pubPublicationDescription = contentItemService.getContentItemByUri(pub + "PublicationDescription");
		List<KiWiTriple> subClasses = pubPublicationDescription.getResource().listIncoming("rdfs:subClassOf");
		for (KiWiTriple t : subClasses) {
			ret.add(t.getSubject().getContentItem());
		}
		
		return ret;
	}*/
	
/*	public List<PropertyUI> getProperties() {
		
		if (taggedItem == null || !currentContentItem.getKiwiIdentifier().equals(taggedItem.getKiwiIdentifier())) {
			properties = null;
			taggedItem = null;
		}
		
		if (properties == null && annotationType != null) {
			
			taggedItem = currentContentItem;
			
			properties = new LinkedList<PropertyUI> ();
			
			// Create a list of triples that should be there from the ontology.
			//List<KiWiTriple> triples = new LinkedList<KiWiTriple> ();
	
			StringBuilder sparqlStr = new StringBuilder();
			sparqlStr.append("SELECT ?S WHERE { " +
			"{ " +
			"{ ?S <" + Constants.NS_RDF + "type> <" + Constants.NS_OWL + "ObjectProperty> . } UNION " +
			"{ ?S <" + Constants.NS_RDF + "type> <" + Constants.NS_OWL + "DatatypeProperty> . } " +
			"} " +
			" { ");
			
			sparqlStr.append(("{ ?S <" + Constants.NS_RDFS + "domain> " + annotationType.getResource().getSeRQLID() + " . }"));
			
			sparqlStr.append("} }");
			
			final String query = sparqlStr.toString();
			Query q = kiwiEntityManager.createQuery(
					query, 
					SPARQL, KiWiProperty.class);

			List<KiWiProperty> props = q.getResultList();
			
			for (KiWiProperty property : props) {
				
				KiWiTriple t = null;
			
				t = new KiWiTriple();
				t.setSubject(taggedItem.getResource());
				t.setProperty((KiWiUriResource)property.getResource());
				
				PropertyUI p = new PropertyUI();
				p.setTriple(t);
				
				if (t.getObject() != null) {
					if (t.getObject().isLiteral()) {
						p.setLabel(((KiWiLiteral)t.getObject()).getContent());
					}
					else {
						p.setLabel(((KiWiResource)t.getObject()).getLabel());
					}
				}
				
				properties.add(p);
			}
		}
		
		return properties;
	}*/

	/*
	public void annotate() throws NamespaceResolvingException {	
		ContentItem annotation = contentItemService.createContentItem();
		
		annotation.addType((KiWiUriResource)annotationType.getResource());
		annotation.addType(tripleStore.createUriResource(pub + "PublicationDescription"));
		annotation.getResource().addOutgoingNode(tripleStore.createUriResource(pub+"aboutPublication"), currentContentItem.getResource());
		annotation.setTitle("Description of  `" + currentContentItem.getTitle() + "'");
		contentItemService.saveContentItem(annotation);
		
		for (PropertyUI p : properties) {
			if (p.isDatatype()) {
				if (!"".equals(p.getLabel())) {
					tripleStore.createTriple(annotation.getResource(), p.getTriple().getProperty(), tripleStore.createLiteral(p.getLabel()));
				}
			}
			else {
				if (p.getTriple().getObject() == null) {
					if (!"".equals(p.getLabel())) {
						ContentItem ci = contentItemService.createContentItem();
						contentItemService.updateTitle(ci, p.getLabel());
												
						contentItemService.saveContentItem(ci);
						
						tripleStore.createTriple(annotation.getResource(), p.getTriple().getProperty(), ci.getResource());
					}
				}
				else {
					tripleStore.createTriple(annotation.getResource(), p.getTriple().getProperty(), p.getTriple().getObject());
				}
			}
		}
		
		annotationType = null;
		taggedItem = null;
		properties = null;
	}*/
	

	/*
	public void annotateFragment() throws NamespaceResolvingException {
		ContentItem annotation = contentItemService.createContentItem();
		
		annotation.addType((KiWiUriResource)annotationType.getResource());
		annotation.addType(tripleStore.createUriResource(pub + "PublicationDescription"));
		annotation.getResource().addOutgoingNode(tripleStore.createUriResource(pub+"aboutPublication"), currentContentItem.getResource());
		annotation.setTitle("Description of  `" + currentContentItem.getTitle() + "'");
		contentItemService.saveContentItem(annotation);
		
		for (PropertyUI p : properties) {
			if (p.isDatatype()) {
				if (!"".equals(p.getLabel())) {
					tripleStore.createTriple(annotation.getResource(), p.getTriple().getProperty(), tripleStore.createLiteral(p.getLabel()));
				}
			}
			else {
				if (p.getTriple().getObject() == null) {
					if (!"".equals(p.getLabel())) {
						ContentItem ci = contentItemService.createContentItem();
						contentItemService.updateTitle(ci, p.getLabel());
												
						contentItemService.saveContentItem(ci);
						
						tripleStore.createTriple(annotation.getResource(), p.getTriple().getProperty(), ci.getResource());
					}
				}
				else {
					tripleStore.createTriple(annotation.getResource(), p.getTriple().getProperty(), p.getTriple().getObject());
				}
			}
		}
		
		fragmentAnnotation = annotation;
		// updateFragmentAnnotation();
		
		annotationType = null;
		taggedItem = null;
		properties = null;
	}*/

	/*
	public String getDescriptionText(String descriptionUri) throws NamespaceResolvingException {
		// get the type
		ContentItem type = null;
		
		ContentItem description = contentItemService.getContentItemByUri(descriptionUri);
		
		// String pub = "http://nlp.fit.vutbr.cz/2010/publications#";
		KiWiResource publicationDescription = tripleStore.createUriResource(pub+"PublicationDescription"); 
		
		types: for (KiWiResource t : description.getTypes()) {
			if (t.getKiwiIdentifier().equals(publicationDescription.getKiwiIdentifier())) {
				continue types;
			}
			
			for (KiWiNode superclass : t.listOutgoingNodes("rdfs:subClassOf")) {
				if ( ((KiWiResource)superclass).getKiwiIdentifier().equals(publicationDescription.getKiwiIdentifier())) {
					type = t.getContentItem();
					break types;
				}
			}
		}
		
		if (type != null) {
			StringBuilder sb = new StringBuilder();
			
			sb.append(type.getResource().getLabel());
		
			List<KiWiTriple> outgoing = description.getResource().listOutgoing();
			outgoings: for (KiWiTriple t : outgoing) {
				for (KiWiTriple u : t.getProperty().listOutgoing("rdfs:domain")) {
					if (((KiWiResource)u.getObject()).getKiwiIdentifier().equals(type.getKiwiIdentifier())) {
						sb.append(", ");
						sb.append(t.getProperty().getLabel());
						sb.append(": ");
						
						if (t.getObject().isLiteral()) {
							sb.append(((KiWiLiteral)t.getObject()).getContent());
						}
						else {
							sb.append(((KiWiResource)t.getObject()).getLabel());
						}
						continue outgoings;
					}
				}
			}
			
			return sb.toString();
		}
		else {
			log.error("#0 has no PublicationDescription type", descriptionUri);
		}
		
		return "";
	}*/
	
	
	/*
	public void setFragmentAnnotation(ContentItem i) {
		fragmentAnnotation = i;
	}*/

	/*
	public ContentItem getFragmentAnnotation() {
		
		if (editorAction == null) {
			return null;
		}
		
		if (editorAction.getTaggedResource() != taggedResource) {
			taggedResource = editorAction.getTaggedResource();
			fragmentAnnotation = null;
		}
		
		if (fragmentAnnotation == null) {
		
			KiWiResource r = editorAction.getTaggedResource();
		
			if (r == null) {
				return null;
			}
			
			List<KiWiTriple> outgoing = editorAction.getOutgoingTriples(r);
			for (KiWiTriple t : outgoing) {
				if (t.getProperty().getKiwiIdentifier().equals("uri::" + pub + "isFragmentOfDescription")) {
					fragmentAnnotation = ((KiWiResource)t.getObject()).getContentItem();
					break;
				}
			}
		}
		
		return fragmentAnnotation;
	}*/
	
	/*
	public List<ContentItem> getFragmentAnnotations() throws NamespaceResolvingException {
		
		List<ContentItem> annotations = new LinkedList<ContentItem>();
		
		List<KiWiTriple> aboutts = currentContentItem.getResource().listIncoming("pub:aboutPublication");
		for (KiWiTriple t : aboutts) {
			annotations.add(((KiWiResource)t.getSubject()).getContentItem());
		}
		
		return annotations;
	}*/
	
	/*
	public void updateFragmentAnnotation() {
		
		if (editorAction == null) {
			log.info("editor action null");
			return;
		}
		
		KiWiResource r = editorAction.getTaggedResource();
		List<KiWiTriple> outgoing = editorAction.getOutgoingTriples(r);
		for (KiWiTriple t : outgoing) {
			if (t.getProperty().getKiwiIdentifier().equals("uri::" + pub + "isFragmentOfDescription")) {
				if ( fragmentAnnotation != null &&  ((KiWiResource)t.getObject()).getKiwiIdentifier().equals(fragmentAnnotation.getKiwiIdentifier())) {
					log.info("not changing fragment annotation");
					return;
				}
				else {
					log.info("deleting fragment annotation");
					t.setDeleted(true);
				}
			}
		}
		
		if (fragmentAnnotation != null) {
			KiWiTriple t = new KiWiTriple();
			t.setSubject(r);
			t.setProperty(tripleStore.createUriResource(pub + "isFragmentOfDescription"));
			t.setObject(fragmentAnnotation.getResource());
			
			outgoing.add(t);
			
			log.info("Creating triple #0", t);
		}
	}*/
	
	
	public List<Tag> getTags() {
		List<Tag> ret = new LinkedList<Tag>();
		ret.addAll(taggingService.getTags(currentContentItem));
		
		for (FragmentFacade ff : fragmentService.getContentItemFragments(currentContentItem, FragmentFacade.class)) {
			ret.addAll(taggingService.getTags(ff.getDelegate()));
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
		List<KiWiTriple> firstTriples = tag.getResource().listIncoming("rdf:first");
		List<KiWiTriple> restTriples = tag.getResource().listIncoming("rdf:rest");
		
		List<ContentItem> ret = new LinkedList<ContentItem> ();
		
		for (KiWiTriple triple : firstTriples) {
			ret.add(triple.getSubject().getContentItem());
		}
		
		for (KiWiTriple triple : restTriples) {
			ret.add(triple.getSubject().getContentItem());
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
