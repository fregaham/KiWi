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

import gate.Annotation;
import gate.Document;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import kiwi.api.tagging.TaggingService;
import kiwi.api.triplestore.TripleStore;
import kiwi.model.Constants;
import kiwi.model.content.ContentItem;
import kiwi.model.content.TextContent;
import kiwi.model.informationextraction.ClassifierEntity;
import kiwi.model.informationextraction.Context;
import kiwi.model.informationextraction.Example;
import kiwi.model.informationextraction.InstanceEntity;
import kiwi.model.informationextraction.Suggestion;
import kiwi.model.kbase.KiWiResource;
import kiwi.model.tagging.Tag;
import kiwi.model.user.User;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;

/**
 * A ML extractlet that uses the document bag-of-words as features for 
 * classification to extract tags. To use this extractlet for a particular 
 * tag, you need to create the `Extraction' perspective for the tag, and 
 * initialize the classifier there.
 * 
 * @author Marek Schmidt
 *
 */
@Name("kiwi.informationextraction.documentTagExtractlet")
@Scope(ScopeType.STATELESS)
public class DocumentTagExtractlet extends AbstractMLExtractlet {

	@Logger
	private Log log;
	
	@In
	TripleStore tripleStore;
	
	public DocumentTagExtractlet() {
		super("kiwi.informationextraction.documentTagExtractlet");
	}
	
	@Override
	public Suggestion generateSuggestion(InstanceEntity entity, ClassifierEntity classifier) {
		Suggestion ret = super.generateSuggestion(entity, classifier);
		ret.setKind(Suggestion.TAG);
		ret.getResources().add(classifier.getResource());
		ret.setLabel(classifier.getResource().getContentItem().getTitle());
		
		return ret;
	}
	
	@Override
	public boolean canClassify(KiWiResource resource) {
		return resource.hasType(tripleStore.createUriResource(Constants.NS_KIWI_CORE+"Tag"));
	}
	
	@Override
	public void initClassifier(ClassifierEntity classifier) {
		
		EntityManager entityManager = (EntityManager)Component.getInstance("entityManager");
		
		Long taggingResourceContentItemId = classifier.getResource().getContentItem().getId();
		
		// Get the list of text contentids of content items positively tagged with the tag.
		Query q = entityManager.createNamedQuery("kiwi.informationextraction.informationExtractionService.listTaggedItemsByTaggingItem");
    	q.setParameter("id", taggingResourceContentItemId);
    		
    	Set<Long> textContentIds = new HashSet<Long>();
    	
		@SuppressWarnings("unchecked")
		Collection<ContentItem> taggedes = q.getResultList();
			
		for (ContentItem tagged : taggedes) {
			if (tagged.getTextContent() != null) {
				textContentIds.add(tagged.getTextContent().getId());
			}
		}
		
		q = entityManager.createNamedQuery("kiwi.informationextraction.informationExtractionService.listInstancesByExtractletName");
		q.setParameter("name", name);
			
		@SuppressWarnings("unchecked")
		Collection<InstanceEntity> instances = q.getResultList();
		for (InstanceEntity instance : instances) {
			
			Suggestion suggestion = new Suggestion();
			suggestion.setKind(Suggestion.TAG);
			suggestion.getResources().add(classifier.getResource());
			suggestion.setLabel(classifier.getResource().getContentItem().getTitle());
			suggestion.setExtractletName(name);
			suggestion.setInstance(instance);
			suggestion.setClassifier(classifier);
			// initial score to zero.
			suggestion.setScore(0);
			
			entityManager.persist(suggestion);
			
			log.info("created suggestion #0", suggestion.getId());
				
			if (instance.getSourceTextContent() != null && textContentIds.contains(instance.getSourceTextContent().getId())) {
				Example example = new Example();
				example.setSuggestion(suggestion);
				example.setType(Example.POSITIVE);
				
				TaggingService taggingService = (TaggingService)Component.getInstance("taggingService");
				
				User user = null;
				List<Tag> tags = taggingService.getTags(instance.getSourceResource().getContentItem());
				for (Tag tag : tags) {
					if (tag.getTaggingResource().getId().equals(taggingResourceContentItemId)) {
						user = tag.getTaggedBy();
					}
				}
				
				if (user == null) {
					log.warn("No author found for tagging for instance #0", instance.getId());
				}
				
				example.setUser(user);
				
				entityManager.persist(example);
				
				log.info("created example #0", example.getId());
			}
			else {
				// do nothing.
			}
		}
	}

	/**
	 * Each document is an instance. We use the bag of stems as features for document classification. 
	 */
	@Override
	public Collection<InstanceEntity> extractInstances(KiWiResource context,
			TextContent content, Document gateDoc, Locale language) {
Collection<InstanceEntity> ret = new LinkedList<InstanceEntity> ();
		
		if (content == null) {
			return ret;
		}
		
		gate.AnnotationSet tokens = gateDoc.getAnnotations().get("Token");
		
		Collection<String> features = new LinkedList<String>();
		for (Annotation token : tokens) {
			gate.FeatureMap f = token.getFeatures();
			String stem = (String)f.get("stem");
			
			features.add(stem);
		}
		
		InstanceEntity inst = new InstanceEntity();
		inst.setSourceResource(context);
		inst.setSourceTextContent(content);
		inst.setContext(new Context());
		inst.setExtractletName(name);
			
		inst.setFeatures(features);
			
		ret.add(inst);
		
		return ret;
	}

}
