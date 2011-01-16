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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Locale;
import java.util.logging.Logger;

import javax.persistence.FlushModeType;
import javax.persistence.Query;

import org.jboss.seam.Component;

import cc.mallet.classify.Classification;
import cc.mallet.classify.Classifier;
import cc.mallet.classify.ClassifierTrainer;
import cc.mallet.classify.NaiveBayes;
import cc.mallet.classify.NaiveBayesTrainer;
import cc.mallet.pipe.FeatureSequence2FeatureVector;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.Target2Label;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Label;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.types.Labeling;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

import kiwi.api.informationextraction.FeatureStoreService;
import kiwi.model.content.TextContent;
import kiwi.model.informationextraction.ClassifierEntity;
import kiwi.model.informationextraction.Example;
import kiwi.model.informationextraction.InstanceEntity;
import kiwi.model.informationextraction.Suggestion;
import kiwi.model.kbase.KiWiResource;

/**
 * An abstract class implementing common functionality for Machine Learning Extractlets
 * 
 * @author Marek Schmidt
 *
 */
public abstract class AbstractMLExtractlet extends AbstractExtractlet {
		
	public AbstractMLExtractlet(String name) {
		super(name);
	}
	
	protected Suggestion generateSuggestion(InstanceEntity entity, ClassifierEntity classifier) {
		Suggestion ret = new Suggestion();
		
		ret.setClassifier(classifier);
		ret.setExtractletName(this.name);
		ret.setInstance(entity);
		
		return ret;
	}
	
	@Override
	public void initClassifier(ClassifierEntity classifier) {
	}
	
	@Override
	public Collection<Suggestion> extract(Collection<InstanceEntity> instances) {
		Collection<Suggestion> ret = new LinkedList<Suggestion> ();
						
		Query q = entityManager.createNamedQuery("kiwi.informationextraction.informationExtractionService.listClassifiersByExtractletName");
		q.setParameter("name", this.name);
				
		@SuppressWarnings("unchecked")
		Collection<ClassifierEntity> classifiers = (Collection<ClassifierEntity>) q.getResultList();
		
		for (ClassifierEntity classifier : classifiers) {
			Collection<Suggestion> suggestions = classifyInstances(classifier, instances);
			ret.addAll(suggestions);
		}
		
		return ret;
	}
	
	protected Collection<String> getInstanceFeatures(InstanceEntity instance) {
		Collection<String> features = instance.getFeatures();
		
		if (features == null) {
			FeatureStoreService featureStore = (FeatureStoreService)Component.getInstance("kiwi.informationextraction.featureStoreService");
			features = featureStore.get(instance.getExtractletName(), instance.getId());
			instance.setFeatures(features);
		}
		
		return features;
	}
	
	@Override
	public abstract Collection<InstanceEntity> extractInstances(KiWiResource context, TextContent content, Document gateDoc, Locale language);
	
	@Override
	public void updateSuggestions(ClassifierEntity classifier, Collection<Suggestion> suggestions) {
		
		Logger log = Logger.getLogger("AbstractMLExtractlet");
		log.info("updateSuggestions");
		
		entityManager.setFlushMode(FlushModeType.COMMIT);

		if (classifier.getMallet() == null) {
			log.info("mallet classifier == null!");
			return;
		}
		
		Classifier malletClassifier = (Classifier)classifier.getMallet();
		Pipe instancePipe = malletClassifier.getInstancePipe();
		
		Alphabet alphabet = malletClassifier.getAlphabet();
		LabelAlphabet labelAlphabet = malletClassifier.getLabelAlphabet();
				
		assert instancePipe != null;
		assert alphabet != null;
		assert labelAlphabet != null;
		assert malletClassifier != null;
		
		Label posLabel = labelAlphabet.lookupLabel("+");
		
		for (Suggestion suggestion : suggestions) {
					
			InstanceEntity instance = suggestion.getInstance();
			
			Collection<String> features = getInstanceFeatures(instance);
			
			TokenSequence tokens = new TokenSequence();
			for (String feature : features) {
				assert feature != null;
				tokens.add(new Token(feature));
			}
			
			Classification malletClassification = malletClassifier.classify(tokens);
			Labeling malletLabeling = malletClassification.getLabeling();
			float malletScore = (float)malletLabeling.value(posLabel);
		
			log.info("updateSuggestion: " + name + " " + suggestion.getInstance().getSourceResource().getContentItem().getTitle() + " " + malletScore);
		
			suggestion.setScore(malletScore);
		}
		
		log.info("updateSuggestions close");
	}
	
	@Override
	public Collection<Suggestion> classifyInstances(ClassifierEntity classifier, Collection<InstanceEntity> instances) {
		
		Collection<Suggestion> ret = new LinkedList<Suggestion> ();
		for (InstanceEntity instance : instances) {
			Suggestion suggestion = generateSuggestion (instance, classifier);
			if (suggestion != null) {
				ret.add(suggestion);
			}
		}
		
		updateSuggestions(classifier, ret);
		
		return ret;
	}

	
	@Override
	public void trainClassifier(ClassifierEntity classifier) {
		Query q;
		
		Logger log = Logger.getLogger("AbstractMLExtractlet");
		log.info("trainClassifier");
		
		entityManager.setFlushMode(FlushModeType.COMMIT);
		
		Alphabet instanceAlphabet = new Alphabet();
		
		LabelAlphabet labelAlphabet = new LabelAlphabet();
		labelAlphabet.lookupLabel("+", true);
		labelAlphabet.lookupLabel("-", true);
		
		ArrayList<Pipe> featurePipeList = new ArrayList<Pipe>();
		featurePipeList.add(new TokenSequence2FeatureSequence(instanceAlphabet));
		featurePipeList.add(new FeatureSequence2FeatureVector());
		featurePipeList.add(new Target2Label(labelAlphabet));
		
		
		Pipe featurePipe = new SerialPipes(featurePipeList);
		featurePipe.setTargetProcessing(false);
			
		InstanceList ilist = new InstanceList(featurePipe);
		
		q = entityManager.createNamedQuery("kiwi.informationextraction.informationExtractionService.listExamplesByClassifier");
		q.setParameter("classifier", classifier);
		
		FeatureStoreService featureStore = (FeatureStoreService)Component.getInstance("kiwi.informationextraction.featureStoreService");
		
		@SuppressWarnings("unchecked")
		Collection<Example> examples = q.getResultList();
		for (Example x : examples) {
						
			InstanceEntity instanceEntity = x.getSuggestion().getInstance();
			Collection<String> features = featureStore.get(instanceEntity.getExtractletName(), instanceEntity.getId());
			
			TokenSequence tokens = new TokenSequence();
			for (String feature : features) {
				tokens.add(new Token(feature));
			}
			
			Instance malletInstance = new Instance(tokens, null, null, null);
			
			if (x.getType() == Example.POSITIVE) {
				malletInstance.setTarget("+");
				
				log.info("example + " + features.toString());
			}
			else if (x.getType() == Example.NEGATIVE) {
				malletInstance.setTarget("-");
				
				log.info("example - " + features.toString());
			}
			else {
				continue;
			}
				
			ilist.addThruPipe(malletInstance);
		}
		
		if (ilist.size() > 0) {
			ClassifierTrainer<NaiveBayes> trainer = new NaiveBayesTrainer();
			NaiveBayes malletClassifier = trainer.train(ilist);

			classifier.setMallet(malletClassifier);
		}
		
		log.info("trainClassifier end");
	}
}