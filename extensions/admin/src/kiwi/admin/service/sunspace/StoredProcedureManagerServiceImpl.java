/*
 * File : StoredProcedureManagerServiceImpl.java.java
 * Date : Sep 15, 2010
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


package kiwi.admin.service.sunspace;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import kiwi.admin.api.sunspace.StoredProcedureManagerException;
import kiwi.admin.api.sunspace.StoredProcedureManagerServiceLocal;
import kiwi.admin.api.sunspace.StoredProcedureManagerServiceRemote;
import kiwi.api.jdbc.HibernateJDBCService;
import kiwi.model.ceq.StoredProcedure;
import kiwi.service.equity.StoredProcedureWork;
import kiwi.util.izpack.DatabaseSystem;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;


/**
 * @author Mihai
 * @version 0.9
 * @since 0.9
 */
@Name("storedProcedureManagerService")
public class StoredProcedureManagerServiceImpl implements
        StoredProcedureManagerServiceLocal, StoredProcedureManagerServiceRemote {

    /**
     * All the logging messages will go over this field.
     */
    @Logger
    private static Log log;

    @In
    private EntityManager entityManager;

    @In
    private HibernateJDBCService hibernateJDBCService;

    @Override
    public boolean exist(StoredProcedure procedure) {
        final String databaseType = procedure.getDatabaseType();
        final String name = procedure.getName();

        final StoredProcedure existProcedure = getProcedure(databaseType, name);
        return existProcedure != null;
    }

    @Override
    public void store(StoredProcedure procedure) {
        final String databaseType = procedure.getDatabaseType();
        final String name = procedure.getName();

        final StoredProcedure existProcedure = getProcedure(databaseType, name);
        if (existProcedure == null) {
            entityManager.persist(procedure);
            return;
        }

        existProcedure.setContent(procedure.getContent());
        existProcedure.setAction(procedure.getAction());
        existProcedure.setDescription(procedure.getDescription());
        existProcedure.setActive(procedure.isActive());
    }

    private StoredProcedure getProcedure(final String databaseType,
            final String name) {
        final Query query =
                entityManager
                        .createNamedQuery("select.storedProcedureForNameAndDatabase");
        query.setParameter("name", name);
        query.setParameter("databaseType", databaseType);
        try {
            final StoredProcedure existProcedure =
                    (StoredProcedure) query.getSingleResult();
            return existProcedure;
        } catch (NoResultException noResultException) {
            return null;
        }
    }

    @Override
    public void remove(StoredProcedure procedure) {
        final String databaseType = procedure.getDatabaseType();
        final String name = procedure.getName();

        final StoredProcedure existProcedure = getProcedure(databaseType, name);
        if (existProcedure == null) {
            return;
        }

        final Query query =
                entityManager
                        .createNamedQuery("remove.storedProcedureForNameAndDatabase");
        query.setParameter("databaseType", databaseType);
        query.setParameter("name", name);
    }

    @Override
    public Set<StoredProcedure> getStoredAllProcedures(String databaseType) {
        final Query query =
                entityManager
                        .createNamedQuery("select.storedProcedureForDatabase");
        query.setParameter("databaseType", databaseType);

        final List resultList = query.getResultList();
        final Set<StoredProcedure> result =
                new TreeSet<StoredProcedure>(
                        StoredProcedureComprator.getInstance());
        result.addAll(resultList);

        return result;
    }

    @Override
    public Set<StoredProcedure> getStoredAllProcedures(String databaseType,
            String actionType) throws StoredProcedureManagerException {
        final Query query =
                entityManager
                        .createNamedQuery("select.storedProcedureForActionAndDatabase");
        query.setParameter("databaseType", databaseType);
        query.setParameter("action", actionType);

        final List resultList = query.getResultList();
        final Set<StoredProcedure> result = new HashSet<StoredProcedure>();
        result.addAll(result);

        return result;
    }

    @Override
    public StoredProcedure getStoredProcedure(String databaseType,
            String actionType) throws StoredProcedureManagerException {
        final Query query =
                entityManager.createNamedQuery("select.storedActiveProcedures");
        query.setParameter("action", actionType);
        query.setParameter("databaseType", databaseType);
        query.setParameter("active", true);

        try {
            final StoredProcedure result =
                    (StoredProcedure) query.getSingleResult();
            return result;
        } catch (NoResultException noResultException) {
            return null;
        }
    }

    @Override
    public Set<StoredProcedure> getStoredAllProcedures(String databaseType,
            String actionType, boolean active)
            throws StoredProcedureManagerException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void install(StoredProcedure procedure)
            throws StoredProcedureManagerException {
        final DatabaseSystem datasbaseSystem =
                hibernateJDBCService.getDatasbaseSystem();
        final String sqlStatement = getSQLStatement(procedure, datasbaseSystem);
        final StoredProcedureWork work = new StoredProcedureWork(sqlStatement);
        log.debug("Try to install #0", work);
        hibernateJDBCService.execute(work);
        log.debug("#0 was succesfull installed", work);
    }

    private String getSQLStatement(StoredProcedure procedure,
            DatabaseSystem databaseSystem) {
        final StoredProcedureBuilder builder;
        switch (databaseSystem) {
            case POSTGRESS:
                builder = PoststgresStoreProcedureBuilder.getInstance();
                break;
            case HYPERDRIVE:
                builder = HyperdriveStoreProcedureBuilder.getInstance();
                break;

            default:
                throw new IllegalArgumentException();
        }

        final String result = builder.getSQLStatement(procedure);
        return result;
    }

    /*
     * (non-Javadoc)
     * @see
     * kiwi.admin.api.sunspace.StoredProcedureManagerService#
     * getDefaultStoredProcedure(java.lang.String)
     */
    @Override
    public StoredProcedure getDefaultStoredProcedure(String databaseType)
            throws StoredProcedureManagerException {

        final StoredProcedure result = new StoredProcedure();
        result.setDatabaseType(databaseType);
        final String procName = "default-" + databaseType.toLowerCase();
        final String procedureContent;
        try {
            procedureContent = getProcedureContent(procName);
            result.setContent(procedureContent);
            return result;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new StoredProcedureManagerException(e);
        }
    }

    /**
     * Loads using the classloader the given resource and returns
     * its content like a string.
     * 
     * @param procName the resource name.
     * @return the content for the given resource (as a String).
     * @throws IOException by any IO related errors.
     */
    private String getProcedureContent(String procName) throws IOException {
        final ClassLoader classLoader = this.getClass().getClassLoader();
        final InputStream in = classLoader.getResourceAsStream(procName);
        final InputStreamReader inReader = new InputStreamReader(in);
        final BufferedReader reader = new BufferedReader(inReader);
        String readLine = reader.readLine();
        final StringBuffer result = new StringBuffer();
        while (readLine != null) {
            result.append(readLine);
            result.append("\n");
            readLine = reader.readLine();
        }

        return result.toString();
    }

}
