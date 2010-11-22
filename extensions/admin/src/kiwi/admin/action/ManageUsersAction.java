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
package kiwi.admin.action;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import kiwi.api.user.UserService;
import kiwi.model.user.User;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.log.Log;
import org.richfaces.model.selection.Selection;

/**
 * ManageUsersAction
 *
 * @author Sebastian Schaffert
 *
 */
@Name("kiwi.admin.manageUsersAction")
@Scope(ScopeType.PAGE)
public class ManageUsersAction {

	@Logger
	private Log log;

	@In
	private FacesMessages facesMessages;

	
	@In
	private UserService userService;
	
	
	private List<User> users;
	
	private Selection selection;
	
	private User selectedUser;
	
	private String selectedUserPassword, selectedUserValidate;
	
	public List<User> getUsers() {
		if(users == null) {
			users = new LinkedList<User>();
			
			for(User u : userService.getAllCreatedUsers()) {
				users.add(u);
			}
		}
		return users;
	}

	/**
	 * @return the selectedUser
	 */
	public Selection getSelection() {
		return selection;
	}

	/**
	 * @param selection the selectedUser to set
	 */
	public void setSelection(Selection selection) {
		this.selection = selection;
		selectedUser = null;
		for(Iterator it = selection.getKeys(); it.hasNext(); ) {
			selectedUser = users.get((Integer)it.next());
			log.info("selected user #0", selectedUser.getLogin());
		}
	}
	
	public boolean isUserSelected() {
		return selectedUser != null;
	}

	/**
	 * @return the selectedUser
	 */
	public User getSelectedUser() {
		return selectedUser;
	}

	/**
	 * @param selectedUser the selectedUser to set
	 */
	public void setSelectedUser(User selectedUser) {
		this.selectedUser = selectedUser;
	}
	
	public void saveSelectedUser() {
		if(selectedUser != null) {
			if(selectedUserPassword != null && !"".equals(selectedUserPassword)) {
				if(selectedUserPassword.equals(selectedUserValidate)) {
					if(!userService.changePassword(selectedUser, selectedUserPassword)) {
						facesMessages.add("Could not update password");
					}
				} else {
					facesMessages.add("passwords do not match");
				}
			} else {
				userService.saveUser(selectedUser);				
			}
			
		}
	}

	/**
	 * @return the selectedUserPassword
	 */
	public String getSelectedUserPassword() {
		return selectedUserPassword;
	}

	/**
	 * @param selectedUserPassword the selectedUserPassword to set
	 */
	public void setSelectedUserPassword(String selectedUserPassword) {
		this.selectedUserPassword = selectedUserPassword;
	}

	/**
	 * @return the selectedUserValidate
	 */
	public String getSelectedUserValidate() {
		return selectedUserValidate;
	}

	/**
	 * @param selectedUserValidate the selectedUserValidate to set
	 */
	public void setSelectedUserValidate(String selectedUserValidate) {
		this.selectedUserValidate = selectedUserValidate;
	}
	
	
	
}
