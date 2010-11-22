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

package kiwi.service.render.savelet;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ejb.Stateless;

import kiwi.api.content.ContentItemService;
import kiwi.api.entity.KiWiEntityManager;
import kiwi.api.fragment.FragmentFacade;
import kiwi.api.fragment.FragmentService;
import kiwi.api.triplestore.TripleStore;
import kiwi.exception.NamespaceResolvingException;
import kiwi.model.content.ContentItem;
import kiwi.model.content.TextContent;
import kiwi.model.content.TextFragment;
import kiwi.model.kbase.KiWiResource;
import kiwi.model.kbase.KiWiTriple;
import kiwi.model.kbase.KiWiUriResource;
import kiwi.model.user.User;

import nu.xom.Element;

import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;

/**
 * This savelet updates the content of the fragments inserted 
 * into this page and removes the fragments that are no longer
 * referenced in the content.
 * 
 * @author Marek Schmidt
 *
 */
@Stateless
@AutoCreate
@Name("kiwi.service.render.savelet.FragmentsSavelet")
public class FragmentsSavelet implements TextContentSavelet {
	
	@In
	private ContentItemService contentItemService;
	
	@In
	private FragmentService fragmentService;
	
	@In
	private TripleStore tripleStore;

	@Logger
	private Log log;
	
	@In(create=true) 
	private User currentUser;
	
	@In
	private KiWiEntityManager kiwiEntityManager;
	
	@In("storingService.contentItemCache")
	private Map<String, ContentItem> contentItemCache;
	
	/* (non-Javadoc)
	 * @see kiwi.service.render.savelet.Savelet#apply(kiwi.model.kbase.KiWiResource, java.lang.Object)
	 */
	@Override
	public TextContent apply(KiWiResource context, TextContent content) {
		ContentItem ci = context.getContentItem();
		
		Set<KiWiResource> referenced = new HashSet<KiWiResource> ();
		
		for (String fragmentId : content.getFragmentIds()) {
			
			ContentItem fragment_ci = contentItemCache.get(fragmentId);
			if (fragment_ci == null) {
				FragmentFacade ff = fragmentService.getContentItemFragment(ci, fragmentId, FragmentFacade.class);
				fragment_ci = ff.getDelegate();
			}
			
			if (fragment_ci == null) {
				log.info("ff is null for fragmentId #0", fragmentId);
				
				// This should not happen, we delete such fragment from the text content:
				if (fragmentId.startsWith("uri::")) {
					TextFragment tf = content.getFragment(new KiWiUriResource(fragmentId.substring(5)));
					Element start = tf.getBookmarkStart();
					Element end = tf.getBookmarkEnd();
					start.getParent().removeChild(start);
					end.getParent().removeChild(end);
				}
				
				continue;
			}
			
			TextFragment fragment = content.getFragment(((KiWiUriResource)fragment_ci.getResource()));
			
			referenced.add(fragment_ci.getResource());

			contentItemService.updateTextContentItem(fragment_ci, fragment.getFragmentTree());

			// update last author
			fragment_ci.setAuthor(currentUser);

			// update modification
			fragment_ci.setModified(new Date());

			contentItemService.saveContentItem(fragment_ci);
		}
		
		// We delete the non-referenced fragments
		Collection<KiWiTriple> triples;
		try {
			triples = ci.getResource().listIncoming("http://www.kiwi-project.eu/kiwi/special/fragmentOf");
			for (KiWiTriple triple : triples) {
				KiWiResource subject = triple.getSubject();
				if (subject.isUriResource()) {
					KiWiUriResource subject_uri = (KiWiUriResource)triple.getSubject();
					if (!referenced.contains(subject_uri)) {
						FragmentFacade ff = null;
						ContentItem fragment_ci = contentItemCache.get(subject_uri.getKiwiIdentifier());
						
						if (fragment_ci != null) {
							ff = kiwiEntityManager.createFacade(fragment_ci, FragmentFacade.class);
						}
						else {
							ff = fragmentService.getContentItemFragment(ci, subject_uri.getKiwiIdentifier(), FragmentFacade.class);
						}
						
						if (ff != null) {
							fragmentService.removeFragment(ff);
						}
						else {
							// This could happen if the fragment CI has been deleted by some other means, so only the triple remains, 
							tripleStore.removeTriple(triple);
						}
					}
				}
			}
		} catch (NamespaceResolvingException e) {
			e.printStackTrace();
		}
    	
		return content;
	}
}
