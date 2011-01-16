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
package kiwi.model.recommendation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import kiwi.model.content.ContentItem;
import kiwi.model.user.User;



/**
 *
 * @author Martin Leginus
 */

public class CollaborativeWorkBean implements Serializable, Comparable<CollaborativeWorkBean> {

	/**
	 * @param topic
	 * @param contentItem
	 * @param earnedSocialCapitalValue
	 */
	public CollaborativeWorkBean(String topic, ContentItem contentItem, float earnedSocialCapitalValue) {
		super();
		this.earnedSocialCapitalValue = earnedSocialCapitalValue;
		this.contentItem = contentItem;
	}
	
	public CollaborativeWorkBean() {
	}

	private static final long serialVersionUID = -3880265577645037062L;
	
	private float earnedSocialCapitalValue;
	
	private int amountOfActivities;
    
	private int numberOfComments = 0;
	
	private int numberOfTaggings = 0;
	
	private int numberOfEditings = 0;
	
	private int numberOfRatings = 0;
	
    private ContentItem contentItem;
    
    private Set<User> involvedUsers = null;
    
    private boolean tagActivity, commentActivity, ratingActivity, editActivity;
    
    public ContentItem getContentItem() {
		return contentItem;
	}

	public void setContentItem(ContentItem contentItem) {
		this.contentItem = contentItem;
	}

	public float getEarnedSocialCapitalValue() {
		return earnedSocialCapitalValue;
	}

	public void setEarnedSocialCapitalValue(float earnedSocialCapitalValue) {
		this.earnedSocialCapitalValue = earnedSocialCapitalValue;
	}
	public void addEarnedSocialCapitalValue(float earnedSocialCapitalValue) {
		this.earnedSocialCapitalValue += earnedSocialCapitalValue;
	}
	
	
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 * Method used for ranking contentItem by contentScores necessary when
	 * calculating multi factor recommendations
	 */
	public int compareTo(CollaborativeWorkBean socialCapitalRecommendation) {
        if (this.earnedSocialCapitalValue == socialCapitalRecommendation.getEarnedSocialCapitalValue())
            return 0;
        else if (this.earnedSocialCapitalValue < socialCapitalRecommendation.getEarnedSocialCapitalValue())
            return 1;
        else
            return -1;
    }

	/**
	 * @return
	 */
	public List<User> getInvolvedUsers() {
		if (involvedUsers == null) { 
			return Collections.emptyList();
		}
		else {
			return new ArrayList<User>(involvedUsers);
		}
	}

	/**
	 * @param involvedUsers
	 */
	public void setInvolvedUsers(Set<User> involvedUsers) {
		this.involvedUsers = involvedUsers;
	}

		
	/**
	 * @param user
	 */
	public void addUser(User user) {
		if (this.involvedUsers == null) {
			this.involvedUsers = new HashSet<User>();
		}
		this.involvedUsers.add(user);
	}

	public void setAmountOfActivities(int amountOfActivities) {
		this.amountOfActivities = amountOfActivities;
	}

	public int getAmountOfActivities() {
		this.amountOfActivities = this.numberOfComments + this.numberOfEditings + this.numberOfRatings + this.numberOfTaggings;
		return amountOfActivities;
	}

	public void setEditActivity(boolean editActivity) {
		this.editActivity = editActivity;
	}

	public boolean isEditActivity() {
		return editActivity;
	}

	public void setCommentActivity(boolean commentActivity) {
		this.commentActivity = commentActivity;
	}

	public boolean isCommentActivity() {
		return commentActivity;
	}

	public void setTagActivity(boolean tagActivity) {
		this.tagActivity = tagActivity;
	}

	public boolean isTagActivity() {
		return tagActivity;
	}

	public void setRatingActivity(boolean ratingActivity) {
		this.ratingActivity = ratingActivity;
	}

	public boolean isRatingActivity() {
		return ratingActivity;
	}
	public int getNumberOfComments() {
		return numberOfComments;
	}
	
	public void addToNumberOfComments() {
		this.numberOfComments++;
	}

	public int getNumberOfTaggings() {
		return numberOfTaggings;
	}
	
	public void addToNumberOfTaggings() {
		this.numberOfTaggings++;
	}	

	public int getNumberOfEditings() {
		return numberOfEditings;
	}
	
	public void addToNumberOfEditings() {
		this.numberOfEditings++;
	}
	public int getNumberOfRatings() {
		return numberOfRatings;
	}
	public void addToNumberOfRatings() {
		this.numberOfRatings++;
	}

}
