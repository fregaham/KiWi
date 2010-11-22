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

package kiwi.service.config;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.servlet.http.HttpServletRequest;

import kiwi.api.config.ConfigurationServiceLocal;
import kiwi.api.config.ConfigurationServiceRemote;
import kiwi.api.event.KiWiEvents;
import kiwi.api.extension.KiWiApplication;
import kiwi.model.Constants;
import kiwi.model.user.User;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.MapConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.ejb.packaging.PersistenceMetadata;
import org.hibernate.ejb.packaging.PersistenceXmlLoader;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.annotations.Synchronized;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.core.Conversation;
import org.jboss.seam.core.Events;
import org.jboss.seam.log.Log;
import org.jboss.seam.web.ServletContexts;

/**
 * @author Sebastian Schaffert
 *
 */
@Name("configurationService")
@Scope(ScopeType.APPLICATION)
@AutoCreate
@Synchronized
@Startup(depends="org.jboss.seam.core.applicationContext")
public class ConfigurationServiceImpl implements ConfigurationServiceLocal, ConfigurationServiceRemote {

    private static final double VERSION = 0.9;
    
    // default configuration (components names)
    private static final String[] savelets_source = {
        "kiwi.service.render.savelet.ExtractLinksSavelet",
        "kiwi.service.render.savelet.HtmlCleanerSavelet"
    };

    private static final String[] savelets_text = {
        "kiwi.service.render.savelet.NavigationalLinksSavelet",
        "kiwi.service.render.savelet.RdfaSavelet",
        "kiwi.service.render.savelet.FragmentsSavelet",
        "kiwi.service.render.savelet.ComponentSavelet"
    };

    private static final String[] renderlets_html_xom = {
        "kiwi.service.render.renderlet.ComponentRenderlet",
		"kiwi.service.render.renderlet.ComponentDisplayRenderlet",
        "kiwi.service.render.renderlet.RdfaRenderlet",
        "kiwi.service.render.renderlet.HtmlLinkRenderlet",
        "kiwi.service.render.renderlet.HtmlRdfaRenderlet",
        "kiwi.service.render.renderlet.HtmlFragmentRenderlet",
        "kiwi.service.render.renderlet.ImageLinkRenderlet",
        "kiwi.service.render.renderlet.QueryRenderlet"
    };

    private static final String[] renderlets_editor_xom = {
        "kiwi.service.render.renderlet.ComponentRenderlet",
        "kiwi.service.render.renderlet.RdfaRenderlet",
        "kiwi.service.render.renderlet.EditorLinkRenderlet"
    };

    private static final String[] renderlets_annotation_xom = {
        "kiwi.service.render.renderlet.HtmlLinkRenderlet",
        "kiwi.service.render.renderlet.AnnotationLinksRenderlet"
    };


    @Logger
    private Log log;

  	
 	private String configFile;
 	
 	private Configuration config;
 	private HashMap<String,Configuration> userConfigurations;
 	
 	
 	public static boolean needsSetup      = false;
 	public static boolean setupInProgress = false;
 	public static boolean configurationInProgress = false;
 	
 	// indicates whether we are in testing mode
 	public static boolean testing = false;

    /**
     * Transforms a given path from relative path to an absolute
     * path. More precisely if the given <code>path</code>
     * argument does not start with / then the given path is
     * considered relative path and it is prefixed with the
     * current running directory.<br>
     * If the path starts with / then the same string is
     * returned.
     * 
     * @param path the path to analyze.
     * @return a absolute path, if the given <code>path</code>
     *         argument starts with / character or the same
     *         String if it starts.
     */
    private String getPath(String path) {

        if (new File(path).isAbsolute()) {
            return path;
        }

        final String prefix = System.getProperty("user.dir");
        final StringBuffer result = new StringBuffer(prefix);
        result.append(File.separator);
        result.append(path);

        return result.toString();
    }
    
	@Create
	public void initialise() {
		log.info("KiWi ConfigurationService starting up ...");
		ConfigurationServiceImpl.configurationInProgress = true;

		HashMap<String,String> persistenceProperties = initPersistenceConfiguration();
		
		// initialise Apache Commons Configuration using a XML-file-based configuration
		if(persistenceProperties.get("kiwi.config") != null) {
			configFile = persistenceProperties.get("kiwi.config");
		} else if(persistenceProperties.get("kiwi.work.dir") != null) {
			configFile = persistenceProperties.get("kiwi.work.dir") + File.separator + "system-config.properties";
		}
		try {
			File f = new File(configFile);
			if(!f.exists()) {
				f.createNewFile();
			}
			config = new PropertiesConfiguration(f);
		} catch (Exception e) {
			log.error("error while initialising configuration: #0; creating memory-only configuration",e.getMessage());
			log.error(e);
			config = new MapConfiguration(new HashMap());
		}
		
		// copy over persistence.xml configuration
		for(String key : persistenceProperties.keySet()) {
			config.setProperty(key, persistenceProperties.get(key));
		}
		
		userConfigurations = new HashMap<String, Configuration>();
		
		if(persistenceProperties.get("kiwi.work.dir") != null) {
			File f = new File(persistenceProperties.get("kiwi.work.dir") + File.separator + "config");
			f.mkdirs();
		}
		
		if (config.containsKey("kiwi.solr.home")) {
		    // in this way the solr can use relative and absolute
            // path.
            final String solrHomeConf = getConfiguration("kiwi.solr.home");
            final String realPath = getPath(solrHomeConf);
            System.setProperty("solr.solr.home", realPath);
		} else {
			System.setProperty("solr.solr.home", getWorkDir() + "/solr");
		}

		System.setProperty("solr.data.dir",System.getProperty("solr.solr.home")+"/data");

		
//		if(version.getStringValue() == null || !version.getStringValue().equals(VERSION)) {
		
		// if the setup configuration is not yet set, this indicates that the system needs
		// setup; we set the appropriate flag in the application context; this flag is processed
		// by CheckSetupFilter and redirects to the setup process
		if(!config.containsKey("kiwi.setup")) {
			ConfigurationServiceImpl.needsSetup = true;
		}
		

		if (!config.containsKey("kiwi.version") || VERSION > config.getDouble("kiwi.version")) {
			log.info("no configuration found; initialising with default values ...");
			
			// initialise configuration
			setDoubleConfiguration("kiwi.version",VERSION);

			initWiklet("savelets.source", savelets_source);
			initWiklet("savelets.text", savelets_text);
			initWiklet("savelets.media", new String[0]);
			
			initWiklet("renderlets.html.source", new String[0]);
			initWiklet("renderlets.html.xom", renderlets_html_xom);
			
			initWiklet("renderlets.editor.source", new String[0]);
			initWiklet("renderlets.editor.xom", renderlets_editor_xom);
			
			initWiklet("renderlets.annotation.source", new String[0]);
			initWiklet("renderlets.annotation.xom", renderlets_annotation_xom);

			initWiklet("renderlets.media", new String[0]);

			
			//entityManager.flush();
		}
		
		setConfiguration(Constants.CFG_RELATIVE_PATH,"seam/resource/services");
		
		Events.instance().raiseAsynchronousEvent(KiWiEvents.CONFIGURATIONSERVICE_INIT,"init");
		Events.instance().raiseAsynchronousEvent(KiWiEvents.CONFIGURATIONSERVICE_CREATE_ADMIN,"init");
		// load the default stored procedures
		Events.instance().raiseAsynchronousEvent("loadDefaultStoredprocedures");
	}
	
	private void initWiklet(String configurationName, String[] componentNames) {
		List<String> componentNamesConfig = new LinkedList<String>();
		
        for(String cmp_name : componentNames) {
            componentNamesConfig.add(cmp_name);
        }
        setConfiguration(configurationName,componentNamesConfig);

    }

    private HashMap<String,String> initPersistenceConfiguration() {
        log.info("KiWi Configuration Service: looking up Hibernate configuration...");
        
        HashMap<String,String> result = new HashMap<String, String>();
        
        final Ejb3Configuration cfg = new Ejb3Configuration();
        try {
            final Enumeration<URL> xmls = Thread.currentThread()
            .getContextClassLoader()
            .getResources( "META-INF/persistence.xml" );
            if ( ! xmls.hasMoreElements() ) {
                log.info( "Could not find any META-INF/persistence.xml file in the classpath");
            }
            while ( xmls.hasMoreElements() ) {
                final URL url = xmls.nextElement();
                log.info( "Analysing persistence.xml: #0", url );
                try {
                    final List<PersistenceMetadata> metadataFiles = PersistenceXmlLoader.deploy(
                            url,
                            new HashMap(),
                            cfg.getHibernateConfiguration().getEntityResolver(),
                            PersistenceUnitTransactionType.RESOURCE_LOCAL );
                    for ( final PersistenceMetadata metadata : metadataFiles ) {

                        for(final Object key : metadata.getProps().keySet()) {
                            if("kiwi".equalsIgnoreCase(key.toString().substring(0, 4))) {
                                log.info( "#0 = #1", key, metadata.getProps().get(key) );
                                result.put(key.toString(), metadata.getProps().get(key).toString());
                            }
                        }
                    }
                } catch(Exception ex) {
                    log.error("error reading #0: is this a valid persistence configuration?",url.toString());
                }
            }
        } catch(final Exception ex) {
            log.error("could not read persistence.xml",ex);
        }
        return result;
    }


    @BypassInterceptors
	public String getSetupPage() {
		return getConfiguration("setupPage_1", getBaseUri()+"/content/SetupPage");
	}


	/* (non-Javadoc)
	 * @see kiwi.api.kspace.ConfigurationService#getStartPage()
	 */
    @BypassInterceptors
	public String getStartPage() {
		return getConfiguration("startpage", getBaseUri()+"/content/FrontPage");
	}

    /**
     * Get the base URI out of the current request. The base URI
     * is used e.g. to generate URIs of internal content items
     * 
     * @see kiwi.api.config.ConfigurationService#getBaseUri()
     */
	@BypassInterceptors
	public String getBaseUri() {

		String baseUri = (String) Contexts.getSessionContext().get("baseUri");
		
		if(baseUri == null) {
			final ServletContexts servletContexts = ServletContexts.getInstance();
			final HttpServletRequest request = servletContexts.getRequest();

			if(request != null && request.getScheme() != null && request.getServerName() != null) {
				baseUri = request.getScheme() + "://" + request.getServerName();
				if(!(
		             (request.getScheme()=="http" && request.getServerPort()==80) ||
		             (request.getScheme()=="https" && request.getServerPort()==443)
		             )){
		            baseUri += (":" + request.getServerPort());
				}
			    baseUri+= request.getContextPath();
			} else {
				return "http://localhost";
			}
			Contexts.getSessionContext().set("baseUri", baseUri);
		}
		return baseUri;
	}

	private String serverUri;

	@BypassInterceptors
	public String getServerUri() {
		if(serverUri == null) {
			final ServletContexts servletContexts = ServletContexts.getInstance();
			final HttpServletRequest request = servletContexts.getRequest();

			if(request != null) {
				serverUri = request.getScheme() + "://" + request.getServerName();
				if(!(
		             (request.getScheme()=="http" && request.getServerPort()==80) ||
		             (request.getScheme()=="https" && request.getServerPort()==443)
		             )){
		            serverUri += (":" + request.getServerPort());
				}
				serverUri += "/";
			} else {
				return "http://localhost/";
			}
		}

	    return serverUri;
	}

	
	
	/* (non-Javadoc)
	 * @see kiwi.api.config.ConfigurationService#isConfigurationSet(java.lang.String)
	 */
	@Override
    @BypassInterceptors
	public boolean isConfigurationSet(String key) {
		return config.containsKey(key);
	}

	/**
	 *
	 *
	 */
	@Override
    @BypassInterceptors
	public String getConfiguration(String key) {
		return config.getString(key);
	}

	@Override
    @BypassInterceptors
	public String getConfiguration(String key, String defaultValue) {
		return config.getString(key, defaultValue);
	}
	
	@Override
    @BypassInterceptors
	public double getDoubleConfiguration(String key) {
		return config.getDouble(key, 0.0);
    }
	
	@Override
    @BypassInterceptors
	public double getDoubleConfiguration(String key, double defaultValue) {
		return config.getDouble(key, defaultValue);
    }
	
	/* (non-Javadoc)
	 * @see kiwi.api.config.ConfigurationService#setDoubleConfiguration(java.lang.String, double)
	 */
	@Override
    @BypassInterceptors
	public void setDoubleConfiguration(String key, double value) {
		config.setProperty(key, value);
		save();
		Events.instance().raiseEvent("configurationChanged");
	}

	@Override
    @BypassInterceptors
	public int getIntConfiguration(String key) {
		return config.getInt(key, 0);
    }
	
	@Override
    @BypassInterceptors
	public int getIntConfiguration(String key, int defaultValue) {
		return config.getInt(key, defaultValue);
    }
	
	/* (non-Javadoc)
	 * @see kiwi.api.config.ConfigurationService#setIntConfiguration(java.lang.String, int)
	 */
	@Override
	public void setIntConfiguration(String key, int value) {
		config.setProperty(key, value);
		save();
		Events.instance().raiseEvent("configurationChanged");
	}
	
	@Override
    @BypassInterceptors
	public boolean getBooleanConfiguration(String key) {
		return config.getBoolean(key, true);
    }
	
	@Override
    @BypassInterceptors
	public boolean getBooleanConfiguration(String key, boolean defaultValue) {
		return config.getBoolean(key, defaultValue);
    }
	
	/* (non-Javadoc)
	 * @see kiwi.api.config.ConfigurationService#setIntConfiguration(java.lang.String, int)
	 */
	@Override
	public void setBooleanConfiguration(String key, boolean value) {
		config.setProperty(key, value);
		save();
		Events.instance().raiseEvent("configurationChanged");
	}

	/* (non-Javadoc)
	 * @see kiwi.api.config.ConfigurationService#getListConfiguration(java.lang.String)
	 */
	@Override
    @BypassInterceptors
	public List<String> getListConfiguration(String key) {
		return config.getList(key,Collections.EMPTY_LIST);
	}

	/* (non-Javadoc)
	 * @see kiwi.api.config.ConfigurationService#getListConfiguration(java.lang.String, java.util.List)
	 */
	@Override
    @BypassInterceptors
	public List<String> getListConfiguration(String key, List<String> defaultValue) {
		return config.getList(key, defaultValue);
	}

	/* (non-Javadoc)
	 * @see kiwi.api.config.ConfigurationService#setListConfiguration(java.lang.String, java.util.List)
	 */
	@Override
	public void setListConfiguration(String key, List<String> value) {
		config.setProperty(key, value);
		save();
		Events.instance().raiseEvent("configurationChanged");
	}

	public void removeConfiguration(String key) {
		config.clearProperty(key);
	}

	public void setConfiguration(String key, String value) {
		config.setProperty(key, value);
		save();
		Events.instance().raiseEvent("configurationChanged");
	}

	@Override
	public void setConfiguration(String key, List<String> values) {
		config.setProperty(key,values);
		save();
		Events.instance().raiseEvent("configurationChanged");
	}
	
	
	

	public Configuration getUserConfiguration(User user) {
		Configuration userConfig = userConfigurations.get(user.getLogin());
		if(userConfig == null) {
 		
			String userConfigFile = getConfiguration("kiwi.work.dir") + File.separator + "config" + File.separator + user.getLogin() + ".conf.xml"; 
	
			try {
				File f = new File(userConfigFile);
				if(f.exists()) {
					f.createNewFile();
				}
				userConfig = new PropertiesConfiguration(f);
			} catch(Exception ex) {
				log.error("could not create user configuration in file #0: #1",userConfigFile,ex.getMessage());
				userConfig = new MapConfiguration(new HashMap());
			}
			userConfigurations.put(user.getLogin(),userConfig);
		}
		return userConfig;
	}
	
	

	/* (non-Javadoc)
	 * @see kiwi.api.config.ConfigurationService#isUserConfigurationSet(kiwi.model.user.User, java.lang.String)
	 */
	@Override
	public boolean isUserConfigurationSet(User user, String key) {
		return getUserConfiguration(user).containsKey(key);
	}

	@Override
	public String getUserConfiguration(User user, String key, String defaultValue) {
		return getUserConfiguration(user).getString(key, defaultValue);
	}
	

	@Override
	public String getUserConfiguration(User user, String key) {
		return getUserConfiguration(user).getString(key);
	}
	

	/* (non-Javadoc)
	 * @see kiwi.api.config.ConfigurationService#getUserListConfiguration(kiwi.model.user.User, java.lang.String)
	 */
	@Override
	public List getUserListConfiguration(User user, String key) {
		return getUserListConfiguration(user, key, Collections.EMPTY_LIST);
	}

	/* (non-Javadoc)
	 * @see kiwi.api.config.ConfigurationService#getUserListConfiguration(kiwi.model.user.User, java.lang.String, java.util.List)
	 */
	@Override
	public List getUserListConfiguration(User user, String key, List defaultValue) {
		return getUserConfiguration(user).getList(key, defaultValue);
	}

	@Override
	public void removeUserConfiguration(User user, String key) {
		getUserConfiguration(user).clearProperty(key);
	}

	@Override
	public void setUserListConfiguration(User user, String key, List<String> values) {
		getUserConfiguration(user).setProperty(key, values);
	}

	@Override
	public void setUserConfiguration(User user, String key, String value) {
		getUserConfiguration(user).setProperty(key, value);
	}
	
//	@Override
//	public void setUserRSAConfiguration(User user, String key, String value) {
//		UserConfiguration conf = getUserConfiguration(user,key);
//		if(conf == null) {
//			conf = new UserConfiguration(user,key);
//		}
//		conf.setEncryptedKey(value);
//		setUserConfiguration(conf);
//	}


	/**
     * The work directory of the Sesame 2 native store. Sesame will create its own subdirectory
     * beneath this directory called "triples" and store the native database there.
     */
    @BypassInterceptors
	public String getWorkDir() {
		final String value = getConfiguration("kiwi.work.dir");
		return value!=null?value:"/tmp/kiwi";
	}

	@Override
	@BypassInterceptors
	public String getApplicationPrefix(KiWiApplication app) {
		return "kiwi.app."+app.getIdentifier()+".";
	}
	
	/**
     * redirect to the IDP server, 
     * which should then redirect to the FoafSslAuthenticationServlet
     */
    public void redirectToIDP() {

        /**
         * end conversation
         */
        Conversation.instance().end(true);

        try {
            FacesContext.getCurrentInstance().getExternalContext()
                .redirect(getBaseUri()+"/seam/resource/idp?authreqissuer="+
                        getBaseUri()+"/seam/resource/foafSsl");
        } catch (IOException e) {
            log.error("Redirect was not possible: #0 ", e.getMessage());
            e.printStackTrace();
        }
    }

    
    public void save() {
    	if(config instanceof PropertiesConfiguration) {
    		try {
				((PropertiesConfiguration)config).save();
			} catch (ConfigurationException e) {
				log.error("could not save system configuration: #0",e.getMessage());
			}
    	}
    }
    
    public void save(User user) {
    	Configuration userConfig = getUserConfiguration(user);
    	
    	if(userConfig instanceof PropertiesConfiguration) {
    		try {
				((PropertiesConfiguration)userConfig).save();
			} catch (ConfigurationException e) {
				log.error("could not save user configuration for user #0: #1",user.getLogin(),e.getMessage());
			}    		
    	}
    }
}
