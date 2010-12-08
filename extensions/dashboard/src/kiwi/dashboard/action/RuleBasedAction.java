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
package kiwi.dashboard.action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

import kiwi.api.content.ContentItemService;
import kiwi.api.entity.KiWiEntityManager;
import kiwi.api.tagging.TaggingService;
import kiwi.api.triplestore.TripleStore;
import kiwi.model.Constants;
import kiwi.model.content.ContentItem;
import kiwi.model.kbase.KiWiLiteral;
import kiwi.model.kbase.KiWiTriple;
import kiwi.model.kbase.KiWiUriResource;
import kiwi.model.recommendation.RuleBasedRecommendation;
import kiwi.model.tagging.Tag;
import kiwi.model.user.User;
import kiwi.service.reasoning.ReasoningConstants;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;


/**
 * A component supporting the management of personal rules. Used by the 
 * Dashboard rules.xhtml view.
 * 
 * @author freddurao
 *
 */
@Name("kiwi.dashboard.ruleAction")
@Scope(ScopeType.PAGE)
public class RuleBasedAction implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@In
	@Out
	private User currentUser;
	
	@In(create=true)
	private EntityManager entityManager;
	
	@In(create=true)
	private KiWiEntityManager kiwiEntityManager;
	
	@In(create=true)
	private ContentItemService contentItemService;

	@In(create=true)
	private TaggingService taggingService;		

	private String tagRec;
	
	private String myTag;	
	
	private String firstQuery;
	
	private String secondQuery;
	
	private String ruleType;
	
	@In 
    FacesMessages facesMessages;
	
	@In
	private TripleStore tripleStore;

	private List<RuleBasedRecommendation> ruleBasedRecommendations;
	
	private String RECOMMEND_ME_PAGES_WITH_TAGS="recTag";
	
	private String TAG_PAGES_WITH_TAGS="ruleTag";
	
	private String TAG_MY_PAGES_WITH_TAGS="myTag";

	/**
	 * @return
	 */
	public List<RuleBasedRecommendation> getRuleBasedRecommendations() {
		if(ruleBasedRecommendations == null) {
			ruleBasedRecommendations = new LinkedList<RuleBasedRecommendation>();
		}
		ruleBasedRecommendations = loadRuleBasedRecommendations();
		return ruleBasedRecommendations;
	}

	/**
	 * Created a personal rule
	 */
	public void createRule(String ruleType) {
		    this.setRuleType(ruleType);
		    boolean validated = false;
			RuleBasedRecommendation ruleBasedRecommendation = new RuleBasedRecommendation();
			ruleBasedRecommendation.setUser(currentUser);
			ruleBasedRecommendation.setRuleType(ruleType);			
		    if (ruleType.equals(RECOMMEND_ME_PAGES_WITH_TAGS) && tagRec!=null && !tagRec.isEmpty()) {
				ruleBasedRecommendation.setTagRec(tagRec);
				validated = true;
			}else  if (ruleType.equals(TAG_MY_PAGES_WITH_TAGS)&& myTag!=null && !myTag.isEmpty()) {
				ruleBasedRecommendation.setMyTag(myTag);
				validated = true;				
			}else  if (ruleType.equals(TAG_PAGES_WITH_TAGS)&& firstQuery!=null && secondQuery!=null && !firstQuery.isEmpty() && !secondQuery.isEmpty()) {
				ruleBasedRecommendation.setFirstQuery(firstQuery);
				ruleBasedRecommendation.setSecondQuery(secondQuery);
				validated = true;				
			}
		    if (validated) {
				entityManager.persist(ruleBasedRecommendation);
				entityManager.flush();
				FacesMessages.instance().add("Rule created successfully.");
			}else{
				FacesMessages.instance().add("A field must be filled out");
			}
		    
		    this.setFirstQuery(null);
		    this.setSecondQuery(null);
		    this.setTagRec(null);
		    this.setMyTag(null);
	}

	/**
	 * Save the rule in the triple store
	 * @param ruleBasedRecommendation
	 * 
	 * 
	 * 			String rule2 = "tag-1: ($tagging holygoat:taggedResource $ci), " +
					              "($tagging holygoat:associatedTag $tag), (" +
					              "$tagging holygoat:taggedBy $user), " +
					              "($tagging holygoat:taggedOn $date), " +
					              "($tag kiwi:title \"ahoj\") ->" +
					              " ($newtagging holygoat:taggedResource $ci)," +
					              " ($newtagging holygoat:associatedTag $newtag)," +
					              " ($newtagging holygoat:taggedBy $user), " +
					              "($newtagging holygoat:taggedOn $date), " +
					              "($newtag kiwi:title \"inferred\")";
		
			 // @prefix holygoat: <http://www.holygoat.co.uk/owl/redwood/0.1/tags/>		
	 * 
	 */
	
	
	public void generateRule(RuleBasedRecommendation ruleBasedRecommendation) {
			KiWiUriResource programUri = tripleStore.createUriResource(ReasoningConstants.KIWI_RDFS_SKWRL_PROGRAM);//the default program, see TriplestoreProgramLoader.getContent()
			KiWiUriResource property = tripleStore.createUriResource(ReasoningConstants.KIWI_PROGRAM_HAS_RULE);
			String rule = this.getRuleByType(ruleBasedRecommendation);
			System.out.println(rule);
			KiWiLiteral literal = tripleStore.createLiteral(rule);
			KiWiTriple kiwiTriple = tripleStore.createTriple(programUri, property, literal);
			kiwiEntityManager.persist(kiwiTriple);
			ruleBasedRecommendation.setKiwiTriple(kiwiTriple);
			entityManager.persist(ruleBasedRecommendation);
			entityManager.flush();
			
			//kiwiEntityManager.persist(ruleBasedRecommendation);
			
			gotTagging(ruleBasedRecommendation);
			FacesMessages.instance().add("Rules fired !");
	}
	
	
	/**
	 * @param ruleBasedRecommendation
	 */
	public void gotTagging(RuleBasedRecommendation ruleBasedRecommendation) {
		
		Set<Long> ciIds = new HashSet<Long>();
		
		Set<Long> ciTagsIds = new HashSet<Long>();
		
		List<Tag> tags = new ArrayList<Tag>();
		
		List<ContentItem> taggedItemsSet = new ArrayList<ContentItem>();
		
		List<ContentItem> taggedContentItems = new ArrayList<ContentItem>();
		
		if (ruleBasedRecommendation.getRuleType().equals(TAG_PAGES_WITH_TAGS)) {
			tags.addAll(taggingService.getTagsByLabel(ruleBasedRecommendation.getFirstQuery()));
		}else if (ruleBasedRecommendation.getRuleType().equals(TAG_MY_PAGES_WITH_TAGS)) {
			tags.addAll(taggingService.getTagsByUser(currentUser));	
		}

		for (Tag tag : tags) {
			if (!ciIds.contains(tag.getTaggingResource().getId())) {
				taggedContentItems.addAll(taggingService.listTaggedItems(tag.getTaggingResource()));
				ciIds.add(tag.getTaggingResource().getId());
			}
		}
		
		for (ContentItem contentItem : taggedContentItems) {
			if (!ciTagsIds.contains(contentItem.getId())) {
				taggedItemsSet.add(contentItem);
				ciTagsIds.add(contentItem.getId());
			}
		}
		
		for (ContentItem contentItemToBeTagged : taggedItemsSet) {
		    	ContentItem newTag = contentItemService.createContentItem();
				if (ruleBasedRecommendation.getRuleType().equals(TAG_PAGES_WITH_TAGS)) {
					contentItemService.updateTitle(newTag, ruleBasedRecommendation.getSecondQuery());
				}else if (ruleBasedRecommendation.getRuleType().equals(TAG_MY_PAGES_WITH_TAGS)) {
					contentItemService.updateTitle(newTag, ruleBasedRecommendation.getMyTag());
				}		    	
		    	contentItemService.saveContentItem(newTag);
		    	
				if (ruleBasedRecommendation.getRuleType().equals(TAG_PAGES_WITH_TAGS)) {
					taggingService.createTagging(ruleBasedRecommendation.getSecondQuery(), contentItemToBeTagged, newTag, currentUser);
				}else if (ruleBasedRecommendation.getRuleType().equals(TAG_MY_PAGES_WITH_TAGS)) {
					taggingService.createTagging(ruleBasedRecommendation.getMyTag(), contentItemToBeTagged, newTag, currentUser);
				}			    	
		}
	}

	

	/**
	 * @param ruleType
	 * @param ruleBasedRecommendation
	 */
	public String getRuleByType(RuleBasedRecommendation ruleBasedRecommendation){
		Map<String, String> ruleMap = new HashMap<String, String>();
		
		String ruleRecommendation = "tag-"+currentUser.getLogin().concat("-").concat(""+ruleBasedRecommendation.getId())+":" +
		  "(($x rdf:type "+Constants.NS_KIWI_CORE+"ContentItem)," +
		  "($x "+Constants.NS_HGTAGS+"taggedWithTag \""+ruleBasedRecommendation.getTagRec()+"\")) -> " +
		  "(($x "+Constants.NS_KIWI_CORE+"recommendTo "+Constants.NS_KIWI_CORE+currentUser.getLogin()+"))";
		ruleMap.put(RECOMMEND_ME_PAGES_WITH_TAGS, ruleRecommendation);
		
	
		String ruleMyTags = "tag-"+currentUser.getLogin().concat("-").concat(""+ruleBasedRecommendation.getId())+": " +
		  "(($tagging holygoat:taggedResource $ci),($tagging holygoat:associatedTag $tag)," +
		  "($tagging "+Constants.NS_HGTAGS+"associatedTag \""+ruleBasedRecommendation.getFirstQuery()+"\")) -> " +
		  "(($newtagging holygoat:taggedResource $ci),($newtagging "+Constants.NS_HGTAGS+"associatedTag \""+ruleBasedRecommendation.getSecondQuery()+"\"))";
		ruleMap.put(TAG_PAGES_WITH_TAGS, ruleMyTags);
		
	
		String ruleTags = "tag-"+currentUser.getLogin().concat("-").concat(""+ruleBasedRecommendation.getId())+":" +
		  "(($tagging holygoat:taggedResource $ci),($tagging holygoat:associatedTag $tag)," +
		  "($tagging "+Constants.NS_HGTAGS+"taggedBy \""+Constants.NS_KIWI_CORE+currentUser.getLogin()+"\")) -> " +
		  "(($newtagging holygoat:taggedResource $ci),($newtagging "+Constants.NS_HGTAGS+"associatedTag \""+ruleBasedRecommendation.getMyTag()+"\"))";		
		ruleMap.put(TAG_MY_PAGES_WITH_TAGS, ruleTags);
		return ruleMap.get(ruleBasedRecommendation.getRuleType());

	}

	/**
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<RuleBasedRecommendation> loadRuleBasedRecommendations() {
		List<RuleBasedRecommendation> ruleBasedRecommendations  = null;
		String s = "select u from RuleBasedRecommendation u where u.user =:user ";
		javax.persistence.Query q = entityManager.createQuery(s);
        q.setParameter("user", currentUser);
        try {
        	ruleBasedRecommendations = (List<RuleBasedRecommendation>)q.getResultList();
 		} catch (NoResultException ex) {
 			//
		}
		return ruleBasedRecommendations;
	}
	
	/**
	 * @param rule
	 */
	public void removeRule(RuleBasedRecommendation rule) {
		if (rule.getKiwiTriple()!=null) {
		    tripleStore.removeTriple(rule.getKiwiTriple());
		    entityManager.remove(rule);
			entityManager.flush();
			FacesMessages.instance().add("Rule deleted successfully.");	
		}

	}	

	public String getFirstQuery() {
		return firstQuery;
	}

	public void setFirstQuery(String firstQuery) {
		this.firstQuery = firstQuery;
	}

	public String getSecondQuery() {
		return secondQuery;
	}

	public void setSecondQuery(String secondQuery) {
		this.secondQuery = secondQuery;
	}

	public String getRuleType() {
		return ruleType;
	}

	public void setRuleType(String ruleType) {
		this.ruleType = ruleType;
	}

	public String getTagRec() {
		return tagRec;
	}

	public void setTagRec(String tagRec) {
		this.tagRec = tagRec;
	}

	public String getMyTag() {
		return myTag;
	}

	public void setMyTag(String myTag) {
		this.myTag = myTag;
	}
	
	
}
