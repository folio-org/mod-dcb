CREATE TYPE StatusEnum as ENUM ('CREATED', 'OPEN', 'CANCELLED', 'IN_TRANSIT', 'AWAITING_PICKUP', 'ITEM_CHECKED_OUT', 'ITEM_CHECKED_IN', 'IN_TRANSIT_TO_LENDING', 'CLOSED', 'ERROR');
CREATE CAST (character varying as StatusEnum) WITH INOUT AS IMPLICIT;

CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY,

    item_id                UUID    NOT NULL,
    item_title             VARCHAR NOT NULL,
    item_barcode           VARCHAR NOT NULL,
    pickup_location        VARCHAR NOT NULL,
    material_type          VARCHAR NOT NULL,
    lending_library_code   VARCHAR NOT NULL,

    patron_id              UUID    NOT NULL,
    patron_group           VARCHAR NOT NULL,
    patron_barcode         VARCHAR NOT NULL,
    borrowing_library_code VARCHAR NOT NULL,

    status                 StatusEnum
);
