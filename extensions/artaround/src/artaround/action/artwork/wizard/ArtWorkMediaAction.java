/*
	Copyright © Rolf Sint, Michael Schneider, Christian Osterrieder, 2010
 */

package artaround.action.artwork.wizard;

import gate.creole.annic.apache.lucene.store.OutputStream;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.LinkedList;

import kiwi.model.content.ContentItem;
import kiwi.model.ontology.SKOSConcept;
import kiwi.model.user.User;

import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.io.FilenameUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.End;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.TransactionPropagationType;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.log.Log;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;
import org.richfaces.event.UploadEvent;
import org.richfaces.model.UploadItem;

import artaround.action.artwork.ArtWorkBean;
import artaround.action.artwork.MediaTmp;
import artaround.action.artwork.techniqueTree.TechniqueAction;
import artaround.action.utils.FileService;
import artaround.action.utils.ImageScaleService;
import artaround.action.utils.PropertiesReader;
import artaround.datamodel.artwork.ArtWorkFacade;
import artaround.service.ArtWorkService;

@Name("artWorkMediaAction")
@Scope(ScopeType.CONVERSATION)
// @Transactional
public class ArtWorkMediaAction {

	@Logger
	private static Log log;

	// check out the new created artwork
	@Out(scope = ScopeType.SESSION, required = false)
	private ContentItem currentContentItem;

	@In(create = true)
	private User currentUser;

	@In
	private ArtWorkBean artWorkBean;

	private int uploadsAvailable = 5;

	@In
	private ArtWorkService artWorkService;

	@In(required = false)
	private TechniqueAction techniqueAction;

	/**
	 * upload a media file <br/>
	 * if the media file is an image the image is scaled to the default sizes <br/>
	 * the media files is stored in the file repository
	 * 
	 * @param event
	 */
	public void listener(FileUploadEvent event) {

		UploadedFile item = event.getFile();

		log.info("File: '#0' with type '#1' was uploaded", item.getFileName(),
				item.getContentType());

		String name = FilenameUtils.getName(item.getFileName());
		String type = item.getContentType();

		log.info("File successfully read!");

		String repos = PropertiesReader
				.getProperty(PropertiesReader.FILE_REPOSITORY);
		String cache = PropertiesReader
				.getProperty(PropertiesReader.FILE_CACHE);

		File file = null;
		// store the orig in file repos:
		try {
			// create unique filename with timestamp+filename:
			name = "" + new Date().getTime() + "_" + name;
			file = FileService.copyFile(item.getContents(), new File(repos
					+ "/" + name));
		} catch (Exception e) {
			log.error("error copy file ... " + e.getMessage());
			e.printStackTrace();
		}

		try {
			// create the mini thumbnail image:
			// TODO: Werte aus property file auslesen
			Integer width = new Integer(100);
			Integer height = new Integer(75);
			File mini = new File(cache + "/" + width + "x" + height + "_"
					+ name);
			ImageScaleService.createScaledFile(file, mini, width, height, true);

			// create the thumbnail image:
			width = new Integer(
					PropertiesReader.getProperty(PropertiesReader.THUMB_SIZE_X));
			height = new Integer(
					PropertiesReader.getProperty(PropertiesReader.THUMB_SIZE_Y));
			File thumb = new File(cache + "/" + width + "x" + height + "_"
					+ name);
			ImageScaleService
					.createScaledFile(file, thumb, width, height, true);

			// create the mini preview image:
			width = new Integer(280);
			height = new Integer(150);
			File miniprev = new File(cache + "/" + width + "x" + height + "_"
					+ name);
			ImageScaleService.createScaledFile(file, miniprev, width, height,
					true);

			// create the preview image:
			width = new Integer(
					PropertiesReader
							.getProperty(PropertiesReader.PREVIEW_SIZE_X));
			height = new Integer(
					PropertiesReader
							.getProperty(PropertiesReader.PREVIEW_SIZE_Y));
			File prev = new File(cache + "/" + width + "x" + height + "_"
					+ name);
			ImageScaleService.createScaledFile(file, prev, width, height, true);

			// create the detail image:
			width = new Integer(
					PropertiesReader
							.getProperty(PropertiesReader.DETAIL_SIZE_X));
			height = new Integer(
					PropertiesReader
							.getProperty(PropertiesReader.DETAIL_SIZE_Y));
			File detail = new File(cache + "/" + width + "x" + height + "_"
					+ name);
			ImageScaleService.createScaledFile(file, detail, width, height,
					true);
		} catch (Exception e) {
			e.printStackTrace();
		}

		MediaTmp mt = new MediaTmp();
		mt.setMimeType(type);
		mt.setFileName(name);

		LinkedList<MediaTmp> artWorkList = artWorkBean.getArtAroundMediaList();
		if (artWorkList == null) {
			artWorkList = new LinkedList<MediaTmp>();
		}

		artWorkList.add(mt);
		artWorkBean.setArtAroundMediaList(artWorkList);
	}

	// @Transactional(TransactionPropagationType.REQUIRED)
	@End(root = true, beforeRedirect = true)
	// destroy all conversations
	public String storeArtWork() {

		artWorkBean.setAuthorName(currentUser.getFirstName() + " "
				+ currentUser.getLastName());
		ArtWorkFacade artWorkFacade = artWorkService.createArtWork(artWorkBean);

		if (techniqueAction != null) {
			addTechniques(artWorkFacade);
		}

		currentContentItem = artWorkFacade.getDelegate();
		return "/artaround/pages/artworks/tagKunstwerk.xhtml";
	}

	/**
	 * @param artWorkFacade
	 */
	private void addTechniques(ArtWorkFacade artWorkFacade) {
		LinkedList<SKOSConcept> techniques = artWorkFacade.getTechniques();
		if (techniques == null) {
			techniques = new LinkedList<SKOSConcept>();
		}

		LinkedList<SKOSConcept> chosenTechniques = techniqueAction
				.getChosenConcepts();

		for (SKOSConcept skosConcept : chosenTechniques) {
			techniques.add(skosConcept);
		}

		artWorkFacade.setTechniques(techniques);
	}

	//
	// public List<MultimediaFacade> getMediaFromArtwork(ArtWorkFacade a) {
	// return artaroundMediaList;
	// }
	//
	public int getUploadsAvailable() {
		return uploadsAvailable;
	}

	public void setUploadsAvailable(int uploadsAvailable) {
		this.uploadsAvailable = uploadsAvailable;
	}
	//
	// public MultimediaFacade getSelectedMultimedia() {
	// return selectedMultimedia;
	// }
	//
	// public ArtWorkFacade getSelectedArtWork() {
	// return selectedArtWork;
	// }
	//
	// public void setSelectedArtWork(ArtWorkFacade selectedArtWork) {
	// this.selectedArtWork = selectedArtWork;
	// }
}
