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

package kiwi.wiki.action.ie;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import kiwi.api.event.KiWiEvents;
import kiwi.api.informationextraction.InformationExtractionService;
import kiwi.api.tagging.TagRecommendation;
import kiwi.api.tagging.TagRecommendationService;
import kiwi.api.tagging.TaggingService;
import kiwi.api.triplestore.TripleStore;
import kiwi.model.Constants;
import kiwi.model.content.ContentItem;
import kiwi.model.informationextraction.Suggestion;
import kiwi.model.kbase.KiWiResource;
import kiwi.model.kbase.KiWiUriResource;
import kiwi.model.tagging.Tag;
import kiwi.model.user.User;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;

/**
 * 
 * Manages a list of fragment and content suggestions for the current content item
 * 
 * @author Marek Schmidt
 *
 */
@Name("ie.suggestions")
@Scope(ScopeType.CONVERSATION)
public class SuggestionsAction implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@In(create=true)
	ContentItem currentContentItem;
	
	@In(create=true)
	private User currentUser;
	
	@In
	TaggingService taggingService;
	
	@In
	TripleStore tripleStore;
	
	@In
	TagRecommendationService tagRecommendationService;
	
	private List<Suggestion> suggestions;
	
	private List<Suggestion> typeSuggestions;
	
	@Logger
	private Log log;
	
	public List<Suggestion> getSuggestions () {
		if (suggestions == null) {
			InformationExtractionService ie = (InformationExtractionService)Component.getInstance("kiwi.informationextraction.informationExtractionService");	
			setSuggestions (ie.getSuggestionsByContentItem(currentContentItem));
			typeSuggestions = null;
		}
		
		return suggestions;
	}
	
	public void setSuggestions(Collection<Suggestion> newSuggestions) {
		this.suggestions = new ArrayList<Suggestion> (newSuggestions);
		
//	 	We sort them by score, to make the job of other methods easier
		Comparator<Suggestion> suggestionScoreScomparator = new Comparator<Suggestion>() {
			@Override
			public int compare(Suggestion o1, Suggestion o2) {
				if (o1.getScore() > o2.getScore()){
					return -1;
				}
				else if (o1.getScore() < o2.getScore()) {
					return 1;
				}
			
				return 0;
			}
		};
	
		// We sort them by score, to make the job of other methods easier
		Collections.sort(suggestions, suggestionScoreScomparator);
		typeSuggestions = null;
	}
	
	public void init() {
		InformationExtractionService ie = (InformationExtractionService)Component.getInstance("kiwi.informationextraction.informationExtractionService");	
		//ie.initAndClassifyInstances(currentContentItem);
		ie.extractInformation(currentContentItem);
	}
	
	public void compute() {
		InformationExtractionService ie = (InformationExtractionService)Component.getInstance("kiwi.informationextraction.informationExtractionService");	
		ie.trainAndSuggestForItem(currentContentItem);
	}
	
	public void acceptSuggestion(Suggestion s) {
		InformationExtractionService ie = (InformationExtractionService)Component.getInstance("kiwi.informationextraction.informationExtractionService");
		ie.acceptSuggestion(s, currentUser);
		ie.realizeSuggestion(s, currentUser);
		
		if (suggestions != null) {
			suggestions.remove(s);
		}
		
		typeSuggestions = null;
	}
	
	public void rejectSuggestion(Suggestion s) {
		InformationExtractionService ie = (InformationExtractionService)Component.getInstance("kiwi.informationextraction.informationExtractionService");
		ie.rejectSuggestion(s, currentUser);
		
		if (suggestions != null) {
			suggestions.remove(s);
		}
		
		typeSuggestions = null;
	}
	
	
	
	public List<Suggestion> getTypeSuggestions() {
		
		if (typeSuggestions == null) {
				
			typeSuggestions = new LinkedList<Suggestion> ();
			final int limit = 7;
		
			iterateSuggestions: for (Suggestion suggestion : getSuggestions()) {
				// TODO: configurable threshold? (0.5 should work fine for the suggestions generated by the DocumentTypeExtractlet...)
				if (suggestion.getKind() == Suggestion.TYPE && suggestion.getScore() > 0.5f) {
				
					for (KiWiResource suggestionType : suggestion.getTypes()) {
						if (suggestionType.isUriResource()) {
							if (currentContentItem.getResource().hasType(suggestionType.getKiwiIdentifier().substring(5))) {
								continue iterateSuggestions;
							}
						}
					}
				
					typeSuggestions.add (suggestion);
					if (typeSuggestions.size() >= limit) {
						break;
					}
				}
			}
		}
				
		return typeSuggestions;
	}
	
	public List<Suggestion> getTagSuggestions() {
		List<Suggestion> ret = new LinkedList<Suggestion> ();
		final int limit = 7;
		
		iterateSuggestions: for (Suggestion suggestion : getSuggestions()) {
			if (suggestion.getKind() == Suggestion.TAG) {
				
				for (Tag t : taggingService.getTaggings(currentContentItem)) {
					if (suggestion.getLabel().equals(t.getTaggingResource().getTitle())) {
						continue iterateSuggestions;
					}
				}
				
				ret.add (suggestion);
				if (ret.size() >= limit) {
					break;
				}
			}
		}
		
		return ret;
	}
	
	@Observer(value=KiWiEvents.SUGGESTIONS_UPDATED, create=false)
	public void onSugestionsUpdated(ContentItem ci) {
		if (ci != null && currentContentItem != null && ci.getId() != null && ci.getId().equals(currentContentItem.getId())) {
			suggestions = null;
			typeSuggestions = null;
		}
	}
	
	private static class TagRecommendationRatingComparator implements Comparator<TagRecommendation> {
		@Override
		public int compare(TagRecommendation o1, TagRecommendation o2) {
			int ret =  - Float.compare(o1.getRating(), o2.getRating());
			if (ret == 0) {
				return - o1.getLabel().compareTo(o2.getLabel());
			}
			return ret;
		}
		
	}
	
	/**
	 * This is a copy of the TagRecommendationServiceImpl, but which uses the current suggestion list instead of the
	 * information extraction service.
	 * 
	 * This way, we can retrieve current tag suggestions in the editor, even if the content item has not been saved yet.
	 * @return
	 */
	public Map<String, Collection<TagRecommendation>> getGroupedRecommendations() {
		Map<String, Collection<TagRecommendation>> ret = new LinkedHashMap<String, Collection<TagRecommendation>> ();
				
		KiWiUriResource t_concept = tripleStore.createUriResource(Constants.NS_SKOS+"Concept");
		
		Comparator<TagRecommendation> tagRecommendationRatingComparator = new TagRecommendationRatingComparator();
		TreeSet<TagRecommendation> others = new TreeSet<TagRecommendation> (tagRecommendationRatingComparator);
		
		for (Suggestion es : getSuggestions()) {
			
			if (es.getKind() != Suggestion.TAG) {
				continue;
			}
			
			ContentItem item = null;
			
			if (es.getResources().size() >= 1) {
				item = es.getResources().iterator().next().getContentItem();
				
				if (es.getResources().size() > 1) {
					log.info("ambiguous entity: #0", es.getLabel());
					for (KiWiResource resource : es.getResources()) {
						log.info("    #0 (#1)", resource.getKiwiIdentifier(), resource.getContentItem().getTitle());
					}
				}
			}

			TagRecommendation tagRecommendation = new TagRecommendation(
					es.getLabel(),
					item != null && item.getResource().hasType(t_concept),
					item != null,
					es.getScore()
			);
			tagRecommendation.setItem(item);
			tagRecommendation.setSuggestion(es);
			
			// Special case for uncategorized entities, to be merged with general "Uncategorized"
			if (es.getTypes().size() == 0) {
				others.add(tagRecommendation);
			}
			else {
				for (KiWiResource type : es.getTypes()) {
					TreeSet<TagRecommendation> set = (TreeSet<TagRecommendation>) ret.get(type.getLabel());
					if (set == null) {
						set = new TreeSet<TagRecommendation>(tagRecommendationRatingComparator);
						ret.put(type.getLabel(), set);
					}
					set.add(tagRecommendation);
				}
			}
		}
		
		others.addAll(tagRecommendationService.getRecommendations(currentContentItem));
		if (others.size() > 0) {
			ret.put("Uncategorized", others);
		}
		
		return ret;
	}
}
