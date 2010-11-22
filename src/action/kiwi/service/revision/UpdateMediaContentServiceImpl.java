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

package kiwi.service.revision;

import java.io.Serializable;

import javax.ejb.Stateless;
import javax.mail.MethodNotSupportedException;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

import kiwi.api.revision.UpdateMediaContentServiceLocal;
import kiwi.api.revision.UpdateMediaContentServiceRemote;
import kiwi.api.revision.UpdateTextContentService.PreviewStyle;
import kiwi.api.transaction.TransactionService;
import kiwi.api.triplestore.TripleStore;
import kiwi.exception.ContentItemDoesNotExistException;
import kiwi.exception.ContentItemMissingException;
import kiwi.model.content.ContentItem;
import kiwi.model.content.ContentItemI;
import kiwi.model.content.MediaContent;
import kiwi.model.revision.CIVersion;
import kiwi.model.revision.MediaContentUpdate;
import kiwi.model.user.User;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.jboss.seam.log.Logging;

/**
 * MediaContentService creates, commits and reverts the current MediaContent for
 * a certain ContentItem. 
 * 
 * @author Stephanie Stroka (sstroka@salzburgresearch.at)
 *
 */
@Name("updateMediaContentService")
@AutoCreate
@Stateless
@Scope(ScopeType.STATELESS)
public class UpdateMediaContentServiceImpl implements Serializable,
		UpdateMediaContentServiceLocal, UpdateMediaContentServiceRemote {

	@In
	private TransactionService transactionService;
	
	@In
	private EntityManager entityManager;
	
	@In
	private User currentUser;
	
	@Logger
	private Log log;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@Override
	public MediaContentUpdate createMediaContentUpdate(ContentItemI _item, MediaContent newMediaContent) {
		ContentItem item = _item.getDelegate();
		MediaContent old = item.getMediaContent();
		if(old != newMediaContent) {
			MediaContentUpdate mcu = new MediaContentUpdate();
			mcu.setCurrentMediaContent(newMediaContent);
			
	    	mcu.setPreviousMediaContent(old);
	    	item.setMediaContent(newMediaContent);
	    	
	     	item.setAuthor(currentUser);
	     	
	     	EntityManager entityManager = (EntityManager) 
	     		Component.getInstance("entityManager");
			TripleStore tripleStore = (TripleStore) 
				Component.getInstance("tripleStore");
			if(newMediaContent != null) {
				entityManager.persist(newMediaContent);
			}
	     	tripleStore.persist(item);
			
			// add this update to the current transaction data
			try {
				transactionService.getTransactionCIVersionData(item).setMediaContentUpdate(mcu);
				Log log = Logging.getLog(this.getClass());
				log.info("UpdateMediaContentServiceImpl.createMediaContentUpdate() called ts.getTransactionCIVersionData()");
			} catch (ContentItemMissingException e) {
				e.printStackTrace();
			}
			return mcu;
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see kiwi.api.revision.UpdateMediaContentService#commitUpdate(kiwi.model.revision.MediaContentUpdate)
	 */
	public void commitUpdate(CIVersion version) {
		if(version.getMediaContentUpdate() != null) {
			EntityManager em = (EntityManager) Component.getInstance("entityManager");
			MediaContentUpdate mcu = version.getMediaContentUpdate();
			
			em.persist(mcu);
		}
	}

	/* (non-Javadoc)
	 * @see kiwi.api.revision.UpdateMediaContentService#createPreview(kiwi.model.revision.MediaContentUpdate, java.lang.String)
	 */
	public String createPreview(CIVersion version, PreviewStyle style, boolean showDeleted) {
		return null;
	}

	/* (non-Javadoc)
	 * @see kiwi.api.revision.UpdateMediaContentService#rollbackUpdate(kiwi.model.revision.MediaContentUpdate)
	 */
	public void rollbackUpdate(CIVersion version) {
		version.setMediaContentUpdate(null);
	}

	/* (non-Javadoc)
	 * @see kiwi.api.revision.UpdateMediaContentService#undo(kiwi.model.revision.MediaContentUpdate)
	 */
	public void restore(CIVersion version) {
		MediaContent mc = null;
		MediaContentUpdate mcu = version.getMediaContentUpdate();
		if(mcu != null) {
			mc = mcu.getCurrentMediaContent();
		} else {
			javax.persistence.Query q = entityManager.createNamedQuery("version.lastMediaContent");
			q.setParameter("ci",version.getRevisedContentItem());
			q.setParameter("vid",version.getVersionId());
			q.setMaxResults(1);
			try {
				mc = (MediaContent) q.getSingleResult();
			} catch (NoResultException ex) {
				log.debug("No previous MediaContent to restore. Thus, " +
						"we just delete the current MediaContent.");
			}
		}
		createMediaContentUpdate(version.getRevisedContentItem(), mc);
	}

	@Override
	public void undo(CIVersion version) throws ContentItemDoesNotExistException {
		try {
			throw new MethodNotSupportedException("UpdateTaggingService.restore(CIVersion version) is not supported");
		} catch (MethodNotSupportedException e) {
			e.printStackTrace();
		}
	}
}
