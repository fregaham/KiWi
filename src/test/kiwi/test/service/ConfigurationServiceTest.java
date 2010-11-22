/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 The KiWi Project. All rights reserved.
 * http://www.kiwi-project.eu
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  KiWi designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 * 
 * Contributor(s):
 * 
 * 
 */

package kiwi.test.service;

import java.util.LinkedList;

import javax.persistence.EntityManager;

import kiwi.api.config.ConfigurationService;
import kiwi.api.user.UserService;
import kiwi.exception.UserExistsException;
import kiwi.model.user.User;
import kiwi.test.base.KiWiTest;

import org.jboss.seam.Component;
import org.jboss.seam.log.Log;
import org.jboss.seam.log.Logging;
import org.jboss.seam.security.Identity;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Sebastian Schaffert
 *
 */
@Test
public class ConfigurationServiceTest extends KiWiTest {

	
	@Test
	public void testConfiguration() throws Exception {
    	
    	
    	/*
		 * Check whether we can set configuration options and they are immediately set correctly
		 */
    	new FacesRequest() {

            @Override
            protected void invokeApplication() {

            	Identity.setSecurityEnabled(false);

            	Log log = Logging.getLog(this.getClass());
            	log.info(">>>>>>>>> ConfigurationTest (1)");
            	ConfigurationService cs = (ConfigurationService) Component.getInstance("configurationService");
            	EntityManager        em = (EntityManager) Component.getInstance("entityManager");

            	cs.setConfiguration("key1","value1");
            	cs.setConfiguration("key2", "value2");
            	
            	LinkedList<String> ll = new LinkedList<String>();
            	ll.add("lv1");
            	ll.add("lv2");
            	ll.add("lv3");
            	cs.setListConfiguration("key3",ll);
            	
            	Assert.assertEquals(cs.getConfiguration("key1"), "value1");
            	Assert.assertEquals(cs.getConfiguration("key2"), "value2");
            	Assert.assertEquals(cs.getListConfiguration("key1").size(), 1);
            	Assert.assertEquals(cs.getListConfiguration("key2").size(), 1);
            	Assert.assertEquals(cs.getListConfiguration("key3").size(), 3);
            	//Assert.assertEquals(em.createNativeQuery("select count(*) from CONFIGURATION_LISTVALUE").getSingleResult(), 13L);
            	
            	log.info(">>>>>>>>> ConfigurationTest (1) end");
            }
    	}.run();

		/*
		 * Check whether we can retrieve configuration options in the second request
		 */
    	new FacesRequest() {

            @Override
            protected void invokeApplication() {

            	Identity.setSecurityEnabled(false);
            	
            	Log log = Logging.getLog(this.getClass());
            	log.info(">>>>>>>>> ConfigurationTest (2)");
            	
            	ConfigurationService cs = (ConfigurationService) Component.getInstance("configurationService");
            	EntityManager        em = (EntityManager) Component.getInstance("entityManager");

            	Assert.assertEquals(cs.getConfiguration("key1"), "value1");
            	Assert.assertEquals(cs.getConfiguration("key2"), "value2");
            	Assert.assertEquals(cs.getListConfiguration("key1").size(), 1);
            	Assert.assertEquals(cs.getListConfiguration("key2").size(), 1);
            	Assert.assertEquals(cs.getListConfiguration("key3").size(), 3);
            	
            	log.info(">>>>>>>>> ConfigurationTest (2) end");
            }
    	}.run();

		/*
		 * Check whether we can modify configuration options and they are immediately set correctly
		 */
    	new FacesRequest() {

            @Override
            protected void invokeApplication() {

            	Identity.setSecurityEnabled(false);
            	
            	Log log = Logging.getLog(this.getClass());
            	log.info(">>>>>>>>> ConfigurationTest (3)");
            	
            	ConfigurationService cs = (ConfigurationService) Component.getInstance("configurationService");
            	EntityManager        em = (EntityManager) Component.getInstance("entityManager");

            	cs.setConfiguration("key1","value11");
            	cs.setConfiguration("key2", "value22");
            	
            	
            	Assert.assertEquals(cs.getConfiguration("key1"), "value11");
            	Assert.assertEquals(cs.getConfiguration("key2"), "value22");
            	Assert.assertEquals(cs.getListConfiguration("key1").size(), 1);
            	Assert.assertEquals(cs.getListConfiguration("key2").size(), 1);
            	Assert.assertEquals(cs.getListConfiguration("key3").size(), 3);
            	//Assert.assertEquals(em.createNativeQuery("select count(*) from CONFIGURATION_LISTVALUE").getSingleResult(), 13);
            	
            	log.info(">>>>>>>>> ConfigurationTest (3) end");
            }
    	}.run();
    	
		/*
		 * Check whether modifications are persisted
		 */
    	new FacesRequest() {

            @Override
            protected void invokeApplication() {

            	Identity.setSecurityEnabled(false);
            	
            	Log log = Logging.getLog(this.getClass());
            	log.info(">>>>>>>>> ConfigurationTest (4)");
            	
            	ConfigurationService cs = (ConfigurationService) Component.getInstance("configurationService");
            	EntityManager        em = (EntityManager) Component.getInstance("entityManager");

            	
            	Assert.assertEquals(cs.getConfiguration("key1"), "value11");
            	Assert.assertEquals(cs.getConfiguration("key2"), "value22");
            	Assert.assertEquals(cs.getListConfiguration("key1").size(), 1);
            	Assert.assertEquals(cs.getListConfiguration("key2").size(), 1);
            	Assert.assertEquals(cs.getListConfiguration("key3").size(), 3);
            	//Assert.assertEquals(em.createNativeQuery("select count(*) from CONFIGURATION_LISTVALUE").getSingleResult(), 13);
            	
            	log.info(">>>>>>>>> ConfigurationTest (4) end");
            }
    	}.run();
    	/*
    	 * Check whether we can delete configuration options and they are immediately set correctly
    	 */
    	new FacesRequest() {

            @Override
            protected void invokeApplication() {

            	Identity.setSecurityEnabled(false);
            	
            	Log log = Logging.getLog(this.getClass());
            	log.info(">>>>>>>>> ConfigurationTest (5)");
            	
            	ConfigurationService cs = (ConfigurationService) Component.getInstance("configurationService");
            	
            	cs.removeConfiguration("key1");
            	cs.removeConfiguration("key2");
            	cs.removeConfiguration("key3");
            	
            	log.info(">>>>>>>>> ConfigurationTest (5) end");
            	
            }
    	}.run();
    	
      	
	}

	@Test
	public void testUserConfiguration() throws Exception {
    	
    	
    	/*
		 * Check whether we can set configuration options and they are immediately set correctly
		 */
    	new FacesRequest() {

            @Override
            protected void invokeApplication() {

            	Identity.setSecurityEnabled(false);

            	Log log = Logging.getLog(this.getClass());
            	log.info(">>>>>>>>> UserConfigurationTest (1)");
            	ConfigurationService cs = (ConfigurationService) Component.getInstance("configurationService");
            	EntityManager        em = (EntityManager) Component.getInstance("entityManager");

            	UserService        us = (UserService) Component.getInstance("userService");
            	User user=null;
				try {
					user = us.createUser("testusrA", "Hans", "Mustermann", "hansPW");
				} catch (UserExistsException e) {
					log.error("testusrA exists already");
				}
            	
            	if(user==null)user = us.getUserByLogin("testusrA");
            	
            	if(user==null)Assert.fail("user shouldn't be null!");
            	
            	cs.setUserConfiguration(user,"keyA","value1");
            	cs.setUserConfiguration(user,"keyB", "value2");
            	
            	LinkedList<String> ll = new LinkedList<String>();
            	
             	ll.add("lv1");
            	ll.add("lv2");
            	ll.add("lv3");
            	cs.setUserListConfiguration(user,"keyC",ll);
            	
            	Assert.assertEquals(cs.getUserConfiguration(user,"keyA"), "value1");
            	Assert.assertEquals(cs.getUserConfiguration(user,"keyB"), "value2");
            	Assert.assertEquals(cs.getUserListConfiguration(user,"keyA").size(), 1);
            	Assert.assertEquals(cs.getUserListConfiguration(user,"keyB").size(), 1);
            	Assert.assertEquals(cs.getUserListConfiguration(user,"keyC").size(), 3);
            	//Assert.assertEquals(em.createNativeQuery("select count(*) from CONFIGURATION_LISTVALUE").getSingleResult(), 13L);
            	
            	log.info(">>>>>>>>> UserConfigurationTest (1) end");
            }
    	}.run();

		/*
		 * Check whether we can retrieve configuration options in the second request
		 */
    	new FacesRequest() {

            @Override
            protected void invokeApplication() {

            	Identity.setSecurityEnabled(false);
            	
            	Log log = Logging.getLog(this.getClass());
            	log.info(">>>>>>>>> UserConfigurationTest (2)");
            	
            	ConfigurationService cs = (ConfigurationService) Component.getInstance("configurationService");
            	EntityManager        em = (EntityManager) Component.getInstance("entityManager");

            	UserService        us = (UserService) Component.getInstance("userService");
            	User user = us.getUserByLogin("testusrA");

            	Assert.assertEquals(cs.getUserConfiguration(user,"keyA"), "value1");
            	Assert.assertEquals(cs.getUserConfiguration(user,"keyB"), "value2");
            	Assert.assertEquals(cs.getUserListConfiguration(user,"keyA").size(), 1);
            	Assert.assertEquals(cs.getUserListConfiguration(user,"keyB").size(), 1);
            	Assert.assertEquals(cs.getUserListConfiguration(user,"keyC").size(), 3);
            	
            	log.info(">>>>>>>>> UserConfigurationTest (2) end");
            }
    	}.run();

		/*
		 * Check whether we can modify configuration options and they are immediately set correctly
		 */
    	new FacesRequest() {

            @Override
            protected void invokeApplication() {

            	Identity.setSecurityEnabled(false);
            	
            	Log log = Logging.getLog(this.getClass());
            	log.info(">>>>>>>>> UserConfigurationTest (3)");
            	
            	ConfigurationService cs = (ConfigurationService) Component.getInstance("configurationService");
            	EntityManager        em = (EntityManager) Component.getInstance("entityManager");

            	UserService        us = (UserService) Component.getInstance("userService");
            	User user = us.getUserByLogin("testusrA");
            	
            	cs.setUserConfiguration(user,"keyA","value11");
            	cs.setUserConfiguration(user,"keyB", "value22");

            	
            	Assert.assertEquals(cs.getUserConfiguration(user,"keyA"), "value11");
            	Assert.assertEquals(cs.getUserConfiguration(user,"keyB"), "value22");
            	Assert.assertEquals(cs.getUserListConfiguration(user,"keyA").size(), 1);
            	Assert.assertEquals(cs.getUserListConfiguration(user,"keyB").size(), 1);
            	Assert.assertEquals(cs.getUserListConfiguration(user,"keyC").size(), 3);
            	//Assert.assertEquals(em.createNativeQuery("select count(*) from CONFIGURATION_LISTVALUE").getSingleResult(), 13);
            	
            	log.info(">>>>>>>>> UserConfigurationTest (3) end");
            }
    	}.run();
    	
		/*
		 * Check whether modifications are persisted
		 */
    	new FacesRequest() {

            @Override
            protected void invokeApplication() {

            	Identity.setSecurityEnabled(false);
            	
            	Log log = Logging.getLog(this.getClass());
            	log.info(">>>>>>>>> UserConfigurationTest (4)");
            	
            	ConfigurationService cs = (ConfigurationService) Component.getInstance("configurationService");
            	EntityManager        em = (EntityManager) Component.getInstance("entityManager");

            	UserService        us = (UserService) Component.getInstance("userService");
            	User user = us.getUserByLogin("testusrA");
            	
            	Assert.assertEquals(cs.getUserConfiguration(user,"keyA"), "value11");
            	Assert.assertEquals(cs.getUserConfiguration(user,"keyB"), "value22");
            	Assert.assertEquals(cs.getUserListConfiguration(user,"keyA").size(), 1);
            	Assert.assertEquals(cs.getUserListConfiguration(user,"keyB").size(), 1);
            	Assert.assertEquals(cs.getUserListConfiguration(user,"keyC").size(), 3);
            	//Assert.assertEquals(em.createNativeQuery("select count(*) from CONFIGURATION_LISTVALUE").getSingleResult(), 13);
            	
            	log.info(">>>>>>>>> UserConfigurationTest (4) end");
            }
    	}.run();
    	/*
    	 * Check whether we can delete configuration options and they are immediately set correctly
    	 */
    	new FacesRequest() {

            @Override
            protected void invokeApplication() {

            	Identity.setSecurityEnabled(false);
            	
            	Log log = Logging.getLog(this.getClass());
            	log.info(">>>>>>>>> UserConfigurationTest (5)");
            	
            	ConfigurationService cs = (ConfigurationService) Component.getInstance("configurationService");

            	UserService        us = (UserService) Component.getInstance("userService");
            	log.info(">>>>>>>>> UserConfigurationTest - service decl (5)");
            	User user = us.getUserByLogin("testusrA");
            	log.info(">>>>>>>>> UserConfigurationTest - get user (5)");
            	
            	Assert.assertNotNull(user);
            	log.info(">>>>>>>>> UserConfigurationTest - user != null (5)");
            	cs.removeUserConfiguration(user,"keyA");
            	log.info(">>>>>>>>> UserConfigurationTest - remove 1 (5)");
            	cs.removeUserConfiguration(user,"keyB");
            	log.info(">>>>>>>>> UserConfigurationTest - remove 2 (5)");
            	cs.removeUserConfiguration(user,"keyC");
            	log.info(">>>>>>>>> UserConfigurationTest - remove 3 (5)");
            	
            	log.info(">>>>>>>>> UserConfigurationTest (5) end");
            	
            }
    	}.run();
    	

	}

}
