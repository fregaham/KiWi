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
package dkms.action.memberRegistration;

import kiwi.api.entity.KiWiEntityManager;
import kiwi.model.user.User;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.log.Log;

import dkms.action.memberRegistration.DkmsMemberBean;
import dkms.datamodel.DkmsUserFacade;


@Name("dkmsMemberAction")
@Scope(ScopeType.CONVERSATION)
@Transactional
public class DkmsMemberAction {
	
	
	private DkmsMemberBean dkmsMemberBean;
	
//	@In
//	private DkmsUserFacade dkmsUserFacade;
	
	@In
	private User currentUser;
	
    @Logger
    private Log log;
	
	@In
	private KiWiEntityManager kiwiEntityManager;
	
	private DkmsUserFacade userFacade;
	
	@Create
	public void begin(){
		
		userFacade = kiwiEntityManager.createFacade(currentUser.getContentItem(), DkmsUserFacade.class);
		
		log.info(userFacade.getGebDate());
		log.info(userFacade.getGender());
		log.info(userFacade.getPostalAdressStreetAndNumber());
		log.info(userFacade.getPlaceOfBirth());
		log.info(userFacade.getPostalAdressCity());
		log.info(userFacade.getPostalAdressCountryCode());
		log.info(userFacade.getMobilePhoneNumber());
		log.info(userFacade.getFacebook());
		log.info(userFacade.getTwitter());
		
		
		dkmsMemberBean = new DkmsMemberBean();
		dkmsMemberBean.setFirstName(currentUser.getFirstName());
		log.info(currentUser.getFirstName());
		dkmsMemberBean.setLastName(currentUser.getLastName());
		log.info(currentUser.getLastName());
		dkmsMemberBean.setEmailAdress(currentUser.getEmail());
		log.info(currentUser.getEmail());
		//-- Facade infrmation --//
		dkmsMemberBean.setGebDate(userFacade.getGebDate());
		dkmsMemberBean.setGender(userFacade.getGender());
		dkmsMemberBean.setPostalAdressStreetAndNumber(userFacade.getPostalAdressStreetAndNumber());
		dkmsMemberBean.setPlaceOfBirth(userFacade.getPlaceOfBirth());
		dkmsMemberBean.setPostalAdressCity(userFacade.getPostalAdressCity());
		dkmsMemberBean.setPostalAdressCountryCode(userFacade.getPostalAdressCountryCode());
		dkmsMemberBean.setMobilePhoneNumber(userFacade.getMobilePhoneNumber());
		dkmsMemberBean.setFacebook(userFacade.getFacebook());
		dkmsMemberBean.setTwitter(userFacade.getTwitter());
	}
	
	public void store(){
		
		currentUser.setFirstName(dkmsMemberBean.getFirstName());
		currentUser.setLastName(dkmsMemberBean.getLastName());
		currentUser.setEmail(dkmsMemberBean.getEmailAdress());
		userFacade.setGebDate(dkmsMemberBean.getGebDate());
		userFacade.setGender(dkmsMemberBean.getGender());
		userFacade.setPostalAdressStreetAndNumber(dkmsMemberBean.getPostalAdressStreetAndNumber());
		userFacade.setPostalAdressCity(dkmsMemberBean.getPostalAdressCity());
		userFacade.setPostalAdressCountryCode(dkmsMemberBean.getPostalAdressCountryCode());
		userFacade.setPlaceOfBirth(dkmsMemberBean.getPlaceOfBirth());
		userFacade.setMobilePhoneNumber(dkmsMemberBean.getMobilePhoneNumber());
		userFacade.setFacebook(dkmsMemberBean.getFacebook());
		userFacade.setTwitter(dkmsMemberBean.getTwitter());
		
		kiwiEntityManager.persist(userFacade.getDelegate());
	}
	
	public DkmsMemberBean getProfileBean() {
		return dkmsMemberBean;
	}

	public void setProfileBean(DkmsMemberBean dkmsMemberBean) {
		this.dkmsMemberBean = dkmsMemberBean;
	}
}
