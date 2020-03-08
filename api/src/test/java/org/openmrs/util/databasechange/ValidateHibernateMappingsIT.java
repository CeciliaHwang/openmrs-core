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
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.Entity;
import liquibase.exception.LiquibaseException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openmrs.util.MariaDbTest;
import org.openmrs.liquibase.ChangeLogVersionFinder;
import org.openmrs.util.OpenmrsClassScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates Hibernate mapping files.
 */
public class ValidateHibernateMappingsIT extends MariaDbTest {
	private static final Logger log = LoggerFactory.getLogger(ValidateHibernateMappingsIT.class);

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void shouldValidateHibernateMappings() throws SQLException, LiquibaseException, ManagedProcessException, IOException {
		ChangeLogVersionFinder changeLogVersionFinder = new ChangeLogVersionFinder();
		Map<String, List<String>> changesetCombinations = changeLogVersionFinder.getChangeLogCombinations();

		// test all possible combinations of liquibase snapshot and update files
		//
		for ( List<String> snapshotAndUpdateFileNames: changesetCombinations.values() ) {

			this.createDatabase();

			log.info(
				"liquibase files used for creating and updating the OpenMRS database are: " + snapshotAndUpdateFileNames
			);

			for ( String fileName: snapshotAndUpdateFileNames ) {
				// process the core data file only for the first generation of liquibase snapshot files
				//
				if ( fileName.contains( "1.9.x/liquibase-core-data.xml" )) {
					log.info( "processing " + fileName );
					this.updateDatabase( fileName );
				}

				// exclude the core data file for subsequent generations of liquibase snapshot files
				//
				if ( !fileName.contains( "liquibase-core-data.xml" ) ) {
					log.info( "processing " + fileName );
					this.updateDatabase( fileName );
				}
			}

			// this is the core of this test: building the session factory validates if the generated database schema 
			// corresponds to Hibernate mappings
			//
			this.buildSessionFactory();

			this.dropDatabase();
		}
	}

	private SessionFactory buildSessionFactory() {
		Configuration configuration = new Configuration().configure();
		Set<Class<?>> entityClasses = OpenmrsClassScanner.getInstance().getClassesWithAnnotation( Entity.class);
		for (Class<?> clazz : entityClasses) {
			configuration.addAnnotatedClass(clazz);
		}
		configuration.setProperty( Environment.DIALECT, MySQL5LessStrictDialect.class.getName());
		configuration.setProperty(Environment.URL, getConfigurationBuilder().getURL(OPENMRS));
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
