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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import kiwi.api.content.ContentItemService;
import kiwi.api.entity.KiWiEntityManager;
import kiwi.api.informationextraction.LabelService;
import kiwi.api.triplestore.TripleStore;
import kiwi.exception.NamespaceResolvingException;
import kiwi.model.Constants;
import kiwi.model.content.ContentItem;
import kiwi.model.content.TextContent;
import kiwi.model.informationextraction.ClassifierEntity;
import kiwi.model.informationextraction.Context;
import kiwi.model.informationextraction.Example;
import kiwi.model.informationextraction.InstanceEntity;
import kiwi.model.informationextraction.Label;
import kiwi.model.informationextraction.Suggestion;
import kiwi.model.kbase.KiWiResource;
import kiwi.model.kbase.KiWiTriple;
import kiwi.model.kbase.KiWiUriResource;
import kiwi.model.ontology.KiWiProperty;
import kiwi.util.KiWiXomUtils;
import kiwi.util.KiWiXomUtils.NodePos;
import kiwi.util.KiWiXomUtils.NodePosIterator;

import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Text;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;

/**
 * 
 * @author Marek Schmidt
 *
 */
@Name("kiwi.informationextraction.labelExtractlet")
@Scope(ScopeType.STATELESS)
public class LabelExtractlet extends AbstractMLExtractlet {

	@Logger
	private Log log;
	
	@In
	TripleStore tripleStore;
	
	@In
	KiWiEntityManager kiwiEntityManager;
	
	@In(value="kiwi.informationextraction.labelService", create=true)
	LabelService labelService;
	
	@In(create=true)
	ContentItemService contentItemService;
	
	public LabelExtractlet() {
		super("kiwi.informationextraction.labelExtractlet");
	}
	
	@Override
	public Suggestion generateSuggestion(InstanceEntity entity, ClassifierEntity classifier) {
		Suggestion ret = super.generateSuggestion(entity, classifier);
		ret.setKind(Suggestion.DATATYPE);
		ret.getRoles().add(classifier.getResource());
		ret.setLabel(entity.getValue());
		
		return ret;
	}
	
	@Override
	public boolean canClassify(KiWiResource resource) {
		
		KiWiUriResource typeDatatypeProperty = tripleStore.createUriResource(Constants.NS_OWL + "DatatypeProperty");
		KiWiResource typeInteger = tripleStore.getXSDType(Integer.class);
				
		if (resource.hasType(typeDatatypeProperty)) {
			return true;
		}
		
		return false;
	}
	
	@Override
	public void initClassifier(ClassifierEntity classifier) {
		
		EntityManager entityManager = (EntityManager)Component.getInstance("entityManager");
		Query q;
		
		KiWiResource classifierResource = (KiWiResource)classifier.getResource();
		
		if (!classifierResource.isUriResource()) {
			log.info("classifier resource #0 is not URI!", classifierResource.getKiwiIdentifier());
			return;
		}
		
		Set<String> potentialKiwiids = new HashSet<String> ();
		for (KiWiTriple triple : tripleStore.getTriplesByP((KiWiUriResource)classifierResource)) {
			potentialKiwiids.add(triple.getSubject().getKiwiIdentifier());
			
			log.info("adding #0 to list of potentialy interesting kiwiids", triple.getSubject().getKiwiIdentifier());
		}
		
		// Get the list of potential labels, that are associated with this classifier entity
		Set<String> potentialLabels = new HashSet<String> ();
		for (Label label : labelService.getLabelsByResource(classifierResource)) {
			potentialLabels.add(label.getString());
		}
			
		q = entityManager.createNamedQuery("kiwi.informationextraction.informationExtractionService.listInstancesByExtractletName");
		q.setParameter("name", name);
			
		@SuppressWarnings("unchecked")
		Collection<InstanceEntity> instances = q.getResultList();
	allInstances:
		for (InstanceEntity instance : instances) {
			
			// skip old textcontents.
			if (!instance.getSourceTextContent().getId().equals(instance.getSourceResource().getContentItem().getTextContent().getId())) {
				continue;
			}
			
			// Skip those instance entites that have labels of resources that don't have this label... 
			if (!potentialLabels.contains(instance.getValue())) {
				continue;
			}
			
			Suggestion suggestion = new Suggestion();
			suggestion.setKind(Suggestion.DATATYPE);
			suggestion.getRoles().add(classifierResource);
			suggestion.setLabel(instance.getValue());
			suggestion.setExtractletName(name);
			suggestion.setInstance(instance);
			suggestion.setClassifier(classifier);
			// initial score to zero.
			suggestion.setScore(0);
			
			entityManager.persist(suggestion);
			
			// log.info("created suggestion #0", suggestion.getId());
				
			if (instance.getSourceTextContent() != null) {

				if (!potentialKiwiids.contains(instance.getSourceResource().getKiwiIdentifier())) {
					// only pages that has the property as a value are potential positive examples...
					continue;
				}
				
				//log.info("potentialy interesting instance: #0", instance.getId());
				
				/*NodePos npBegin = KiWiXomUtils.getNode(instance.getSourceTextContent().getXmlDocument(), 0, instance.getContext().getInBegin());
				NodePos npEnd = KiWiXomUtils.getNode(instance.getSourceTextContent().getXmlDocument(), 0, instance.getContext().getInEnd());
				*/
				
				KiWiXomUtils.NodePosIterator npiter = new KiWiXomUtils.NodePosIterator(instance.getSourceTextContent().getXmlDocument());
				while(npiter.hasNext()) {
					NodePos np = npiter.next();
					
					if (np.getPos() == instance.getContext().getInBegin()) {
						
						if (np.getNode() instanceof Element) {
														
							Element e = (Element)np.getNode();
							if (e.getAttribute("property") != null) {
								log.info("found element with property");
								String propertyAttr = e.getAttributeValue("property");
								String[] properties = propertyAttr.trim().split(" ");
								for (String property : properties) {
									// log.info("comparing property")
									if (classifierResource.getKiwiIdentifier().equals(property)) {
										
										Example example = new Example();
										example.setSuggestion(suggestion);
										example.setType(Example.POSITIVE);
										example.setUser(instance.getSourceResource().getContentItem().getAuthor());
										
										entityManager.persist(example);
										
										//log.info("created example #0", example.getId());
										
										//log.info("np.getPos = #0, ctx inBegin: #1", np.getPos(), instance.getContext().getInBegin());
										
										continue allInstances;
									}
								}
							}
						}
					}
				}		
			}
			else {
				// do nothing.
			}
		}
	}

	@Override
	public Collection<InstanceEntity> extractInstances(KiWiResource context,
			TextContent content, Document gateDoc, Locale language) {
		Collection<InstanceEntity> ret = new LinkedList<InstanceEntity> ();
		
		if (content == null) {
			return ret;
		}
		
		String plain = (String)gateDoc.getFeatures().get(Document.DOCUMENT_STRING_CONTENT_PARAMETER_NAME);
		
		gate.AnnotationSet tokens = gateDoc.getAnnotations().get("Token");
		gate.AnnotationSet lookups = gateDoc.getAnnotations().get("Lookup");
		
		for (Annotation lookup : lookups) {
			gate.FeatureMap f = lookup.getFeatures();
			String majorType = (String)f.get("majorType");
			String minorType = (String)f.get("minorType");
						
			if ("label".equals(majorType)) {
				
				/*Long labelId = Long.valueOf(minorType, 16);
				Label l = entityManager.find(Label.class, labelId);
				if (l == null) {
					continue;
				}*/
				
				InstanceEntity inst = new InstanceEntity();
				inst.setSourceResource(context);
				inst.setSourceTextContent(content);
				inst.setExtractletName(name);
					
				int begin = lookup.getStartNode().getOffset().intValue();
				int end = lookup.getEndNode().getOffset().intValue();
					
				inst.setValue(plain.substring(begin, end));
					
				Context ctx = generateBlockContext(content.getXmlDocument(), 
						begin, end
						);
					
				inst.setContext(ctx);
					
				// TODO: modify the method to generate the feature set directly instead of using string builder
				String featstr = generateContextFeatures(gateDoc, lookup.getStartNode().getOffset(), lookup.getEndNode().getOffset(), ctx);
				List<String> features = new LinkedList<String>();
				for (String ff : featstr.split(" ")) {
					features.add(ff);
				}
				inst.setFeatures(features);
				
				//log.info("creating numberextractlet instance #0", inst.getContext().toString());
					
				ret.add(inst);
			}
			
			//features.add(stem);
		}
		
		return ret;
	}

private void generateFeaturesFromGateTokens (List<gate.Annotation> tokens, String prefix, StringBuilder sb) {
		
		Set<String> nouns = new HashSet<String> ();
		nouns.add("NN");
		nouns.add("NNP");
		nouns.add("NNS");
		nouns.add("NNPS");
		
		int i = 0;
		for (Annotation token : tokens) {
			gate.FeatureMap f = token.getFeatures();
			String stem = (String)f.get("stem");
			String category = (String)f.get("category");
			String orth = (String)f.get("orth");
			
			// with position 
			if (i < 4) {
				sb.append(' ');
				sb.append(prefix);
				sb.append(i);
				sb.append('s');
				sb.append(stem);
		
				sb.append(' ');
				sb.append(prefix);
				sb.append(i);
				sb.append('c');
				sb.append(category);
			
				sb.append(' ');
				sb.append(prefix);
				sb.append(i);
				sb.append('o');
				sb.append(orth);
			}
			
			// without position
			if (nouns.contains(category)) {
				sb.append(' ');
				sb.append(prefix);
				sb.append('s');
				sb.append(stem);
			}
			
			++i;
			
			if (i > 10) break;
		}
	}
	
	/**
	 * Generate a context for a fragment specified by text coordinates.
	 * The context will be based on the the smallest common XML subtree.
	 * @param doc
	 * @param begin
	 * @return
	 */
	private Context generateBlockContext(nu.xom.Document doc, int begin, int end) {
		
		NodePos npBegin = KiWiXomUtils.getNode(doc, 0, begin);
		NodePos npEnd = KiWiXomUtils.getNode(doc, 0, end);
			
		Node parent = KiWiXomUtils.getLeastCommonParent(npBegin.getNode(), npEnd.getNode());
		
		Node contextBegin = KiWiXomUtils.getFirstTextNode(parent);
		Node contextEnd = KiWiXomUtils.getLastTextNode(parent);
		
		int contextBeginPos = -1;
		int contextEndPos = -1;
		
		NodePosIterator npi = new NodePosIterator(doc);
		while(npi.hasNext()) {
			NodePos np = npi.next();
			
			if (np.getNode().equals(contextBegin)) {
				contextBeginPos = np.getPos();
			}
			if (np.getNode().equals(contextEnd)) {
				if (contextEnd instanceof Text) {
					contextEndPos = np.getPos() + ((Text)contextEnd).getValue().length();
				}
			}
		}
		
		String plain = KiWiXomUtils.xom2plain(doc);
		if (contextBeginPos < 0) contextBeginPos = 0;
		if (contextEndPos < 0 || contextEndPos > plain.length()) contextEndPos = plain.length();
		
		Context ret = new Context();
		ret.setLeftContext(plain.substring(contextBeginPos, begin));
		ret.setInContext(plain.substring(begin, end));
		ret.setRightContext(plain.substring(end, contextEndPos));
		ret.setContextBegin(contextBeginPos);
		ret.setContextEnd(contextEndPos);
		ret.setInBegin(begin);
		ret.setInEnd(end);
		ret.setIsFragment(true);
		
		return ret;
	}
	

	private String generateContextFeatures(gate.Document doc, long contextAnnotationBegin, long contextAnnotationEnd, Context ctx) {

		gate.AnnotationSet tokens = doc.getAnnotations().get("Token");
		gate.AnnotationSet preTokensAS = null;
		gate.AnnotationSet postTokensAS = null;
		gate.AnnotationSet inTokensAS = null;
			
		preTokensAS = tokens.getContained((long)ctx.getContextBegin(), contextAnnotationBegin);
		postTokensAS = tokens.getContained(contextAnnotationEnd, (long)ctx.getContextEnd());
		inTokensAS = tokens.getContained(contextAnnotationBegin, contextAnnotationEnd);

		LinkedList<Annotation> preTokens = new LinkedList<Annotation> ();
		LinkedList<Annotation> postTokens = new LinkedList<Annotation> ();
		LinkedList<Annotation> inTokens = new LinkedList<Annotation> ();
		
		preTokens.addAll(preTokensAS);
		postTokens.addAll(postTokensAS);
		inTokens.addAll(inTokensAS);
		
		Comparator<Annotation> annotationComparator =  new Comparator<Annotation>() {
			@Override
			public int compare(Annotation o1, Annotation o2) {
				return o1.getStartNode().getOffset().compareTo(o2.getStartNode().getOffset());
		}};
		
		Comparator<Annotation> annotationComparatorReverse =  new Comparator<Annotation>() {
			@Override
			public int compare(Annotation o1, Annotation o2) {
				return o2.getStartNode().getOffset().compareTo(o1.getStartNode().getOffset());
		}};
			
		Collections.sort(preTokens, annotationComparatorReverse);
		Collections.sort(postTokens, annotationComparator);
		Collections.sort(inTokens, annotationComparator);
		
		StringBuilder sb = new StringBuilder();
		generateFeaturesFromGateTokens (preTokens, "-", sb);
		generateFeaturesFromGateTokens (postTokens, "+", sb);
		generateFeaturesFromGateTokens (inTokens, "", sb);
				
		return sb.toString();
	}
	
	public Collection<Example> extractExamples(KiWiResource context, TextContent content, Document gateDoc, Locale language, Collection<Suggestion> suggestions, Collection<InstanceEntity> instances) {
		
		// extract all datatype properties as instances and save them in the label service...
		// TODO, produce actual examples also...
		
		KiWiXomUtils.NodePosIterator npiter = new KiWiXomUtils.NodePosIterator(content.getXmlDocument());
		while(npiter.hasNext()) {
			NodePos np = npiter.next();
			
			if (np.getNode() instanceof Element) {
												
				Element e = (Element)np.getNode();
				if (e.getAttribute("property") != null) {
					log.info("found element with property");
					String propertyAttr = e.getAttributeValue("property");
					String[] properties = propertyAttr.trim().split(" ");
					for (String property : properties) {
					
						ContentItem propertyItem = contentItemService.getContentItemByKiwiId(property);
						if (propertyItem == null) {
							continue;
						}
						
						// log.info("comparing property")
						String labelString = KiWiXomUtils.xom2plain(e);
						
						Label label = new Label();
						label.setResource(propertyItem.getResource());
						label.setType(Label.TYPE_PROPERTY_LITERAL);
						label.setString(labelString);
						
						labelService.storeLabel(label);
						
						/*int begin = np.getPos();
						int end = begin + label.length();
						
						InstanceEntity inst = new InstanceEntity();
						inst.setSourceResource(context);
						inst.setSourceTextContent(content);
						inst.setExtractletName(name);
													
						inst.setValue(label);
							
						Context ctx = generateBlockContext(content.getXmlDocument(), 
								begin, end
								);
							
						inst.setContext(ctx);
							
						// TODO: modify the method to generate the feature set directly instead of using string builder
						String featstr = generateContextFeatures(gateDoc, begin, end, ctx);
						List<String> features = new LinkedList<String>();
						for (String ff : featstr.split(" ")) {
							features.add(ff);
						}
						inst.setFeatures(features);
						
						Suggestion suggestion = new Suggestion();
						suggestion.setKind(Suggestion.DATATYPE);
						suggestion.getResources().add(classifierResource);
						suggestion.setLabel(label);
						suggestion.setExtractletName(name);
						suggestion.setInstance(inst);
						suggestion.setClassifier(classifier);
						// initial score to zero.
						suggestion.setScore(0);
						
						Example example = new Example();
						example.setSuggestion(suggestion);
						example.setType(Example.POSITIVE);
						example.setUser(null);*/
								
						// entityManager.persist(example);
					}
				}
			}
		}
		
		return Collections.emptyList();
	}
}
