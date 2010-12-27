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
package kiwi.action.skill;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import kiwi.api.search.SemanticIndexingService;
import kiwi.api.search.SolrService;
import kiwi.api.skill.SkillService;
import kiwi.api.user.UserService;
import kiwi.model.skill.UserSkill;
import kiwi.model.user.User;

import org.hibernate.validator.Range;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.datamodel.DataModel;
import org.jboss.seam.faces.FacesMessages;

/**
 * 
 * 
 * @author Fred Durao, Nilay Coskun, Martin Leginus
 * 
 */
@Name("userSkillAction")
@Scope(ScopeType.CONVERSATION)
public class SkillAction implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@In
	private SolrService solrService;

	@In("kiwi.core.semanticIndexingService")
	private SemanticIndexingService semanticIndexingService;

	@In
	private FacesMessages facesMessages;
	
	@In
	private SkillService skillService;
	
	@In
	private UserService userService;
	
	private String selectedUser,selectedSkill;
	private Double skillTreshold = 0.0;
	
	private List<UserSkill> userSkills = new ArrayList<UserSkill>();
	
	private List<String> allUsers = new LinkedList<String>();
	
	private Set<String> allSkills = new TreeSet<String>();
	
	@DataModel
	private List<User> userList = new LinkedList<User>();
	
	@DataModel
	private List<SkillBean> skillList;	
	
	public SkillService getSkillService() {
		return skillService;
	}

	/**
	 * computeUserSkills
	 */
//	@Transactional
//	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void indexAndComputeUserSkills() {
		solrService.rebuildIndex();
		semanticIndexingService.reIndex();
		
		skillService.computeUserSkills();
		facesMessages.add("User skills computed");
	}

	/**
	 * computeUserSkills
	 */
//	@Transactional
//	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void computeUserSkills() {
		skillService.computeUserSkills();
		this.initTagList();
		facesMessages.add("User skills computed");		
	}
	
	/**
	 * deleteUserSkills
	 */
	public void deleteUserSkills() {
		skillService.deleteUserSkills();
	}
	
	/**
	 * deleteUserSkills
	 */
	public void removeSkill(String skillName) {
		skillService.removeSkill(skillName);
		this.initTagList();
		facesMessages.add("Skill removed");
	}
	/**
	 * deleteUserSkills
	 */
	public void removeSkillByUser(SkillBean skillBean) {
		Map<String, Float> skill = new HashMap<String, Float>();
		skill.put(skillBean.getSkill(),Float.valueOf(skillBean.getValue()).floatValue());
		skillService.removeSkillByUser(skill,skillBean.getUser());
		this.initTagList();
		facesMessages.add("Skill"+ skillBean.getSkill() +" removed, owned by" + skillBean.getUser().getLogin());
	}

	@Factory("userSkillAction.userSkills")
	public List<UserSkill> getUserSkills() {
		userSkills = skillService.listUserSkills();
		if (userSkills == null) {
			userSkills = skillService.listUserSkills();
		}
		return userSkills;
	}
	/**
	 * Returns user's skills according to a login
	 *
	 */
	public List<SkillBean> getUserSkillsByUserLogin(){
		UserSkill skills = skillService.getSkillsByUserLogin(selectedUser,skillTreshold);
		skillList = new LinkedList<SkillBean>();
		if (skills == null) {
			this.initTagList();
		}
		else {
			for (String skill :skills.getSkills().keySet()){
				if(skills.getSkills().get(skill)>=skillTreshold && skillService.getFakeSkillsBySkillName(skill)==null){
					SkillBean tmpBean = new SkillBean();
					tmpBean.setSkill(skill);
					tmpBean.setUser(skills.getUser());
					tmpBean.setValue(skills.getSkills().get(skill).toString());
					skillList.add(tmpBean);
				}
			}
		}
		return skillList;
	}
	/**
	 * Returns users according to a selected skill
	 */
	public List<User> getUsersBySkill() {
		userList = skillService.getUsersBySkill(selectedSkill,skillTreshold);
		return userList;
	}
	

	
	/**
	 * A factory method for setting up the list of my tags.
	 *
	 */
	@Factory("skillList")
	public void initTagList() {
			skillList = new LinkedList<SkillBean>();
			List<UserSkill> userSkills = getUserSkills();
			for (UserSkill userSkill2 : userSkills) {
				Map<String, Float> userSkill2Map = userSkill2.getSkills();
				for (String skillKey : userSkill2Map.keySet()) {
					if (skillService.getFakeSkillsBySkillName(skillKey)==null) {
						SkillBean skillBean = new SkillBean();
						skillBean.setUser(userSkill2.getUser());
						skillBean.setSkill(skillKey);
						skillBean.setValue(userSkill2Map.get(skillKey).toString());
						skillList.add(skillBean);
					}
				}
			}
	}

	public List<SkillBean> getSkillList() {
		return skillList;
	}

	public void setSkillList(List<SkillBean> skillList) {
		this.skillList = skillList;
	}
	
	public void loadFakeSkillList(){
		skillService.loadFakeSkillList();
		facesMessages.add("Fake Skill List Loaded");		
	}

	public void setAllUsers(List<String> allUsers) {
		this.allUsers = allUsers;
	}
	/**
	 * Returns all users's login expect anonymous user
	 * @return
	 */
	public List<String> getAllUsers() {
		List<User> users = userService.getAllCreatedUsers();
		allUsers = new LinkedList<String>();
		for(User user : users) {
			if (!user.getLogin().equals("anonymous")){
				allUsers.add(user.getLogin());
			}
		}
		return allUsers;
	}

	public void setSelectedUser(String selectedUser) {
		this.selectedUser = selectedUser;
	}

	public String getSelectedUser() {
		return selectedUser;
	}

	public void setSelectedSkill(String selectedSkill) {
		this.selectedSkill = selectedSkill;
	}

	public String getSelectedSkill() {
		return selectedSkill;
	}

	public void setAllSkills(Set<String> allSkills) {
		this.allSkills = allSkills;
	}

	/**
	 * It returns list of all skills of all users
	 * @return
	 */
	public Set<String> getAllSkills() {
		allSkills = new TreeSet<String>();
		userSkills = getUserSkills();
		for (UserSkill userSkill : userSkills) {
			for (String skill :userSkill.getSkills().keySet()){
				allSkills.add(skill);	
			}
		}
		return allSkills;
	}
	public List<User> getUserList() {
		return userList;
	}

	public void setUserList(List<User> userList) {
		this.userList = userList;
	}

	public void setSkillTreshold(Double skillTreshold) {
		this.skillTreshold = skillTreshold;
	}
	@Range(min=0,max=1,message="Entered value is out of the range, it must be between 0.0 - 1.0")
	public Double getSkillTreshold() {
		return skillTreshold;
	}	
	
}
