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
package tagit2.action.content;

import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;

import kiwi.api.comment.CommentService;
import kiwi.api.content.ContentItemService;
import kiwi.api.entity.KiWiEntityManager;
import kiwi.api.geo.Location;
import kiwi.api.multimedia.MultimediaService;
import kiwi.api.render.RenderingService;
import kiwi.api.tagging.TaggingService;
import kiwi.model.content.ContentItem;
import kiwi.model.facades.PointOfInterestFacade;
import kiwi.model.ontology.SKOSConcept;
import kiwi.model.user.User;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.End;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.log.Log;
import org.richfaces.event.UploadEvent;
import org.richfaces.model.UploadItem;

import tagit2.api.category.CategoryService;
import tagit2.util.error.Message;

@Scope(ScopeType.CONVERSATION)
@Name("tagit2.locationAction")
//@Transactional
public class LocationAction implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final int NONE = 0;
	private static final int IMAGE_UPLOAD = 1;
	private static final int ADD_COMMENT = 2;
	private static final int EDIT_TEXT = 3;

	private final String CAT_DEFAULT = "Bitte auswählen";
	private final String SUBCAT_DEFAULT = "Bitte auswählen";

	private static final String ADRESS_TEXT = "Nach Adresse suchen";

	private PointOfInterestFacade poi;

	private String title;
	private Location location;
	private String adress;
	private String description;
	private String date;

	private String category;
	private String subCategory;

	private int mode = -1;

	private LinkedList<ContentItem> images;
	private LinkedList<ContentItem> upload;

	private boolean hasChanged = false;

	@Logger
	Log log;

	@In
	private KiWiEntityManager kiwiEntityManager;

	@In(create = true)
	private TaggingService taggingService;

	@In
	private MultimediaService multimediaService;

	@In
	private ContentItemService contentItemService;

	@In(create = true)
	private ContentItem currentContentItem;

	@In(create = true)
	private CommentService commentService;

	@In
	private RenderingService renderingPipeline;

	@In(value = "tagit2.categoryService", create = true)
	private CategoryService categoryService;

	@In
	private User currentUser;

	// ************** conversation actions
	@Begin(join = true)
	public void begin() {
		if (poi == null || currentContentItem.getId() != poi.getId()) {

			poi = kiwiEntityManager.createFacade(currentContentItem,
					PointOfInterestFacade.class);
			title = poi.getTitle();
			mode = NONE;

			location = new Location(poi.getLatitude(), poi.getLongitude());
			if (poi.getAddress() == null || poi.getAddress().equals("")) {
				adress = ADRESS_TEXT;
			} else {
				adress = poi.getAddress();
			}
			images = poi.getMultimedia();
			upload = new LinkedList<ContentItem>();

			initCategoryStrings();

			DateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
			setDate(formatter.format(poi.getModified()));

			log.info("startet LocationAction with item item '#0'", title);
		} else {
			log.info("locationAction is already running");
		}
	}

	private void initCategoryStrings() {
		if (poi.getCategory() != null)
			category = poi.getCategory().getPreferredLabel();
		else
			category = "keine";
		if (poi.getSubCategory() != null)
			subCategory = poi.getSubCategory().getPreferredLabel();
		else
			subCategory = "keine";
	}

	@End
	public void cancel() {
		if (hasChanged)
			kiwiEntityManager.persist(poi);

		mode = -1;
	}

	@End
	public void save() {
		// some save stuff
		poi.setLatitude(location.getLatitude());
		poi.setLongitude(location.getLongitude());
		poi.setAddress(adress);

		kiwiEntityManager.persist(poi);

		mode = -1;
	}

	// ************* check storing
	public Message check(boolean loggedIn) {
		if (loggedIn) {
			// check if location has changed
			Message m = new Message();

			if (title.equals("")) {
				m.addLine("Titel ist leer");
				m.setType(Message.ERROR);
			}

			if (m.getType() == Message.ERROR) {
				return m;
			}

			if (location.getLatitude() == -1) {
				location.setLatitude(poi.getLatitude());
				location.setLongitude(poi.getLongitude());
				if (poi.getAddress() != null) {
					adress = poi.getAddress();
				}
			}
			if (location.getLatitude() != poi.getLatitude()
					|| location.getLongitude() != poi.getLongitude())
				m.addLine("Verschiebung des Punktes nach "
						+ location.toString());
			if (poi.getAddress() != null) {
				if (!poi.getAddress().equals(adress)
						&& !adress.equals(ADRESS_TEXT)) {
					if (adress.equals("")) {
						m.addLine("Adresse zur&#xFC;ckgesetzt");
					} else {
						m.addLine("Adresse ge&#xE4;ndert in '" + adress + "'");
					}
				}
			} else if (!adress.equals("") && !adress.equals(ADRESS_TEXT)) {
				m.addLine("Adresse ge&#xE4;ndert in '" + adress + "'");
			}

			return m;
		}

		else
			return new Message();
	}

	public void saveLocation() {
		poi.setLatitude(location.getLatitude());
		poi.setLongitude(location.getLongitude());
		poi.setAddress(adress);
	}

	// ************* image and comment event modes
	public void startImageUpload() {
		mode = IMAGE_UPLOAD;
	}

	public void saveImageUpload() {
		if (!upload.isEmpty()) {
			for (ContentItem media : upload) {
				kiwiEntityManager.persist(media);
				multimediaService.extractMetadata(media);
				images.addFirst(media);
			}

			poi.setMultimedia(images);

			// refresh multimedialist
			upload = new LinkedList<ContentItem>();

			hasChanged = true;
		}
		mode = NONE;
	}

	public void cancelImageUpluad() {
		upload = new LinkedList<ContentItem>();

		mode = NONE;
	}

	private String commenttitle;
	private String comment;

	public void startComment() {
		comment = "";
		commenttitle = "";
		mode = ADD_COMMENT;
	}

	public void saveComment() {
		if (!title.equals("")) {

			commentService.createReply(currentContentItem, currentUser,
					commenttitle, comment);

			contentItemService.saveContentItem(currentContentItem);
		}
		mode = NONE;
	}

	public void cancelComment() {
		mode = NONE;
	}

	private String editDescription;

	public void startEdit() {
		log.info("start edit");
		editDescription = renderingPipeline.renderEditor(poi.getDelegate(), currentUser);
		mode = EDIT_TEXT;
		if (poi.getCategory() == null)
			category = CAT_DEFAULT;
		if (poi.getSubCategory() == null)
			subCategory = SUBCAT_DEFAULT;
	}

	public void saveEdit() {
		contentItemService.updateTextContentItem(poi, editDescription);

		contentItemService.updateTitle(poi, title);

		// save categories
		SKOSConcept cat = categoryService.getCategory(category);

		if (cat != null) {
			if (poi.getCategory() == null
					|| cat.getId() != poi.getCategory().getId()) {
				poi.setCategory(cat);
				if (!poiContainsTag(category))
					taggingService.createTagging(category, poi.getDelegate(),
							cat.getDelegate(), currentUser);
			}
			if (!subCategory.equals("") && !subCategory.equals(SUBCAT_DEFAULT)) {
				if (poi.getSubCategory() == null
						|| !poi.getSubCategory().getPreferredLabel().equals(
								subCategory)) {
					SKOSConcept subcat = categoryService.getSubcategory(
							subCategory, cat);
					poi.setSubCategory(subcat);
					if (!poiContainsTag(subCategory))
						taggingService.createTagging(subCategory, poi
								.getDelegate(), subcat.getDelegate(),
								currentUser);
				}

			} else {
				subCategory = "keine";
			}
		} else {
			category = "keine";
		}

		hasChanged = true;

		mode = NONE;
	}

	private boolean poiContainsTag(String cat) {
		return taggingService.hasTag(poi.getDelegate(), cat);
	}

	public void cancelEdit() {
		title = poi.getTitle();
		initCategoryStrings();
		mode = NONE;

	}

	// ************* other action
	/**
	 * To order comments (from new to old)
	 */
	public List<ContentItem> getAllComments() {
		// List<ContentItem> cl = new LinkedList<ContentItem>();
		// for( ContentItem c :currentContentItem.getComments()) {
		// cl.add(c);
		// }
		// //sort must be happen in onother list
		// Collections.sort(cl, new Comparator<ContentItem>(){
		// @Override
		// public int compare(ContentItem o1, ContentItem o2) {
		// return o2.getCreated().compareTo(o1.getCreated());
		// }
		// });
		// return cl;
		return commentService.listComments(currentContentItem);
	}

	public void listener(UploadEvent event) {

		UploadItem item = event.getUploadItem();

		log.info("File: '#0' with type '#1' was uploaded", item.getFileName(),
				item.getContentType());

		String name = FilenameUtils.getName(item.getFileName());
		String type = item.getContentType();
		byte[] data = item.getData();

		if (item.isTempFile()) {
			try {
				data = FileUtils.readFileToByteArray(item.getFile());
			} catch (IOException e) {
				log.error("error reading file #0", item.getFile()
						.getAbsolutePath());
			}
		}

		type = multimediaService.getMimeType(name, data);

		ContentItem contentItem = contentItemService.createMediaContentItem(
				data, type, name);
		contentItem.setAuthor(currentUser);

		upload.add(contentItem);
	}

	// ************** getters and setters
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public void setAdress(String adress) {
		this.adress = adress;
	}

	public String getAdress() {
		return adress;
	}

	public LinkedList<ContentItem> getImages() {
		return images;
	}

	public int getImageListSize() {
		return images.size();
	}

	public int getMode() {
		return mode;
	}

	public void setCommenttitle(String commenttitle) {
		this.commenttitle = commenttitle;
	}

	public String getCommenttitle() {
		return commenttitle;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getComment() {
		return comment;
	}

	public String getEditDescription() {
		return editDescription;
	}

	public void setEditDescription(String editDescription) {
		this.editDescription = editDescription;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getSubCategory() {
		return subCategory;
	}

	public void setSubCategory(String subCategory) {
		this.subCategory = subCategory;
	}

	public List<String> getCatgoryStrings() {
		return categoryService.listCategoryStrings();
	}

	public List<String> getSubCategoryStrings() {
		log.info("get subcategories for #0", category);
		return categoryService.listSubcategoryStrings(categoryService
				.getCategory(category));
	}

	public String getCAT_DEFAULT() {
		return CAT_DEFAULT;
	}

	public String getSUBCAT_DEFAULT() {
		return SUBCAT_DEFAULT;
	}

	public String getDescription() {
		return renderingPipeline.renderEditor(poi.getDelegate(), currentUser);
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getDate() {
		return date;
	}
}
