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
package dkms.service;

import java.util.Date;
import java.util.Map;

import kiwi.api.content.ContentItemService;
import kiwi.api.entity.KiWiEntityManager;
import kiwi.api.user.UserService;
import kiwi.exception.RegisterException;
import kiwi.exception.UserExistsException;
import kiwi.model.user.User;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;

import dkms.datamodel.DkmsUserFacade;



@Name("dkms.userService")
@Scope(ScopeType.STATELESS)
@AutoCreate
@Transactional
public class DkmsUserService {

	@In
    protected UserService userService;
	
	@In
	private ContentItemService contentItemService;
	
	@In
	private KiWiEntityManager kiwiEntityManager;
	
	@In
	protected Map<String, String> messages;
	
	public DkmsUserFacade createUser(String login, String firstName, String lastName, String password, String email, int memberType, int userType, Date gebDate) throws UserExistsException, RegisterException {

			User user = userService.createUser(login, firstName, lastName, password);
			user.setEmail(email);
			final DkmsUserFacade userFacade = kiwiEntityManager.createFacade(user.getContentItem(), DkmsUserFacade.class);
			
			userFacade.setClosed(false);
			

			userFacade.setMember(1);
			if(gebDate != null){
				userFacade.setGebDate(gebDate);
			}

			userFacade.setUserType(userType);

			
			kiwiEntityManager.persist(userFacade);
			
			contentItemService.updateTitle(userFacade.getDelegate(), firstName+" "+lastName);
			return userFacade;
		
	}
	
	public DkmsUserFacade getUser( User user ) {
		return kiwiEntityManager.createFacade(user.getContentItem(), DkmsUserFacade.class);
	}
	
}
