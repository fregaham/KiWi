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
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import kiwi.model.content.ContentItem;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 *
 * @author freddurao
 */
@Entity
@Table(name = "SocialCapitalRecommendation")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class SocialCapitalRecommendation implements Serializable, Comparable<SocialCapitalRecommendation> {

	public SocialCapitalRecommendation(String topic, ContentItem recommendedItem, float earnedSocialCapitalValue) {
		super();
		this.earnedSocialCapitalValue = earnedSocialCapitalValue;
		this.recommendedItem = recommendedItem;
	}
	
	public SocialCapitalRecommendation() {
	}

	private static final long serialVersionUID = -3880265577645037062L;
	
	/** The database identifier of this activity. */
	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE)
	private Long id;	

	private float earnedSocialCapitalValue;
    
    private ContentItem recommendedItem;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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
	public ContentItem getRecommendedItem() {
		return recommendedItem;
	}

	public void setRecommendedItem(ContentItem recommendedItem) {
		this.recommendedItem = recommendedItem;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 * Method used for ranking contentItem by contentScores necessary when
	 * calculating multi factor recommendations
	 */
	public int compareTo(SocialCapitalRecommendation socialCapitalRecommendation) {
        if (this.earnedSocialCapitalValue == socialCapitalRecommendation.getEarnedSocialCapitalValue())
            return 0;
        else if (this.earnedSocialCapitalValue < socialCapitalRecommendation.getEarnedSocialCapitalValue())
            return 1;
        else
            return -1;
    }	

}
