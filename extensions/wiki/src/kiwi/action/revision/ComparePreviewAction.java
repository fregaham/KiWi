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
package kiwi.action.revision;

import java.io.Serializable;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import kiwi.action.revision.style.StyleCreator;
import kiwi.api.render.RenderingService;
import kiwi.api.revision.RevisionBean;
import kiwi.api.revision.RevisionService;
import kiwi.api.revision.UpdateTextContentService;
import kiwi.model.content.TextContent;
import kiwi.model.revision.TextContentUpdateDiff;
import nu.xom.Document;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;


@Name("kiwi.action.revision.comparePreviewAction")
@Scope(ScopeType.CONVERSATION)
public class ComparePreviewAction implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String previewComparisonLeft;
	private String previewComparisonRight;
	
	private String titleLeft;
	
	private String titleRight;
	
	private RevisionBean revisionBeanLeft;
	
	private RevisionBean revisionBeanRight;
	
	@In
	private UpdateTextContentService updateTextContentService;
	
	@In
	private EntityManager entityManager;
	
	@In
	private RenderingService renderingPipeline;
	
	@In
	private RevisionService revisionService;
	
	public void compare(RevisionBean rev1, RevisionBean rev2) {
		StyleCreator styleCreator = (StyleCreator) Component.getInstance("kiwi.action.revision.style.styleCreator");
		styleCreator.generateStyle();
		
		revisionBeanRight = rev1;
		revisionBeanLeft = rev2;
		if(rev1.getVersion().getTextContentUpdate() != null &&
				rev2.getVersion().getTextContentUpdate() != null) {
			
			TextContent tc1 = rev1.getVersion().getTextContentUpdate().getTextContent();
			TextContent tc2 = rev2.getVersion().getTextContentUpdate().getTextContent();
			generateComparisonPreview(tc1, tc2, rev1, rev2);
			
		} else if(rev1.getVersion().getTextContentUpdate() != null) {
			previewComparisonLeft = "<span class=\"revision_" + rev1.getVersionNumber() + "\">" + 
				rev1.getVersion().getTextContentUpdate().getTextContent().getXmlString() + "</span>";
		} else if(rev2.getVersion().getTextContentUpdate() != null) {
			previewComparisonRight = "<span class=\"revision_" + rev2.getVersionNumber() + "\">" + 
				rev2.getVersion().getTextContentUpdate().getTextContent().getXmlString() + "</span>";
		} else {
			Query q1 = entityManager.createNamedQuery("version.lastTextContent");
			q1.setParameter("ci", rev1.getVersion().getRevisedContentItem());
			q1.setParameter("vid", rev1.getVersion().getVersionId());
			
			q1.setMaxResults(1);
			TextContent tc1 = null;
			try {
				tc1 = (TextContent) q1.getSingleResult();
			} catch (NoResultException e1) {
				tc1 = null;
			}

			Query q2 = entityManager.createNamedQuery("version.lastTextContent");
			q2.setParameter("ci", rev2.getVersion().getRevisedContentItem());
			q2.setParameter("vid", rev2.getVersion().getVersionId());
			
			q2.setMaxResults(1);
			TextContent tc2 = null;
			try {
				tc2 = (TextContent) q2.getSingleResult();
			} catch (NoResultException e) {
				tc2 = null;
			}
			
			if(tc1 != null && tc2 != null) {
				generateComparisonPreview(tc1, tc2, rev1, rev2);
			} else if(tc1 != null) {
				previewComparisonLeft = "<span>"+tc1.getXmlString()+"</span>";
			} else if(tc2 != null) {
				previewComparisonRight = "<span>"+tc2.getXmlString()+"</span>";
			} else {
				previewComparisonRight = "<span style=\"font-style:italic\">No textual updates available - Hence, no differences</span>";
			}
			
			/* ...yeah, that makes totally sense... stupid me -_-
			 * 
			previewComparison = "<span class=\"revision_" + rev2.getVersionNumber() + "\" style=\"text-decoration:line-through;\">" + 
				rev2.getVersion().getTextContentUpdate().getTextContent().getXmlString() + "</span>";
				*/
		}
	}

	/**
	 * Generates a styled view of the differences between two textcontentupdates 
	 * @param tcu1
	 * @param tcu2
	 * @param rev1
	 * @param rev2
	 */
	@SuppressWarnings("deprecation")
	private void generateComparisonPreview(TextContent tc1, TextContent tc2, RevisionBean rev1, RevisionBean rev2) {
		setTitleLeft(rev2.getTitle());
		setTitleRight(rev1.getTitle());
		
		if(tc1 != null && 
				tc2 != null) {
			List<TextContentUpdateDiff> diffsLeft = updateTextContentService.generateChanges(
					renderingPipeline.renderHTML(tc2.getXmlString(), rev1.getVersion().getRevisedContentItem()) ,
					renderingPipeline.renderHTML(tc1.getXmlString(), rev1.getVersion().getRevisedContentItem()));
			List<TextContentUpdateDiff> diffsRight = updateTextContentService.generateChanges(
					renderingPipeline.renderHTML(tc1.getXmlString(), rev1.getVersion().getRevisedContentItem()) ,
					renderingPipeline.renderHTML(tc2.getXmlString(), rev1.getVersion().getRevisedContentItem()));
			
			Document docLeft;
			Document docRight;
			try {
				docLeft = updateTextContentService.setStyleWithXOM(
						tc2.getXmlDocument(), diffsLeft, rev1.getVersionNumber(), true);
				setPreviewComparisonLeft(docLeft.toXML());
				docRight= updateTextContentService.setStyleWithXOM(
						tc1.getXmlDocument(), diffsRight, rev2.getVersionNumber(), true);
				setPreviewComparisonRight(docRight.toXML());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * @param previewComparisonLeft the previewComparisonLeft to set
	 */
	public void setPreviewComparisonLeft(String previewComparisonLeft) {
		this.previewComparisonLeft = previewComparisonLeft;
	}

	/**
	 * @return the previewComparisonLeft
	 */
	public String getPreviewComparisonLeft() {
		return previewComparisonLeft;
	}

	public String getPreviewComparisonRight() {
		return previewComparisonRight;
	}

	public void setPreviewComparisonRight(String previewComparisonRight) {
		this.previewComparisonRight = previewComparisonRight;
	}

	public String getTitleLeft() {
		return titleLeft;
	}

	public void setTitleLeft(String titleLeft) {
		this.titleLeft = titleLeft;
	}

	public String getTitleRight() {
		return titleRight;
	}

	public void setTitleRight(String titleRight) {
		this.titleRight = titleRight;
	}

	public String restoreLeft() {
		revisionService.restore(revisionBeanLeft.getVersion().getRevision());
		return "/wiki/home.xhtml";
	}
	
	public String restoreRight() {
		revisionService.restore(revisionBeanRight.getVersion().getRevision());
		return "/wiki/home.xhtml";
	}
	
	public String showDetailsLeft() {
		PreviewAction previewAction = (PreviewAction) 
				Component.getInstance("kiwi.action.revision.previewAction");
//		previewAction.setSelectedRevision(revisionBeanLeft);
		previewAction.beginPreview(revisionBeanLeft);
		return "/wiki/preview.xhtml";
	}
	
	public String showDetailsRight() {
		PreviewAction previewAction = (PreviewAction) 
				Component.getInstance("kiwi.action.revision.previewAction");
//		previewAction.setSelectedRevision(revisionBeanLeft);
		previewAction.beginPreview(revisionBeanRight);
		return "/wiki/preview.xhtml";
	}
}
