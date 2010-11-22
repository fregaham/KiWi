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

package kiwi.service.ontology;

import static kiwi.model.kbase.KiWiQueryLanguage.SPARQL;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import kiwi.api.entity.KiWiEntityManager;
import kiwi.api.ontology.OntologyServiceLocal;
import kiwi.api.ontology.OntologyServiceRemote;
import kiwi.api.search.KiWiSearchCriteria;
import kiwi.api.search.SolrService;
import kiwi.api.triplestore.TripleStore;
import kiwi.exception.NamespaceResolvingException;
import kiwi.model.Constants;
import kiwi.model.content.ContentItem;
import kiwi.model.kbase.KiWiResource;
import kiwi.model.kbase.KiWiTriple;
import kiwi.model.kbase.KiWiUriResource;
import kiwi.model.ontology.KiWiClass;
import kiwi.model.ontology.KiWiProperty;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.log.Log;

/**
 * @author Sebastian Schaffert
 *
 */
@Stateless
@Name("ontologyService")
@Scope(ScopeType.STATELESS)
@AutoCreate
public class OntologyServiceImpl implements OntologyServiceLocal, OntologyServiceRemote {

	/** Inject the Seam logger for logging. */
	@Logger
	private Log log;
	
	/** Inject the KiWiEntityManager for querying. */
	@In
	private KiWiEntityManager kiwiEntityManager;
	
	/** For direct HQL queries */
	@In
	private EntityManager entityManager;
	
	/** for class autocompletion */
	@In
	private SolrService solrService;
	
	@In
	private TripleStore tripleStore;
	
	/**
	 * List all classes that are defined in the KiWi system.
	 * 
	 * @return a list of KiWiClass objects representing the classes in the KiWi system
	 * @see kiwi.api.ontology.OntologyService#listClasses()
	 */
	public List<KiWiClass> listClasses() {
//		Query query = kiwiEntityManager.createQuery(
//				"SELECT ?S WHERE { " +
//				"{ ?S <" + Constants.NS_RDF + "type> <" + Constants.NS_OWL + "Class> } " +
//				" UNION " +
//				"{ ?S <" + Constants.NS_RDF + "type> <" + Constants.NS_RDFS + "Class> } }", 
//				SPARQL, KiWiClass.class);
		List<KiWiClass> classes = new LinkedList<KiWiClass>();
		
		Query q = entityManager.createNamedQuery("ontologyService.listClasses");
		q.setHint("org.hibernate.cacheable", true);

		Set setItems = new LinkedHashSet(kiwiEntityManager.createFacadeList(q.getResultList(),KiWiClass.class,false));		
		classes.addAll(setItems);
		
		return classes;
	}

	/**
	 * List all properties that are defined in the KiWi system.
	 * 
	 * @return a list of KiWiProperty objects representing the properties in the KiWi system
	 * @see kiwi.api.ontology.OntologyService#listProperties()
	 */
	public List<KiWiProperty> listProperties() {
		Query query = kiwiEntityManager.createQuery(
				"SELECT ?S WHERE { " +
				"{ ?S <" + Constants.NS_RDF + "type> <" + Constants.NS_OWL + "ObjectProperty> } " +
				" UNION " +
				"{ ?S <" + Constants.NS_RDF + "type> <" + Constants.NS_OWL + "AnnotationProperty> } " +
				" UNION " +
				"{ ?S <" + Constants.NS_RDF + "type> <" + Constants.NS_OWL + "DatatypeProperty> } " +
				" UNION " +
				"{ ?S <" + Constants.NS_RDF + "type> <" + Constants.NS_RDFS + "Property> } }", 
				SPARQL, KiWiProperty.class);
		return query.getResultList();
	}
	
	/**
	 * List all properties that are defined in the KiWi system.
	 * 
	 * @return a list of KiWiProperty objects representing the properties in the KiWi system
	 * @see kiwi.api.ontology.OntologyService#listProperties()
	 */
	public List<KiWiProperty> listDatatypeProperties() {
		Query query = kiwiEntityManager.createQuery(
				"SELECT ?S WHERE { " +
				"{ ?S <" + Constants.NS_RDF + "type> <" + Constants.NS_OWL + "AnnotationProperty> } " +
				" UNION " +
				"{ ?S <" + Constants.NS_RDF + "type> <" + Constants.NS_OWL + "DatatypeProperty> } }", 
				SPARQL, KiWiProperty.class);
		return query.getResultList();
	}
	
	

	/**
	 * List the properties that currently exist between a given subject and object. 
	 * @param subject the subject of the relation
	 * @param object the object of the relation
	 * @return the list of properties that are used in relations between subject and object
	 */
	@Override
	public List<KiWiProperty> listExistingProperties(KiWiResource subject, KiWiResource object) {
		Query query = kiwiEntityManager.createQuery(
				"SELECT ?P WHERE { " + subject.getSeRQLID() + " ?P " + object.getSeRQLID() + " } ",
				SPARQL, KiWiProperty.class);
		return query.getResultList();
	}

	/**
	 * List the properties that are applicable between a given subject and object. Whether a
	 * property is applicable is decided based on the domain and range of the property: if one of 
	 * the types of the subject is in the range, and one of the types of the object is in the 
	 * domain, the property is applicable.
	 * 
	 * @param subject the subject to check
	 * @param object the object to check
	 * @return a list of applicable properties
	 * @see kiwi.api.ontology.OntologyService#listApplicableProperties(kiwi.model.content.ContentItem, kiwi.model.content.ContentItem)
	 */
	public List<KiWiProperty> listApplicableProperties(KiWiResource subject, KiWiResource object) {
		String sparqlStr = "SELECT ?S WHERE { " +
		"{ ?S <" + Constants.NS_RDF + "type> <" + Constants.NS_OWL + "ObjectProperty> . " +
		"  ?S <" + Constants.NS_RDFS + "domain> ?D . " +
		"  ?S <" + Constants.NS_RDFS + "range> ?R . " +
		"  "+ subject.getSeRQLID()+" <" + Constants.NS_RDF + "type> ?D . " +
		"  "+ object.getSeRQLID()+" <" + Constants.NS_RDF + "type> ?R " +				
		"} } ";
		log.info("listApplicableProperties sparql: ' #0'", sparqlStr);
		Query query = kiwiEntityManager.createQuery(
				sparqlStr, 
				SPARQL, KiWiProperty.class);
		return query.getResultList();
	}

	/**
	 * List the datatype properties that are applicable for a given subject. Whether a property is
	 * applicable is decided based on the range of the property: if one of the types of the subject
	 * is in the range, the property is applicable. 
	 * <p>
	 * This method currently ignores the domain of the property. In the future, OntologyService
	 * could provide a method that takes into account the actual datatype.
	 * 
	 * @param subject the subject for which the properties are supposed to be listed
	 * @return a list of KiWiProperty facades that are applicable
	 */
	public List<KiWiProperty> listApplicableDataTypeProperties(KiWiResource subject) {
		Query query = kiwiEntityManager.createQuery(
				"SELECT ?S WHERE { " +
				"{ ?S <" + Constants.NS_RDF + "type> <" + Constants.NS_OWL + "DatatypeProperty> . " +
				"  ?S <" + Constants.NS_RDFS + "domain> ?D . " +
				"  "+ subject.getSeRQLID()+" <" + Constants.NS_RDF + "type> ?D  " +
				"} } ", 
				SPARQL, KiWiProperty.class);
		return query.getResultList();
	}

	
	/**
	 * Provide autocompletion of content items that are of type "owl:Class" or "rdfs:Class" and 
	 * whose title starts with "prefix". Autocompletion is delegated to the SOLR search service.
	 * @param prefix
	 * @return
	 */
	public List<KiWiResource> autocomplete(String prefix) {
		if(prefix.length() >= 2) {
			StringBuilder qString = new StringBuilder();
			
			// add prefix to query string
			qString.append("title:"+prefix.toLowerCase()+"*");
			qString.append(" ");
			
			// add (type:kiwi:Tag OR type:skos:Concept)
			qString.append("(");
			qString.append("type:\"uri::"+Constants.NS_OWL+"Class\"");
			qString.append(" OR ");
			qString.append("type:\"uri::"+Constants.NS_RDFS+"Class\"");
			qString.append(")");
	
			return autocompleteInternal(qString.toString());
		} else {
			return Collections.emptyList();
		}
	}
	
	
	
	/* (non-Javadoc)
	 * @see kiwi.api.ontology.OntologyService#autocompleteProperties(java.lang.String)
	 * 
	 * TODO: autocomplete could be more "intelligent" if we know more about the reason
	 *       we autocomplete ...
	 */
	@Override
	public List<KiWiResource> autocompleteProperties(String prefix) {
		if(prefix.length() >= 2) {
			StringBuilder qString = new StringBuilder();
			
			// add prefix to query string
			qString.append("title:"+prefix.toLowerCase()+"*");
			qString.append(" ");
			
			// add (type:kiwi:Tag OR type:skos:Concept)
			qString.append("(");
			qString.append("type:\"uri::"+Constants.NS_OWL+"ObjectProperty\"");
			qString.append(" OR ");
			qString.append("type:\"uri::"+Constants.NS_OWL+"DatatypeProperty\"");
			qString.append(" OR ");
			qString.append("type:\"uri::"+Constants.NS_RDF+"Property\"");
			qString.append(")");
	
			return autocompleteInternal(qString.toString());
		} else {
			return Collections.emptyList();
		}
	}

	private List<KiWiResource> autocompleteInternal(String qString) {
		
			KiWiSearchCriteria crit = new KiWiSearchCriteria();
			
			crit.setSolrSearchString(qString.toString());
			
			SolrQuery query = solrService.buildSolrQuery(crit);
			query.setStart(0);
			query.setRows(Integer.MAX_VALUE);
			query.setSortField("title_id", ORDER.asc);
			
			query.setFields("title","id");
			
			QueryResponse rsp = solrService.search(query);
			
			if(rsp == null){
				log.error("autocomplete: solr delivered null for the query #0", 
						qString.toString());
				return Collections.emptyList();
			}
			SolrDocumentList docs = rsp.getResults();
			
			// a resourceMapping for KiWiResourceConverter
			HashMap<String,KiWiResource> resourceMappings = new HashMap<String, KiWiResource>();
			List<KiWiResource> result = new LinkedList<KiWiResource>();
			for(SolrDocument doc : docs) {
				ContentItem item = (ContentItem)entityManager.find(ContentItem.class, Long.parseLong((String)doc.getFieldValue("id")));
				if(item != null) {
					result.add(item.getResource());
					
					String label = item.getResource().getLabel() + " (" + item.getResource().getNamespacePrefix() + ")";
					resourceMappings.put(label, item.getResource());
				}
			}
			
			Contexts.getPageContext().set("resourceMappings", resourceMappings);
			
			Collections.sort(result, KiWiResource.LabelComparator.getInstance());
			return result;
	}
	
	/**
	 * List all properties of a specific name 
	 * @param name
	 * @return a list of KiWi properties
	 */
	public List<KiWiProperty> listDatatypePropertiesByName( String name ) {
		Query query = kiwiEntityManager.createQuery(
				"SELECT ?S WHERE { " +
				"?S <"+Constants.NS_KIWI_CORE+"title> \""+name+"\" . " +
				"{{ ?S <" + Constants.NS_RDF + "type> <" + Constants.NS_OWL + "AnnotationProperty> } " +
				" UNION " +
				"{ ?S <" + Constants.NS_RDF + "type> <" + Constants.NS_OWL + "DatatypeProperty> }} "+
				"}", 
				SPARQL, KiWiProperty.class);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see kiwi.api.ontology.OntologyService#listSuperClasses(kiwi.model.kbase.KiWiResource)
	 */
	@Override
	@BypassInterceptors
	public List<KiWiResource> listSuperClasses(KiWiResource r, boolean inferred) {
		return listGenericRDFSOutgoing(r, inferred, "subClassOf", "superclassTripleIds");
	}
	
	
	

	/* (non-Javadoc)
	 * @see kiwi.api.ontology.OntologyService#addSuperClass(kiwi.model.kbase.KiWiResource, kiwi.model.kbase.KiWiResource)
	 */
	@Override
	public void addSuperClass(KiWiResource cls, KiWiResource supercls) {
		KiWiUriResource p = tripleStore.createUriResource(Constants.NS_RDFS+"subClassOf");
		
		tripleStore.createTriple(cls, p, supercls);
	}

	/* (non-Javadoc)
	 * @see kiwi.api.ontology.OntologyService#removeSuperClass(kiwi.model.kbase.KiWiResource, kiwi.model.kbase.KiWiResource)
	 */
	@Override
	public void removeSuperClass(KiWiResource cls, KiWiResource supercls) {
		KiWiUriResource p = tripleStore.createUriResource(Constants.NS_RDFS+"subClassOf");
		
		tripleStore.removeTriple(cls, p, supercls);
	}

	/* (non-Javadoc)
	 * @see kiwi.api.ontology.OntologyService#listSubClasses(kiwi.model.kbase.KiWiResource)
	 */
	@Override
	@BypassInterceptors
	public List<KiWiResource> listSubClasses(KiWiResource r, boolean inferred) {
		List<KiWiResource> result = new LinkedList<KiWiResource>();
		
		HashMap<KiWiResource,Long> tripleIds = new HashMap<KiWiResource, Long>();
		try {
			for(KiWiTriple t : r.listIncoming(Constants.NS_RDFS+"subClassOf", 100)) {
				if( t.isInferred() == inferred ) {
					result.add(t.getSubject());
					
					if(t.isInferred()) {
						tripleIds.put(t.getSubject(),t.getId());
					}
				}
			}
		} catch (NamespaceResolvingException e) {
			log.error("error while resolving namespace:",e);
		}
		
		// outject triple ids in page scope for the UI to access (e.g. for giving explanations)
		Contexts.getPageContext().set("subclassTripleIds", tripleIds);
		
		Collections.sort(result, KiWiResource.LabelComparator.getInstance());
		
		return result;
	}

	/* (non-Javadoc)
	 * @see kiwi.api.ontology.OntologyService#listInstances(kiwi.model.kbase.KiWiResource)
	 */
	@Override
	@BypassInterceptors
	public List<KiWiResource> listInstances(KiWiResource r, boolean inferred) {
		List<KiWiResource> result = new LinkedList<KiWiResource>();
		
		HashMap<KiWiResource,Long> tripleIds = new HashMap<KiWiResource, Long>();
		
		try {
			for(KiWiTriple t : r.listIncoming(Constants.NS_RDF+"type",100)) {
				if( t.isInferred() == inferred && 
					(t.getObject().isUriResource() || t.getObject().isAnonymousResource())) {
					result.add(t.getSubject());
					
					if(t.isInferred()) {
						tripleIds.put(t.getSubject(),t.getId());
					}
				}
			}
		} catch (NamespaceResolvingException e) {
			log.error("error while resolving namespace:",e);
		}
		
		// outject triple ids in page scope for the UI to access (e.g. for giving explanations)
		Contexts.getPageContext().set("instanceTripleIds", tripleIds);
		
		
		Collections.sort(result, KiWiResource.LabelComparator.getInstance());
		
		return result;
	}

	/* (non-Javadoc)
	 * @see kiwi.api.ontology.OntologyService#addInstance(kiwi.model.kbase.KiWiResource, kiwi.model.kbase.KiWiResource)
	 */
	@Override
	@BypassInterceptors
	public void addInstance(KiWiResource cls, KiWiResource instance) {
		instance.addType(cls);
	}

	/* (non-Javadoc)
	 * @see kiwi.api.ontology.OntologyService#removeInstance(kiwi.model.kbase.KiWiResource, kiwi.model.kbase.KiWiResource)
	 */
	@Override
	@BypassInterceptors
	public void removeInstance(KiWiResource cls, KiWiResource instance) {
		instance.removeType(cls);
	}

	/* (non-Javadoc)
	 * @see kiwi.api.ontology.OntologyService#listSubProperties(kiwi.model.kbase.KiWiResource, boolean)
	 */
	@Override
	public List<KiWiResource> listSubProperties(KiWiResource p, boolean inferred) {
		List<KiWiResource> result = new LinkedList<KiWiResource>();
		
		HashMap<KiWiResource,Long> tripleIds = new HashMap<KiWiResource, Long>();
		
		try {
			for(KiWiTriple t : p.listIncoming(Constants.NS_RDFS+"subPropertyOf",100)) {
				if( t.isInferred() == inferred && 
					(t.getObject().isUriResource() || t.getObject().isAnonymousResource())) {
					result.add(t.getSubject());
					
					if(t.isInferred()) {
						tripleIds.put(t.getSubject(),t.getId());
					}
				}
			}
		} catch (NamespaceResolvingException e) {
			log.error("error while resolving namespace:",e);
		}
		
		// outject triple ids in page scope for the UI to access (e.g. for giving explanations)
		Contexts.getPageContext().set("subpropertyTripleIds", tripleIds);
		
		
		Collections.sort(result, KiWiResource.LabelComparator.getInstance());
		
		return result;
	}

	/* (non-Javadoc)
	 * @see kiwi.api.ontology.OntologyService#addSubProperty(kiwi.model.kbase.KiWiResource, kiwi.model.kbase.KiWiResource)
	 */
	@Override
	public void addSubProperty(KiWiResource property, KiWiResource subproperty) {
		addSuperProperty(subproperty, property);
	}

	/* (non-Javadoc)
	 * @see kiwi.api.ontology.OntologyService#removeSubProperty(kiwi.model.kbase.KiWiResource, kiwi.model.kbase.KiWiResource)
	 */
	@Override
	public void removeSubProperty(KiWiResource property, KiWiResource subproperty) {
		removeSuperProperty(subproperty,property);
	}

	/* (non-Javadoc)
	 * @see kiwi.api.ontology.OntologyService#listSuperProperties(kiwi.model.kbase.KiWiResource, boolean)
	 */
	@Override
	@BypassInterceptors
	public List<KiWiResource> listSuperProperties(KiWiResource p, boolean inferred) {
		return listGenericRDFSOutgoing(p, inferred, "subPropertyOf", "superpropertyTripleIds");
	}

	/* (non-Javadoc)
	 * @see kiwi.api.ontology.OntologyService#addSuperProperty(kiwi.model.kbase.KiWiResource, kiwi.model.kbase.KiWiResource)
	 */
	@Override
	public void addSuperProperty(KiWiResource property, KiWiResource superproperty) {
		KiWiUriResource p = tripleStore.createUriResource(Constants.NS_RDFS+"subPropertyOf");
		
		tripleStore.createTriple(property, p, superproperty);
	}

	/* (non-Javadoc)
	 * @see kiwi.api.ontology.OntologyService#removeSuperProperty(kiwi.model.kbase.KiWiResource, kiwi.model.kbase.KiWiResource)
	 */
	@Override
	public void removeSuperProperty(KiWiResource property, KiWiResource superproperty) {
		KiWiUriResource p = tripleStore.createUriResource(Constants.NS_RDFS+"subPropertyOf");
		
		tripleStore.removeTriple(property, p, superproperty);
	}

	/* (non-Javadoc)
	 * @see kiwi.api.ontology.OntologyService#listRange(kiwi.model.kbase.KiWiResource, boolean)
	 */
	@Override
	@BypassInterceptors
	public List<KiWiResource> listRange(KiWiResource p, boolean inferred) {
		return listGenericRDFSOutgoing(p, inferred, "range", "rangeTripleIds");
	}

	/* (non-Javadoc)
	 * @see kiwi.api.ontology.OntologyService#addRange(kiwi.model.kbase.KiWiResource, kiwi.model.kbase.KiWiResource)
	 */
	@Override
	public void addRange(KiWiResource property, KiWiResource range) {
		KiWiUriResource p = tripleStore.createUriResource(Constants.NS_RDFS+"range");
		
		tripleStore.createTriple(property, p, range);
	}

	/* (non-Javadoc)
	 * @see kiwi.api.ontology.OntologyService#removeRange(kiwi.model.kbase.KiWiResource, kiwi.model.kbase.KiWiResource)
	 */
	@Override
	public void removeRange(KiWiResource property, KiWiResource range) {
		KiWiUriResource p = tripleStore.createUriResource(Constants.NS_RDFS+"range");
		
		tripleStore.removeTriple(property, p, range);
	}

	/* (non-Javadoc)
	 * @see kiwi.api.ontology.OntologyService#listDomain(kiwi.model.kbase.KiWiResource, boolean)
	 */
	@Override
	public List<KiWiResource> listDomain(KiWiResource p, boolean inferred) {
		return listGenericRDFSOutgoing(p, inferred, "domain", "domainTripleIds");
	}

	/* (non-Javadoc)
	 * @see kiwi.api.ontology.OntologyService#addDomain(kiwi.model.kbase.KiWiResource, kiwi.model.kbase.KiWiResource)
	 */
	@Override
	public void addDomain(KiWiResource property, KiWiResource domain) {
		KiWiUriResource p = tripleStore.createUriResource(Constants.NS_RDFS+"domain");
		
		tripleStore.createTriple(property, p, domain);
	}

	/* (non-Javadoc)
	 * @see kiwi.api.ontology.OntologyService#removeDomain(kiwi.model.kbase.KiWiResource, kiwi.model.kbase.KiWiResource)
	 */
	@Override
	public void removeDomain(KiWiResource property, KiWiResource domain) {
		KiWiUriResource p = tripleStore.createUriResource(Constants.NS_RDFS+"domain");
		
		tripleStore.removeTriple(property, p, domain);
	}

	/* (non-Javadoc)
	 * @see kiwi.api.ontology.OntologyService#getInverse(kiwi.model.kbase.KiWiResource)
	 */
	@Override
	public KiWiResource getInverse(KiWiResource r) {
		KiWiResource inv = null;
		try {
			List<KiWiTriple> l1 = r.listOutgoing(Constants.NS_OWL+"inverseOf",1);
			List<KiWiTriple> l2 = r.listIncoming(Constants.NS_OWL+"inverseOf",1);
			
			if(l1.size() > 0 && 
					( l1.get(0).getObject().isAnonymousResource() || 
					  l1.get(0).getObject().isUriResource() ) ) {
				inv = (KiWiResource)l1.get(0).getObject();
			} else if(l2.size() > 0) {
				inv = l2.get(0).getSubject();
			}
		} catch (NamespaceResolvingException e) {
			// should never happen
		}
		return inv;
	}

	/* (non-Javadoc)
	 * @see kiwi.api.ontology.OntologyService#setInverse(kiwi.model.kbase.KiWiResource, kiwi.model.kbase.KiWiResource)
	 */
	@Override
	public void setInverse(KiWiResource p1, KiWiResource p2) {
		try {
			// first remove existing inverseOf relations from p1 and p2
			for(KiWiTriple t : p1.listOutgoing(Constants.NS_OWL+"inverseOf")) {
				tripleStore.removeTriple(t);
			}
			for(KiWiTriple t : p1.listIncoming(Constants.NS_OWL+"inverseOf")) {
				tripleStore.removeTriple(t);
			}
			for(KiWiTriple t : p2.listOutgoing(Constants.NS_OWL+"inverseOf")) {
				tripleStore.removeTriple(t);
			}
			for(KiWiTriple t : p2.listIncoming(Constants.NS_OWL+"inverseOf")) {
				tripleStore.removeTriple(t);
			}
			
			// then add two new inverseOf relations from p1 to p2 and p2 to p1
			KiWiUriResource p = tripleStore.createUriResource(Constants.NS_OWL+"inverseOf");
			
			tripleStore.createTriple(p1, p, p2);
			tripleStore.createTriple(p2, p, p1);
			
		} catch (NamespaceResolvingException e) {
			// should not happen
			log.error("namespace resolving exception",e);
		}
		
	}

	/* (non-Javadoc)
	 * @see kiwi.api.ontology.OntologyService#removeInverse(kiwi.model.kbase.KiWiResource, kiwi.model.kbase.KiWiResource)
	 */
	@Override
	public void removeInverse(KiWiResource p1, KiWiResource p2) {
		KiWiUriResource p = tripleStore.createUriResource(Constants.NS_OWL+"inverseOf");
		
		if(p1 != null && p2 != null) {
			tripleStore.removeTriple(p1, p, p2);
			tripleStore.removeTriple(p2, p, p1);
		} else {
			log.error("when removing inverse, one of the resources was null");
		}
	}

	
	
	private List<KiWiResource> listGenericRDFSOutgoing(KiWiResource r, boolean inferred, String property, String ctx_name) {
		List<KiWiResource> result = new LinkedList<KiWiResource>();
		
		HashMap<KiWiResource,Long> tripleIds = new HashMap<KiWiResource, Long>();
		try {
			for(KiWiTriple t : r.listOutgoing(Constants.NS_RDFS+property)) {
				if( t.isInferred() == inferred &&
					(t.getObject().isUriResource() || t.getObject().isAnonymousResource())) {
					result.add((KiWiResource)t.getObject());
					
					if(t.isInferred()) {
						tripleIds.put((KiWiResource)t.getObject(),t.getId());
					}
				}
			}
		} catch (NamespaceResolvingException e) {
			log.error("error while resolving namespace:",e);
		}
		
		// outject triple ids in page scope for the UI to access (e.g. for giving explanations)
		Contexts.getPageContext().set(ctx_name, tripleIds);
		
		Collections.sort(result, KiWiResource.LabelComparator.getInstance());
		
		return result;
		
	}
	
}
