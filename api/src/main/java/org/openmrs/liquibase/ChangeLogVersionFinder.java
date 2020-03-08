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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.openmrs.module.VersionComparator;

/**
 * Provides information about available Liquibase snapshot and update change logs.
 */
public class ChangeLogVersionFinder {

	private static final String SNAPSHOTS_FOLDER_NAME = "liquibase-snapshots";
	private static final String UPDATES_FOLDER_NAME = "liquibase-updates";
	
	private static final String SNAPSHOTS_CORE_DATA_FILENAME = "liquibase-core-data.xml";
	private static final String SNAPSHOTS_SCHEMA_ONLY_FILENAME = "liquibase-schema-only.xml";
	private static final String UPDATES_FILENAME = "liquibase-update-to-latest.xml";

	private static final String LOWER_CASE_X = "x";

	private ChangeLogVersions changeLogVersions;

	/**
	 * The default constructor initialises the default provider of change log versions.
	 */
	public ChangeLogVersionFinder() {
		this.changeLogVersions = new ChangeLogVersions();
	}

	/**
	 * Allows to inject a mock provider of change log versions for unit testing.
	 * 
	 * @param changeLogVersions a provider of change log versions.
	 */
	public ChangeLogVersionFinder( ChangeLogVersions changeLogVersions ) {
		this.changeLogVersions = changeLogVersions;
	}

	public Map<String, List<String>> getChangeLogCombinations() {
		Map<String, List<String>> changeLogCombinations = new HashMap<>();

		for ( String snapshotVersion : getSnapshotVersions() ) {
			List<String> changeLogFilenames = new ArrayList<>();

			changeLogFilenames.addAll( getSnapshotFilenames( snapshotVersion ) );

			changeLogFilenames.addAll( getUpdateFileNames(
				getUpdateVersionsGreaterThan( snapshotVersion )
			) );

			changeLogCombinations.put( snapshotVersion, changeLogFilenames );
		}

		return changeLogCombinations;
	}

	public Map<String, List<String>> getSnapshotCombinations() {
		Map<String, List<String>> changeLogCombinations = new HashMap<>();

		for ( String snapshotVersion : getSnapshotVersions() ) {
			List<String> changeLogFilenames = new ArrayList<>();

			changeLogFilenames.addAll( getSnapshotFilenames( snapshotVersion ) );

			changeLogCombinations.put( snapshotVersion, changeLogFilenames );
		}

		return changeLogCombinations;
	}

	public List<String> getSnapshotFilenames( String version ) {
		String versionAsDotX = getVersionAsDotX( version );
		return Arrays.asList(
			SNAPSHOTS_FOLDER_NAME + File.separator + versionAsDotX + File.separator + SNAPSHOTS_SCHEMA_ONLY_FILENAME,
			SNAPSHOTS_FOLDER_NAME + File.separator + versionAsDotX + File.separator + SNAPSHOTS_CORE_DATA_FILENAME
		);
	}

	public Optional<String> getLatestSnapshotVersion() {
		return getSnapshotVersions()
			.stream()
			.max( new VersionComparator() );
	}

	public Optional<String> getLatestSchemaSnapshotFilename() {
		Optional<String> snapshotVersion = getLatestSnapshotVersion();
		if ( snapshotVersion.isPresent() ) {
			return Optional.of(
				SNAPSHOTS_FOLDER_NAME + File.separator + snapshotVersion.get() + File.separator + SNAPSHOTS_SCHEMA_ONLY_FILENAME
			);
		}
		return Optional.empty();
	}

	public Optional<String> getLatestCoreDataSnapshotFilename() {
		Optional<String> snapshotVersion = getLatestSnapshotVersion();
		if ( snapshotVersion.isPresent() ) {
			return Optional.of(
				SNAPSHOTS_FOLDER_NAME + File.separator + snapshotVersion.get() + File.separator + SNAPSHOTS_CORE_DATA_FILENAME
			);
		}
		return Optional.empty();
	}

	// TODO TRUNK-4830 when is 'equal to or greater than' actually needed ?
	//
	public List<String> getUpdateVersionsEqualToOrGreaterThan( String otherVersion ) {
		String shortestVersion = getVersionAsDotX( otherVersion );
		List<String> result = new ArrayList<>();

		if ( ! getUpdateVersions().contains( shortestVersion )) {
			throw new IllegalArgumentException(
				String.format("liquibase update version '%s' does not exist", shortestVersion )
			);
		}

		result.add( shortestVersion );
		result.addAll( getUpdateVersionsGreaterThan( shortestVersion ) );
		return result;
	}

	public List<String> getUpdateVersionsGreaterThan( String otherVersion ) {
		String versionAsDotX = getVersionAsDotX( otherVersion );
		VersionComparator versionComparator = new VersionComparator();

		return getUpdateVersions()
			.stream()
			.filter( updateVersion -> versionComparator.compare( updateVersion, versionAsDotX ) > 0 )
			.sorted( versionComparator )
			.collect( Collectors.toList() );
	}

	public List<String> getUpdateFileNames( List<String> versions ) {
		return versions
			.stream()
			.map( version -> UPDATES_FOLDER_NAME + File.separator + version + File.separator + UPDATES_FILENAME )
			.collect( Collectors.toList());
	}

	List<String> getSnapshotVersions() {
		return changeLogVersions.getSnapshotVersions();
	}

	List<String> getUpdateVersions() {
		return changeLogVersions.getUpdateVersions();
	}

	String getVersionAsDotX( String version ) {
		Matcher matcher = Pattern
			.compile("(\\d+\\.\\d+\\.)")
			.matcher(version);

		if ( matcher.find()) {
			return matcher.group( 1 ) + LOWER_CASE_X;
		}
		throw new IllegalArgumentException(
			String.format("version string '%s' does not match 'major.minor.' pattern", version )
		);
	}
}
