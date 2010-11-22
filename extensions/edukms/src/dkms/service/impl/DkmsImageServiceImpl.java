/*
	Copyright © dkms, artaround
 */
package dkms.service.impl;

import java.io.File;
import java.io.IOException;
import javax.ejb.Stateless;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import dkms.action.utils.DkmsImageScaleService;
import dkms.action.utils.DkmsPropertiesReader;
import dkms.datamodel.dkmsContentItem.DkmsMultimediaFacade;
import dkms.service.DkmsImageService;




/**
 * 
 * @see dkms.service.DkmsImageService
 */
@Name("dkmsImageService")
@Stateless
@Scope(ScopeType.STATELESS)
@AutoCreate
public class DkmsImageServiceImpl implements DkmsImageService {
	
	@Logger
	private Log log;

	/** 
	 * @see dkms.service.DkmsImageService#createImageURL(dkms.datamodel.dkmsContentItem.DkmsMultimediaFacade, java.lang.Integer, java.lang.Integer)
	 */
	@Override
	public String createImageURL(String filename,
			Integer width, Integer height) {
		
		//check if the image exists in the cache:
		
		if (filename == null){
			//logger error ...
			log.error("No filename stored in multimedia Facade!");
			return null;
		}
		
		//log.info(filename);
		
		//cache path:
		String cache = DkmsPropertiesReader.getProperty(DkmsPropertiesReader.FILE_CACHE);
		String urlServer =  DkmsPropertiesReader.getProperty(DkmsPropertiesReader.SERVER_URL);
		String folderServer = DkmsPropertiesReader.getProperty(DkmsPropertiesReader.IMAGES_DIRECTORY_ON_SERVER);
		
		//filename in cache = widthxheight_filename
		String cacheFilename = width+"x"+height+"_"+filename;
		String cacheURL = cache+"/"+cacheFilename;
		File cfile = new File(cacheURL);
		if (cfile.exists()){
			return urlServer+folderServer+cacheFilename;
		}
		
		//file not exists in cache -> create it:
		//repository path:
		String repos = DkmsPropertiesReader.getProperty(DkmsPropertiesReader.FILE_REPOSITORY);
		String reposURL = repos+filename;
		File rfile = new File(reposURL);
		if (!rfile.exists()){
			log.error("No file exists in file repository!", reposURL);
			return null;
		}
		
		try {
			DkmsImageScaleService.createScaledFile(rfile, cfile, width, height, true);
		} catch (IOException e) {
			// logger
			log.error("Image scale error!", rfile);
			e.printStackTrace();
		}
		
		return urlServer+folderServer+cacheFilename;
	}

}
