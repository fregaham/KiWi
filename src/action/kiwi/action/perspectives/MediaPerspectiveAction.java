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
 * sschaffe
 * 
 */


package kiwi.action.perspectives;


import java.util.Map;

import kiwi.api.content.ContentItemService;
import kiwi.api.event.KiWiEvents;
import kiwi.api.multimedia.MultimediaService;
import kiwi.model.content.ContentItem;
import kiwi.model.content.MediaContent;

import org.apache.commons.io.FilenameUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage.Severity;
import org.jboss.seam.log.Log;
import org.primefaces.component.media.player.FlashPlayer;
import org.primefaces.component.media.player.MediaPlayer;
import org.primefaces.component.media.player.MediaPlayerFactory;
import org.primefaces.component.media.player.QuickTimePlayer;
import org.primefaces.component.media.player.WindowsPlayer;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.UploadedFile;


/**
 * MediaPerspectiveAction
 * 
 * @author Sebastian Schaffert
 */
@Name("kiwi.core.ui.mediaPerspectiveAction")
@Scope(ScopeType.CONVERSATION)
public class MediaPerspectiveAction {

    @Logger
    private Log log;

    @In
    private MultimediaService multimediaService;

    @In
    private ContentItemService contentItemService;

    @In(required = false)
    private ContentItem currentContentItem;

    @In
    private FacesMessages facesMessages;
    
    // cached media content
    private MediaContent mediaContent;

    private boolean unsaved = false;

    @Create
    public void init() {

    }

    public boolean isImage(ContentItem item) {
    	boolean result =
    		(item.getMediaContent() != null
                && item.getMediaContent().getMimeType().startsWith("image")) ||
               (mediaContent != null && mediaContent.getMimeType().startsWith("image"));
    	
    	log.info("media content is an image: #0",result);
    	
    	return result;
    }

    public boolean isMedia(ContentItem item) {
    	boolean result =
    		(item.getMediaContent() != null && 
              (item.getMediaContent().getMimeType().startsWith("video") || item.getMediaContent().getMimeType().startsWith("audio"))) 
             ||
            (mediaContent != null && 
             (mediaContent.getMimeType().startsWith("video") || mediaContent.getMimeType().startsWith("audio")));
    	
    	log.info("media content is a video or audio resource: #0",result);
    	
    	return result;
    }
    
    public boolean hasMediaContent(ContentItem item) {
    	return item.getMediaContent() != null || mediaContent != null;
    }

    public MediaContent getMediaContent(ContentItem item) {
    	if(mediaContent == null) {
    		mediaContent = item.getMediaContent();
    	}
    	return mediaContent;
    }
    
    public String getPlayer(ContentItem item) {
    	if(mediaContent == null) {
    		mediaContent = item.getMediaContent();
    	}
    	String fn = mediaContent.getFileName();
    	
    	Map<String,MediaPlayer> players = MediaPlayerFactory.getPlayers();
		String[] tokens = fn.split("\\.");
		String type = tokens[tokens.length-1];
		
		for(MediaPlayer mp : players.values()) {
			if(mp.isAppropriatePlayer(type)) {
				if(mp instanceof QuickTimePlayer) {
					return "quicktime";
				} else if(mp instanceof FlashPlayer) {
					return "flash";
				} else if(mp instanceof WindowsPlayer) {
					return "windows";
				} else {
					return "real";
				}
			}
		}
		return "flash";
    	
     }
    
    public DefaultStreamedContent getMedia(ContentItem item) {
    	if(mediaContent == null) {
    		mediaContent = item.getMediaContent();
    	}
    	if(mediaContent != null) {
    		return new DefaultStreamedContent(mediaContent.getDataInputStream(), mediaContent.getMimeType(), mediaContent.getFileName().toLowerCase());
    	} else {
    		return null;
    	}
    }

    /**
     * Listener watching file uploads. When a file upload is
     * complete, a new MediaContent object is created based on
     * the uploaded data.
     * 
     * @param event
     */
    public void listener(FileUploadEvent event) {

        UploadedFile item = event.getFile();


        String name = FilenameUtils.getName(item.getFileName()).toLowerCase();
        byte[] data = item.getContents();
        String type = multimediaService.getMimeType(name, data);
        
        log.info("File: '#0' with type '#1' was uploaded", name,type);

        mediaContent = new MediaContent(currentContentItem);
        mediaContent.setData(data);
        mediaContent.setMimeType(type);
        mediaContent.setFileName(name);

        unsaved = true;
        
        facesMessages.add(Severity.INFO, "File #0 has been uploaded. Click on 'Save' to store it as part of the content item", name);
    }

    public void save() {
        if (mediaContent != null) {
            contentItemService.updateMediaContentItem(currentContentItem,
                    mediaContent.getData(), mediaContent.getMimeType(),
                    mediaContent.getFileName());
            unsaved = false;
    		Events.instance().raiseEvent(KiWiEvents.METADATA_UPDATED, currentContentItem);
        }
    }

	/**
	 * @return the unsaved
	 */
	public boolean isUnsaved() {
		return unsaved;
	}

	/**
	 * @param unsaved the unsaved to set
	 */
	public void setUnsaved(boolean unsaved) {
		this.unsaved = unsaved;
	}


}
