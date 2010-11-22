/*
	Copyright © dkms, artaround
 */
package dkms.action.dkmsContentItem;


public class DkmsFileManager {

	private String mimeType;
	
	private String fileName;

	public DkmsFileManager(){
		
	}
	
	public DkmsFileManager(String mimeType, String fileName){
		this.mimeType = mimeType;
		this.fileName = fileName;
	}
	
	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
		
}
