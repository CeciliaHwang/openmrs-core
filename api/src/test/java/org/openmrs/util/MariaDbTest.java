/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.util;

import ch.vorburger.exec.ManagedProcessException;
import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.openmrs.liquibase.LiquibaseProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class MariaDbTest implements LiquibaseProvider {
	private static final Logger log = LoggerFactory.getLogger( MariaDbTest.class);

	private static final String CONTEXT = "some context";
	
	private static final String LIQUIBASECHANGELOG = "LIQUIBASECHANGELOG";
	private static final String LIQUIBASECHANGELOGLOCK = "LIQUIBASECHANGELOGLOCK";

	protected static final String OPENMRS = "openmrs";
	protected static final String DROP_DATABASE_OPENMRS = "drop database " + OPENMRS;

	private static Connection connection;
	private static DB db;
	private static DBConfigurationBuilder configurationBuilder;
	
	@BeforeClass
	public static void beforeClass() throws ManagedProcessException {
		startDatabase();
	}

	@AfterClass
	public static void afterClass() throws Exception {
		stopDatabase();
	}

	public Liquibase getLiquibase( String filename ) throws LiquibaseException {
		Database liquibaseConnection = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(
			new JdbcConnection(connection)
		);
		liquibaseConnection.setDatabaseChangeLogTableName( LIQUIBASECHANGELOG );
		liquibaseConnection.setDatabaseChangeLogLockTableName( LIQUIBASECHANGELOGLOCK );

		return new Liquibase(
			filename,
			new ClassLoaderResourceAccessor(getClass().getClassLoader()),
			liquibaseConnection
		);
	}

	protected void createDatabase() throws ManagedProcessException, SQLException {
		db.createDB( OPENMRS );

		connection = DriverManager.getConnection( configurationBuilder.getURL(OPENMRS), "root", "");
		connection.setAutoCommit(true);
	}

	protected void updateDatabase( String filename ) throws SQLException, LiquibaseException {
		Liquibase liquibase = getLiquibase( filename );
		liquibase.update( new Contexts( CONTEXT ) );
		connection.commit();
	}

	protected boolean dropDatabase() throws SQLException {
		PreparedStatement stmt = connection.prepareStatement( DROP_DATABASE_OPENMRS );
		return stmt.execute();
	}

	protected DBConfigurationBuilder getConfigurationBuilder() {
		return configurationBuilder;
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
}
