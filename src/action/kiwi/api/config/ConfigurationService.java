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
 * Szaby Gruenwald
 *
 */

package kiwi.api.config;

import java.util.List;

import kiwi.api.extension.KiWiApplication;
import kiwi.exception.SystemNotYetSetUpException;
import kiwi.model.user.User;

/**
 * @author Sebastian Schaffert
 *
 */
public interface ConfigurationService {

    public void initialise() throws SystemNotYetSetUpException;

    /**
     * Get the start content item of this KiWi system.
     *
     * @return
     */
    public String getStartPage();

    /**
     * Get the base uri of the system, i.e. a uri that can be entered in the browser to access the
     * start page of this KiWi installation. The base uri is calculated based on the request URI
     * given by the user. The base uri is used by the KiWi system to generate new URIs of content
     * items. In this way, all KiWi resources are "Linked Open Data" compatible.
     *
     * @return
     */
    public String getBaseUri();


    /**
     * Get the server uri of the system, i.e. a uri that when entered in the browser accesses the
     * server that runs the KiWi (and SOLR) applications. Can be used to compute the paths of
     * other applications relative to the current application. Computed like the base uri.
     * @return
     */
    public String getServerUri();

    
    /**
     * Check whether a certain configuration property is set.
     * @param key
     * @return
     */
    public boolean isConfigurationSet(String key);
    
    
    /**
     * Get the configuration for the given key. If there is no such configuration, a new one is
     * created with empty value (returns null).
     *
     * @param key  unique configuration key for lookup
     * @return a configuration object with either the configured value or null as value
     */
    public String getConfiguration(String key);

    /**
     * Get the configuration for the given key. If there is no such configuration, a new one is
     * created using the provided defaultValue as string value.
     *
     * @param key unique configuration key for lookup
     * @param defaultValue default value if configuration not found
     * @return a configuration object with either the configured value or defaultValue
     */
    public String getConfiguration(String key, String defaultValue);

    /**
     * Get the configuration for the given key. If there is no such configuration, 0.0 is returned
     *
     * @param key unique configuration key for lookup
     * @return a double value with either the configured value or 0.0
     */
    
    public double getDoubleConfiguration(String key);
    
    /**
     * Get the configuration for the given key. If there is no such configuration, a new one is
     * created using the provided defaultValue as double value.
     *
     * @param key unique configuration key for lookup
     * @param defaultValue default value if configuration not found
     * @return a configuration object with either the configured value or defaultValue
     */
    public double getDoubleConfiguration(String key, double defaultValue);

    /**
     * Set the system configuration with the given key to the given double value.
     * 
     * @param key
     * @param value
     */
    public void setDoubleConfiguration(String key, double value);
    
    /**
     * Get the configuration for the given key. If there is no such configuration, 0 is returned
     *
     * @param key unique configuration key for lookup
     * @return a int value with either the configured value or 0
     */    
    public int getIntConfiguration(String key);
    
    /**
     * Get the configuration for the given key. If there is no such configuration, a new one is
     * created using the provided defaultValue as double value.
     *
     * @param key unique configuration key for lookup
     * @param defaultValue default value if configuration not found
     * @return a configuration object with either the configured value or defaultValue
     */
    public int getIntConfiguration(String key, int defaultValue);

    /**
     * Set the system configuration with the given key to the given int value.
     * 
     * @param key
     * @param value
     */
    public void setIntConfiguration(String key, int value);

    /**
     * Get the configuration for the given key. If there is no such configuration, true is returned
     *
     * @param key unique configuration key for lookup
     * @return a int value with either the configured value or true
     */    
    public boolean getBooleanConfiguration(String key);
    
    /**
     * Get the configuration for the given key. If there is no such configuration, a new one is
     * created using the provided defaultValue as boolean value.
     *
     * @param key unique configuration key for lookup
     * @param defaultValue default value if configuration not found
     * @return a configuration object with either the configured value or defaultValue
     */
    public boolean getBooleanConfiguration(String key, boolean defaultValue);

    /**
     * Set the system configuration with the given key to the given boolean value.
     * 
     * @param key
     * @param value
     */
    public void setBooleanConfiguration(String key, boolean value);

    /**
     * Get the configuration for the given key. If there is no such configuration, an empty list is returned
     *
     * @param key unique configuration key for lookup
     * @return a list with either the configured value or empty list
     */
    
    public List<String> getListConfiguration(String key);
    
    /**
     * Get the configuration for the given key. If there is no such configuration, a new one is
     * created using the provided defaultValue as double value.
     *
     * @param key unique configuration key for lookup
     * @param defaultValue default value if configuration not found
     * @return a configuration object with either the configured value or defaultValue
     */
    public List<String> getListConfiguration(String key, List<String> defaultValue);

    /**
     * Set the system configuration with the given key to the given int value.
     * 
     * @param key
     * @param value
     */
    public void setListConfiguration(String key, List<String> value);
    
    
    
    /**
     * Check whether the given configuration is set for the given user.
     * 
     * @param user
     * @param key
     * @return
     */
    public boolean isUserConfigurationSet(User user, String key);
    
    /**
     * Get the configuration for the given user and key. If there is no such configuration, a new one is
     * created with empty value (returns null).
     *
     * @param user  the user for whom to get the configuration
     * @param key  unique configuration key for lookup
     * @return a configuration object with either the configured value or null as value
     */
    public String getUserConfiguration(User user, String key);

    /**
     * Get the configuration for the given user and key. If there is no such configuration, a new one is
     * created using the provided defaultValue as string value.
     *
     * @param user  the user for whom to get the configuration
     * @param key unique configuration key for lookup
     * @param defaultValue default value if configuration not found
     * @return a configuration object with either the configured value or defaultValue
     */
    public String getUserConfiguration(User user, String key, String defaultValue);


    /**
     * Set the configuration "key" to the string value "value".
     * @param key
     * @param value
     */
    public void setConfiguration(String key, String value);

    /**
     * Set the configuration "key" to the string value "value".
     * @param key
     * @param value
     */
    public void setConfiguration(String key, List<String> values);


    /**
     * Set the configuration "key" to the string value "value".
     * @param key
     * @param value
     */
    public void setUserConfiguration(User user, String key, String value);

    /**
     * Set the configuration "key" to the string value "value".
     * @param key
     * @param value
     */
    public void setUserListConfiguration(User user, String key, List<String> values);


    /**
     * Return the list configuration value of the given key for the given user. If there is
     * no value for the key, returns the empty list.
     * 
     * @param user
     */
    public List getUserListConfiguration(User user, String key);
    
    /**
     * Return the list configuration value of the given key for the given user. Returns the
     * given defaultValue if no configuration is found for the given key.
     * 
     * @param user
     */
    public List getUserListConfiguration(User user, String key, List defaultValue);
    
    
    /**
     * Remove the configuration identified by "key" from the database.
     * @param key
     */
    public void removeConfiguration(String key);

    /**
     * Remove the user configuration identified by "key" from the database.
     * @param key
     */
    public void removeUserConfiguration(User user, String key);

    /**
     * @return a string representation of work direction
     */
    public String getWorkDir();

    /**
     * For storing application specific settings, build the key for the configuration with a prefix.
     * @param app
     * @return
     */
    public String getApplicationPrefix(KiWiApplication app);

    /**
     * redirect to the IDP server,
     * which should then redirect to the FoafSslAuthenticationServlet
     */
    public void redirectToIDP();

//	public void setUserRSAConfiguration(User user, String key, String value);

}
