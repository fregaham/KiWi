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
package kiwi.service.informationextraction;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.Query;

import kiwi.api.content.ContentItemService;
import kiwi.api.entity.KiWiEntityManager;
import kiwi.api.informationextraction.Extractlet;
import kiwi.api.informationextraction.InformationExtractionServiceLocal;
import kiwi.api.informationextraction.InformationExtractionServiceRemove;
import kiwi.api.informationextraction.KiWiGATEService;
import kiwi.api.tagging.TaggingService;
import kiwi.api.triplestore.TripleStore;
import kiwi.model.Constants;
import kiwi.model.content.ContentItem;
import kiwi.model.content.TextContent;
import kiwi.model.informationextraction.ClassifierEntity;
import kiwi.model.informationextraction.Example;
import kiwi.model.informationextraction.InstanceEntity;
import kiwi.model.informationextraction.Suggestion;
import kiwi.model.kbase.KiWiLiteral;
import kiwi.model.kbase.KiWiResource;
import kiwi.model.kbase.KiWiUriResource;
import kiwi.model.tagging.Tag;
import kiwi.model.user.User;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.log.Log;


/**
 * 
 *  Information extraction service. Provides suggestions for various kinds of
 *  annotation. The extraction itself is handled by "extractlets". 
 *  An Extractlet may produce suggestions on its own, or it can
 *  be assigned a specific resource (such as a tag) and use machine learning
 *  to classify potential instances. The assignment is represented by a 
 *  "Classifier" entity.
 * 
 * @author Marek Schmidt
 *
 */
@Stateless
@AutoCreate
@Name("kiwi.informationextraction.informationExtractionService")
@Scope(ScopeType.STATELESS)
public class InformationExtractionServiceImpl implements
		InformationExtractionServiceLocal, InformationExtractionServiceRemove {
	
	/**
	 * An abstract class representing an information extraction task.
	 * 
	 *  The task may be started synchronously (e.g. in a faces request), 
	 *  or asynchronously by putting it into information extraction processor task queue.
	 *  
	 *  Note that asynchronous task will be running in a different context.
	 * 
	 */
	public static abstract class InformationExtractionTask {
		abstract void run(InformationExtractionProcessor service);
		
		public InformationExtractionTask () {
		}
		
		public void start(InformationExtractionProcessor service) {
			try {
				run(service);
			}
			finally {
				complete();
			}
		}
		
		public void complete() {
		}
	}
	
	
	
	@Logger
	private Log log;
	
	@In(value="kiwi.informationextraction.informationExtractionProcessor", create=true)
	InformationExtractionProcessor informationExtractionProcessor;
	
	@In(create=true)
	TripleStore tripleStore;
	
	@In
	EntityManager entityManager;
	
	@In
	KiWiEntityManager kiwiEntityManager;
	
	@In
	FacesMessages facesMessages;
	
	@In(value="kiwi.informationextraction.gateService", required = false)
	KiWiGATEService kiwiGateService;
	
	@In(create=true)
	ContentItemService contentItemService;
	
	@In(create=true)
	private TaggingService taggingService;
	
	@Override
	public Collection<kiwi.model.informationextraction.Suggestion> extractEntities(ContentItem ci) {
		return extractEntities(ci.getResource(), ci.getTextContent(), ci.getLanguage());
	}

	@Override
	public Collection<kiwi.model.informationextraction.Suggestion> extractTags(ContentItem ci) {
		return extractTags(ci.getResource(), ci.getTextContent(), ci.getLanguage());
	}
	
	@Override
	public Collection<kiwi.model.informationextraction.Suggestion> extractSuggestions (KiWiResource context,
			TextContent content, Locale language) {
		Collection<kiwi.model.informationextraction.Suggestion> ret = new LinkedList<kiwi.model.informationextraction.Suggestion> ();
		gate.Document gateDoc = kiwiGateService.textContentToGateDocument(context, content);
		
		if (gateDoc == null) {
			log.info("Unable to preprocess document");
			return ret;
		}
		
		// run the pipeline for the language of the content item. (should ignore not supported languages)
		kiwiGateService.run(gateDoc, language.getLanguage());
				
		// Now, run all the extractlets.
		for (String extractletName : getExtractletNames()) {
			Extractlet extractlet = (Extractlet)Component.getInstance(extractletName);
			
			// There are two options for extractlet, 
			// 1. use instance entities, in that case, generate them and use the extract(instances) method...
			Collection<InstanceEntity> instances = extractlet.extractInstances(context, content, gateDoc, language);
			
			Collection<Suggestion> newSuggestions = extractlet.extract(instances);
			for (Suggestion suggestion : newSuggestions) {
				suggestion.setExtractletName(extractletName);				
				log.info("suggestion: #0 #1 #2", suggestion.getKind(), suggestion.getExtractletName(), suggestion.getScore());
				
				ret.add(suggestion);
			}
			
			for (kiwi.model.informationextraction.Suggestion suggestion : extractlet.extract(context, content, gateDoc, language)) {
				// force the name, so we can be sure which component to blame for the suggestion.
				suggestion.setExtractletName(extractletName);
				log.info("suggestion: #0 #1 #2", suggestion.getKind(), suggestion.getExtractletName(), suggestion.getScore());
								
				ret.add(suggestion);
			}
		}
		
		return ret;
	}

	@Override
	public Collection<String> getExtractletNames() {
		return informationExtractionProcessor.getExtractletNames();
	}

	/**
	 * The original interface for getting suggestions. It just wraps the new suggestion API.
	 */
	@Override
	public Collection<kiwi.model.informationextraction.Suggestion> extractEntities(KiWiResource context,
			TextContent content, Locale language) {
		if (kiwiGateService == null) {
			log.info("GATE service not initialized.");
			return new LinkedList<kiwi.model.informationextraction.Suggestion>();
		}
		
		Collection<kiwi.model.informationextraction.Suggestion> ret = new LinkedList<kiwi.model.informationextraction.Suggestion> ();
		
		for (kiwi.model.informationextraction.Suggestion suggestion : extractSuggestions (context, content, language)) {
			if (suggestion.getKind() == kiwi.model.informationextraction.Suggestion.ENTITY) {
				ret.add(suggestion);
			}
		}

		return ret;
	}

	/**
	 * The original interface for getting suggestions. It just wraps the new suggestion API.
	 */
	@Override
	public Collection<kiwi.model.informationextraction.Suggestion> extractTags(KiWiResource context,
			TextContent content, Locale language) {
		if (kiwiGateService == null) {
			log.info("GATE service not initialized.");
			return new LinkedList<kiwi.model.informationextraction.Suggestion>();
		}
				
		Collection<kiwi.model.informationextraction.Suggestion> ret = new LinkedList<kiwi.model.informationextraction.Suggestion> ();
		
		for (kiwi.model.informationextraction.Suggestion suggestion : extractSuggestions (context, content, language)) {
			if (suggestion.getKind() == kiwi.model.informationextraction.Suggestion.TAG) {
				ret.add(suggestion);
			}
		}

		return ret;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Collection<ClassifierEntity> getClassifiersForResource(KiWiResource resource) {
		Query q;
						
		q = entityManager.createNamedQuery("kiwi.informationextraction.informationExtractionService.listClassifiersByResourceId");
		q.setParameter("resourceId", resource.getId());
				
		return (Collection<ClassifierEntity>) q.getResultList();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Collection<ClassifierEntity> getClassifiers() {
		Query q;
				
		q = entityManager.createNamedQuery("kiwi.informationextraction.informationExtractionService.listClassifiers");
				
		return (Collection<ClassifierEntity>) q.getResultList();
	}
	
	@Override
	public void initExamplesForClassifierResource(final KiWiResource resource, boolean async) {
		log.info("initExamplesForClassifierResource: #0", resource.getKiwiIdentifier());
		InformationExtractionTask task = new InformationExtractionTask() {
			@Override
			public void run(InformationExtractionProcessor service) {
				service._initExamplesForClassifierResource(resource);
			}
		};
		
		if (async) {
			enqueueTask(task);
		}
		else {
			task.start(informationExtractionProcessor);
		}
	}
	
	private Collection<Extractlet> getExtractlets() {
		Collection<Extractlet> ret = new LinkedList<Extractlet> ();
		for (String extractletName : getExtractletNames()) {
			ret.add((Extractlet)Component.getInstance(extractletName));
		}
		
		return ret;
	}

	public void initInstances() {
		enqueueTask (new InformationExtractionTask() {
			@Override
			public void run(InformationExtractionProcessor service) {
				service._initInstances();
			}
		});
	}
	
	@Override
	public void trainClassifier (final ClassifierEntity classifier) {
		enqueueTask (new InformationExtractionTask() {
			@Override
			public void run(InformationExtractionProcessor service) {
				service._trainClassifier(classifier);
			}
		});
	}
	
	@Override
	public void computeSuggestions(final ClassifierEntity classifier) {
		enqueueTask (new InformationExtractionTask() {
			@Override
			public void run(InformationExtractionProcessor service) {
				service._computeSuggestions(classifier);
			}
		});
	}

	@Override
	public void acceptSuggestion(kiwi.model.informationextraction.Suggestion s, User user) {

		log.info("acceptSuggestion");
		
		// Check detached suggestion, particularly those that have already been deleted.
		if (s.getId() != null) {
			Long id = s.getId();
			if (!entityManager.contains(s)) {
				s = entityManager.find(Suggestion.class, id);
				if (s == null) {
					log.error("Suggestion #0 has been deleted", id);
					return;
				}
			}
			
			try {
				entityManager.refresh(s);
			}
			catch (EntityNotFoundException x) {
				log.error("Suggestion #0 has been deleted", id);
				return;
			}
		}
				
		Extractlet extractlet = (Extractlet)Component.getInstance(s.getExtractletName());
		if (extractlet != null) {
			log.info("acceptSuggestion extractlet #0", s.getExtractletName());
			extractlet.accept(s, user);
		}
		else {
			log.error("acceptSuggestion: no extractlet with the name #0", s.getExtractletName());
		}
	}

	@Override
	public void rejectSuggestion(kiwi.model.informationextraction.Suggestion s, User user) {
		log.info("rejectSuggestion");
		
		// Check detached suggestion, particularly those that have already been deleted.
		if (s.getId() != null) {
			Long id = s.getId();
			if (!entityManager.contains(s)) {
				s = entityManager.find(Suggestion.class, id);
				if (s == null) {
					log.error("Suggestion #0 has been deleted", id);
					return;
				}
			}
			
			try {
				entityManager.refresh(s);
			}
			catch (EntityNotFoundException x) {
				log.error("Suggestion #0 has been deleted", id);
				return;
			}
		}
		
		Extractlet extractlet = (Extractlet)Component.getInstance(s.getExtractletName());
		if (extractlet != null) {
			log.info("rejectSuggestion extractlet #0", s.getExtractletName());
			extractlet.reject(s, user);
		}
		else {
			log.error("rejectSuggestion: no extractlet with the name #0", s.getExtractletName());
		}
	}

	@Override
	public void trainAndSuggest(ClassifierEntity classifier) {
		
		// SystemStatus status = new SystemStatus("recomputing tag suggestions for tag " + tag.getTitle());
		// status.setProgress(0);
		// statusService.addSystemStatus(status);
		
		this.trainClassifier(classifier);
				
		// status.setProgress(50);
		
		this.computeSuggestions(classifier);
				
		// statusService.removeSystemStatus(status);
	}
	
	@Override
	public void trainAndSuggestForItem(final ContentItem item) {
		enqueueTask (new InformationExtractionTask() {
			@Override
			public void run(InformationExtractionProcessor service) {
				// TODO:
				// service._trainAndSuggestForItem(item);
			}
		});
	}
	
	@Override
	public void trainAndSuggestForClassifierResource(final KiWiResource resource, boolean async) {
		InformationExtractionTask task = new InformationExtractionTask() {
			@Override
			public void run(InformationExtractionProcessor service) {
				service._trainAndSuggestForClassifierResource(resource);
			}
		};
		
		if (async) {
			enqueueTask(task);
		}
		else {
			task.start(informationExtractionProcessor);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Collection<Example> getPositiveExamples (ClassifierEntity classifier) {
		EntityManager em = (EntityManager)Component.getInstance("entityManager");
		Query q = em.createNamedQuery("kiwi.informationextraction.informationExtractionService.listPositiveExamplesByClassifier");
    	q.setParameter("classifier", classifier);
		q.setHint("org.hibernate.cacheable", true);
    	
		return q.getResultList();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Collection<Example> getNegativeExamples (ClassifierEntity classifier) {
		EntityManager em = (EntityManager)Component.getInstance("entityManager");
		Query q = em.createNamedQuery("kiwi.informationextraction.informationExtractionService.listNegativeExamplesByClassifier");
    	q.setParameter("classifier", classifier);
		q.setHint("org.hibernate.cacheable", true);
    	
		return q.getResultList();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Collection<Example> getNegativeExamplesByClassifierResource(
			KiWiResource resource) {
		EntityManager em = (EntityManager)Component.getInstance("entityManager");
		Query q = em.createNamedQuery("kiwi.informationextraction.informationExtractionService.listNegativeExamplesByClassifierResource");
    	q.setParameter("resource", resource);
		q.setHint("org.hibernate.cacheable", true);
    	
		return q.getResultList();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Collection<Example> getPositiveExamplesByClassifierResource(
			KiWiResource resource) {
		EntityManager em = (EntityManager)Component.getInstance("entityManager");
		Query q = em.createNamedQuery("kiwi.informationextraction.informationExtractionService.listPositiveExamplesByClassifierResource");
    	q.setParameter("resource", resource);
		q.setHint("org.hibernate.cacheable", true);
    	
		return q.getResultList();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Collection<kiwi.model.informationextraction.Suggestion> getSuggestionsByContentItem(ContentItem item) {
		EntityManager em = (EntityManager)Component.getInstance("entityManager");
		Query q = em.createNamedQuery("kiwi.informationextraction.informationExtractionService.listSuggestionsByResourceSortedByScore");
    	q.setParameter("resourceid", item.getResource().getId());
		q.setHint("org.hibernate.cacheable", true);
		q.setMaxResults(50);
		    	
		return q.getResultList();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Collection<kiwi.model.informationextraction.Suggestion> getSuggestionsByTextContent(TextContent textcontent) {
		EntityManager em = (EntityManager)Component.getInstance("entityManager");
		Query q = em.createNamedQuery("kiwi.informationextraction.informationExtractionService.listSuggestionsByInstanceSourceTextContentSortedByScore");
    	q.setParameter("contentid", textcontent.getId());
		//q.setHint("org.hibernate.cacheable", true);
		//q.setMaxResults(50);
		    	
		return q.getResultList();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Collection<kiwi.model.informationextraction.Suggestion> getSuggestionsByClassifier(ClassifierEntity classifier) {
		EntityManager em = (EntityManager)Component.getInstance("entityManager");
		Query q = em.createNamedQuery("kiwi.informationextraction.informationExtractionService.listSuggestionsByClassifierSortedByScore");
    	q.setParameter("classifier", classifier);
		q.setHint("org.hibernate.cacheable", true);
		q.setMaxResults(50);
    	
		return q.getResultList();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Collection<kiwi.model.informationextraction.Suggestion> getSuggestionsByClassifierResource(
			KiWiResource resource) {
		
		EntityManager em = (EntityManager)Component.getInstance("entityManager");
		Query q = em.createNamedQuery("kiwi.informationextraction.informationExtractionService.listSuggestionsByClassifierResourceSortedByScore");
    	q.setParameter("resource", resource);
		// q.setHint("org.hibernate.cacheable", true);
		q.setMaxResults(7);
    	
		return q.getResultList();
		
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void deleteClassifier(ClassifierEntity classifier) {		
		Query q;
		
		q = entityManager.createNamedQuery("kiwi.informationextraction.informationExtractionService.deleteExamplesByClassifier");
		q.setParameter("classifier", classifier);
		q.executeUpdate();
		
		q = entityManager.createNamedQuery("kiwi.informationextraction.informationExtractionService.listSuggestionsByClassifier");
		q.setParameter("classifier", classifier);
		// q.setHint("javax.persistence.cache.retrieveMode", "BYPASS");
		Collection<kiwi.model.informationextraction.Suggestion> suggestions = q.getResultList();
		for (kiwi.model.informationextraction.Suggestion suggestion : suggestions) {
			suggestion.getResources().clear();
			suggestion.getTypes().clear();
			suggestion.getRoles().clear();
			if(!entityManager.contains(suggestion) && suggestion.getId() != null) {
				suggestion = entityManager.merge(suggestion);
			}
			entityManager.remove(suggestion);
		}
				
		entityManager.remove(classifier);
	}
		
	@Override
	public ClassifierEntity createClassifier(KiWiResource resource, String extractletName) {
		log.info("creating classifier #0 #1", resource, extractletName);
		
		ClassifierEntity cls = new ClassifierEntity();
		cls.setResource(resource);
		cls.setExtractletName(extractletName);
		
		entityManager.persist(cls);
				
		return cls;
	}
	
	
	@Override
	public void createAndInitClassifier(final KiWiResource resource, final String extractletName, boolean async) {
		InformationExtractionTask task = new InformationExtractionTask() {
			@Override
			public void run(InformationExtractionProcessor service) {
				service._createAndInitClassifier(resource, extractletName);
			}
		};
		
		if (async) {
			enqueueTask(task);
		}
		else {
			task.start(informationExtractionProcessor);
		}
	}
	
	private InformationExtractionTask enqueueTask (InformationExtractionTask task) {
		informationExtractionProcessor.enqueueTask(task);
		return task;
	}


	/**
	 * Realizes a suggestion.
	 * Creates whatever the suggestion suggests to be created.
	 * 
	 * @param s a suggestion
	 * @param user the user framed with the creation of the whatever.
	 */
	@Override
	public void realizeSuggestion(kiwi.model.informationextraction.Suggestion s, User user) {
		
		if (s.getKind() == Suggestion.DATATYPE) {
			KiWiResource subject = s.getInstance().getSourceResource();
			KiWiUriResource predicate = (KiWiUriResource)s.getRoles().get(0);
			
			KiWiLiteral object = null;
			if (s.getTypes().size() == 1) {
				KiWiResource type = s.getTypes().get(0);
				object = tripleStore.createLiteral(s.getInstance().getValue(), s.getInstance().getSourceResource().getContentItem().getLanguage(), type);
			}
			else {
				object = tripleStore.createLiteral(s.getInstance().getValue());
			}
			// KiWiLiteral value, language, type)
			tripleStore.createTriple(subject, predicate, object);
		}
		else if (s.getKind() == Suggestion.TAG) {
			KiWiResource subject = s.getInstance().getSourceResource();
			
			log.info("createTagging: #0 #1 #2 #4", s.getLabel(), subject.getContentItem().getKiwiIdentifier(), s.getClassifier().getResource().getContentItem().getKiwiIdentifier(), user.getContentItem().getKiwiIdentifier());
			taggingService.createTagging(s.getLabel(), subject.getContentItem(), s.getClassifier().getResource().getContentItem(), user);
		}
		else if (s.getKind() == Suggestion.TYPE) {
			KiWiResource subject = s.getInstance().getSourceResource();
			KiWiUriResource property = tripleStore.createUriResource(Constants.NS_RDF + "type");
			KiWiResource object = s.getClassifier().getResource();
			tripleStore.createTriple(subject, property, object);
		}
	}
	
	@Override
	public void unrealizeSuggestion(kiwi.model.informationextraction.Suggestion s, User user) { 
		
		if (s.getKind() == Suggestion.DATATYPE) {
			KiWiResource subject = s.getInstance().getSourceResource();
			KiWiUriResource predicate = (KiWiUriResource)s.getClassifier().getResource();
			
			KiWiLiteral object = null;
			if (s.getTypes().size() == 1) {
				KiWiResource type = s.getTypes().get(0);
				object = tripleStore.createLiteral(s.getInstance().getValue(), s.getInstance().getSourceResource().getContentItem().getLanguage(), type);
			}
			else {
				object = tripleStore.createLiteral(s.getInstance().getValue());
			}
			// KiWiLiteral value, language, type)
			tripleStore.removeTriple(subject, predicate, object);
		}
		else if (s.getKind() == Suggestion.TAG) {
			KiWiResource subject = s.getInstance().getSourceResource();
			
			// taggingService.createTagging(s.getLabel(), subject.getContentItem(), s.getClassifier().getResource().getContentItem(), user);
			for (Tag tag : taggingService.getTaggings(subject.getContentItem())) {
				if (tag.getTaggedBy().equals(user) && tag.getTaggingResource().equals(s.getClassifier().getResource().getContentItem())) {
					taggingService.removeTagging(tag);
					break;
				}
			}
		}
		else if (s.getKind() == Suggestion.TYPE) {
			KiWiResource subject = s.getInstance().getSourceResource();
			KiWiUriResource property = tripleStore.createUriResource(Constants.NS_RDF + "type");
			KiWiResource object = s.getClassifier().getResource();
			tripleStore.removeTriple(subject, property, object);
		}
	}
	
	

	@Override
	public void extractInformationAsync(final ContentItem item) {
		enqueueTask (new InformationExtractionTask() {
			@Override
			public void run(InformationExtractionProcessor service) {
				service._extractInformation(item);
			}
		});
	}
	
	@Override
	public void extractInformation(final ContentItem item) {
		
		InformationExtractionTask task = (new InformationExtractionTask() {
			@Override
			public void run(InformationExtractionProcessor service) {
				service._extractInformation(item);
			}
		});
		
		task.start(informationExtractionProcessor);
	}
	
	
	
	@Override
	public void removeExample(Example example) {
		entityManager.remove(example);
	}

	@Override
	public Collection<String> getExtractletsAbleToClassify(KiWiResource resource) {		
		List<String> ret = new LinkedList<String>();
		
		for (Extractlet extractlet : getExtractlets()) {
			if (extractlet.canClassify(resource)) {
				ret.add(extractlet.getName());
			}
		}
		
		return ret;
	}

	@Override
	public void setEnabled(boolean enabled) {
		informationExtractionProcessor.setEnabled(enabled);
	}

	@Override
	public boolean getEnabled() {
		return informationExtractionProcessor.getEnabled();
	}

	@Override
	public void setOnline(boolean online) {
		informationExtractionProcessor.setOnline(online);
	}

	@Override
	public boolean getOnline() {
		return informationExtractionProcessor.getOnline();
	}

	@Override
	public void init() {
		informationExtractionProcessor.init();
	}
}
