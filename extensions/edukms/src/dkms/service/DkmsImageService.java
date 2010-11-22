/*
	Copyright © dkms, artaround
 */

package dkms.service;

import dkms.datamodel.dkmsContentItem.DkmsMultimediaFacade;


public interface DkmsImageService {
	
	/**
	 * creates the URL of an image
	 * @param multimediaFacade
	 * @return the URL of an image
	 */
	public String createImageURL(String fileName, Integer width, Integer height);

}
