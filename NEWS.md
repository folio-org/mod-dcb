## v1.2.4 2024-12-12
* MODDCB-152: Support for intermediate requests

## v1.2.3 2024-12-02
* MODDCB-90: Accept existing circulation request ID
* MODDCB-105: Accept existing circulation request ID (borrowing transaction)
* MODDCB-111: Allow manual transaction status change from CREATED to OPEN
* MODDCB-117: Add ecsRequestPhase to the circulation request schema
* MODDCB-124: Merge esc-tlr feature branch into master

## v1.2.2 2024-11-20
* MODDCB-145: Issue with spaces in service point name

## v1.2.1 2024-11-13
* MODDCB-143 Adding missed interface dependencies in module descriptor

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
