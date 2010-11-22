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

package kiwi.wiki.action;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;

import javax.faces.context.FacesContext;
import javax.ws.rs.PathParam;

import kiwi.api.content.ContentItemService;
import kiwi.api.entity.KiWiEntityManager;
import kiwi.api.event.KiWiEvents;
import kiwi.api.render.RenderingService;
import kiwi.context.CurrentUserFactory;
import kiwi.model.Constants;
import kiwi.model.content.ContentItem;
import kiwi.model.user.User;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.richfaces.component.UITogglePanel;

/**
 * @author Sebastian Schaffert
 * 
 */
@Name("viewAction")
@Scope(ScopeType.CONVERSATION)
// event scoped so that it is rerendered when the page is displayed again
public class ViewAction implements Serializable {

	private static final long serialVersionUID = 5668835011461415359L;
	
	@Logger
	private static Log log;

	@In(create = true)
	private User currentUser;

	@In(create = true)
	private ContentItem currentContentItem;

	@In
	private ContentItemService contentItemService;

    @In
    private KiWiEntityManager kiwiEntityManager;

	private String currentContentHtml;
	private String description;
	
	private String htmlRef;
	
	@In
	private RenderingService renderingPipeline;
	
	public void begin() {
		description = renderingPipeline.renderEditor(currentContentItem, currentUser);
	}

	/**
	 * @return the currentContentHtml
	 */
//	public String getCurrentContentHtml() {
//		if(currentContentHtml == null) {
//			log.debug(currentContentItem.getTitle());
//			
//			
//			if (currentContentItem.getTextContent() != null) {
//				currentContentHtml = renderingPipeline.renderHTML(currentContentItem);
//				log.debug("rendering html content: #0", currentContentHtml);
//			} else {
//				currentContentHtml = "<p>Please add initial content</p>";
//			}
//		}
//		return currentContentHtml;
//	}

	// For small description editors like the one on the imageTemplate.xhtml
	public void saveDescription(){
    	contentItemService.updateTextContentItem(currentContentItem, description);
		switchDescToView();
		kiwiEntityManager.persist(currentContentItem);
	}
	public void cancelSaveDescription(){
		switchDescToView();
	}
	private void switchDescToView(){
		UITogglePanel descriptionTogglePanel=(UITogglePanel) 
		FacesContext.getCurrentInstance().getViewRoot().findComponent("formWikiContent:DescriptionEditor");
		if(descriptionTogglePanel!=null)
		descriptionTogglePanel.setValue("descView");

	}

	public String getDescription(){
		return description;
	}
	
	public void setDescription(String desc){
		description = desc;
	}
	
	/**
	 * @param currentContentHtml
	 *            the currentContentHtml to set
	 */
	public void setCurrentContentHtml(String currentContentHtml) {
		this.currentContentHtml = currentContentHtml;
	}

	public String getHtmlRef() {
		return htmlRef;
	}

	public void setHtmlRef(String htmlRef) {
		this.htmlRef = htmlRef;
	}

	@Observer(value={KiWiEvents.CONTENT_UPDATED, KiWiEvents.METADATA_UPDATED},create=false)
	public void clear(ContentItem item) {
		this.currentContentHtml = null;
	}
	
	public String getCurrentContentHtml() {
		
		if (currentContentItem.getTextContent() != null) {
			currentContentHtml = renderingPipeline
					.renderHTML(currentContentItem, currentUser);
		}  else {
			currentContentHtml = "<p>Please add initial content</p>";
		}

		// hackish workaround for Facelets-Bug (331), fixes KIWI-528
//		currentContentHtml = currentContentHtml.replaceAll("\\s<", "#{' '}<");

		// strip out all kiwi xml namespace declarations, workaround for
		// KIWI-540 / Facelets-350
		// currentContentHtml =
		// currentContentHtml.replaceAll("xmlns(:[a-zA-Z]+)?=\"[^\"]+\"", "");
		currentContentHtml = currentContentHtml.replaceAll(
				"xmlns:kiwi=\"[^\"]+\"", "");

		// fixes KIWI-500, prevents currentContentHtml from being surrounded
		// with any <html>...</html>
		String result = "<html><head></head><body><ui:composition "
				+ "xmlns:ui=\"http://java.sun.com/jsf/facelets\" "
				+ "xmlns:kiwi=\"" + Constants.NS_KIWI_HTML + "\" " + "xmlns=\""
				+ Constants.NS_XHTML + "\">\n" + currentContentHtml
				+ "</ui:composition></body></html>\n";

		// // line wrapping; needed so that RESTEasy does not break

		log.debug("getHtml: #0", result);

		return currentContentHtml;
	}
}
