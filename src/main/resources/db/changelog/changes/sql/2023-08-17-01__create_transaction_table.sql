create table transactions
(
    transaction_id         varchar not null primary key,

    item_uuid              uuid    not null,
    item_title             varchar not null,
    item_barcode           varchar not null,
    pickup_location        varchar not null,
    material_type          varchar not null,
    lending_library_code   varchar not null,

    patron_uuid            uuid    not null,
    patron_group           varchar not null,
    patron_barcode         varchar not null,
    borrowing_library_code varchar not null,

    status                 varchar not null
);
