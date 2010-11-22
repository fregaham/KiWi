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
import gate.AnnotationSet;
import gate.Document;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.persistence.Query;

import nu.xom.Element;
import nu.xom.Nodes;
import nu.xom.XPathContext;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;

import cc.mallet.fst.CRF;
import cc.mallet.fst.CRFTrainerByLabelLikelihood;
import cc.mallet.fst.SimpleTagger;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Sequence;

import kiwi.api.informationextraction.Extractlet;
import kiwi.api.informationextraction.FeatureStoreService;
import kiwi.api.triplestore.TripleStore;
import kiwi.model.Constants;
import kiwi.model.content.TextContent;
import kiwi.model.informationextraction.ClassifierEntity;
import kiwi.model.informationextraction.Context;
import kiwi.model.informationextraction.Example;
import kiwi.model.informationextraction.InstanceEntity;
import kiwi.model.informationextraction.Suggestion;
import kiwi.model.kbase.KiWiResource;
import kiwi.model.kbase.KiWiUriResource;
import kiwi.util.KiWiXomUtils;

/**
 *
 * An extractlet that uses sequence tagging (CRF) to extract datatype properties (such as, foaf:firstName)
 * 
 * @author Marek Schmidt
 *
 */
@Name("kiwi.informationextraction.sequenceExtractlet")
@Scope(ScopeType.STATELESS)
public class SequenceExtractlet extends AbstractExtractlet implements
		Extractlet {
	
	@Logger
	private Log log;
	
	@In(value="kiwi.informationextraction.featureStoreService", create=true)
	FeatureStoreService featureStoreService;

	@In
	TripleStore tripleStore;
	
	public SequenceExtractlet() {
		super("kiwi.informationextraction.sequenceExtractlet");
	}
	
	@Override
	public boolean canClassify(KiWiResource resource) {
		
		KiWiUriResource typeDatatypeProperty = tripleStore.createUriResource(Constants.NS_OWL + "DatatypeProperty");				
		return resource.hasType(typeDatatypeProperty);
	}
	
	private List<Annotation> getTokenList(Document gateDoc) {
		AnnotationSet as = gateDoc.getAnnotations();
		AnnotationSet tokens = as.get("Token");
		List<Annotation> tokenAnnotations = new LinkedList<Annotation>();
		tokenAnnotations.addAll(tokens);
		
		Comparator<Annotation> annotationComparator =  new Comparator<Annotation>() {
			@Override
			public int compare(Annotation o1, Annotation o2) {
				return o1.getStartNode().getOffset().compareTo(o2.getStartNode().getOffset());
			}};

		Collections.sort(tokenAnnotations, annotationComparator);
		
		return tokenAnnotations;
	}
	
	private Instance exampleToInstance(Example example) {
					
		InstanceEntity instance = example.getSuggestion().getInstance();
		if (instance.getFeatures() == null) {
			instance.setFeatures(featureStoreService.get(getName(), instance.getId()));
		}
		
		Context ctx = instance.getContext();
		
		StringBuilder sb = new StringBuilder();
		for (String token : instance.getFeatures()) {
			String[] tokenFeatures = token.split("-");
			int tokenBegin = Integer.parseInt(tokenFeatures[tokenFeatures.length - 2]);
			int tokenEnd = Integer.parseInt(tokenFeatures[tokenFeatures.length - 1]);
			
			for (int i = 0; i < tokenFeatures.length - 2; ++i) {
				if (i > 0) {
					sb.append(' ');
				}
				
				sb.append(tokenFeatures[i]);				
			}
			
			sb.append(' ');
			if (example.getType() == Example.POSITIVE && tokenBegin >= ctx.getInBegin() && tokenEnd <= ctx.getInEnd()) {
				sb.append('I');
			}
			else {
				sb.append('O');
			}
			
			sb.append('\n');
		}
		
		return new Instance(sb.toString(), null, null, null);// p.pipe(new Instance(sb.toString(), null, null, null));
	}
	
	@Override
	public void initClassifier(ClassifierEntity classifier) {
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void trainClassifier(ClassifierEntity classifier) {
		Query q;
		
		Pipe p = new SimpleTagger.SimpleTaggerSentence2FeatureVectorSequence();
		p.getTargetAlphabet().lookupIndex("O");
		
		p.setTargetProcessing(true);
		
		InstanceList list = new InstanceList(p);
		
		
		log.info("trainClassifier");
		
		q = entityManager.createNamedQuery("kiwi.informationextraction.informationExtractionService.listExamplesByClassifier");
		q.setParameter("classifier", classifier);
		
		// FeatureStoreService featureStore = (FeatureStoreService)Component.getInstance("kiwi.informationextraction.featureStoreService");
		
		Collection<Example> examples = q.getResultList();
		for (Example x : examples) {
			list.addThruPipe(exampleToInstance(x));
		}
				
		CRF crf = new CRF(list.getPipe(), (Pipe)null);
		
		crf.addFullyConnectedStatesForLabels();
		
		crf.setWeightsDimensionAsIn(list, false);
		
		CRFTrainerByLabelLikelihood trainer = new CRFTrainerByLabelLikelihood(crf);
		trainer.train(list);
		
		classifier.setMallet(crf);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Collection<Suggestion> extract(KiWiResource context,
			TextContent content, Document gateDoc, Locale language) {
		
		if (content == null) {
			return Collections.emptyList();
		}
		
		List<Annotation> tokenAnnotations = getTokenList(gateDoc);
		
		
		Collection<Suggestion> ret = new LinkedList<Suggestion> ();
		
		Query q = entityManager.createNamedQuery("kiwi.informationextraction.informationExtractionService.listClassifiersByExtractletName");
		q.setParameter("name", this.name);
				
		Collection<ClassifierEntity> classifiers = (Collection<ClassifierEntity>) q.getResultList();
		
		for (ClassifierEntity classifier : classifiers) {
			ret.addAll(this.extract(context, content, gateDoc, language, tokenAnnotations, classifier));
		}
		
		return ret;
	}
	
	private Instance documentToInstance(List<Annotation> tokenAnnotations) {		
		StringBuilder sb = new StringBuilder();
		for(Annotation token : tokenAnnotations) {
			gate.FeatureMap f = token.getFeatures();
			String category = (String)f.get("category");
			String stem = (String)f.get("stem");
			
			sb.append(stem.replace('-', '_'));
			sb.append(' ');
			sb.append(category.replace('-', '_'));
			sb.append('\n');
		}
		
		return new Instance(sb.toString(), null, null, null);
	}
	
	private Collection<Suggestion> extract(KiWiResource context, TextContent content, Document gateDoc, Locale locale, List<Annotation> tokenAnnotations, ClassifierEntity classifier) {
		
		String plain = (String)gateDoc.getFeatures().get(Document.DOCUMENT_STRING_CONTENT_PARAMETER_NAME);
		
		Collection<Suggestion> ret = new LinkedList<Suggestion> ();
		
		CRF crf = (CRF)classifier.getMallet();
		
		if (crf == null) {
			return ret;
		}
		
		Pipe p = crf.getInputPipe();
		p.setTargetProcessing(false);
		
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		crf.print(pw);
		
		// log.info("CRF: #0", sw.getBuffer().toString());
		
		Instance featureVectorSequence = p.pipe(documentToInstance(tokenAnnotations));
		
		try {
			Sequence<?> seq = (Sequence<?>) featureVectorSequence.getData();
			Sequence<?> outputSequence = crf.transduce(seq);
			
			List<int[]> spans = new LinkedList<int[]>();
			
			log.info("outputSequence: #0, tokenAnnotations: #1", outputSequence.size(), tokenAnnotations.size());
			
			boolean inside = false;
			int begin = 0;
			for (int i = 0; i < outputSequence.size(); ++i) {
				
				String outputLabel = outputSequence.get(i).toString();
				// Annotation token = tokenAnnotations.get(i);
				
				if ("I".equals(outputLabel)) {
					if (!inside) {
						begin = tokenAnnotations.get(i).getStartNode().getOffset().intValue();
						inside = true;
					}
				}
				else {
					if (inside) {
						int[] span = new int[2];
						span[0] = begin;
						span[1] = tokenAnnotations.get(i - 1).getEndNode().getOffset().intValue();
						spans.add(span);
						inside = false;
					}
				}
			}
			
			if (inside) {
				int[] span = new int[2];
				span[0] = begin;
				span[1] = tokenAnnotations.get(outputSequence.size() - 1).getEndNode().getOffset().intValue();
				spans.add(span);
			}
			
			for (int[] span : spans) {
				Suggestion es = suggestionFromSpan(context, content, gateDoc, plain, classifier, span);
				if (es != null) {
					// Compute the feedback score, 
					int pos = 0, neg = 0;
					for (Example example : getExamples(es)) {
						if (example.getType() == Example.POSITIVE) {
							pos++;
						}
						else {
							neg++;
						}
					}
					
					// log.info("score for #0: #1 / #2 = #3", suggestion.getLabel(), ((float)pos + 1), (float)(pos + neg + 1), ((float)pos + 1) / (float)(pos + neg + 1));
					
					// compute the score as a probability (fraction of positive vs. all) with add 1 smoothing
					float score = ((float)pos + 1) / (float)(pos + neg + 1);
					es.setFeedbackScore(score);
					
					// TODO: score by likelihood
					es.setScore(score);
					
					ret.add(es);
				}
			}			
		}
		catch(Throwable x) {
			log.error("Error transducing", x);
			return ret;
		}
		
		
		return ret;
	}
	
	private Suggestion suggestionFromSpan(KiWiResource context, TextContent content, Document gateDoc, String plain, ClassifierEntity classifier, int[] span) {
		AnnotationSet sentences = gateDoc.getAnnotations().getCovering("Sentence", (long)span[0], (long)span[1]);
		if (sentences.size() != 1) {
			log.info("No or too many sentences covering #0:#1", span[0], span[1]);
			return null;
		}
		
		Annotation sentence = sentences.iterator().next();
		AnnotationSet tokensInSentence = gateDoc.getAnnotations().get("Token").getContained(sentence.getStartNode().getOffset(), sentence.getEndNode().getOffset());

		List<Annotation> tokensInSentenceList = new LinkedList<Annotation>();
		tokensInSentenceList.addAll(tokensInSentence);
		
		Comparator<Annotation> annotationComparator =  new Comparator<Annotation>() {
			@Override
			public int compare(Annotation o1, Annotation o2) {
				return o1.getStartNode().getOffset().compareTo(o2.getStartNode().getOffset());
			}};

		Collections.sort(tokensInSentenceList, annotationComparator);
		
		StringBuilder preContext = new StringBuilder();
		StringBuilder inContext = new StringBuilder();
		StringBuilder postContext = new StringBuilder();
		List<String> features = new LinkedList<String>();
		for (Annotation token : tokensInSentenceList) {
			gate.FeatureMap f = token.getFeatures();
			String string = plain.substring(token.getStartNode().getOffset().intValue(), token.getEndNode().getOffset().intValue());
			String category = ((String)f.get("category")).replace('-', '_');
			String stem = ((String)f.get("stem")).replace('-', '_');
			
			// we can't use whitespace inside the strings (they are used to separate tokens in the feature store.
			String strtoken = stem + '-' + category + '-' + token.getStartNode().getOffset().toString() + '-' + token.getEndNode().getOffset().toString();
			features.add(strtoken);
			
			StringBuilder b;
			if (span[0] > token.getStartNode().getOffset()) {
				b = preContext;
			}
			else if (span[1] < token.getEndNode().getOffset()) {
				b = postContext;
			}
			else {
				b = inContext;
			}
			
			if (b.length() > 0) {
				b.append(' ');
			}
			b.append(string);
		}			

		Suggestion es = new Suggestion();
		es.setExtractletName(getName());
		es.setKind(Suggestion.DATATYPE);
		es.setLabel(classifier.getResource().getLabel());
		es.setClassifier(classifier);
		es.getRoles().add(classifier.getResource());
							
		InstanceEntity inst = new InstanceEntity();
		inst.setExtractletName(name);
		inst.setSourceResource(context);
		inst.setSourceTextContent(content);
		inst.setFeatures(features);
			
		Context ctx = new Context();
		ctx.setContextBegin(sentence.getStartNode().getOffset().intValue());
		ctx.setContextEnd(sentence.getEndNode().getOffset().intValue());
		ctx.setInBegin(span[0]);
		ctx.setInEnd(span[1]);
		ctx.setInContext(inContext.toString());
		ctx.setLeftContext(preContext.toString());
		ctx.setRightContext(postContext.toString());
		ctx.setIsFragment(true);
			
		inst.setContext(ctx);
			
		es.setInstance(inst);
		
		es.setScore(1.0f);
		
		return es;
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public Collection<Example> extractExamples(KiWiResource context,
			TextContent content, Document gateDoc, Locale language,
			Collection<Suggestion> suggestions,
			Collection<InstanceEntity> instances) {
		
		String plain = (String)gateDoc.getFeatures().get(Document.DOCUMENT_STRING_CONTENT_PARAMETER_NAME);
		
		Collection<Example> ret = new LinkedList<Example>();

		Query q = entityManager.createNamedQuery("kiwi.informationextraction.informationExtractionService.listClassifiersByExtractletName");
		q.setParameter("name", this.name);
				
		Collection<ClassifierEntity> classifiers = (Collection<ClassifierEntity>) q.getResultList();
		
		for (ClassifierEntity classifier : classifiers) {
			KiWiResource resource = classifier.getResource();
			
			nu.xom.Document xom = content.getXmlDocument();
			
			// Get the links as entity suggestions.
			XPathContext namespaces = new XPathContext();
			namespaces.addNamespace("kiwi", Constants.NS_KIWI_HTML);
			namespaces.addNamespace("html", Constants.NS_XHTML);
			
			Nodes properties = xom.query(
					"//html:span[@property='" + resource.getKiwiIdentifier() + "']", namespaces);
			
			for(int i=0; i < properties.size(); i++) {
				
				Element property = (Element)properties.get(i);
				
				int[] span = KiWiXomUtils.getNodeSpan(xom.getRootElement(), 0, property);
				
				Suggestion es = suggestionFromSpan(context, content, gateDoc, plain, classifier, span);
				if (es != null) {
					Example example = new Example();
					example.setSuggestion(es);
					example.setType(Example.POSITIVE);
					example.setUser(null);
						
					ret.add(example);
				}
			}
		}
		
		return ret;
	}
}
