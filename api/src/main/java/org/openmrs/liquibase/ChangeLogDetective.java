/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.liquibase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Figures out which Liquibase change logs were used to initialise an OpenMRS database and which change logs need
 * to be run on top of that when updating the database.
 */
public class ChangeLogDetective {
	private static final Logger log = LoggerFactory.getLogger( ChangeLogDetective.class);

	private ChangeLogVersionFinder changeLogVersionFinder;
	
	public ChangeLogDetective() {
		changeLogVersionFinder = new ChangeLogVersionFinder(  );
	}
	
	/**
	 * Returns the version of the Liquibase snapshot that had been used to initialise the OpenMRS database.
	 *
	 * The version is needed to determine which Liquibase update files need to be checked for un-run change sets
	 * and may need to be (re-)run to apply the latest changes to the OpenMRS database.
	 *
	 * @param liquibaseProvider provides access to a Liquibase instance
	 * @return the version of the Liquibase snapshot that had been used to initialise the OpenMRS database
	 * @throws Exception
	 */
	public String getInitialLiquibaseSnapshotVersion( 
		String context, 
		LiquibaseProvider liquibaseProvider 
	) throws Exception {
		Map<String, List<String>> snapshotCombinations = changeLogVersionFinder.getSnapshotCombinations();

		if ( snapshotCombinations.isEmpty() ) {
			throw new IllegalStateException(
				"identifying the snapshot version that had been used to initialize the OpenMRS database failed as no candidate change sets were found"
			);
		}

		for ( String version:  snapshotCombinations.keySet() ) {
			int unrunChangeSetsCount = 0;

			log.info( String.format( "looking for un-run change sets for snapshot version '%s'", version ) );
			List<String> changeSets = snapshotCombinations.get( version );

			for ( String filename: changeSets ) {
				Liquibase liquibase = liquibaseProvider.getLiquibase( filename );
				List<ChangeSet> unrunChangeSets = liquibase.listUnrunChangeSets( new Contexts( context ), new LabelExpression() );
				log.info( String.format( "file '%s' contains %d un-run change sets", filename, unrunChangeSets.size() ) );
				unrunChangeSetsCount += unrunChangeSets.size();
			}

			if ( unrunChangeSetsCount == 0 ) {
				return version;
			}
		}

		throw new IllegalStateException(
			"identifying the snapshot version that had been used to initialize the OpenMRS database failed as no candidate change set resulted in zero un-run changes"
		);
	}

	/**
	 * Returns a list of  Liquibase update files that contain un-run change sets.
	 *
	 * @param snapshotVersion the snapshot version that had been used to initialise the OpenMRS database
	 * @param liquibaseProvider provides access to a Liquibase instance
	 * @return a list of  Liquibase update files that contain un-run change sets.
	 * @throws Exception
	 */
	public List<String> getUnrunLiquibaseUpdateFileNames(
		String snapshotVersion,
		String context,
		LiquibaseProvider liquibaseProvider
	) throws Exception {
		List<String> unrunLiquibaseUpdates = new ArrayList<>();

		List<String> updateVersions = changeLogVersionFinder.getUpdateVersionsGreaterThan( snapshotVersion );
		List<String> updateFileNames = changeLogVersionFinder.getUpdateFileNames( updateVersions );

		for ( String filename: updateFileNames ) {
			Liquibase liquibase = liquibaseProvider.getLiquibase( filename );
			List<ChangeSet> unrunChangeSets = liquibase.listUnrunChangeSets( new Contexts( context ), new LabelExpression() );
			log.info( String.format( "file '%s' contains %d un-run change sets", filename, unrunChangeSets.size() ) );

			if ( unrunChangeSets.size() > 0 ) {
				unrunLiquibaseUpdates.add( filename );
			}
		}

		return unrunLiquibaseUpdates;
	}
}
