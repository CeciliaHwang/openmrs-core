# Creating Liquibase snapshots for OpenMRS
This document explains why Liquibase snapshots are introduced to OpenMRS and how versioning of database changes is 
affected by snapshots. It also contains instructions for creating new Liquibase snapshots, running them from the 
console and adding them to the OpenMRS code base.

## Why Liquibase snapshots?
OpenMRS uses [Liquibase](http://www.liquibase.org/index.html) to evolve its database model. The respective change sets 
have grown in the course of time and contain unneeded changes. E.g. there are tables or columns that are created, 
modified and eventually deleted as they are no longer used.

Large and partly outdated Liquibase change sets affect OpenMRS in two ways:

* Unneeded changes slow down the initialisation of OpenMRS
* Liquibase change sets are hard to read and understand as they contain unused code.

An alternative model is based on snapshots where historic change sets are consolidated into smaller change sets.

## Versioning of database changes with snapshots
This section compares Liquibase files before and after the introduction of Liquibase snapshots.

### Change sets in [OpenMRS 2.1.x](https://github.com/openmrs/openmrs-core/tree/2.1.x/api/src/main/resources)
OpenMRS 2.1.x is the **last version that contains the full history** of database changes. The respective Liquibase 
files are:

* `liquibase-snapshots/1.9.x/liquibase-schema-only.xml` defines the OpenMRS schema

* `liquibase-snapshots/1.9.x/liquibase-core-data.xml` defines core data

* `liquibase-updates/2.0.x/liquibase-update-to-latest.xml` contains all database changes introduced **until** OpenMRS 2.0, 
some changes go back to 2009

* `liquibase-updates/2.1.x/liquibase-update-to-latest.xml` contains all database changes introduced **since** OpenMRS 2.0

### Change sets in OpenMRS 2.2.x
OpenMRS 2.2.x is the **first version using Liquibase snapshots**. Please note that this version did not exist at the 
time of writing this document.

* `liquibase-snapshots/2.1.x/liquibase-schema-only.xml` defines the OpenMRS schema. This file is a **snapshot** generated from OpenMRS 2.1.x.

* `liquibase-snapshots/2.1.x/liquibase-core-data.xml` defines core data. Again, this file is a **snapshot** generated from OpenMRS 2.1.x.

* `liquibase-updates/2.2.x/liquibase-update-to-latest.xml` contains database changes introduced by OpenMRS 2.2.x

### Change sets in (hypothetic) OpenMRS 2.7.x
Looking forward to a (hypothetic) version 2.7.x of OpenMRS, the respective change sets are:

* `liquibase-snapshots/2.6.x/liquibase-schema-only.xml` defines the OpenMRS schema. This file is a **snapshot** generated from OpenMRS 2.6.x.

* `liquibase-snapshots/2.6.x/liquibase-core-data.xml` defines core data. Again, this file is a **snapshot** generated from OpenMRS 2.6.x.

* `liquibase-snapshots/2.7.x/liquibase-update-to-latest.xml` contains database changes introduced by OpenMRS 2.7.x

### Further Liquibase files 
* `liquibase-update-to-latest.xml` is used by integration tests and includes multiple `liquibase-updates/*/liquibase-update-to-latest.xml` files.
  
* `liquibase-empty-changelog.xml` is used as a default Liquibase file by the org.openmrs.util.DatabaseUpdater class.

## When to generate Liquibase shapshots
Liquibase snapshots need to be created...

1. when a **new minor or major version** of OpenMRS is created (such as 2.3.x or 3.0.x), new snapshot files need to be 
generated for the **previous** versions. The new snapshot files are added to a new subfolder 
`liquibase-snapshots/<previous version>/` in the OpenMRS **master branch**. Liquibase updates introduced with the new 
version are added to `liquibase-updates/<new version>/liquibase-update-to-latest.xml`. The examples for the (hypothetic)
OpemMRS version 2.7.x further above illustrates the different version numbers to use for the new folders.

2. when a **database change is added to an existing minor or major version**, the snapshot files of later versions 
need to be updated so that they include the change.

## Generating and applying Liquibase snapshots
The pom file of the openmrs-liquibase module contains a template for generating Liquibase snapshots from an existing database and applying snapshots to an OpenMRS database.

### How to generate Liquibase snapshots
#### Step 1 - Drop your local OpenMRS schema
E.g. by running this script:

	openmrs-core/liquibase/scripts/drop_openmrs_schema.sql
	
Take care **not** to run this script on a production database.

#### Step 2 - Build and initialise OpenMRS
	cd <some root folder>/openmrs-core
	mvn clean install
	cd webapp
	rm -r ~/.OpenMRS
	mvn jetty:run

Open [http://localhost:8080/openmrs/initialsetup](http://localhost:8080/openmrs/initialsetup) and choose the following options:

* **simple installation** in step 2 of the installation wizard

* **not to add demo data** in step 3 of the installation wizard

#### Step 3 - Create a snapshot of the OpenMRS schema
	cd <some root folder>/openmrs-core/liquibase
	mvn \
	  -DoutputChangelogfile=liquibase-schema-only-SNAPSHOT.xml \
	  -Dusername=<database user> \
	  -Dpassword=<database password> \
	  liquibase:generateChangeLog
	
The file `liquibase-schema-only-SNAPSHOT.xml` is created in the folder `<some root folder>/openmrs-core/api/src/main/resources`

#### Step 4 - Double check generated data types
Double check the following data types in the `liquibase-schema-only-SNAPSHOT.xml` file and correct them if needed.

The attribute **`value`** of the table **`clob_datatype_storage `** must be of type `CLOB` (and **not** `LONGTEXT`):

    <changeSet ...>
        <createTable tableName="clob_datatype_storage">
            ...
            <column name="value" type="CLOB">
            ...
        </createTable>
    </changeSet>

The attribute **`sort_weight`** of the table **`form_field`** must be of type `FLOAT` or `FLOAT(12)` (and **not** `DOUBLE`):

    <changeSet ...>
        <createTable tableName="form_field">
            ...
            <column name="sort_weight" type="FLOAT"/>
            ...
        </createTable>
    </changeSet>

For reasons of consistency, the attribute **`merged_data`** of the table **`person_merge_log `** should be of type `TEXT` (and **not** `LONGTEXT`):

    <changeSet ...>
        <createTable tableName="person_merge_log">
            ...
            <column name="value" type="TEXT">
            ...
        </createTable>
    </changeSet>

#### Step 5 - Create a snapshot of the OpenMRS core data
	cd <some root folder>/openmrs-core/liquibase
	mvn \
	  -DdiffTypes=data \
	  -DoutputChangelogfile=liquibase-core-data-SNAPSHOT.xml \
	  -Dusername=<database user> \
	  -Dpassword=<database password> \
	  liquibase:generateChangeLog
	
The file `liquibase-core-data-SNAPSHOT.xml` is also created in the folder `<some root folder>/openmrs-core/api/src/main/resources`

#### Step 6 - Remove references to liquibase tables
In both files, search for `liquibasechangelog` and `liquibasechangeloglock` and remove the respective change sets.

#### Step 7 - Add the OpenMRS license header to new files
The header can be copied from an existing file.

#### Step 8 - Change the order of change sets
Change the order of change sets in `liquibase-core-data-SNAPSHOT.xml` as follows:

1. `<databaseChangeLog ... \>`
2. ... `<insert tableName="person">` ...
3. ... `<insert tableName="users">` ...
4. ... `<insert tableName="care_setting">` ...
5. ... `<insert tableName="concept_class">` ...
6. ... `<insert tableName="concept_datatype">` ...
7. ... `<insert tableName="concept">` ...
8. ... followed by all other change sets as generated by liquibase


### How to test Liquibase snapshots
#### Step 1 - Drop your local OpenMRS schema
E.g. by running this script:

	openmrs-core/liquibase/scripts/drop_openmrs_schema.sql
	
Again, take care **not** to run this script on a production database.

#### Step 2 - Create an empty OpenMRS database
E.g. by running this script:

	openmrs-core/liquibase/scripts/create_openmrs_database.sql
	
#### Step 3 - Use the snapshots to update the OpenMRS database 
Execute

	cd <some root folder>/openmrs-core/liquibase
	mvn \
	  -Dchangelogfile=liquibase-schema-only-SNAPSHOT.xml  \
	  -Dusername=<database user> \
	  -Dpassword=<database password> \
	  liquibase:update

and 

	cd <some root folder>/openmrs-core/liquibase
	mvn \
	  -Dchangelogfile=liquibase-core-data-SNAPSHOT.xml  \
	  -Dusername=<database user> \
	  -Dpassword=<database password> \
	  liquibase:update

### How to add snapshots to OpenMRS master
The new snapshots files are now ready to be added to the OpenMRS master branch. 

#### Step 1 - Add the new snapshot files 
Add a new subfolder to the folder `resources/liquibase-snapshots/` in the master branch as outlined further above. 

For example, when generating snapshots from OpenMRS 2.6.x, the new subfolder is `resources/liquibase-snapshots/2.6.x/`. 

Copy the snapshot files into the new subfolder and rename the two files:

* `liquibase-schema-only-SNAPSHOT.xml` becomes `liquibase-schema-only.xml`
* `liquibase-core-data-SNAPSHOT.xml` becomes `liquibase-core-data.xml` 
	
#### Step 2 - Create a new liquibase update file
Add a new subfolder to the`resources/liquibase-updates/` folder in the master branch as outlined further above. 

For example, when generating snapshots from OpenMRS 2.6.x, the new subfolder is `resources/liquibase-updates/2.7.x`. 

The minor version number of the new subfolder is increased by one as this folder contains all liquibase change sets that are introduced 

* *after* OpenMRS version 2.6 was created 
* and *before* OpenMRS version 2.7 will be created

In the new subfolder, create a liquibase file called `liquibase-update-to-latest.xml`. You can use `resources/liquibase-empty-changelog.xml` as a template for creating the new file.

Include the new file in `resources/liquibase-update-to-latest.xml`, it is used by integration tests (as mentioned above).

#### Step 3 - Validate Hibernate mappings

Run `org.openmrs.util.databasechange.ValidateHibernateMappingsIT` to check whether the data types in the new liquibase files are compatible with the data types specified in the Hibernate mappings. 

The test can be run in two ways:

* By running `mvn clean test -Pskip-default-test -Pintegration-test` in the console
* Alternatively, by running the test in IntelliJ or another IDE 

#### Step 4 - Build and initialise OpenMRS with the new snapshot and update files
Drop your local OpenMRS database and build and initialise OpenMRS as described in the section "How to generate Liquibase snapshots".

## References
[https://issues.openmrs.org/browse/TRUNK-4830](https://issues.openmrs.org/browse/TRUNK-4830)

[http://www.liquibase.org/documentation/maven](http://www.liquibase.org/documentation/maven)

[https://dev.mysql.com/doc/refman/8.0/en/mysql-batch-commands.html](https://dev.mysql.com/doc/refman/8.0/en/mysql-batch-commands.html)
