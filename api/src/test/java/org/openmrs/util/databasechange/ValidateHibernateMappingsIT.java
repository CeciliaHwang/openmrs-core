/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.util.databasechange;

import ch.vorburger.exec.ManagedProcessException;
import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.persistence.Entity;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.MySQL5Dialect;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openmrs.util.LiquibaseVersionFinder;
import org.openmrs.util.OpenmrsClassScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates Hibernate mapping files.R
 */
public class ValidateHibernateMappingsIT {
	private static final Logger log = LoggerFactory.getLogger(ValidateHibernateMappingsIT.class);

	private static final String LIQUIBASECHANGELOG = "LIQUIBASECHANGELOG";
	private static final String LIQUIBASECHANGELOGLOCK = "LIQUIBASECHANGELOGLOCK";
	private static final String OPENMRS = "openmrs";
	private static final String DROP_DATABASE_OPENMRS = "drop database " + OPENMRS;

	private static Connection connection;
	private static DB db;
	private static DBConfigurationBuilder configurationBuilder;

	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	@BeforeClass
	public static void beforeClass() throws ManagedProcessException {
		startDatabase();
	}
	
	@AfterClass
	public static void afterClass() throws Exception {
		stopDatabase();
	}
	
	private static void startDatabase() throws ManagedProcessException {
		configurationBuilder = DBConfigurationBuilder.newBuilder();
		configurationBuilder.setPort(0); // automatically detect free port

		db = DB.newEmbeddedDB(configurationBuilder.build());
		db.start();
	}

	private static void stopDatabase() throws ManagedProcessException, SQLException {
		connection.close();
		db.stop();
	}
	
	@Test
	public void shouldValidateHibernateMappings() throws SQLException, LiquibaseException, ManagedProcessException {
		LiquibaseVersionFinder versionFinder = new LiquibaseVersionFinder();
		Set<List<String>> changesetCombinationsAsSet = versionFinder.getLiquibaseChangesetCombinations();
		List<List<String>> changesetCombinationsAsList = new ArrayList<>(changesetCombinationsAsSet);

		// test all possible combinations of liquibase snapshot and update files
		//
		for ( List<String> snapshotAndUpdateFileNames: changesetCombinationsAsList ) {
			
			this.createDatabase();

			log.info(
				"liquibase files used for creating and updating the OpenMRS database are: " + snapshotAndUpdateFileNames
			);

			for ( String fileName: snapshotAndUpdateFileNames ) {
				// process the core data file only for the first generation of liquibase snapshot files
				//
				if ( fileName.contains( "1.9.x/liquibase-core-data.xml" )) {
					log.info( "processing " + fileName );
					this.upgradeDatabase( fileName );
				}
				
				// exclude the core data file for subsequent generations of liquibase snapshot files
				//
				if ( !fileName.contains( "liquibase-core-data.xml" ) ) {
					log.info( "processing " + fileName );
					this.upgradeDatabase( fileName );
				}
			}

			// this is the core of this test: building the session factory validates if the generated database schema 
			// corresponds to Hibernate mappings
			//
			this.buildSessionFactory();
			
			this.dropDatabase();
		}
	}

	private void createDatabase() throws ManagedProcessException, SQLException {
		db.createDB( OPENMRS );

		connection = DriverManager.getConnection( configurationBuilder.getURL(OPENMRS), "root", "");
		connection.setAutoCommit(true);
	}

	private void upgradeDatabase(String filename) throws SQLException, LiquibaseException {
			Database liquibaseConnection = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(
				new JdbcConnection(connection)
			);
			liquibaseConnection.setDatabaseChangeLogTableName( LIQUIBASECHANGELOG );
			liquibaseConnection.setDatabaseChangeLogLockTableName( LIQUIBASECHANGELOGLOCK );

			Liquibase liquibase = new Liquibase(
				filename, 
				new ClassLoaderResourceAccessor(getClass().getClassLoader()), 
				liquibaseConnection
			);
			
			liquibase.update("");
			connection.commit();
	}

	private boolean dropDatabase() throws SQLException {
		PreparedStatement stmt = connection.prepareStatement( DROP_DATABASE_OPENMRS );
		return stmt.execute();
	}

	private SessionFactory buildSessionFactory() {
		Configuration configuration = new Configuration().configure();
		Set<Class<?>> entityClasses = OpenmrsClassScanner.getInstance().getClassesWithAnnotation( Entity.class);
		for (Class<?> clazz : entityClasses) {
			configuration.addAnnotatedClass(clazz);
		}
		configuration.setProperty( Environment.DIALECT, MySQL5LessStrictDialect.class.getName());
		configuration.setProperty(Environment.URL, configurationBuilder.getURL(OPENMRS));
		configuration.setProperty(Environment.DRIVER, "com.mysql.jdbc.Driver");
		configuration.setProperty(Environment.USER, "root");
		configuration.setProperty(Environment.PASS, "");
		configuration.setProperty(Environment.USE_SECOND_LEVEL_CACHE, "false");
		configuration.setProperty(Environment.USE_QUERY_CACHE, "false");

		// Validate HBMs against the actual schema
		configuration.setProperty(Environment.HBM2DDL_AUTO, "validate");

		return configuration.buildSessionFactory();
	}
}
