create table public.bom_calc_result (
  calc_id character varying(36) not null
  , item_id character varying(20) not null
  , top_parent_id character varying(20) not null
  , gross_qty numeric(38, 3) default 0.000 not null
  , available_stock numeric(38, 3) default 0.000 not null
  , net_qty numeric(38, 3) default 0.000 not null
  , created_at timestamp(6) without time zone default CURRENT_TIMESTAMP not null
  , primary key (calc_id, item_id)
);
create table public.bom_master (
  parent_id character varying(20) not null
  , child_id character varying(20) not null
  , quantity numeric(10, 3) not null
  , primary key (parent_id, child_id)
);
create table public.bom_structure (
  parent_item_id character varying(20) not null
  , child_item_id character varying(20) not null
  , quantity numeric(10, 2) not null
  , lead_time integer not null
  , primary key (parent_item_id, child_item_id)
);
create table public.item_master (
  item_id character varying(20) not null
  , item_name character varying(100) not null
  , item_type integer not null
  , unit character varying(10)
  , primary key (item_id)
);
create table public.stock_master (
  item_id character varying(20) not null
  , stock_quantity numeric(18, 3) not null
  , defective_quantity numeric(18, 3) default 0.000 not null
  , hold_quantity numeric(18, 3) default 0.000 not null
  , primary key (item_id)
);
