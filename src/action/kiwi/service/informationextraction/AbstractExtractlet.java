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

import gate.Document;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.jboss.seam.Component;
import org.jboss.seam.annotations.In;

import kiwi.api.informationextraction.Extractlet;
import kiwi.api.informationextraction.FeatureStoreService;
import kiwi.model.content.TextContent;
import kiwi.model.informationextraction.ClassifierEntity;
import kiwi.model.informationextraction.Context;
import kiwi.model.informationextraction.Example;
import kiwi.model.informationextraction.InstanceEntity;
import kiwi.model.informationextraction.Suggestion;
import kiwi.model.kbase.KiWiResource;
import kiwi.model.user.User;

/**
 * 
 * @author Marek Schmidt
 *
 */
public abstract class AbstractExtractlet implements Extractlet {
	protected String name;
	
	@In
	EntityManager entityManager;
	
	public AbstractExtractlet(String name) {
		this.name = name;
	}
	
	@Override
	public String getName() {
		return this.name;
	}
	
	@Override
	public boolean canClassify(KiWiResource resource) {
		return false;
	}
	
	@Override
	public void accept(Suggestion suggestion, User user) {
				
		if (suggestion.getId() == null) {
			entityManager.persist(suggestion);
			if (suggestion.getInstance().getId() == null) {
				InstanceEntity instance = suggestion.getInstance();
				entityManager.persist(instance);
				if (instance.isHasFeatures() && instance.getFeatures() != null) {
					FeatureStoreService featureStore = (FeatureStoreService)Component.getInstance("kiwi.informationextraction.featureStoreService");
					featureStore.put(getName(), instance.getId(), instance.getFeatures());
				}
			}
		}
		else if (!entityManager.contains(suggestion)) {
			suggestion = entityManager.merge(suggestion);
		}
		
		Example example = new Example();
		example.setSuggestion(suggestion);
		example.setUser(user);
		example.setType(Example.POSITIVE);
		
		
		entityManager.persist(example);
	}
	
	@Override
	public void reject(Suggestion suggestion, User user) {
				
		if (suggestion.getId() == null) {
			entityManager.persist(suggestion);
			if (suggestion.getInstance().getId() == null) {
				InstanceEntity instance = suggestion.getInstance();
				entityManager.persist(instance);
				if (instance.isHasFeatures() && instance.getFeatures() != null) {
					FeatureStoreService featureStore = (FeatureStoreService)Component.getInstance("kiwi.informationextraction.featureStoreService");
					featureStore.put(getName(), instance.getId(), instance.getFeatures());
				}
			}
		}
		else if (!entityManager.contains(suggestion)) {
			suggestion = entityManager.merge(suggestion);
		}
		
		Example example = new Example();
		example.setSuggestion(suggestion);
		example.setUser(user);
		example.setType(Example.NEGATIVE);
		
		
		entityManager.persist(example);
	}
	
	/**
	 * Returns the examples that matches the specified suggestion (e.g. for listing all the examples with the same context)
	 * @param s
	 * @return
	 */
	protected Collection<Example> getExamples(Suggestion s) {
						
		Collection<Example> ret = new LinkedList<Example>();
		
		EntityManager em = (EntityManager)Component.getInstance("entityManager");
		
		Query q = em.createNamedQuery("kiwi.informationextraction.informationExtractionService.listExamplesByContextHashAndExtractletName");
		q.setParameter("hash", s.getInstance().getContextHash());
		q.setParameter("name", this.name);
		@SuppressWarnings("unchecked")
		List<Example> examples = (List<Example>)q.getResultList();
		for (Example example : examples) {
						
			Context suggestionContext = s.getInstance().getContext();
			Context exampleContext = example.getSuggestion().getInstance().getContext();
			
			if (suggestionContext.getInContext() != null) {
				if (!suggestionContext.getInContext().equals(exampleContext.getInContext())) {
					continue;
				}
			}
			else {
				if (exampleContext.getInContext() != null) {
					continue;
				}
			}
			
			if (suggestionContext.getLeftContext() != null) {
				if (!suggestionContext.getLeftContext().equals(exampleContext.getLeftContext())) {
					continue;
				}
			}
			else {
				if (exampleContext.getLeftContext() != null) {
					continue;
				}
			}
			
			if (suggestionContext.getRightContext() != null) {
				if (!suggestionContext.getRightContext().equals(exampleContext.getRightContext())) {
					continue;
				}
			}
			else {
				if (exampleContext.getRightContext() != null) {
					continue;
				}
			}
			
			ret.add(example);
		}
		
		return ret;
	}
	
	@Override
	public Collection<Suggestion> extract(KiWiResource context, TextContent content, Document gateDoc, Locale language) {
		return new LinkedList<Suggestion> ();
	}
	
	
	@Override
	public Collection<Suggestion> extract(Collection<InstanceEntity> instances) {
		return new LinkedList<Suggestion> ();
	}
	
	@Override
	public void initClassifier(ClassifierEntity entity) {
	}
	
	@Override
	public Collection<InstanceEntity> extractInstances(KiWiResource context, TextContent content, Document gateDoc, Locale language) {
		return new LinkedList<InstanceEntity> ();
	}
	
	@Override
	public void updateSuggestions(ClassifierEntity classifier, Collection<Suggestion> suggestions) {
		
	}
	
	@Override
	public Collection<Suggestion> classifyInstances(ClassifierEntity classifier, Collection<InstanceEntity> instances) {
		return null;
	}
	
	@Override
	public void trainClassifier(ClassifierEntity entity) {
		
	}
	
	@Override
	public Collection<Example> extractExamples(KiWiResource context, TextContent content, Document gateDoc, Locale language, Collection<Suggestion> suggestions, Collection<InstanceEntity> instances) {
		return Collections.emptyList();
	}
}
