/*
 * File : HibernateJDBCserviceImpl.java.java
 * Date : Sep 29, 2010
 * 
 *
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
 */


package kiwi.service.jdbc;


import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.spi.PersistenceUnitTransactionType;

import kiwi.api.jdbc.HibernateJDBCServiceLocal;
import kiwi.api.jdbc.HibernateJDBCServiceRemote;
import kiwi.util.izpack.DatabaseSystem;

import org.hibernate.Session;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.ejb.packaging.PersistenceMetadata;
import org.hibernate.ejb.packaging.PersistenceXmlLoader;
import org.hibernate.jdbc.Work;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;


/**
 * Application scoped seam component named "hibernateJDBCService"
 * used to execute JDBC related actions. <br>
 * <b>Note : </b> this component can be used only in the KiWi
 * (enterprise) application. <br>
 * <b>Note : </b> This service must be used when the JPA standard
 * features are not enough (e.g. BLOB manipulation, native SQL,
 * stored procedures).
 * 
 * @author mihai
 * @version 0.9
 * @since 0.9
 */
@Name("hibernateJDBCService")
@Scope(ScopeType.APPLICATION)
@AutoCreate
public class HibernateJDBCServiceImpl implements HibernateJDBCServiceLocal,
        HibernateJDBCServiceRemote {

    /**
     * The key for the hibernate dialect property.
     */
    private static final String HIBERNATE_DIALECT_KEY = "hibernate.dialect";

    /**
     * All the logging messages will go over this field.
     */
    @Logger
    private static Log log;

    /**
     * The actual persistence context.
     */
    @In
    private EntityManager entityManager;

    /**
     * The data base system (descriptor) for the underlying
     * persistence layer.
     */
    private DatabaseSystem databaseSystem;

    /**
     * Builds an <code>HibernateJDBCServiceImpl</code> instance.
     */
    public HibernateJDBCServiceImpl() {
        // UNIMPLEMENTED
    }

    /**
     * Acts like an constructor (for seam environment).
     * 
     * @throws IOException if the KiWi-dev-ds.xml file can not be
     *             processed from any IO reason.
     * @throws IllegalStateException if the "ear.path" system
     *             propriety is not existing.
     * @throws IllegalArgumentException if the KiWi-dev-ds.xml
     *             contains other database than : postgres, mysql
     *             or h2.
     */
    @Create
    public void init() throws IOException {

        final Map<String, String> persistenceProperties;
        try {
            persistenceProperties = getPersistenceProperties();
        } catch (Exception e) {

            // Mihai : I know, it is not elegant to cathc all the
            // exception but in this case the try/catch sequence
            // will be to big.
            log.error(e.getMessage(), e);
            throw new IOException(e);
        }

        final String dialect = persistenceProperties.get(HIBERNATE_DIALECT_KEY);
        if (dialect == null) {
            final String msg =
                    "The hibernate dialect is null prove your persistence.xml file";
            final IllegalArgumentException exception =
                    new IllegalArgumentException(msg);
            log.error(msg, exception);
            throw exception;
        }
        databaseSystem = DatabaseSystem.getDatabaseForDialect(dialect);
        if (databaseSystem == null) {
            final IllegalArgumentException illegalArgException =
                    new IllegalArgumentException("The " + dialect
                            + " is not supported.");
            log.warn(illegalArgException.getMessage(), illegalArgException);
            throw illegalArgException;
        }
    }

    @Override
    public void execute(Work work) {
        // Mihai : This way to handle the JPA bounds this
        // implementation to Hibernate. Other ORM/JPA
        // implementations will cause here a ClassCastException
        final Session session = (Session) entityManager.getDelegate();
        session.doWork(work);
    }

    @Override
    public DatabaseSystem getDatasbaseSystem() {
        return databaseSystem;
    }

    /**
     * Loads all the properties for the underlying persistence
     * layer and sore them in a map.
     * 
     * @return all the the properties for the underlying
     *         persistence layer and sore them in a map.
     * @throws IOException by any IO related errors.
     * @throws Exception by any kind of errors.
     */
    private Map<String, String> getPersistenceProperties() throws Exception {
        final Map<String, String> result = new HashMap<String, String>();

        // mihai : I have this code from the hibernate classes, I
        // know it is not so nice but if it works for hibernate
        // it must also work for me.
        // I don't have any other 'decent' way to obtains the
        // information persistence layer informations.
        final Ejb3Configuration cfg = new Ejb3Configuration();
        final Enumeration<URL> persistenceXML =
                Thread.currentThread().getContextClassLoader()
                        .getResources("META-INF/persistence.xml");

        for (; persistenceXML.hasMoreElements();) {
            final URL nextURL = persistenceXML.nextElement();
            final HashMap overwrite = new HashMap();
            final List<PersistenceMetadata> metadataFiles =
                    PersistenceXmlLoader.deploy(nextURL, overwrite, cfg
                            .getHibernateConfiguration().getEntityResolver(),
                            PersistenceUnitTransactionType.RESOURCE_LOCAL);
            for (final PersistenceMetadata metadata : metadataFiles) {
                for (final Object key : metadata.getProps().keySet()) {
                    final String keyStr = key.toString();
                    final String valueStr =
                            metadata.getProps().get(key).toString();
                    result.put(keyStr, valueStr);
                }
            }
        }

        return result;
    }
}
