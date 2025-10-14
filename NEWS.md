## 2.0.0-SNAPSHOT 2025-XX-XX
* MODDCB-196: Allow patrons to request items from own library via DCB
* MODDCB-208: Fix cql query for querying patron group by name [MODDCB-208](https://folio-org.atlassian.net/browse/MODDCB-208)
* MODDCB-208: Bump Spring Boot from 3.4.3 to 3.5.0 and update other dependencies
* MODDCB-206: Populate locationCode for DCB Transaction API
* MODDCB-216: Item Storage API version update from 10.1 to 11.0 [MODDCB-216](https://folio-org.atlassian.net/browse/MODDCB-216)
* MODDCB-187: Create Umbrella DCB Inventory Holding if missing before using it [MODDCB-187](https://folio-org.atlassian.net/browse/MODDCB-187)
* MODDCB-219: 403 from /circulation/loans during transaction status check [MODDCB-219](https://folio-org.atlassian.net/browse/MODDCB-219)
* MODDCB-223: Add missing permission for DCB transaction status API [MODDCB-223](https://folio-org.atlassian.net/browse/MODDCB-223)
* MODDCB-214: Load shadow locations from DCB-Hub [MODDCB-214](https://folio-org.atlassian.net/browse/MODDCB-214)
* MODDCB-207: Populate effective location id for circulation item [MODDCB-207](https://folio-org.atlassian.net/browse/MODDCB-207)
* MODDCB-222: Implement an ability to trigger the shadow location refresh operation [MODDCB-222](https://folio-org.atlassian.net/browse/MODDCB-222)
* MODDCB-234: Update POST API for DCB transaction creation to support localNames field [MODDCB-234](https://folio-org.atlassian.net/browse/MODDCB-234)

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
