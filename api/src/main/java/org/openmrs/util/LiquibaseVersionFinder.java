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

import org.openmrs.module.VersionComparator;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class LiquibaseVersionFinder {

	private static final String LIQUIBASE_SNAPSHOTS_FOLDER = "liquibase-snapshots";
	private static final String LIQUIBASE_CORE_DATA_FILENAME = "liquibase-core-data.xml";
	private static final String LIQUIBASE_SCHEMA_FILENAME = "liquibase-schema-only.xml";

	private static final String LIQUIBASE_UPDATE_FOLDER = "liquibase-updates";
	private static final String LIQUIBASE_UPDATE_FILENAME = "liquibase-update-to-latest.xml";

	private File snapshotFolder;
	private File updateFolder;
	
	public LiquibaseVersionFinder() {
		snapshotFolder = new File(
			this.getClass()
				.getClassLoader()
				.getResource( LIQUIBASE_SNAPSHOTS_FOLDER )
				.getFile()
		);

		updateFolder = new File(
			this.getClass()
				.getClassLoader()
				.getResource( LIQUIBASE_UPDATE_FOLDER )
				.getFile()
		);
	}

	public Set<List<String>> getLiquibaseChangesetCombinations() {
		Set<List<String>> changesetCombinations = new HashSet<>();

		for ( String snapshotVersion : getLiquibaseSnapshotVersions() ) {
			List<String> changesetFilenames = new ArrayList<>();
			
			changesetFilenames.addAll( getLiquibaseSnapshotFilenames( snapshotVersion ) );
			changesetFilenames.addAll( getApplicableLiquibaseUpdateFileNames( snapshotVersion ) );
			
			changesetCombinations.add( changesetFilenames );
		}
		
		return changesetCombinations;
	}

	public Optional<String> getLatestLiquibaseSchemaSnapshotFilename() {
		Optional<String> snapshotVersion = getLatestLiquibaseSnapshotVersion();
		if ( snapshotVersion.isPresent() ) {
			return Optional.of( 
				LIQUIBASE_SNAPSHOTS_FOLDER + File.separator + snapshotVersion.get() + File.separator + LIQUIBASE_SCHEMA_FILENAME 
			);
		}
		return Optional.empty();
	}

	public Optional<String> getLatestLiquibaseCoreDataSnapshotFilename() {
		Optional<String> snapshotVersion = getLatestLiquibaseSnapshotVersion();
		if ( snapshotVersion.isPresent() ) {
			return Optional.of(
				LIQUIBASE_SNAPSHOTS_FOLDER + File.separator + snapshotVersion.get() + File.separator + LIQUIBASE_CORE_DATA_FILENAME 
			);
		}
		return Optional.empty();
	}

	public List<String> getApplicableLiquibaseUpdateFileNames( String currentVersion ) {
		return getApplicableLiquibaseUpdateVersions( currentVersion )
			.stream()
			.map( version -> LIQUIBASE_UPDATE_FOLDER + File.separator + version + File.separator + LIQUIBASE_UPDATE_FILENAME )
			.collect( Collectors.toList());
	}

	public Optional<String> getLatestLiquibaseSnapshotVersion() {
		return getLiquibaseSnapshotVersions()
			.stream()
			.max( new VersionComparator() );
	}
	
	List<String> getApplicableLiquibaseUpdateVersions( String currentVersion) {
		if ( currentVersion.isEmpty() ) {
			throw new IllegalArgumentException( "current version must not be empty" );
		}
		
		VersionComparator versionComparator = new VersionComparator();
		
		return getLiquibaseUpdateVersions()
			.stream()
			.filter( updateVersion -> versionComparator.compare( updateVersion, currentVersion ) > 0 )
			.sorted( versionComparator )
			.collect( Collectors.toList() );
	}

	public List<String> getLiquibaseSnapshotFilenames( String version ) {
		return Arrays.asList(
			LIQUIBASE_SNAPSHOTS_FOLDER + File.separator + version + File.separator + LIQUIBASE_SCHEMA_FILENAME,
			LIQUIBASE_SNAPSHOTS_FOLDER + File.separator + version + File.separator + LIQUIBASE_CORE_DATA_FILENAME
		);
	}

	Set<String> getLiquibaseSnapshotVersions() {
		return getSubfolderNames( snapshotFolder );
	}

	Set<String> getLiquibaseUpdateVersions() {
		return getSubfolderNames( updateFolder );
	}

	Set<String> getSubfolderNames( File folder ) {
		if ( folder.listFiles() != null ) {
			return Arrays.asList( folder.listFiles() )
				.stream()
				.filter( file -> file.isDirectory() )
				.map( file -> file.getName() )
				.collect( Collectors.toSet() );
		}
		return new HashSet<>(  );
	}

	// The setter allows to inject mock folders in unit tests.
	//
	void setSnapshotFolder( File snapshotFolder ) {
		this.snapshotFolder = snapshotFolder;
	}

	// The setter allows to inject mock folders in unit tests.
	//
	void setUpdateFolder( File updateFolder ) {
		this.updateFolder = updateFolder;
	}
}
