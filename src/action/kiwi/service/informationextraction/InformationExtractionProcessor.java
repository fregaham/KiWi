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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.Query;

import kiwi.api.config.ConfigurationService;
import kiwi.api.content.ContentItemService;
import kiwi.api.event.KiWiEvents;
import kiwi.api.informationextraction.Extractlet;
import kiwi.api.informationextraction.FeatureStoreService;
import kiwi.api.informationextraction.KiWiGATEService;
import kiwi.api.system.StatusService;
import kiwi.api.transaction.TransactionService;
import kiwi.api.triplestore.TripleStore;
import kiwi.model.Constants;
import kiwi.model.content.ContentItem;
import kiwi.model.informationextraction.ClassifierEntity;
import kiwi.model.informationextraction.Context;
import kiwi.model.informationextraction.Example;
import kiwi.model.informationextraction.InstanceEntity;
import kiwi.model.informationextraction.Suggestion;
import kiwi.model.kbase.KiWiResource;
import kiwi.model.kbase.KiWiUriResource;
import kiwi.model.status.SystemStatus;
import kiwi.service.informationextraction.InformationExtractionServiceImpl.InformationExtractionTask;
import kiwi.service.transaction.KiWiSynchronizationImpl;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.async.Asynchronous;
import org.jboss.seam.annotations.async.IntervalDuration;
import org.jboss.seam.core.Events;
import org.jboss.seam.log.Log;
import org.jboss.seam.transaction.Transaction;

@Name("kiwi.informationextraction.informationExtractionProcessor")
@Scope(ScopeType.APPLICATION)
@TransactionManagement(TransactionManagementType.BEAN)
public class InformationExtractionProcessor implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 395501202842981965L;
	
	@In
	private TransactionService transactionService;
	
	@Logger
	private Log log;
		
	@In(create=true)
	private TripleStore tripleStore;
	
	@In(create=true)
	private ContentItemService contentItemService;

	@In
	private EntityManager entityManager;
	
	@In
	private ConfigurationService configurationService;
	
	@In(value="kiwi.informationextraction.gateService", required = false)
	private KiWiGATEService kiwiGateService;
	
	@In("kiwi.core.statusService")
	private StatusService statusService;
	
	private Queue<InformationExtractionTask> taskQueue;
	private Lock runningLock;
	private boolean running = false;
	
	private List<String> extractlets;
	
	private boolean enabled;
	private boolean online;
	
	@Create
	public void create() {
		
		enabled = configurationService.getBooleanConfiguration("kiwi.informationextraction.informationExtractionService.enabled", false);
		online = configurationService.getBooleanConfiguration("kiwi.informationextraction.informationExtractionService.online", false);
		
		taskQueue = new ConcurrentLinkedQueue<InformationExtractionTask>();
		runningLock = new ReentrantLock();
		
		if (configurationService.isConfigurationSet("kiwi.informationextraction.extractlets")) {
			extractlets = configurationService.getListConfiguration("kiwi.informationextraction.extractlets");
		}
		else {
			extractlets = new LinkedList<String> ();
			extractlets.add("kiwi.informationextraction.englishGateEntityExtractlet");
			extractlets.add("kiwi.informationextraction.deutschGateEntityExtractlet");
			extractlets.add("kiwi.informationextraction.documentTagExtractlet");
			extractlets.add("kiwi.informationextraction.documentTypeExtractlet");
			extractlets.add("kiwi.informationextraction.numberExtractlet");
			// extractlets.add("kiwi.informationextraction.actionItemExtractlet");
			extractlets.add("kiwi.informationextraction.sequenceExtractlet");
			extractlets.add("rrs.extractlet.articleToMetaExtractlet");
			configurationService.setListConfiguration("kiwi.informationextraction.extractlets", extractlets);
		}
		
		init();
	}
	
	public void init() {
		enqueueTask (new InformationExtractionServiceImpl.InformationExtractionTask() {
			@Override
			public void run(InformationExtractionProcessor service) {
				service._init();
			}
		});
	}
	
	@Asynchronous
	public void runTasks(@IntervalDuration Long interval) throws Exception {
		
		//log.info("runTasks");
		
		// atomic read/write to running variable
		try{
			runningLock.lock();
			if (running) {
				return;
			}
			else {
				running = true;
			}
		}
		finally{
			runningLock.unlock();
		}
		
		// make sure we set the running to false at the end
		try{
			Transaction.instance().setTransactionTimeout(60000);
			while(!taskQueue.isEmpty()) {
				InformationExtractionTask task = taskQueue.poll();
				if (task != null) {
					log.info("Running IE task");
					
					try {
						Transaction.instance().begin();
						transactionService.registerSynchronization(
		                		KiWiSynchronizationImpl.getInstance(), 
		                		transactionService.getUserTransaction() );
						entityManager.joinTransaction();
						entityManager.setFlushMode(FlushModeType.COMMIT);
						task.run(this);
						Transaction.instance().commit();
					}
					finally {
						task.complete();
					}
				}
			}
		}
		finally{
			// atomic write to running variable
			
			try {
				runningLock.lock();
				running = false;
			}
			finally {
				runningLock.unlock();
			}
			
			//log.info("runTasks exit");
		}
	}
	
	public void _init() {
		if (kiwiGateService != null) {
			SystemStatus status = new SystemStatus("initialization of the Information Extraction Service");
			status.setProgress(0);
			statusService.addSystemStatus(status);
			
			kiwiGateService.init();
			
			statusService.removeSystemStatus(status);
		}
	}
	
	@SuppressWarnings("unchecked")
	public Collection<ClassifierEntity> getClassifiersForResource(KiWiResource resource) {
		Query q;
						
		q = entityManager.createNamedQuery("kiwi.informationextraction.informationExtractionService.listClassifiersByResourceId");
		q.setParameter("resourceId", resource.getId());
				
		return (Collection<ClassifierEntity>) q.getResultList();
	}
	
	public void _initExamplesForClassifierResource(KiWiResource resource) {
		for (final ClassifierEntity classifier : getClassifiersForResource(resource)) {
			_initExamples(classifier);
		}
	}
	
	@SuppressWarnings("unchecked")
	public void _initExamples(final ClassifierEntity classifier) {
		
		entityManager.setFlushMode(FlushModeType.COMMIT);
		
		SystemStatus status = new SystemStatus("initialization of classifier " + classifier);
		status.setProgress(0);
		statusService.addSystemStatus(status);
		
		log.info("starting initialization for classifier #0", classifier.getId());
		
		if (classifier.getExtractletName() != null) {
			
			Query q;

			q = entityManager.createNamedQuery("kiwi.informationextraction.informationExtractionService.deleteExamplesByClassifier");
			q.setParameter("classifier", classifier);
			q.executeUpdate();
			
			q = entityManager.createNamedQuery("kiwi.informationextraction.informationExtractionService.listSuggestionsByClassifier");
			q.setParameter("classifier", classifier);			
			Collection<kiwi.model.informationextraction.Suggestion> suggestions = q.getResultList();
			for (kiwi.model.informationextraction.Suggestion suggestion : suggestions) {
				suggestion.getResources().clear();
				suggestion.getTypes().clear();
				suggestion.getRoles().clear();
				suggestion = entityManager.merge(suggestion);
				entityManager.remove(suggestion);
			}
			
//			entityManager.flush();
			
			Extractlet extractlet = (Extractlet)Component.getInstance(classifier.getExtractletName());
			extractlet.initClassifier(classifier);
			
			/*
			for (Example example : examples) {
				entityManager.persist(example);
			}*/
		}
		
//		entityManager.flush();
		
		log.info("ended initialization for classifier #0", classifier.getId());		
		statusService.removeSystemStatus(status);
	}
	
	public void _initInstances() {
		SystemStatus status = new SystemStatus("initialization of instances for Information Extraction");
		status.setProgress(0);
		statusService.addSystemStatus(status);
		
		entityManager.setFlushMode(FlushModeType.COMMIT);
		
		FeatureStoreService featureStoreService = (FeatureStoreService)Component.getInstance("kiwi.informationextraction.featureStoreService");
		
		Query q;

		// TODO: can't this be done somehow using cascade delete? does hibernate support it in some way?
		q = entityManager.createNamedQuery("kiwi.informationextraction.informationExtractionService.deleteExamples");
		q.executeUpdate();

		// TODO: can't this be done somehow without using native queries?
		q = entityManager.createNativeQuery("delete from Suggestion_Resource");
		q.executeUpdate();
		q = entityManager.createNativeQuery("delete from Suggestion_Type");
		q.executeUpdate();
		q = entityManager.createNativeQuery("delete from Suggestion_Role");
		q.executeUpdate();
		
		q = entityManager.createNamedQuery("kiwi.informationextraction.informationExtractionService.deleteSuggestions");
		q.executeUpdate();
		
		// delete all existing instances first.
		q = entityManager.createNamedQuery("kiwi.informationextraction.informationExtractionService.deleteInstances");
		q.executeUpdate();
		
		
		// prepare instance extractors
		// Collection<InstanceExtractorEntity> instanceExtractors = getInstanceExtractors();
	
		Collection<Extractlet> extractlets = getExtractlets();
		
		// clean the alphabets
		/*for (InstanceExtractorEntity extractor : instanceExtractors) {
			extractor.setMalletAlphabet(new Alphabet());
		}*/
	
		// Blacklist of types, for which we do not generate instances
		// TODO: user defined rules for what extractors use on which types?
		Set<KiWiResource> typeBlacklist = new HashSet<KiWiResource>();

		KiWiUriResource contentItemType = tripleStore.createUriResource(Constants.NS_KIWI_CORE + "ContentItem");
		typeBlacklist.add (tripleStore.createUriResource(Constants.NS_OWL + "Class"));
		typeBlacklist.add (tripleStore.createUriResource(Constants.NS_OWL + "Ontology"));
		typeBlacklist.add (tripleStore.createUriResource(Constants.NS_OWL + "ObjectProperty"));
		typeBlacklist.add (tripleStore.createUriResource(Constants.NS_OWL + "DatatypeProperty"));
		
		typeBlacklist.add (tripleStore.createUriResource(Constants.NS_RDF + "Property"));
		
		typeBlacklist.add (tripleStore.createUriResource(Constants.NS_RDFS + "Class"));
		
		typeBlacklist.add (tripleStore.createUriResource(Constants.NS_SKOS + "Concept"));
		
		Set<ContentItem> cis = contentItemService.getContentItems();
		int count = 0;
items: 
		for (ContentItem ci : cis) {
			
			status.setProgress((100 * count) / cis.size());
			++count;
			
			// it has to be a content item...
			if (!ci.getResource().hasType(contentItemType)) {
				continue items;
			}
			// ...and may not be of one of blacklist types.
			for (KiWiResource type : ci.getTypes()) {
				if (typeBlacklist.contains(type)) {
					continue items;
				}
			}
			
			// process the content by a GATE pipeline, first create a GATE representation of the text content:
			gate.Document gateDoc = kiwiGateService.textContentToGateDocument(ci.getResource(), ci.getTextContent());
			
			if (gateDoc == null) {
				log.info("Unable to preprocess document");
				continue items;
			}
			
			// run the pipeline for the language of the content item. (should ignore not supported languages)
			kiwiGateService.run(gateDoc, ci.getLanguage().getLanguage());
			
			
			// we still don't have enough confusion about what "ie" means... 
			/*for (InstanceExtractorEntity ie : instanceExtractors) {
				for (InstanceEntity i : ie.extractInstances(ci, gateDoc)) {
					em.persist(i);
					
					log.debug("created instance #0 #1, #2", i.getSourceContentItem().getTitle(), i.getId(), i.getContext().toString());
				}
			}*/
			for (Extractlet extractlet : extractlets) {
				
				for (InstanceEntity instance : extractlet.extractInstances(ci.getResource(), ci.getTextContent(), gateDoc, ci.getLanguage())) {
					entityManager.persist(instance);
										
					if (instance.getFeatures() != null) {
						featureStoreService.put(instance.getExtractletName(), instance.getId(), instance.getFeatures());
					}
					
					log.debug("created instance #0 #1, #2", instance.getSourceResource().getContentItem().getTitle(), instance.getId(), instance.getContext().toString());
				}
				
				featureStoreService.flush(extractlet.getName());
			}
			
		}

		// entityManager.flush();
		
		statusService.removeSystemStatus(status);
	}
	
	public void _trainClassifier (ClassifierEntity classifier) {
		
		entityManager.setFlushMode(FlushModeType.COMMIT);
		
		if (!entityManager.contains(classifier)) {
			classifier = entityManager.find(ClassifierEntity.class, classifier.getId());
		}
		
		Extractlet extractlet = getExtractlet(classifier.getExtractletName());
		extractlet.trainClassifier(classifier);
		
		log.debug("training end");
	}
	
	@SuppressWarnings("unchecked")
	public void _computeSuggestions(ClassifierEntity classifier) {
		
		SystemStatus status = new SystemStatus("Computing suggestions for classifier " + classifier);
		status.setProgress(0);
		statusService.addSystemStatus(status);
		
		entityManager.setFlushMode(FlushModeType.COMMIT);
		
		Query q;
		
		if (!entityManager.contains(classifier)) {
			classifier = entityManager.find(ClassifierEntity.class, classifier.getId());
		}
		
		Extractlet extractlet = getExtractlet(classifier.getExtractletName());
		
		q = entityManager.createNamedQuery("kiwi.informationextraction.informationExtractionService.listSuggestionsByClassifier");
		q.setParameter("classifier", classifier);
		
		Collection<kiwi.model.informationextraction.Suggestion> suggestions = q.getResultList();
		extractlet.updateSuggestions(classifier, suggestions);
		
		statusService.removeSystemStatus(status);
	}
	

	public void _trainAndSuggestForClassifierResource (KiWiResource resource) {
				
		SystemStatus status = new SystemStatus("recomputing tag suggestions for resource " + resource.getContentItem().getTitle());
		status.setProgress(0);
		statusService.addSystemStatus(status);
				
		for (ClassifierEntity classifier : getClassifiersForResource(resource)) {
			_trainClassifier(classifier);
			_computeSuggestions(classifier);
		}
		    	 
		statusService.removeSystemStatus(status);
	}
	
	public void _createAndInitClassifier(KiWiResource resource, String extractletName) {
		log.info("creating classifier #0 #1", resource, extractletName);
		
		ClassifierEntity cls = new ClassifierEntity();
		cls.setResource(resource);
		cls.setExtractletName(extractletName);
		
		entityManager.persist(cls);
		
		this._initExamples(cls);
	}
	
	/**
	 * Merge examples of old content with the new suggestions.
	 * The examples that couldn't be matched to the new suggestions are deleted.
	 * 
	 * This helps making the example list clean, by removing examples based on old versions of content items. 
	 * 
	 * @param item
	 * @param newSuggestions
	 */
	private void _mergeOldExamples(ContentItem item, Collection<Suggestion> newSuggestions) {
		
		if (item == null || item.getResource() == null || item.getTextContent() == null) {
			return;
		}
		
		Query q = entityManager.createNamedQuery("kiwi.informationextraction.informationExtractionService.listOldExamplesByInstanceSourceResource");
		q.setParameter("resourceid", item.getResource().getId());
		q.setParameter("contentid", item.getTextContent().getId());
		@SuppressWarnings("unchecked")
		Collection<Example> oldExamples = q.getResultList();
		
		examples: for (Example example : oldExamples) {
			
			log.info("merging example #0", example.getId());
			
			// TODO: index the newSuggestions in a treeset with label, classifier ids and context hash as keys.
			
			Suggestion oldSuggestion = example.getSuggestion();
			
			for (Suggestion newSuggestion : newSuggestions) {
				
				// Check all the reasons these suggestions are not compatible
				if (!oldSuggestion.getLabel().equals(newSuggestion.getLabel())) {
					continue;
				}
				
				if (oldSuggestion.getClassifier() != null && newSuggestion.getClassifier() != null ) {
					if (!oldSuggestion.getClassifier().getId().equals(newSuggestion.getClassifier().getId())) {
						continue;
					}
				}
				else if (oldSuggestion.getClassifier() != newSuggestion.getClassifier()) {
					continue;
				}
				
				InstanceEntity oldInstance = oldSuggestion.getInstance();
				InstanceEntity newInstance = newSuggestion.getInstance();
				
				if (oldInstance.getContext().getIsFragment() != newInstance.getContext().getIsFragment()) {
					continue;
				}
				
				Context oldContext = oldInstance.getContext();
				Context newContext = newInstance.getContext();
				
				if (oldContext.hashCode() != newContext.hashCode()) {
					continue;
				}
				
				if (oldContext.getContextBegin() != newContext.getContextBegin() ||
					oldContext.getContextEnd() != newContext.getContextEnd() ||
					oldContext.getInBegin() != newContext.getInBegin() ||
					oldContext.getInEnd() != newContext.getInEnd()) {
					continue;
				}
				
				// If the suggestion made to this point, it is probably compatible.
				log.info("setting new suggestion for example #0: #1", example.getId(), newSuggestion.getId());
				example.setSuggestion(newSuggestion);
				continue examples;
			}
			
			// No compatible suggestion found. We remove the old example.
			log.info("removing old example #0", example.getId());
			entityManager.remove(example);
		}
	}
	
	
	
	/**
	 * Recreates all suggestion for a content item.
	 * It deletes all existing suggestions.
	 * Then runs the instance extractors
	 * For each instance extractors a classifier is run to classify the instances.
	 * Then all information extraction plugins are run. 
	 * 
	 * Then all the examples are reconstructed from existing annotation. 
	 * @param item
	 */
	@SuppressWarnings("unchecked")
	public void _extractInformation(ContentItem item) {
		
		if (item.getTextContent() == null) {
			return;
		}
		
		SystemStatus status = new SystemStatus("initialization of instances for Information Extraction on CI " + item.getTitle());
		status.setProgress(0);
		statusService.addSystemStatus(status);
				
		try{
			Query q;
			
			if(item.getTextContent() != null) {
				// delete examples only from the current textcontent (keeping examples of older versions of the same content item)
				q = entityManager.createNamedQuery("kiwi.informationextraction.informationExtractionService.deleteExamplesByInstanceSourceTextContent");
				q.setParameter("contentid", item.getTextContent().getId());
				q.executeUpdate();
			}
			
			// delete examples from any version whose user is null (these examples are extracted from the content itself)
			q = entityManager.createNamedQuery("kiwi.informationextraction.informationExtractionService.deleteExamplesByInstanceSourceResourceWhereUserNull");
			q.setParameter("resourceid", item.getResource().getId());
			q.executeUpdate();
			
	
 			entityManager.flush();
			
			// Delete all suggestions about this content item (including old textcontent no point of having suggestions of old versions
			// TODO: do it better, perhaps using native SQL... the problem is those ManyToMany tables...
			q = entityManager.createNamedQuery("kiwi.informationextraction.informationExtractionService.listUnusedSuggestionsByInstanceSourceResource");
			q.setParameter("resourceid", item.getResource().getId());
			Collection<Suggestion> suggestions = q.getResultList();
			for (kiwi.model.informationextraction.Suggestion suggestion : suggestions) {
				suggestion.getResources().clear();
				suggestion.getTypes().clear();
				suggestion.getRoles().clear();
				suggestion = entityManager.merge(suggestion);
				entityManager.remove(suggestion);
			}
			
			/*q = em.createNamedQuery("kiwi.informationextraction.informationExtractionService.deleteSuggestionsByInstanceSourceItem");
			q.setParameter("itemid", item.getId());
			q.executeUpdate();*/
//			entityManager.flush();
			
			// delete instances only from the current textcontent (keep instances of old textcontent, which may be used by examples)
			/*q = entityManager.createNamedQuery("kiwi.informationextraction.informationExtractionService.deleteUnusedInstancesBySourceResource");
			q.setParameter("resourceid", item.getResource().getId());
			q.executeUpdate();*/
			
			FeatureStoreService featureStoreService = (FeatureStoreService)Component.getInstance("kiwi.informationextraction.featureStoreService");
			
			// We delete the unused instances. We also delete its features from the feature store, if it has any.
			/*q = entityManager.createNamedQuery("kiwi.informationextraction.informationExtractionService.listUnusedInstancesBySourceResource");
			q.setParameter("resourceid", item.getResource().getId());
			for (InstanceEntity instance : (Collection<InstanceEntity>)q.getResultList()) {
				if (instance.isHasFeatures()) {
					featureStoreService.remove(instance.getExtractletName(), instance.getId());
				}
				entityManager.remove(instance);
			}*/
			
//			entityManager.flush();
			// TODO: delete instances of the old content of the same content item that are not used as examples.
			
			// process the content by a GATE pipeline, first create a GATE representation of the text content:
			gate.Document gateDoc = kiwiGateService.textContentToGateDocument(item.getResource(), item.getTextContent());
			
			if (gateDoc == null) {
				log.info("Unable to preprocess document");
				return;
			}
			
			// run the pipeline for the language of the content item. (should ignore not supported languages)
			kiwiGateService.run(gateDoc, item.getLanguage().getLanguage());			
			List<Suggestion> newSuggestions = new LinkedList<Suggestion>();
			// Now, run all the extractlets.
			for (String extractletName : getExtractletNames()) {
				Extractlet extractlet = (Extractlet)Component.getInstance(extractletName);
				
				// There are two options for extractlet, 
				// 1. use instance entities, in that case, generate them and use the extract(instances) method...
				//   (we want to store instance entities even if no suggestions are generated, e.g. for future classifiers.)
				Collection<InstanceEntity> instances = extractlet.extractInstances(item.getResource(), item.getTextContent(), gateDoc, item.getLanguage());
				
				suggestions = extractlet.extract(instances);
				for (Suggestion suggestion : suggestions) {
					newSuggestions.add(suggestion);
					suggestion.setExtractletName(extractletName);
					entityManager.persist(suggestion);
					
					log.info("suggestion: #0 #1 #2 #3", suggestion.getId(), suggestion.getKind(), suggestion.getExtractletName(), suggestion.getScore());
				}
				
				for (InstanceEntity instance : instances) {
					entityManager.persist(instance);
					
					// index the features, use the extractlet name as the "category" in the feature store.
					if (!extractletName.equals(instance.getExtractletName())) {
						log.error("extractlet #0 produces instances #1", extractletName, instance.getExtractletName());
					}
					
					if (instance.getFeatures() != null) {
						featureStoreService.put(extractletName, instance.getId(), instance.getFeatures());
					}
				}
				
//				entityManager.flush();
				
				// 2. it doesn't use instance entities, or it does, but creates them directly... 
				suggestions = extractlet.extract(item.getResource(), item.getTextContent(), gateDoc, item.getLanguage());
				for (kiwi.model.informationextraction.Suggestion suggestion : suggestions) {
					newSuggestions.add(suggestion);
					
					// force the name, so we can be sure which component to blame for the suggestion.
					suggestion.setExtractletName(extractletName);
					entityManager.persist(suggestion);
					
					if (suggestion.getInstance() != null) {
						// persist the instance, also, index its features in the feature store 
						InstanceEntity instanceEntity = suggestion.getInstance();
						entityManager.persist(instanceEntity);
						
						// index the features, use the extractlet name as the "category" in the feature store.
						if (!extractletName.equals(instanceEntity.getExtractletName())) {
							log.error("extractlet #0 produces instances #1", extractletName, instanceEntity.getExtractletName());
						}
						
						if (instanceEntity.getFeatures() != null) {
							featureStoreService.put(extractletName, instanceEntity.getId(), instanceEntity.getFeatures(), false);
						}
					}
				}
				
//				entityManager.flush();
				
				Collection<Example> examples = extractlet.extractExamples(item.getResource(), item.getTextContent(), gateDoc, item.getLanguage(), suggestions, instances);
				for (Example example : examples) {
					// persist the examples
					InstanceEntity instance = example.getSuggestion().getInstance();
					if (instance != null) {
						entityManager.persist(instance);
						
						if (instance.getFeatures() != null) {
							featureStoreService.put(extractletName, instance.getId(), instance.getFeatures(), false);
						}
					}
					entityManager.persist(example.getSuggestion());
					entityManager.persist(example);
				}
				
				featureStoreService.flush(extractletName);
			}
			
			entityManager.flush();
			
			_mergeOldExamples(item, newSuggestions);
			
			entityManager.flush();
			
			q = entityManager.createNamedQuery("kiwi.informationextraction.informationExtractionService.listOldUnusedSuggestionsByInstanceSourceResource");
			q.setParameter("resourceid", item.getResource().getId());
			q.setParameter("contentid", item.getTextContent().getId());
			suggestions = q.getResultList();
			for (kiwi.model.informationextraction.Suggestion suggestion : suggestions) {
				suggestion.getResources().clear();
				suggestion.getTypes().clear();
				suggestion.getRoles().clear();
				suggestion = entityManager.merge(suggestion);
				entityManager.remove(suggestion);
			}
			
			entityManager.flush();
			
			q = entityManager.createNamedQuery("kiwi.informationextraction.informationExtractionService.listOldUnusedInstancesBySourceResource");
			q.setParameter("resourceid", item.getResource().getId());
			q.setParameter("contentid", item.getTextContent().getId());
			for (InstanceEntity instance : (Collection<InstanceEntity>)q.getResultList()) {
				if (instance.isHasFeatures()) {
					featureStoreService.remove(instance.getExtractletName(), instance.getId());
					featureStoreService.flush(instance.getExtractletName());
				}
				entityManager.remove(instance);
			}
			
			Events.instance().raiseTransactionSuccessEvent(KiWiEvents.SUGGESTIONS_UPDATED, item);
		}
		finally {
			statusService.removeSystemStatus(status);
		}
	}
	
	private Collection<Extractlet> getExtractlets() {
		Collection<Extractlet> ret = new LinkedList<Extractlet> ();
		for (String extractletName : getExtractletNames()) {
			ret.add((Extractlet)Component.getInstance(extractletName));
		}
		
		return ret;
	}
	
	private Extractlet getExtractlet(String extractletName) {
		return (Extractlet)Component.getInstance(extractletName);
	}
	
	public void enqueueTask (InformationExtractionTask task) {
		if (enabled) {
			taskQueue.add(task);
		}
	}
	
	public Collection<String> getExtractletNames() {
		return extractlets;
	}
	
	public void setEnabled(boolean enabled) {
		
		if (!this.enabled && enabled) {
			this.enabled = enabled;
			
			// This can mean the gate service is not yet initialized, do it.
			init();
		}
		else {
			this.enabled = enabled;
		}
	}
	
	public boolean getEnabled() {
		return enabled;
	}
	
	public void setOnline(boolean online) {
		this.online = online;
	}
	
	public boolean getOnline() {
		return this.online;
	}
}
