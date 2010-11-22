/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2008-2009, The KiWi Project (http://www.kiwi-project.eu)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * - Redistributions of source code must retain the above copyright notice, 
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, 
 *   this list of conditions and the following disclaimer in the documentation 
 *   and/or other materials provided with the distribution.
 * - Neither the name of the KiWi Project nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software 
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * Contributor(s):
 * 
 * 
 */

package kiwi.wiki.action;

import static kiwi.model.kbase.KiWiQueryLanguage.SPARQL;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.faces.model.SelectItem;
import javax.persistence.Query;

import kiwi.api.content.ContentItemService;
import kiwi.api.entity.KiWiEntityManager;
import kiwi.api.ontology.OntologyService;
import kiwi.api.search.KiWiSearchCriteria;
import kiwi.api.search.SolrService;
import kiwi.api.tagging.TaggingService;
import kiwi.api.triplestore.TripleStore;
import kiwi.model.Constants;
import kiwi.model.content.ContentItem;
import kiwi.model.informationextraction.Suggestion;
import kiwi.model.kbase.KiWiResource;
import kiwi.model.kbase.KiWiTriple;
import kiwi.model.kbase.KiWiUriResource;
import kiwi.model.ontology.KiWiClass;
import kiwi.model.ontology.KiWiProperty;
import kiwi.wiki.action.ie.SuggestionsAction;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Synchronized;
import org.jboss.seam.annotations.datamodel.DataModel;
import org.jboss.seam.cache.CacheProvider;
import org.jboss.seam.log.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONStringer;

/**
 * @author Marek Schmidt
 * 
 * TODO: refactor to share code between link and component, which are mostly the same thing (internally even more so)
 * 	     fix the caching issue if the ontology changes (cache per-type list of properties) 
 *
 */
@Name("kiwirdfaAction")
@Scope(ScopeType.CONVERSATION)
@Synchronized
public class KiwirdfaAction implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@In
	CacheProvider cacheProvider;
	
	@Logger
	Log log;
	
	@In
	private ContentItemService contentItemService;
	
	@In(create=true)
	private ContentItem currentContentItem;
	
	@In
	private OntologyService ontologyService;

	@In
	private KiWiEntityManager kiwiEntityManager;

	/*@In
	private TaggingService taggingService;*/
	
	/*@In
	private TripleStore tripleStore;*/
	
	@In(value="editorAction.state")
	private EditorModel state;
	
	@In
	private SolrService solrService;
	
	
	//private ContentItem currentContext;
	private KiWiResource currentContext;
	private String currentContextLabel;
	private List<KiWiResource> currentContextTypes;

		
	//private KiWiProperty property;
	
	@DataModel("possibleProperties")
	List<KiWiProperty> possibleProperties;
	
	
	private KiWiProperty componentRelation;
	private List<KiWiResource> componentTypes;
	private KiWiResource componentResource;
	private KiWiResource oldComponentResource;
	private String componentTitle;
	
	
	private KiWiProperty selectedComponentRelation;
	// private KiWiClass selectedComponentType;
	
	private KiWiProperty linkRelation;
	private String linkObject;
	private ContentItem linkObjectItem;
//	private String linkText;
	
	// private KiWiClass linkType;
	//private ContentItem linkContentItem;
	// private String linkTitle;
	
	private KiWiProperty selectedLinkRelation;
	// private KiWiClass selectedLinkType;
	// private String selectedLinkTitle;

	/*
	private KiWiProperty iterationRelation;
	private KiWiProperty selectedIterationRelation;

	private KiWiProperty iterationItemRelation;
	private KiWiClass iterationItemType;
	// private ContentItem iterationItemContentItem;
	private KiWiResource iterationItemResource;
	private String iterationItemTitle;
	
	private KiWiClass selectedIterationItemType;*/
	
	public String getCurrentContextLabel() {
		/*KiWiResource context = getCurrentContext();
		if (context.getContentItem() != null) {
			return context.getContentItem().getTitle();
		}
		else{
			return context.getKiwiIdentifier();
		}*/
		
		return currentContextLabel;
	}
	
	public List<KiWiResource> getCurrentContextTypes() {
		if (currentContextTypes == null) {
			currentContextTypes = new ArrayList<KiWiResource> (currentContentItem.getTypes());
		}
		
		return currentContextTypes;
	}

	/*
	public KiWiResource getCurrentContext () {
		if (currentContext == null) {
			currentContext = currentContentItem.getResource();
		}
		return currentContext;
	}*/
	
	/*
	public void setCurrentContext (KiWiResource currentContext) {
		this.currentContext = currentContext;
		
		log.info("set current context: #0", this.currentContext);
	}*/
	
	/*
	public void setCurrentContextKiwiId (String kiwiId) {
		log.info("setCurrentContextKiwiId: \"#0\"", kiwiId);
		if ("".equals(kiwiId) || kiwiId == null) {
			this.setCurrentContext(currentContentItem.getResource());
		}
		else {
			this.setCurrentContext(state.getResourceByNestedId(kiwiId));
		}
	}*/
	
	/*
	public KiWiProperty getProperty () {
		return property;
	}
	
	public String getPropertyKiwiId() {
		if (property == null) {
			log.info("getPropertyKiwiId property is null!");
			return null;
		}
		
		return property.getKiwiIdentifier();
	}*/
	
	/*
	public void setPropertyKiwiId(String kiwiId) {
		if (kiwiId == null || "".equals(kiwiId)) {
			property = null;
		}
		else {
			ContentItem propertyCi = contentItemService.getContentItemByKiwiId(kiwiId);
			property = kiwiEntityManager.createFacade(propertyCi, KiWiProperty.class);
		}
	}*/
	
	/**
	 * Sets the currently edited context and the property (from the client)
	 * @param js
	 */
	/*
	public void setPropertyJS (String js) {
		String[] split = js.split(" ", 2);
		setCurrentContextKiwiId(split[0]);
		setPropertyKiwiId(split[1]);
	}*/
	
	/*
	public void setProperty(KiWiProperty property) {
		this.property = property;
	}*/
	
	/**
	 * Creates or updates the component based on selected values in the kiwirdfaAbout form.
	 */
	public void createOrUpdateComponent() {
		
		log.info("create about");
		/*
		// No distinction between creating and updating, just create a new one if none is created yet... 
		if (componentResource == null) {
			componentResource = state.createNestedItem();
		}
		
		// Update the types based on the selection user has made. We need to delete the old type. 
		// We can update the relation in the savelet, but we need to update the type now (so proper properties of the component are immediately available)
		if (selectedComponentType == null || !selectedComponentType.equals(componentType)) {
			
			if (componentType != null) {
				for (KiWiTriple triple : state.getOutgoingTriples(componentResource)) {
					if (triple.isContentBasedOutgoing() != null && triple.isContentBasedOutgoing()) {
						if ((Constants.NS_RDF + "type").equals(triple.getProperty().getUri())) {
							triple.setDeleted(true);
						}
					}
				}
			}
			
			componentType = selectedComponentType;
			
			if (componentType != null) {
				// 	KiWiTriple t = tripleStore.createTriple(componentContentItem.getResource(), Constants.NS_RDF + "type", componentType.getResource());
				KiWiTriple t = new KiWiTriple();
				t.setSubject(componentResource);
				t.setProperty(tripleStore.createUriResourceByNamespaceTitle("rdf", "type"));
				t.setObject(componentType.getResource());
				t.setContentBasedOutgoing(true);
			
				state.getOutgoingTriples(componentResource).add(t);
			}
		}*/
	}
	
	private void generateTypeConstraints(StringBuilder sparqlStr, List<KiWiResource> subjectTypes) {
		int constraints = 0;
			
		for (KiWiResource resource : subjectTypes) {
			if (constraints > 0) {
				sparqlStr.append(" UNION ");
			} 
						
			sparqlStr.append(("{ ?S <" + Constants.NS_RDFS + "domain> " + resource.getSeRQLID() + " . }"));
						
			++constraints;
		}
	}
	
	private List<KiWiProperty> listPropertiesByQuery(String query) {
		// TODO: this will return old data if the ontology changes...
		// is there a simple way to check for that and invalidate just this cache region?
		List<KiWiProperty> ret = (List<KiWiProperty>)cacheProvider.get("kiwirdfaAction.sparql", query);
		if (ret == null) {
			Query q = kiwiEntityManager.createQuery(
				query, 
				SPARQL, KiWiProperty.class);

			ret = q.getResultList();
			
			cacheProvider.put("kiwirdfaAction.sparql", query, ret);
		}
		
		return ret;
	}

	private List<KiWiProperty> listApplicableDatatypeProperties(List<KiWiResource> subjectTypes) {
		StringBuilder sparqlStr = new StringBuilder();
		
		sparqlStr.append("SELECT ?S WHERE { " +
		"{ ?S <" + Constants.NS_RDF + "type> <" + Constants.NS_OWL + "DatatypeProperty> . } { "); // +
		
		generateTypeConstraints (sparqlStr, subjectTypes);
			
		sparqlStr.append("} }");
		
		log.debug("listApplicableDatatypeProperties sparql: ' #0'", sparqlStr);
		String query = sparqlStr.toString();
		
		return listPropertiesByQuery(query);
	}
	
	private List<KiWiProperty> listApplicableObjectProperties(List<KiWiResource> subjectTypes) {
		StringBuilder sparqlStr = new StringBuilder();
		
		sparqlStr.append("SELECT ?S WHERE { " +
		"{ ?S <" + Constants.NS_RDF + "type> <" + Constants.NS_OWL + "ObjectProperty> . } { "); // +
		
		generateTypeConstraints (sparqlStr, subjectTypes);
			
		sparqlStr.append("} }");
		
		log.debug("listApplicableDatatypeProperties sparql: ' #0'", sparqlStr);
		String query = sparqlStr.toString();
		
		return listPropertiesByQuery(query);
	}
	
	public List<KiWiProperty> getPossibleProperties() {	
		// KiWiResource context = getCurrentContext();
		possibleProperties =  listApplicableDatatypeProperties(getCurrentContextTypes());
		
		log.debug("list of possible properties: #0", possibleProperties);
		
		return possibleProperties;
	}
	
	/* TODO: move to OntologyService */ 
	private List<KiWiProperty> listApplicableProperties(List<KiWiResource> subjectTypes) {
		StringBuilder sparqlStr = new StringBuilder();
		sparqlStr.append("SELECT ?S WHERE { " +
		"{ " +
		"{ ?S <" + Constants.NS_RDF + "type> <" + Constants.NS_OWL + "ObjectProperty> . } UNION " +
		"{ ?S <" + Constants.NS_RDF + "type> <" + Constants.NS_OWL + "DatatypeProperty> . } " +
		"} " +
		" { "); 

		generateTypeConstraints (sparqlStr, subjectTypes);
		
		sparqlStr.append("} }");

		log.debug("listApplicableProperties sparql: ' #0'", sparqlStr);
		
		String query = sparqlStr.toString();
		
		return listPropertiesByQuery(query);
	}
	
	List<SelectItem> possibleComponentRelations;
	
	public List<SelectItem> getPossibleComponentRelations() {
		// KiWiResource context = getCurrentContext();
		List<SelectItem> ret = new LinkedList<SelectItem> ();
		for (KiWiProperty property : listApplicableObjectProperties(getCurrentContextTypes())) {
			
			// support subset of OWL semantics. Only one range type is supported.
			// let's just not specify type for any other case... 
			/*if (property.getRange().size() == 1) {
				KiWiClass range = property.getRange().iterator().next();
				SelectItem si = new SelectItem(property.getResource().getKiwiIdentifier() + " " + range.getResource().getKiwiIdentifier(), 
						property.getTitle() + " (" + property.getResource().getNamespacePrefix() + ")" +
						": " + 
						range.getTitle() + " (" + range.getResource().getNamespacePrefix() + ")");
				ret.add(si);
			}
			else {				
				SelectItem si = new SelectItem(property.getResource().getKiwiIdentifier() + " ", 
						property.getTitle() + " (" + property.getResource().getNamespacePrefix() + ")");
				ret.add(si);
			}*/
			
			SelectItem si = new SelectItem(property.getResource().getKiwiIdentifier(), 
					property.getTitle() + " (" + property.getResource().getNamespacePrefix() + ")");
			ret.add(si);
		}
		
		possibleComponentRelations = ret;
		
		return possibleComponentRelations;
	}
	
	public String getSelectedComponentRelation() {
		//StringBuilder sb = new StringBuilder();
		//sb.append(selectedComponentRelation == null ? "" : selectedComponentRelation.getKiwiIdentifier());
		// sb.append(" ");
		// sb.append(selectedComponentType == null ? "" : selectedComponentType.getKiwiIdentifier());
		// return sb.toString();
		
		return selectedComponentRelation == null ? "" : selectedComponentRelation.getKiwiIdentifier();
	}
	
	public void setSelectedComponentRelation(String componentRelation) {
		// log.info("set selected component relation: \"#0\"", componentRelation);
		
		if (componentRelation == null) {
			selectedComponentRelation = null;
			// selectedComponentType = null;
		}
		else {
			ContentItem ci = contentItemService.getContentItemByKiwiId(componentRelation);
			if (ci != null) {
				selectedComponentRelation = kiwiEntityManager.createFacade(ci, KiWiProperty.class);
			}
			else {
				selectedComponentRelation = null;
			}
			/*String[] split = componentRelation.split(" ", 2);
			if ("".equals(split[0])) {
				selectedComponentRelation = null;
			}
			else {
				selectedComponentRelation = kiwiEntityManager.createFacade(
					contentItemService.getContentItemByKiwiId(split[0]),
					KiWiProperty.class);
			}
			
			if ("".equals(split[1])) {
				selectedComponentType = null;
			}
			else {
				selectedComponentType = kiwiEntityManager.createFacade(
						contentItemService.getContentItemByKiwiId(split[1]),
						KiWiClass.class);
			}*/
			
		}	
	}
	
	public KiWiResource getComponentResource() {
		return this.componentResource;
	}
	
	public String getComponentTitle() {
		return this.componentTitle;
	}
	
	public void setComponentTitle(String componentTitle) {
		this.componentTitle = componentTitle;
	}
	
	public String getComponentResourceTitle() {
		String title = state.getTitle(componentResource);
		if (title == null && componentResource != null && componentResource.getId() != null) {
			return componentResource.getContentItem().getTitle(); 
		}
		
		return title;
	}
	
	public void changeComponent() {
		if (this.componentResource != null) {
			this.componentTitle = state.getTitle(componentResource);
			// The componentResource can be a "virtual" resource, or a real one...
			if (componentTitle == null && componentResource.getId() != null) {
				componentTitle = componentResource.getContentItem().getTitle();
			}
			this.componentResource = null;
		}
	}
	
	public void setSuggestedComponent(String kiwiid) {
		componentResource = state.getResourceByNestedId(kiwiid);
		if (componentResource != null) {
			componentTitle = state.getTitle(componentResource);
			// The componentResource can be a "virtual" resource, or a real one...
			if (componentTitle == null && componentResource.getId() != null) {
				componentTitle = componentResource.getContentItem().getTitle();
			}
		}
	}
	
	// public void acceptSuggested
	
	/**
	 * Creates or updates the link based on selected values in the kiwirdfaLink form.
	 */
	public void createOrUpdateLink() {
		
		log.debug("create link");

		/* If we have the link...  
		if (linkContentItem != null) {
			
			// Update the title
			selectedLinkTitle = linkContentItem.getTitle();
		}
		else {
			// We don't have the link, or the link does not link to anywhere... (so this link works only as a template
			selectedLinkTitle = "";
		}*/
	}
	
	/**
	 * Updates the link values based on selected link in the kiwirdfaLinkSelect form. 
	 * 
	 * The kiwirdfaLink form is made for creating and updating the "template" (relation and type), while this kiwirdfaLinkSelect 
	 * form for updating the link target.
	 */ 
	/*
	public void updateLinkSelect () {
		log.info("update link select");
		if (!linkTitle.equals(selectedLinkTitle) || (linkContentItem == null && linkTitle != null && !linkTitle.equals(""))) {
			
			linkContentItem = contentItemService.getContentItemByTitle(linkTitle);
			if (linkContentItem != null) {
				log.info("found content item #0" + linkContentItem.getTitle());
				selectedLinkTitle = linkContentItem.getTitle();
			}
			else {
				log.info("not found content item with a title #0" + linkTitle);
				selectedLinkTitle = "";
			}
		}
		else {
		}
	}*/
	
	List<SelectItem> possibleLinkRelations;
	
	public List<SelectItem> getPossibleLinkRelations() {
		// KiWiResource context = getCurrentContext();
		List<SelectItem> ret = new LinkedList<SelectItem> ();
		
		SelectItem si;
		
		si = new SelectItem("", "links to");
		ret.add(si);
		
		for (KiWiProperty property : listApplicableProperties(getCurrentContextTypes())) {
			si = new SelectItem(property.getResource().getKiwiIdentifier(), 
				property.getTitle() + " (" + property.getResource().getNamespacePrefix() + ")");
			ret.add(si);
		}
		
		possibleLinkRelations = ret;
		
		return possibleLinkRelations;
	}
	
	public String getSelectedLinkRelation() {
		if (selectedLinkRelation == null) {
			return "";
		}
		else {
			return selectedLinkRelation.getKiwiIdentifier();
		}
	}
	
	public boolean isSelectedLinkRelationDatatypeProperty() {
		if (selectedLinkRelation != null) {
			if (selectedLinkRelation.getResource().hasType(Constants.NS_OWL + "DatatypeProperty")) {
				return true;
			}
		}
		
		return false;
	}
	
	private List<ContentItem> propertySuggestions;
	
	public List<ContentItem> getPropertySuggestions() {
		if (propertySuggestions != null) {
			return propertySuggestions;
		}
		
		return Collections.emptyList();
	}
	
	private List<ContentItem> objectSuggestions;
	
	private List<ContentItem> objectTypeSuggestions;
	
	public List<ContentItem> getObjectSuggestions() {
		if (objectSuggestions != null) {
			return objectSuggestions;
		}
		
		return Collections.emptyList();
	}
	
	public void acceptObjectSuggestion(ContentItem ci) {
		linkObjectItem = ci;
		if (ci != null) {
			linkObject = ci.getTitle();
		}
	}
	
	public void acceptPropertySuggestion(ContentItem ci) {
		setSelectedLinkRelation(ci.getKiwiIdentifier());
	}
	
	public List<ContentItem> getObjectTypeSuggestions() {
		if (objectTypeSuggestions != null) {
			return objectTypeSuggestions;
		}
		
		return Collections.emptyList();
	}
	
	public void loadSuggestions() {
		
		propertySuggestions = new LinkedList<ContentItem>();
		objectSuggestions = new LinkedList<ContentItem>();
		objectTypeSuggestions = new LinkedList<ContentItem>();
		
		SuggestionsAction suggestionsAction = (SuggestionsAction)Component.getInstance("ie.suggestions");
		
		for (Suggestion suggestion : suggestionsAction.getSuggestions()) {
			log.debug("iterating #0", suggestion.getLabel());
			if (linkObject.toLowerCase().contains(suggestion.getLabel().toLowerCase())) {
				
				log.debug("labels match");
				
				for (KiWiResource resource : suggestion.getRoles()) {
					ContentItem property = resource.getContentItem();
					if (!propertySuggestions.contains(property)) {
						propertySuggestions.add(property);
						
						log.debug("adding property suggestion #0", property.getTitle());
					}
				}
				
				for (KiWiResource resource : suggestion.getResources()) {
					ContentItem ci = resource.getContentItem();
					if (!objectSuggestions.contains(ci)) {
						objectSuggestions.add(ci);
						log.debug("adding object suggestion #0", ci.getTitle());
					}
				}
				
				for (KiWiResource resource : suggestion.getTypes()) {
					ContentItem ci = resource.getContentItem();
					if (!objectTypeSuggestions.contains(ci)) {
						objectTypeSuggestions.add(ci);
						log.debug("adding object type suggestion #0", ci.getTitle());
					}
				}
			}
		}
		
		loadOntologicalSuggestions();
	}
	
	public void loadOntologicalSuggestions() {
		Collection<KiWiUriResource> objectTypes;
		if (linkObjectItem != null) {
			objectTypes = new LinkedList<KiWiUriResource>();
			for (KiWiResource type : linkObjectItem.getResource().getTypes()) {
				if (type.isUriResource()) {
					objectTypes.add((KiWiUriResource)type);
				}
			}
		}
		else {
			objectTypes = new LinkedList<KiWiUriResource>();
			for (ContentItem type : objectTypeSuggestions) {
				if (type.getResource().isUriResource()) {
					objectTypes.add((KiWiUriResource)type.getResource());
				}
			}
		}
		
		if (objectTypes.size() > 0) {
			// KiWiResource context = getCurrentContext();
			List<KiWiResource> contextTypes = getCurrentContextTypes();
			
			listProperties : for (KiWiProperty property : listApplicableProperties(contextTypes)) {
				
				log.debug("testing property: #0", property.getDelegate().getTitle());
				
				if (property.getDomain().isEmpty() || property.getRange().isEmpty()) {
					// They are just not interesting enough, you can always select them anyway.
					continue;
				}
				
				for (KiWiClass domain : property.getDomain()) {
					if (!contextTypes.contains((KiWiUriResource)domain.getResource())) {
						log.debug("context doesn't have type: #0", domain.getResource());
						continue listProperties;
					}
				}
				
				for (KiWiClass range : property.getRange()) {
					if (!objectTypes.contains((KiWiUriResource)range.getResource())) {
						log.debug("object types doesn't have type: #0", range.getResource());
						continue listProperties;
					}
				}
				
				if (!propertySuggestions.contains(property.getDelegate())) {
					propertySuggestions.add(property.getDelegate());
				}
			}
		}
	}
	
	/**
	 * A selected link relation in a human-readable form
	 * @return
	 */
	/*public String getSelectedLinkRelationTitle() {
		StringBuilder sb = new StringBuilder();
		sb.append(selectedLinkRelation == null ? "" : selectedLinkRelation.getTitle());
		sb.append(" ");
		sb.append(selectedLinkType == null ? "" : selectedLinkType.getTitle());
		return sb.toString();
	}*/
	
	public void setSelectedLinkRelation(String linkRelation) {
		log.debug("set selected link relation: \"#0\"", linkRelation);
		
		if (linkRelation == null || "".equals(linkRelation)) {
			selectedLinkRelation = null;
		}
		else {
			ContentItem ci = contentItemService.getContentItemByKiwiId(linkRelation);
			if (ci != null) {
				selectedLinkRelation = kiwiEntityManager.createFacade(ci, KiWiProperty.class);
			}
			else {
				selectedLinkRelation = null;
			}
		}	
	}
	
	/**
	 * Returns the current component information for javascript
	 * @return "componentKiwiId relationKiwiId classKiwiId" 
	 * @throws JSONException 
	 */
	public String getComponentJS () throws JSONException {

		// Refresh == "true" means the client needs to ask the server to refresh the content to insert the nested item.
		String refresh = oldComponentResource != componentResource && componentResource != null ? "true" : "false";
		String about = componentResource == null ? componentTitle : componentResource.getKiwiIdentifier();
		String rel = selectedComponentRelation == null ? "" : selectedComponentRelation.getKiwiIdentifier();
		// String types = "";
		
		StringBuilder types = new StringBuilder();
		if (componentTypes != null) {
			for (KiWiResource type : componentTypes) {
				if (types.length() > 0) {
					types.append(' ');      
				}
				
				types.append(type.getKiwiIdentifier());
			} 
		}
		
		String ret = (new JSONStringer()).array().value(refresh).value(about).value(rel).value(types.toString()).endArray().toString();
		log.debug("getComponentJS returing \"#0\"", ret);
		return ret;
	}
	
	/**
	 * Returns the current link information for javascript
	 * @return "linkKiwiId relationKiwiId classKiwiId" 
	 * @throws JSONException 
	 */
	public String getLinkJS () throws JSONException {		
		String target = "";
		String rel = "";
		String property = "";
		String content = "";
		String text = ""; 
		
		if (selectedLinkRelation == null) {
			// a non-semantic link
			
			if (linkObjectItem != null) {
				target = linkObjectItem.getKiwiIdentifier();
			}
			else {
				target = linkObject;
			}
		}
		else if (selectedLinkRelation.getResource().hasType(Constants.NS_OWL + "DatatypeProperty")) {
			// datatype property
			property = selectedLinkRelation.getKiwiIdentifier();
			text = linkObject;
		}
		else {
			// object property
			if (linkObjectItem != null) {
				target = linkObjectItem.getKiwiIdentifier();
			}
			else {
				target = linkObject;
			}
			
			rel = selectedLinkRelation.getKiwiIdentifier();
		}
		
		String ret = (new JSONStringer()).array().value(target).value(rel).value(property).value(content).value(text).endArray().toString();
		
		log.debug("getLinkJS returing \"#0\"", ret);
		
		return ret;
	}
	
	public String getLinkObject() {
		return linkObject;
	}

	public void setLinkObject(String linkObject) {
		this.linkObject = linkObject;
	}
	
	public ContentItem getLinkObjectItem() {
		return linkObjectItem;
	}
	
	public void changeObject() {
		if (linkObjectItem != null) {
			linkObject = linkObjectItem.getTitle();
		}
		linkObjectItem = null;
	}
	
	
	public List<String[]> autocompleteFields(String prefix, String[] fields) {
		if(prefix.length() >= 2) {
			KiWiSearchCriteria crit = new KiWiSearchCriteria();
			
			StringBuilder qString = new StringBuilder();
			
			// add prefix to query string
			qString.append("title:"+prefix+"*");
			// qString.append(" ");
						
			crit.setSolrSearchString(qString.toString());
			
			SolrQuery query = solrService.buildSolrQuery(crit);
			query.setStart(0);
			query.setRows(32);
			query.setSortField("title_id", ORDER.asc);

			for (String field : fields) {
				query.addField(field);
			}
			
			QueryResponse rsp = solrService.search(query);
			
			List<String[]> result = new LinkedList<String[]>();
			
			if(rsp != null) {
				SolrDocumentList docs = rsp.getResults();
			
				for(SolrDocument doc : docs) {
					String[] row = new String[fields.length];
					for (int i = 0; i < fields.length; ++i) {
						row[i] = doc.getFieldValue(fields[i]).toString();
					}
					result.add(row);
				}
			}
			return result;
		} else {
			return Collections.emptyList();
		}
	}
	
	public List<String[]> titlesAutocomplete(Object prefix) {		
		return autocompleteFields(prefix.toString().toLowerCase(), new String[] {"title", "uri", "kiwiid"});
	}
	
	public void setSuggestedObject(String kiwiid) {
		linkObjectItem = contentItemService.getContentItemByKiwiId(kiwiid);
		if (linkObjectItem != null) {
			linkObject = linkObjectItem.getTitle();
		}
	}

	/**
	 * Sets the currently edited component values. Updates the selected values to those new component values.
	 * @param componentJS "contextKiwiId componentKiwiId relationKiwiId classKiwiId"
	 * @throws JSONException 
	 */
	public void setComponentJS (String componentJS) throws JSONException {
		
		log.info("setCompomentJS \"#0\"", componentJS);
		
		if (componentJS.startsWith("\"")) {
			// hack to fix the doubly-jsoned string
			componentJS = (new JSONArray("[" + componentJS + "]")).getString(0);
		}
		
		JSONArray array = new JSONArray(componentJS);

		if (array.length() != 6) {
			log.info("setLinksJS wrong format: #0", componentJS);
			return;
		}
		
		String contextId = array.getString(0);
		String contextTypes = array.getString(1);
		String fragmentId = array.getString(2);
		String componentId = array.getString(3);
		String componentRel = array.getString(4);
		String componentTypes = array.getString(5);
		
		if (contextId == null || "".equals(contextId)) {
			currentContextLabel = currentContentItem.getTitle();
			currentContextTypes = new ArrayList<KiWiResource>(
					currentContentItem.getTypes());
		} else {
			if (contextId.startsWith("bnode::")
					|| contextId.startsWith("uri::")) {
				ContentItem ci = contentItemService
						.getContentItemByKiwiId(contextId);
				if (ci != null) {
					currentContextLabel = ci.getTitle();
				} else {
					currentContextLabel = contextId;
				}
			} else {
				currentContextLabel = contextId;
			}

			if (contextTypes == null) {
				currentContextTypes = Collections.emptyList();
			} else {
				currentContextTypes = new LinkedList<KiWiResource>();
				String[] types = contextTypes.split(" ");
				for (String type : types) {
					ContentItem ci_type = contentItemService
							.getContentItemByKiwiId(type);
					if (ci_type != null) {
						currentContextTypes.add(ci_type.getResource());
					}
				}
			}
		}
		
		// setCurrentContextKiwiId(contextId);
		
		if (contextId == null || "".equals(contextId)) {
			currentContextLabel = currentContentItem.getTitle();
			currentContextTypes = new ArrayList<KiWiResource>(
					currentContentItem.getTypes());
		} else {
			if (contextId.startsWith("bnode::")
					|| contextId.startsWith("uri::")) {
				ContentItem ci = contentItemService
						.getContentItemByKiwiId(contextId);
				if (ci != null) {
					currentContextLabel = ci.getTitle();
				} else {
					currentContextLabel = contextId;
				}
			} else {
				currentContextLabel = contextId;
			}

			if (contextTypes == null) {
				currentContextTypes = Collections.emptyList();
			} else {
				currentContextTypes = new LinkedList<KiWiResource>();
				String[] types = contextTypes.split(" ");
				for (String type : types) {
					ContentItem ci_type = contentItemService
							.getContentItemByKiwiId(type);
					if (ci_type != null) {
						currentContextTypes.add(ci_type.getResource());
					}
				}
			}
		}
		
		
		if (componentId == null) {
			componentResource = null;
			componentTitle = "";
		}
		else {
			if (componentId.startsWith("bnode::") || componentId.startsWith("uri::")) {
				componentResource = state.getResourceByNestedId(componentId);
				componentTitle = state.getTitle(componentResource);
				if (componentTitle == null && componentResource.getId() != null) {
					componentTitle = componentResource.getContentItem().getTitle();
				}
			}
			else {
				componentResource = null;
				componentTitle = componentId;
			}
		}
		
		if (componentRel == null || "".equals(componentRel)) {
			componentRelation = null;
		}
		else {
			ContentItem ci = contentItemService.getContentItemByKiwiId(componentRel);
			if (ci != null) {
				componentRelation = kiwiEntityManager.createFacade(ci, KiWiProperty.class);
			}
			else {
				componentRelation = null;
			}
		}

		/*
		if ("".equals(split[3])) {
			componentType = null;
		}
		else {
			componentType = kiwiEntityManager.createFacade(
					contentItemService.getContentItemByKiwiId(split[3]),
					KiWiClass.class);
		}*/
		
		selectedComponentRelation = componentRelation;
		
		oldComponentResource = componentResource;
		               
		if (componentTypes == null) {
			this.componentTypes = Collections.emptyList();
		} else {
			this.componentTypes = new LinkedList<KiWiResource>();
			String[] types = contextTypes.split(" ");
			for (String type : types) {
				ContentItem ci_type = contentItemService
						.getContentItemByKiwiId(type);
				if (ci_type != null) {
					this.componentTypes.add(ci_type.getResource());
				}
			}
		}
		
		if (!"".equals(fragmentId) && "".equals(componentTitle)) {
			// set the component title to the suggestion label.
			Suggestion suggestion = state.getSuggestion(fragmentId);
			if (suggestion != null) {
				componentTitle = suggestion.getLabel();
				
				// add the suggestion types
				for (KiWiResource type : suggestion.getTypes()) {
					if (!this.componentTypes.contains(type)) {
						this.componentTypes.add(type);
					}
				}
			}
		}

		// selectedComponentType = componentType;
	}
	
	/**
	 * Sets the currently edited link values. Updates the selected values to those new link values.
	 * @param linkJS "contextKiwiId linkKiwiId relationKiwiId classKiwiId title"
	 * @throws JSONException 
	 */
	public void setLinkJS (String linkJS) throws JSONException {
		
		log.info("setLinkJS: #0", linkJS);
		
		if (linkJS.startsWith("\"")) {
			// hack to fix the doubly-jsoned string
			linkJS = (new JSONArray("[" + linkJS + "]")).getString(0);
		}
		
		JSONArray array = new JSONArray(linkJS);

		if (array.length() != 8) {
			log.error("setLinksJS wrong format: #0", linkJS);
			return;
		}
	
		String contextId = array.getString(0);
		String contextTypes = array.getString(1);
		String suggestionId = array.getString(2);
		String target = array.getString(3);
		String rel = array.getString(4);
		String property = array.getString(5);
		String content = array.getString(6);
		String text = array.getString(7);
		
		// setCurrentContextKiwiId(array.getString(0));
		
		if (contextId == null || "".equals(contextId)) {
			currentContextLabel = currentContentItem.getTitle();
			currentContextTypes = new ArrayList<KiWiResource>(
					currentContentItem.getTypes());
		} else {
			if (contextId.startsWith("bnode::")
					|| contextId.startsWith("uri::")) {
				ContentItem ci = contentItemService
						.getContentItemByKiwiId(contextId);
				if (ci != null) {
					currentContextLabel = ci.getTitle();
				} else {
					currentContextLabel = contextId;
				}
			} else {
				currentContextLabel = contextId;
			}

			if (contextTypes == null) {
				currentContextTypes = Collections.emptyList();
			} else {
				currentContextTypes = new LinkedList<KiWiResource>();
				String[] types = contextTypes.split(" ");
				for (String type : types) {
					ContentItem ci_type = contentItemService
							.getContentItemByKiwiId(type);
					if (ci_type != null) {
						currentContextTypes.add(ci_type.getResource());
					}
				}
			}
		}
		
		linkObject = null;
		linkObjectItem = null;
		if (target.startsWith("uri::") || target.startsWith("bnode::")) {
			// It is a kiwiid, try to get the actual resource.
			linkObjectItem = contentItemService.getContentItemByKiwiId(target);
			if (linkObjectItem != null) {
				linkObject = linkObjectItem.getTitle();
			}
		}
		
		if ("".equals(rel) && "".equals(property)) {
			// non-semantic link
			linkRelation = null;
			
			if (linkObject == null) {
				if ("".equals(target)) {
					linkObject = text;
				}
				else {
					linkObject = target;
				}
			}
		}
		else if (!"".equals(rel)) {
			ContentItem ci = contentItemService.getContentItemByKiwiId(rel);
			if (ci != null) {
				linkRelation = kiwiEntityManager.createFacade(ci, KiWiProperty.class);
			}
			else {
				linkRelation = null;
			}
			
			if (linkObject == null) {
				if ("".equals(target)) {
					linkObject = text;
				}
				else {
					linkObject = target;
				}
			}
		}
		else {
			ContentItem ci = contentItemService.getContentItemByKiwiId(property);
			if (ci != null) {
				linkRelation = kiwiEntityManager.createFacade(ci, KiWiProperty.class);
			}
			else {
				linkRelation = null;
			}

			if (linkObject == null) {
				if ("".equals(content)) {
					linkObject = text;
				}
				else {
					linkObject = content;
				}
			}
		}
		
		selectedLinkRelation = linkRelation;
		
		loadSuggestions();
	}
	
	/*
	public String getLinkTitle () {
		return linkTitle;
	}
	
	public void setLinkTitle (String linkTitle) {
		this.linkTitle = linkTitle;
	}*/

	/*
	public void setIterationJS(String iterationJS) {
		log.info("setIterationJS \"#0\"", iterationJS);
		
		String[] split = iterationJS.split(" ", 2);
		if (split.length != 2) {
			log.info("setIterationJS wrong format");
			return;
		}
		
		setCurrentContextKiwiId(split[0]);
		
		if ("".equals(split[1])) {
			iterationRelation = null;
		}
		else {
			ContentItem ci = contentItemService.getContentItemByKiwiId(split[1]);
			if (ci != null) {
				iterationRelation = kiwiEntityManager.createFacade(ci, KiWiProperty.class);
			}
			else {
				iterationRelation = null;
			}
		}
		
		selectedIterationRelation = iterationRelation;
	}

	public String getIterationJS() {
		StringBuilder sb = new StringBuilder();
		sb.append(iterationRelation == null ? "" : iterationRelation.getKiwiIdentifier());
		
		log.info("getIterationJS returing \"#0\"", sb.toString());
		
		return sb.toString();
	}

	public void createOrUpdateIteration() {
		log.info("create iteration");

		iterationRelation = selectedIterationRelation;
	}

	public String getSelectedIterationRelation() {
		StringBuilder sb = new StringBuilder();
		sb.append(selectedIterationRelation == null ? "" : selectedIterationRelation.getKiwiIdentifier());
		return sb.toString();
	}
	
	public void setSelectedIterationRelation(String relation) {
		log.info("set selected iteration relation: \"#0\"", relation);
		
		if (relation == null) {
			selectedIterationRelation = null;
		}
		else {
			selectedIterationRelation = kiwiEntityManager.createFacade(
					contentItemService.getContentItemByKiwiId(relation),
					KiWiProperty.class);
		}	
	}
	*/

	/*
	private List<SelectItem> possibleIterationRelations;

	public List<SelectItem> getPossibleIterationRelations() {
		KiWiResource context = getCurrentContext();
		List<SelectItem> ret = new LinkedList<SelectItem> ();
		for (KiWiProperty property : listApplicableObjectProperties(context)) {
			SelectItem si = new SelectItem(property.getResource().getKiwiIdentifier(), 
				property.getTitle() + " (" + property.getResource().getNamespacePrefix() + ")");

			ret.add (si);
		}
		
		possibleIterationRelations = ret;
		
		return possibleIterationRelations;
	}*/

/*
	public void setIterationItemJS(String iterationItemJS) {
		log.info("setIterationItemJS \"#0\"", iterationItemJS);
		
		String[] split = iterationItemJS.split(" ", 5);
		if (split.length != 5) {
			log.info("setIterationItemJS wrong format");
			return;
		}
		
		setCurrentContextKiwiId(split[0]);
	
		if ("".equals(split[1])) {
			iterationItemResource = null;
		}
		else {
			iterationItemResource = state.getResourceByNestedId(split[1]); //contentItemService.getContentItemByKiwiId(split[1]);
		}
		
		if ("".equals(split[2])) {
			iterationItemRelation = null;
		}
		else {
			iterationItemRelation = kiwiEntityManager.createFacade(
					contentItemService.getContentItemByKiwiId(split[2]),
					KiWiProperty.class);
		}
		
		if ("".equals(split[3])) {
			iterationItemType = null;
		}
		else {
			iterationItemType = kiwiEntityManager.createFacade(
					contentItemService.getContentItemByKiwiId(split[3]),
					KiWiClass.class);
		}

		iterationItemTitle = split[4];
		
		//selectedIterationItemRelation = iterationItemRelation;
		selectedIterationItemType = iterationItemType;
	}*/

	/*
	public String getIterationItemJS() {
		StringBuilder sb = new StringBuilder();
		sb.append(iterationItemResource == null ? "" : iterationItemResource.getKiwiIdentifier());
		sb.append(" ");
		sb.append(iterationItemRelation == null ? "" : iterationItemRelation.getKiwiIdentifier());
		sb.append(" ");
		sb.append(iterationItemType == null ? "" : iterationItemType.getKiwiIdentifier());
		sb.append(" ");
		sb.append(iterationItemTitle);
		log.info("getIterationItemJS returing \"#0\"", sb.toString());
		
		return sb.toString();

	}*/

	/*
	public void createOrUpdateIterationItem() {
		log.info("create iteration item");

		if (iterationItemResource == null) {
			iterationItemResource = state.createNestedItem(); //contentItemService.createContentItem();
		}
		
		if (selectedIterationItemType == null || !selectedIterationItemType.equals(iterationItemType)) {	
			if (iterationItemType != null) {
				for (KiWiTriple triple : state.getOutgoingTriples(iterationItemResource)) {
					if (triple.isContentBasedOutgoing() != null && triple.isContentBasedOutgoing()) {
						if ((Constants.NS_RDF + "type").equals(triple.getProperty().getUri())) {
							triple.setDeleted(true);
						}
					}
				}
			}
			
			iterationItemType = selectedIterationItemType;
			
			if (iterationItemType != null) {
				// 	KiWiTriple t = tripleStore.createTriple(componentContentItem.getResource(), Constants.NS_RDF + "type", componentType.getResource());
				KiWiTriple t = new KiWiTriple();
				t.setSubject(iterationItemResource);
				t.setProperty(tripleStore.createUriResourceByNamespaceTitle("rdf", "type"));
				t.setObject(iterationItemType.getResource());
				t.setContentBasedOutgoing(true);
			
				state.getOutgoingTriples(iterationItemResource).add(t);
			}
		}
		
		// contentItemService.saveContentItem(iterationItemContentItem);
	}*/

	/*
	public String getSelectedIterationItemType() {
		StringBuilder sb = new StringBuilder();
		sb.append(selectedIterationItemType == null ? "" : selectedIterationItemType.getKiwiIdentifier());
		return sb.toString();
	}
	
	public void setSelectedIterationItemType(String type) {
		log.info("set selected iteration item type: \"#0\"", type);
		
		if (type == null) {
			selectedIterationItemType = null;
		}
		else {
			selectedIterationItemType = kiwiEntityManager.createFacade(
					contentItemService.getContentItemByKiwiId(type),
					KiWiClass.class);
		}	
	}*/
	
/*
	private List<SelectItem> possibleIterationItemTypes;

	public List<SelectItem> getPossibleIterationItemTypes() {
		// ContentItem context = getCurrentContext();
		List<SelectItem> ret = new LinkedList<SelectItem> ();

		if (iterationItemRelation == null) {
			log.info("getPossibleIterationItemTypes: no predicate to select range from!");
			return ret;
		}

		for (KiWiClass range : iterationItemRelation.getRange()) {
			SelectItem si = new SelectItem(range.getResource().getKiwiIdentifier(), 
				range.getTitle() + " (" + range.getResource().getNamespacePrefix() + ")");
			ret.add(si);
		}
		
		possibleIterationItemTypes = ret;
		
		return possibleIterationItemTypes;
	}*/
	
	/** 
	 * This actions can potentially modify the property, like converting the URI or something... 
	 */
	public void create () {
		log.info("create");
	}
	
	public void cancel () {
		log.info("cancel");
	}
	
	public void delete() {
		log.info("delete");
	}
	
	public void cancelComponent() {
		log.info("cancelComponent");
	}
	
	public void deleteComponent() {
		log.info("deleteComponent");
	}
	
	public void cancelLink() {
		log.info("cancelLink");
	}
	
	public void deleteLink() {
		log.info("deleteLink");
	}
	
/*	public void cancelLinkSelect() {
		log.info("cancel link select");
	}

	public void cancelIteration() {
		log.info("cancel iteration");
	}

	public void deleteIteration() {
		log.info("delete iteration");
	}

	public void cancelIterationItem() {
		log.info("cancel iteration item");
	}

	public void deleteIterationItem() {
		log.info("delete iteration item");
	}*/
}
