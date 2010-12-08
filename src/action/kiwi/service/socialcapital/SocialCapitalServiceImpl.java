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



package kiwi.service.socialcapital;

import static kiwi.model.kbase.KiWiQueryLanguage.SPARQL;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import kiwi.api.comment.CommentService;
import kiwi.api.entity.KiWiEntityManager;
import kiwi.api.history.HistoryService;
import kiwi.api.interaction.InteractionService;
import kiwi.api.rating.RatingDataFacade;
import kiwi.api.rating.RatingFacade;
import kiwi.api.skill.SkillService;
import kiwi.api.socialcapital.SocialCapitalServiceLocal;
import kiwi.api.socialcapital.SocialCapitalServiceRemote;
import kiwi.api.tagging.TaggingService;
import kiwi.model.Constants;
import kiwi.model.activity.Activity;
import kiwi.model.activity.CommentActivity;
import kiwi.model.activity.EditActivity;
import kiwi.model.activity.RateActivity;
import kiwi.model.activity.TaggingActivity;
import kiwi.model.content.ContentItem;
import kiwi.model.recommendation.CollaborativeWorkBean;
import kiwi.model.recommendation.SocialCapital;
import kiwi.model.skill.UserSkill;
import kiwi.model.user.User;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

/**
 * @author Fred Durao
 *
 */
@Stateless
@Scope(ScopeType.STATELESS)
@AutoCreate
@Name("socialCapitalService")
public class SocialCapitalServiceImpl implements SocialCapitalServiceLocal, SocialCapitalServiceRemote {
	
	@In
	private EntityManager entityManager;
	
	private static final float FIRST_LEVEL_FRIENDSHIP = 1;

	private static final float SECOND_LEVEL_FRIENDSHIP = 2;

	@In
	private InteractionService interactionService;
	
	@In
	private HistoryService historyService;	
	
	@In
	private SkillService skillService;
	
	@In(create = true)
	private KiWiEntityManager kiwiEntityManager;
	
	@In
	private TaggingService taggingService ;
	@In
	private CommentService commentService;
	/**
	 * @param contentItemAuthor
	 * @param socialCapital
	 * @param friendShipLevel
	 * @param topic
	 */
	public void calculateSocialCapital(SocialCapital socialCapital, User contentItemAuthor, float friendShipLevel, User currentUser){
			
		float socialCapitalScore = 0.1f;		
		List<User> friends = new ArrayList<User>();
		
		if (friendShipLevel == 0) {
			socialCapitalScore =  socialCapital.getSocialCapitalScore() + 
						    calculateEarnedKnowledge (socialCapital,socialCapital.getEarnedKnowledgeLevel(),1,currentUser) +
							calculateInteractionLevel(socialCapital,socialCapital.getInteractionLevel()    ,1,contentItemAuthor,currentUser);
			
			
			//DecimalFormat twoDigits = new DecimalFormat("##.##");
			//DecimalFormat twoDigits = new DecimalFormat("###.###");
			//socialCapital.setSocialCapitalScore(Float.valueOf(twoDigits.format(socialCapitalScore)));
			socialCapital.setSocialCapitalScore(Float.valueOf(socialCapitalScore));
			//System.out.println("contentItemAuthor  "+contentItemAuthor.getLogin()+"   currentUser  "+currentUser.getLogin()+"  socialCapitalScore  "+socialCapital.getSocialCapitalScore());
		}else if (friendShipLevel == 1) {
			friends = socialCapital.getDirectFriends();
			printFriends(contentItemAuthor, friends);
		}else if (friendShipLevel == 2) {
			friends = socialCapital.getIndirectFriends();
		}
		for (User friend : friends) {
			socialCapitalScore = socialCapital.getSocialCapitalScore() + 
			(
				getPersonalEquityByUser(friend) +
			    calculateEarnedKnowledge (socialCapital,socialCapital.getEarnedKnowledgeLevel(),friendShipLevel,friend) +
				calculateInteractionLevel(socialCapital,socialCapital.getInteractionLevel()    ,friendShipLevel,contentItemAuthor,friend)
			 );
		}
		
		//DecimalFormat twoDigits = new DecimalFormat("###.###");
		//socialCapital.setSocialCapitalScore(Float.valueOf(twoDigits.format(socialCapitalScore)));
		socialCapital.setSocialCapitalScore(Float.valueOf(socialCapitalScore));
	}

	private void printFriends(User user, List<User> friends){
			for (User friend : friends) {
				System.out.println("friend  "+friend.getLogin());
			}
	}
	
	/**
	 * @param interactionLevelInit
	 * @param friendShipLevel
	 * @param user
	 * @param friend
	 * @return
	 */
	private float calculateInteractionLevel(SocialCapital socialCapital, float interactionLevelInit, float friendShipLevel, User user, User friend){
		float interactionLevelFinal = interactionLevelInit + (getInteractionLevel(user, friend)/friendShipLevel);
		socialCapital.setInteractionLevel(interactionLevelFinal);
		return interactionLevelFinal;
	}	
	
	/**
	 * @param earnedKnowledgeInit
	 * @param friendShipLevel
	 * @param friend
	 * @param topic
	 * @return
	 */
	private float calculateEarnedKnowledge(SocialCapital socialCapital, float earnedKnowledgeInit, float friendShipLevel, User friend){
		float earnedKnowledgeFinal = earnedKnowledgeInit + (getKnowledgeValueByUser(socialCapital,friend)/friendShipLevel);
		socialCapital.setEarnedKnowledgeLevel(earnedKnowledgeFinal);
		return earnedKnowledgeFinal;
	}

	/**
	 * @param user
	 * @return
	 */
	private float getKnowledgeValueByUser(SocialCapital socialCapital, User user){
		float topicKnowledgeLevel = 0.1f;
		UserSkill userSkill = skillService.getSkillsByUser(user);	
		
		/* if needs are empty - knowledge is computed according to skills
		 * otherwise tag labels are used for the computation
		 */
		if(userSkill!=null && userSkill.getSkills()!=null && socialCapital.getNeeds()!=null && socialCapital.getNeeds().isEmpty()){
			for (String skill : userSkill.getSkills().keySet()) {
				topicKnowledgeLevel = topicKnowledgeLevel + userSkill.getSkills().get(skill);	
			}
		}else {
			for (String need : socialCapital.getNeeds()) {
				if (userSkill!=null && userSkill.getSkills().containsKey(need)) {
					topicKnowledgeLevel = topicKnowledgeLevel + userSkill.getSkills().get(need);	
				}
			}
		}
		return topicKnowledgeLevel;
	}	
	
	/**
	 * @param user
	 * @param friend
	 * @return
	 */
	private float getInteractionLevel(User user, User friend){
		float interactionLevel = 0.1f;
		if (interactionService.getUserInteractionByUsers(user,friend)!=null) {	
			interactionLevel = interactionService.getUserInteractionByUsers(user,friend).getInteractivity();
		}
		return interactionLevel;
	}	
	
	
	/**
	 * @param user
	 * @return
	 */
	private float getPersonalEquityByUser(User user){
		float personalEquity = 0.1f;
		return personalEquity;
	}	
	
	
	/* (non-Javadoc)
	 * @see kiwi.api.socialcapital.SocialCapitalService#computeCollaborativeWorkForAll(kiwi.model.user.User)
	 */
	public Set<CollaborativeWorkBean> computeCollaborativeWork(){
		Set<ContentItem> contentItems = new HashSet<ContentItem>();
		// get all pages which have some activity at the moment only pages with comments are considered
		contentItems = getContentItemsWithCollaborativeActivities();
		Set<CollaborativeWorkBean> result = new HashSet<CollaborativeWorkBean>();
		
		for (ContentItem contentItem : contentItems) {
			
			SocialCapital socialCapital = new SocialCapital();
			socialCapital.setTopic(null);
			socialCapital.setNeeds(new ArrayList<String>());
			
			CollaborativeWorkBean collaborativeWork = new CollaborativeWorkBean();
			collaborativeWork.setContentItem(contentItem);
			// it has to be inspected whether users are not involved two times - equal method
			Set<User> users = new HashSet<User>();
			//users.add(contentItem.getAuthor());
			
			for (ContentItem Item: commentService.listComments(contentItem)){
				users.add(Item.getAuthor());
				collaborativeWork.addToNumberOfComments();
				collaborativeWork.setCommentActivity(true);
			}
			
			for (ContentItem tag: taggingService.getTags(contentItem)){
				users.add(tag.getAuthor());
				collaborativeWork.addToNumberOfTaggings();
				collaborativeWork.setTagActivity(true);
			}
			
			List<RatingDataFacade> ratings = kiwiEntityManager.createFacade(
					contentItem, RatingFacade.class).getRatingDataFacades();
			for(RatingDataFacade ratingData:ratings){
				users.add(ratingData.getAuthor());
				collaborativeWork.setRatingActivity(true);
				collaborativeWork.addToNumberOfRatings();
			}
			
			
			List<EditActivity> edits = historyService.listEditsByContentItem(contentItem);
			for (EditActivity edit: edits) {
				if ((edit.getContentItem()!=null) && (edit.getContentItem().getTextContent()!=null) && 
					(edit.getContentItem().getAuthor()!=null) && (!edit.getContentItem().getAuthor().getLogin().isEmpty()) 
					&& (!edit.getContentItem().getAuthor().getLogin().equals(contentItem.getAuthor().getLogin())) &&
					(!edit.getContentItem().getTextContent().getPlainString().isEmpty())) {
					users.add(edit.getUser());
					collaborativeWork.addToNumberOfEditings();
					collaborativeWork.setEditActivity(true);
				}	
			}
			int numberOfActivities = collaborativeWork.getAmountOfActivities();
			
			if (numberOfActivities == 0) {
				continue;
			}
			if (contentItem.getAuthor() != null && !getFriends(contentItem.getAuthor()).isEmpty()) {
				List<User> directFriends = new ArrayList<User>(getFriends(contentItem.getAuthor()));
				socialCapital.setDirectFriends(directFriends);
				calculateSocialCapital(socialCapital,contentItem.getAuthor(),FIRST_LEVEL_FRIENDSHIP ,null );
			}
			for (User involvedUser: users) {
				
			// all users that collaborated on the content item are stored but only for those that are not equal to the author of the content item
			// social capital value is computed 
			collaborativeWork.addUser(involvedUser);
			if (!contentItem.getAuthor().equals(involvedUser)) {
				
				calculateSocialCapital(socialCapital,contentItem.getAuthor(), 0,involvedUser );
				if(socialCapital.getSocialCapitalScore()>0f ){
					
						float capitalValue =  numberOfActivities*Float.valueOf(socialCapital.getSocialCapitalScore());
						collaborativeWork.addEarnedSocialCapitalValue(capitalValue);
				}
			}
		}
		result.add(collaborativeWork);
		}
		return result;
	}
	
	/**
	 * @return ContentItems which have some collaborative activity
	 */
	private Set<ContentItem> getContentItemsWithCollaborativeActivities() {
		javax.persistence.Query q = entityManager.createNamedQuery("activities.listCollaborativeActivities");
		Set<ContentItem> result = new HashSet<ContentItem>();
		List<Activity> activities = q.getResultList();
		for (Activity activity : activities) {
			if (activity instanceof CommentActivity) { 
				result.add(((CommentActivity)activity).getContentItem());
			} 
			else if (activity instanceof EditActivity) {
			result.add(((EditActivity)activity).getContentItem());
			} 
			else if (activity instanceof RateActivity) {
				result.add(((RateActivity)activity).getContentItem());
			}
			else if (activity instanceof TaggingActivity) {
				result.add(((TaggingActivity)activity).getTag().getTaggedResource());
			}
		}
		return result;  
	}	
	private List<User> getFriends(User currentUser) {
		List<User> friends = null;
		javax.persistence.Query q = kiwiEntityManager.createQuery("SELECT ?w WHERE { " +
				" :user <" + Constants.NS_FOAF + "knows>  ?w . }", SPARQL,User.class);
		q.setParameter("user", currentUser);
		q.setHint("org.hibernate.cacheable", true);
		try {
		friends = (List<User>) q.getResultList();
		} catch (PersistenceException ex) {
		ex.printStackTrace();
		}
		if (friends == null) {
		return Collections.EMPTY_LIST;
		} else {
		return friends;
		}
	}
}
