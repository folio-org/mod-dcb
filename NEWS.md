## v2.0.0 2026-04-17

### Breaking changes
* Upgrade to Spring Boot 4.0 ([MODDCB-241](https://folio-org.atlassian.net/browse/MODDCB-241))
* Remove `locationCode` from `TransactionMapper` ([MODDCB-270](https://folio-org.atlassian.net/browse/MODDCB-270))

### New APIs versions
* Provides `transactions v1.6` (was `v1.1`)
* Provides `ecs-request-transactions v1.2` (was `v1.0`)
* Provides `dcb_refresh_shadow_locations v2.0` (new)
* Provides `dcb.settings v1.0` (new)

### Features
* Allow patrons to request items from own library via DCB (selfBorrowing with BORROWING_PICKUP role) ([MODDCB-196](https://folio-org.atlassian.net/browse/MODDCB-196))
* Support for Open - Awaiting delivery request status ([MODDCB-197](https://folio-org.atlassian.net/browse/MODDCB-197))
* Populate effective location id for circulation item ([MODDCB-207](https://folio-org.atlassian.net/browse/MODDCB-207))
* Load shadow locations from DCB-Hub ([MODDCB-214](https://folio-org.atlassian.net/browse/MODDCB-214))
* Implement shadow location refresh operation ([MODDCB-222](https://folio-org.atlassian.net/browse/MODDCB-222))
* Generate runtime umbrella entities ([MODDCB-226](https://folio-org.atlassian.net/browse/MODDCB-226))
* Implement location search by lendingLibraryCode ([MODDCB-233](https://folio-org.atlassian.net/browse/MODDCB-233))
* Update POST API for DCB transaction creation to support localNames field ([MODDCB-234](https://folio-org.atlassian.net/browse/MODDCB-234))
* Add holdCount field to transaction status response ([MODDCB-235](https://folio-org.atlassian.net/browse/MODDCB-235))
* Provide endpoints to toggle borrower role renewals on virtual items ([MODDCB-236](https://folio-org.atlassian.net/browse/MODDCB-236))
* Add handling of expired lender requests ([MODDCB-242](https://folio-org.atlassian.net/browse/MODDCB-242))
* Add request body for /dcb/shadow-locations/refresh ([MODDCB-251](https://folio-org.atlassian.net/browse/MODDCB-251))
* Update existing virtual/DCB patron records with localNames ([MODDCB-252](https://folio-org.atlassian.net/browse/MODDCB-252))
* Implement DCB transaction expiry and Settings API ([MODDCB-257](https://folio-org.atlassian.net/browse/MODDCB-257))
* Allow new transaction creation when EXPIRED transaction exists for same item ([MODDCB-271](https://folio-org.atlassian.net/browse/MODDCB-271))

### Bug fixes
* Remove sensitive data from logs ([MODDCB-175](https://folio-org.atlassian.net/browse/MODDCB-175))
* Support items without barcodes ([MODDCB-188](https://folio-org.atlassian.net/browse/MODDCB-188))
* Fix extension creation ([MODDCB-194](https://folio-org.atlassian.net/browse/MODDCB-194))
* Fix Liquibase checksum issue for create-service-point-expiration-period-table ([MODDCB-199](https://folio-org.atlassian.net/browse/MODDCB-199))
* Populate locationCode for DCB Transaction API ([MODDCB-206](https://folio-org.atlassian.net/browse/MODDCB-206))
* Create Umbrella DCB Inventory Holding if missing before using it ([MODDCB-187](https://folio-org.atlassian.net/browse/MODDCB-187))
* Fix 403 from /circulation/loans during transaction status check ([MODDCB-219](https://folio-org.atlassian.net/browse/MODDCB-219))
* Add missing permission for DCB transaction status API ([MODDCB-223](https://folio-org.atlassian.net/browse/MODDCB-223))
* Fix permissions for cancellation reason client ([MODDCB-226](https://folio-org.atlassian.net/browse/MODDCB-226))
* Fix inventory API call for location and location-units ([MODDCB-240](https://folio-org.atlassian.net/browse/MODDCB-240))
* Fix tenant initialization with enabled system user ([MODDCB-241](https://folio-org.atlassian.net/browse/MODDCB-241))
* Fix close LENDER DCB transaction on check-in regardless of item status when holds exist ([MODDCB-267](https://folio-org.atlassian.net/browse/MODDCB-267))
* Fix DCB circulation item effective location not updated on re-request with shadow location ([MODDCB-270](https://folio-org.atlassian.net/browse/MODDCB-270))

### Tech Debt
* Add codeowners, dependabot configuration, and PR template ([MODDCB-203](https://folio-org.atlassian.net/browse/MODDCB-203))
* Fix CQL query for querying patron group by name ([MODDCB-208](https://folio-org.atlassian.net/browse/MODDCB-208))
* Item Storage API version update from 10.1 to 11.0 ([MODDCB-216](https://folio-org.atlassian.net/browse/MODDCB-216))
* Use GitHub Workflows for Maven ([MODDCB-259](https://folio-org.atlassian.net/browse/MODDCB-259))
* Add JaCoCo plugin for code coverage reporting

### Dependencies
* Bump `spring-boot` from `3.4.3` to `4.0.5`
* Bump `folio-spring-support` from `9.0.0` to `10.0.0`
* Bump `folio-util` from `35.4.0` to `36.0.0`
* Bump `kafka-clients` from `3.9.0` to `4.1.1`
* Bump `wiremock` from `3.2.0` to `3.13.2`
* Bump `rest-assured` from `5.5.1` to `6.0.0`
* Bump `testcontainers` from `1.20.2` to `2.0.4`
* Bump `openapi-generator-maven-plugin` from `7.12.0` to `7.21.0`

## v1.3.0 2025-03-13

* MODDCB-124: Feature toggle
* MODDCB-143: Missing interface dependencies in module descriptor
* MODDCB-145: Issue with spaces in service point name
* MODDCB-152: Support for intermediate requests
* UXPROD-5001: DCB re-request
* UXPROD-5054: Support DCB request on unavailable item
* MODDCB-159: Url encoding fix for barcode
* UXPROD-5090: Update DCB service point values for the attribute Hold shelf expiration period
* UXPROD-5116: Support DCB virtual item renewal in FOLIO

## v1.2.0 2024-10-30

* MODDCB-98: Implement GET API for transaction updates
* MODDCB-102: Code changes for Check-out event improvement
* MODDCB-106: API version update of mod inventory storage
* MODDCB-107: Source id is not setting up properly while creating holdings in DCB
* MODDCB-108: Creating a holding source in mod-dcb
* MODDCB-109: Add missed permissions in mod-dcb
* MODDCB-112: Kafka TLS configuration is not present
* MODDCB-114: Supplier side: Unable to produce service point pickup location in pick lists
* MODDCB-118: Add missing holding source permission
* MODDCB-119: Add DCB calendar and assignment of SPs to it
* MODDCB-121: SYSTEM_USER env var is not taking affect
* MODDCB-129: Upgrade "holdings-storage" to 8.0
* MODDCB-130: DCB Patron barcode issue
* MODDCB-133: inventory API version update from 13.3 to 14.0
* MODDCB-134: Use new permissions instead source-storage.records.get
* MODDCB-136: Increase memory for DCB
* MODDCB-137: Update Spring support version for Ramsons
* MODDCB-140: Update pom.xml and interface dependencies for Ramsons

## v1.1.5 2024-10-03

* MODDCB-114: Supplier side: Unable to produce service point pickup location in pick lists
* MODDCB-119: Add DCB calendar and assignment of SPs to it
* MODDCB-130: DCB Patron barcode issue

## v1.1.4 2024-10-02

* MODDCB-114: Supplier side: Unable to produce service point pickup location in pick lists
* MODDCB-119: Add DCB calendar and assignment of SPs to it
* MODDCB-130: DCB Patron barcode issue

## v1.1.3 2024-10-02

* MODDCB-114: Supplier side: Unable to produce service point pickup location in pick lists
* MODDCB-119: Add DCB calendar and assignment of SPs to it

## v1.1.2 2024-08-07

* MODDCB-112: Kafka TLS configuration is not present
* MODDCB-109: Add missed permissions in mod-dcb

## v1.1.1 2024-07-17

* MODDCB-112: Kafka TLS configuration is not present


## v1.1.0 2024-03-20

* MODDCB-81: Optional DCBTransaction fields should be nullable
* MODDCB-79: TC module submission include: ${ACTUATOR_EXPOSURE:health,info,loggers}
* MODDCB-81: Optional DCBTransaction fields should be nullable
* MODDCB-77:  Added PERSONAL_DATA_DISCLOSURE.md file for TC
* MODDCB-80: Fix transaction audit entity listener and virtual item lookup
* MODDCB-75: Persist error action and error message in the transaction audit table.
* MODDCB-78: update the permission name related to circulation-item and change the provider name to match with path param
* MODDCB-86: Handling invalid barcode issue in Lender role
* MODDCB-92: Upgrading dependencies for quesnelia
* MODDCB-97: Adding missing permission in module descriptor
