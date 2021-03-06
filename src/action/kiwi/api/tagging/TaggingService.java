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
package kiwi.api.tagging;

import java.util.List;
import java.util.Set;

import kiwi.model.content.ContentItem;
import kiwi.model.kbase.KiWiResource;
import kiwi.model.tagging.Tag;
import kiwi.model.user.User;

/**
 * A DAO service for managing tags. Provides convenience methods for creating new tags, modifying
 * tags, and removing tags.
 * 
 * @author Sebastian Schaffert
 *
 */
public interface TaggingService {

	/**
	 * Create a new tagging of taggedItem with taggingItem based on the given label for taggingUser.
	 * This method creates a new tag instance with a generated, unique uri resource. The tag is then
	 * persisted in the entityManager and tripleStore, and returned to the caller.
	 * 
	 * @param label a textual label of the tagging; this is usually the string entered by the user
	 *              which has then been used to look up taggingItem
	 * @param taggedItem the tagged content item
	 * @param taggingItem the content item used as tag
	 * @param taggingUser the user who performed the tagging
	 * @return the newly created and persisted tag
	 */
	public Tag createTagging(String label, ContentItem taggedItem, ContentItem taggingItem, User taggingUser);
	
	
	/**
	 * Verify whether the given label has been associated with the content item.
	 * 
	 * @param taggedItem the content item that has been tagged
	 * @param label the label used for tagging
	 * @return true if the label was used for tagging the item.
	 */
	public boolean hasTag(ContentItem taggedItem, String label);

	
	/**
	 * Adds a tag to the database/triplestore and to the transaction data, 
	 * where it will be put under version-control
	 * @param tag, the tag that will be added
	 * @return tag, the tag that has been added
	 */
	public Tag addTagging(Tag tag);
	
	/**
	 * Remove an existing tagging from the database.
	 * 
	 * @param tag the tagging to remove
	 */
	public void removeTagging(Tag tag);
	
	/**
	 * Provide autocompletion of content items that are of type "kiwi:Tag" or "skos:Concept" and 
	 * whose title starts with "prefix". Autocompletion is delegated to the SOLR search service.
	 * @param prefix
	 * @return
	 */
	public List<String> autocomplete(String prefix);
	
	/**
	 * List the taggings associated with a content item. This method is safer than getTags of 
	 * ContentItem, because it avoids the detached entity problem...
	 * 
	 * @param item
	 * @return
	 */
	public List<Tag> getTaggings(ContentItem item);
	

	/**
	 * Get the content items used as tags for the given content item. This method performs an
	 * aggregation over all taggings grouped by taggingResource.
	 * 
	 * @param item
	 * @return
	 */
	public List<ContentItem> getTags(ContentItem item);
	
	
	/**
	 * Return tags by a given label
	 * @param tagLabel
	 * @return
	 */
	public List<Tag> getTagsByLabel(String tagLabel);
	
	
	/**
	 * Return tags by a given user
	 * @param currentUser
	 * @return
	 */
	public List<Tag> getTagsByUser(User currentUser);
	
	/**
	 * Return a tag by a given id
	 * @param id
	 * @return
	 */
	public Tag getTagById(long id);

	/**
	 * Counts the current usages of a tag, meaning, how many ContentItems are tagged with the tag.
	 * @param contentItem
	 * @return
	 * @author szabyg
	 */
	public Long getTagUsage(ContentItem contentItem);

	/**
	 * Create taggings for a contentItem <code>item</code> with the taggingContentItem 
	 * having the label <code>label</code>. If a contentItem with the label already exists,
	 * it will be reused, otherwise freshly created.
	 * @param item
	 * @param labels
	 * @author szabyg
	 */
	public void addTags(ContentItem item, String[] labels);

	/**
	 * Is the taggingResource controlled vocabulary (SKOS Concept)? 
	 * @return
	 * @author szabyg
	 */
	public boolean isControlled(KiWiResource taggingResource);

	/**
	 * Delete taggings for a ContentItem <code>item</code> with the ContentItems with the labels in the String[] tagLabels.
	 * If <code>shared</code> is <code>true</code>, all users taggings will be removed. 
	 * @param item
	 * @param tagLabels
	 * @param shared
	 * @author szabyg
	 */
	public void removeTaggings(ContentItem item, String[] tagUris, boolean shared);

	/**
	 * Find all Tag objects for a specific tagged and tagging resource.   
	 * @param taggedResId
	 * @param taggingResId
	 * @return
	 */
	public List<Tag> getTagsByTaggedTaggingIds(Long taggedResId, Long taggingResId);
	
	/**
	 * @return
	 */
	public List<ContentItem> getFreeTags();

	/**
	 * Return all tags
	 * Tag instance.
	 * 
	 * @return
	 */
	public List<Tag> getAllTags();

	/**
	 * Return all content items that are used as tags, i.e. occurring as taggingResource in some
	 * Tag instance.
	 * 
	 * @return
	 */
	public List<ContentItem> getTaggingResources();	

	/**
	 * List all content items tagged with a certain item given as argument. May make use 
	 * of either database queries or the search service.
	 * 
	 * @param tag
	 * @return
	 */
	public List<ContentItem> listTaggedItems(ContentItem tag);
	
	/**
	 * List the users that are using the content item passed as argument in tagging 
	 * activities.
	 * 
	 * @param tag
	 * @return
	 */
	public List<User> listTagUsers(ContentItem tag);
	
	/**
	 * Returns the tag frequency of a given tag label assigned by a given user
	 * @param tagLabel
	 * @param user
	 * @return
	 */
	public float getTagFrequencyByUser(String tagLabel, User user);
	
	/**
	 * Returns the tag frequency of a given tag label for a given content item
	 * @param tagLabel
	 * @param contentItem
	 * @param amountOfTags
	 * @return
	 */

	public float getTagFrequencyByContentItem(String tagLabel, ContentItem contentItem, Float amountOfTags);

	/**
	 * @param tagLabel
	 * @param taggedResId
	 * @param taggingResId
	 * @return
	 */
	public List<Tag> getTaggingByTagLabelUserTaggedTaggingIds(String tagLabel, User user, Long taggedResId, Long taggingResId);	

    void addTagsByURI(ContentItem item, Set<String> uris);


    Set<ContentItem> addTagsByLabel(ContentItem item, Set<String> labels);

    /**
     * Returns all the tags where the tag label starts with a given prefix.
     *
     * @param labelPrefix the prefix.
     * @return a list with all all the tags where the tag label starts with
     * a given prefix.
     */
    List<Tag> getTagsByLabelPrefix(String labelPrefix);
    
	/**
	 * Return distinct tag labels
	 * @return
	 */
	public List<String> getDistinctTagLabels();    

}
