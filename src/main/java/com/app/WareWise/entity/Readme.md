# WareWise: ERD Documentation

1. [Overview](#1-overview)
2. [Core tables](#2-core-tables)
3. [Buying side](#3-buying-side)
4. [Selling side](#4-selling-side)
5. [Feature tables](#5-feature-tables)
6. [Enums reference](#6-enums-reference)
7. [Example 1: Grocery store](#7-example-1-grocery-store-freshmart)
8. [Example 2: Medical store](#8-example-2-medical-store-carewell-pharmacy)
9. [Rules to follow in the Spring Boot code](#9-rules-to-follow-in-the-spring-boot-code)
10. [Notes on this DBML version](#10-notes-on-this-dbml-version)

---

## 1. Overview

The database has **16 tables** in 4 groups:

| Group | Tables | What it answers |
| --- | --- | --- |
| **Core** | `users`, `warehouse`, `category`, `product`, `non_perishable_stock`, `perishable_product_batch`, `stock_movement` | What do we sell, where is it, and how much is left? |
| **Buying side** | `supplier`, `purchase_order`, `purchase_order_item` | What did we buy, from whom, and when did it arrive? |
| **Selling side** | `customer`, `sales_order`, `sales_order_item` | What did we sell, to whom, and for how much? |
| **Features** | `alert`, `waste_record`, `audit_log` | What needs attention, what was lost, and who did what? |

The key idea is that `warehouse` is the place where stock lives. A single-shop owner has one warehouse row. A mall chain has one per outlet. An e-commerce business has one per fulfilment centre. The schema stays the same for all of them.

### Two stock tables: the main design decision

Quantities are **not** stored on `product`. They live in one of two tables, and `product.is_perishable` decides which one:

| | Non-perishable product | Perishable product |
| --- | --- | --- |
| Examples | Rice, soap, thermometer, bandages | Milk, tomatoes, medicines, insulin |
| `product.is_perishable` | `false` | `true` |
| Quantity is stored in | `non_perishable_stock` (one row per product per warehouse) | `perishable_product_batch` (one row per product per warehouse per batch) |
| Has an expiry date? | No | Yes, required |
| `stock_movement.batch_id` | Empty | The batch that was affected |
| "How much do we have?" | `non_perishable_stock.quantity` | `SUM(perishable_product_batch.quantity)` for that product and warehouse |

---

## 2. Core tables

These seven tables describe who uses the system, where the goods are kept, what the goods are, and how many are left.

### 2.1 `users`

The people who log in to the system (owner, manager, billing staff).

| Column | Type | Meaning |
| --- | --- | --- |
| `id` | integer, PK | Unique id |
| `name` | varchar(50) | Full name |
| `email` | varchar(50), unique | Login name |
| `password_hash` | varchar(255) | BCrypt hash, never the plain password |
| `role` | `user_roles` | `ADMIN`, `MANAGER` or `STAFF` |
| `active` | boolean | `false` disables login without deleting history |
| `created_at` | timestamp | When the account was created |
| `updated_at` | timestamp | When the account was last changed |

**Used by:** `user_id` on audit logs (foreign key), and `created_by` / `recorded_by` on orders, stock movements and waste records.

### 2.2 `warehouse`

Any physical place that holds stock: a shop, a store room, a mall outlet or a warehouse.

| Column | Type | Meaning |
| --- | --- | --- |
| `id` | integer, PK | Unique id |
| `name` | varchar(50) | Display name, e.g. "Virar Store" |
| `address` | varchar(500), required | Full address |
| `pincode` | varchar(20), required | Postal code |
| `phone` | varchar(20), required | Contact number |
| `active` | boolean | `false` hides a closed outlet |

**Used by:** nearly every other table has a `warehouse_id`. This is how one system supports many locations.

### 2.3 `category`

A simple grouping for products (Dairy, Grains, Tablets, Syrups).

| Column | Type | Meaning |
| --- | --- | --- |
| `id` | integer, PK | Unique id |
| `name` | varchar(75), unique | Category name |

### 2.4 `product`

The **catalogue entry**: what the item is and how it is priced. A product does not store quantities. Quantities live in `non_perishable_stock` or `perishable_product_batch`.

| Column | Type | Meaning |
| --- | --- | --- |
| `id` | integer, PK | Unique id |
| `name` | varchar(50) | Product name |
| `barcode` | varchar(100), unique | Scanned at billing. Can be empty for loose items |
| `category_id` | integer | Link to `category` |
| `unit` | `product_unit` | How it is counted: `PCS`, `KG`, `LITRE`, etc. Default `PCS` |
| `cost_price` | decimal(12,2) | What we usually pay |
| `selling_price` | decimal(12,2) | What the customer pays |
| `is_perishable` | boolean | `false` = quantity goes in `non_perishable_stock`. `true` = quantity goes in `perishable_product_batch` |
| `active` | boolean | `false` hides a discontinued product but keeps old orders valid |
| `created_at` | timestamp | When the product was added |
| `updated_at` | timestamp | When the product was last changed |

### 2.5 `non_perishable_stock`

The **current balance** of a non-perishable product in a warehouse. One row per product per warehouse.

| Column | Type | Meaning |
| --- | --- | --- |
| `id` | integer, PK | Unique id |
| `product_id` | integer | Link to `product` |
| `warehouse_id` | integer | Link to `warehouse` |
| `quantity` | decimal(12,2) | Current quantity. Decimal because of KG and LITRE items |
| `reorder_level` | decimal(12,2) | When `quantity` falls to this number or lower, raise a `LOW_STOCK` alert |
| `updated_at` | timestamp | Last time the quantity changed |

`reorder_level` is stored per warehouse, because the right level differs by place. A busy mall outlet needs a higher reorder level than a small branch.

### 2.6 `perishable_product_batch`

The balance of a **perishable** product, split by batch and expiry date. One product can have several batches in the same warehouse, each with its own expiry date.

| Column | Type | Meaning |
| --- | --- | --- |
| `id` | integer, PK | Unique id (this is what other tables point to) |
| `product_id` | integer | Link to `product` |
| `warehouse_id` | integer | Link to `warehouse` |
| `batch_no` | varchar(250) | Batch number printed on the pack |
| `expiry_date` | date, required | The date after which the batch must not be sold |
| `quantity` | decimal(12,2) | Quantity left in this batch |
| `reorder_level` | decimal(12,2) | Low-stock threshold for the product in this warehouse (see the rule below) |
| `updated_at` | timestamp | Last time the quantity changed |

**Rules:**

- A product's total in a warehouse is the **sum of its batches' `quantity`**. There is no separate total row.
- `reorder_level` is stored on each batch, but the low-stock check compares it with the **sum of all batches**. So keep the same `reorder_level` on every batch of the same product and warehouse. When a new batch is created, copy the value from the existing batch.
- A batch whose `quantity` reaches 0 stays in the table as history.

### 2.7 `stock_movement`

The **inventory ledger**. Every change to a quantity is recorded here as a new row. Rows are never edited or deleted.

| Column | Type | Meaning |
| --- | --- | --- |
| `id` | bigint, PK | Unique id (bigint because this table grows fastest) |
| `product_id` | integer | Which product moved |
| `warehouse_id` | integer | Where it moved |
| `batch_id` | integer | Which `perishable_product_batch` was affected. **Empty for non-perishable products** |
| `type` | `movement_type` | `PURCHASE`, `SALE`, `ADJUSTMENT`, `TRANSFER_IN`, `TRANSFER_OUT`, `RETURN`, `WASTE` |
| `quantity` | decimal(12,2) | **Signed**: positive when stock comes in, negative when it goes out |
| `reference_id` | integer | The id of the order or record that caused this movement |
| `reference_type` | `reference_type` | `PURCHASE_ORDER`, `SALES_ORDER`, `WASTE_RECORD` or `MANUAL`. Says which table `reference_id` points to |
| `description` | varchar(500) | Free-text note |
| `created_by` | integer | The user (`users.id`) who caused it |
| `created_at` | timestamp | When it happened |

The two stock tables tell you the balance. `stock_movement` tells you how the balance got there. Opening stock is entered as an `ADJUSTMENT` movement with `reference_type = MANUAL`.

---

## 3. Buying side

These tables record how goods come into the business.

### 3.1 `supplier`

The vendor or distributor you buy from.

| Column | Type | Meaning |
| --- | --- | --- |
| `id` | integer, PK | Unique id |
| `name` | varchar(50) | Supplier name |
| `phone` | varchar(20), required | Contact number |
| `email` | varchar(50) | Contact email |
| `active` | boolean | `false` hides a supplier you no longer use |

### 3.2 `purchase_order`

The header of one purchase: who supplied it, where it is delivered and what state it is in.

| Column | Type | Meaning |
| --- | --- | --- |
| `id` | integer, PK | Unique id |
| `supplier_id` | integer | Link to `supplier` |
| `warehouse_id` | integer | Which warehouse receives the goods |
| `status` | `purchase_status` | `DRAFT`, `ORDERED`, `RECEIVED`, `CANCELLED` |
| `total_amount` | decimal(12,2) | Sum of `quantity × unit_cost` over the item lines |
| `order_date` | date | When the order was placed |
| `received_date` | date | When the goods arrived |
| `created_by` | integer | The user (`users.id`) who created it |

**Only when the status changes to `RECEIVED` does stock increase.**

### 3.3 `purchase_order_item`

One line of a purchase order: one product, one quantity, one cost.

| Column | Type | Meaning |
| --- | --- | --- |
| `id` | integer, PK | Unique id |
| `purchase_order_id` | integer | Link to `purchase_order` (deleted automatically if the order is deleted) |
| `product_id` | integer | Link to `product` |
| `quantity` | decimal(12,2) | How many were ordered |
| `unit_cost` | decimal(12,2) | Price per unit on this invoice (can differ from `product.cost_price`) |

The line has **no batch number or expiry date**, because those are only known when the goods arrive. For a perishable product, the "receive goods" screen asks for `batch_no` and `expiry_date` for each line, and they are saved straight into `perishable_product_batch`. The purchase order is linked to its batch through `stock_movement` (`reference_type = PURCHASE_ORDER`, `reference_id` = the order id, `batch_id` = the batch).

---

## 4. Selling side

These tables record how goods leave the business.

### 4.1 `customer`

An optional record of buyers. Walk-in shoppers can be billed with no customer. E-commerce and regular customers can be saved.

| Column | Type | Meaning |
| --- | --- | --- |
| `id` | integer, PK | Unique id |
| `name` | varchar(50) | Customer name |
| `phone` | varchar(20) | Used to search at the billing counter |
| `email` | varchar(50) | For receipts and online orders |

### 4.2 `sales_order`

The header of one bill or online order.

| Column | Type | Meaning |
| --- | --- | --- |
| `id` | integer, PK | Unique id (also usable as the bill number) |
| `customer_id` | integer | Link to `customer`. **Empty for walk-in** |
| `warehouse_id` | integer | Which outlet made the sale |
| `status` | `sales_status` | `PENDING`, `COMPLETED`, `SHIPPED`, `RETURNED`, `CANCELLED`. Default `COMPLETED` for shop counters |
| `total_amount` | decimal(12,2) | Final amount payable |
| `tax_percent` | decimal(5,2) | **One tax rate for the whole bill** (default 0) |
| `discount` | decimal(12,2) | Discount amount in rupees (default 0) |
| `payment_method` | `payment_method` | `CASH`, `CARD`, `UPI`, `ONLINE`, `OTHER` |
| `created_by` | integer | The cashier (`users.id`) |
| `created_at` | timestamp | When the bill was made |

How the amounts relate:

```
subtotal     = sum of sales_order_item.total_price
tax          = subtotal × tax_percent / 100
total_amount = subtotal + tax − discount
```

The tax amount is not stored. It can always be recalculated from `tax_percent`.

### 4.3 `sales_order_item`

One line of a bill.

| Column | Type | Meaning |
| --- | --- | --- |
| `id` | integer, PK | Unique id |
| `sales_order_id` | integer | Link to `sales_order` (deleted automatically if the order is deleted) |
| `product_id` | integer | Link to `product` |
| `quantity` | decimal(12,2) | Quantity sold (can be 0.75 KG) |
| `unit_price` | decimal(12,2) | Price at the time of sale (copied from `product.selling_price`) |
| `total_price` | decimal(12,2) | `quantity × unit_price`, before tax |

The price is **copied** into the line so that old bills do not change when a product price is edited later.

The line does not say which batch was sold. That is recorded in `stock_movement` (`reference_type = SALES_ORDER`, `reference_id` = the order id, `batch_id` = the batch). One line can create several movements if the quantity came from more than one batch.

---

## 5. Feature tables

These tables turn the raw data into a useful product: notifications, loss tracking and accountability.

### 5.1 `alert`

A notification for the manager. A scheduled job in Spring Boot creates these rows.

| Column | Type | Meaning |
| --- | --- | --- |
| `id` | integer, PK | Unique id |
| `type` | `alert_type` | `LOW_STOCK`, `NEAR_EXPIRY`, `EXPIRED` |
| `product_id` | integer | Which product |
| `warehouse_id` | integer | Which warehouse |
| `batch_id` | integer | Which `perishable_product_batch` (empty for `LOW_STOCK`) |
| `message` | varchar(250) | Text shown in the Angular notification bell |
| `created_at` | timestamp | When the alert was raised |

There is no read/resolved status on this table. An alert simply exists while the problem exists.

When the alert job runs:

- **`LOW_STOCK`** is raised when `non_perishable_stock.quantity <= reorder_level`, or, for a perishable product, when the **sum of its batches' quantity** is `<= reorder_level`.
- **`NEAR_EXPIRY`** is raised when a batch has `quantity > 0` and `expiry_date <= today + N` days. `N` is no longer stored on the product. Keep it as one setting in `application.yml` (for example `alerts.expiry-days: 7`).
- **`EXPIRED`** is raised when a batch has `quantity > 0` and `expiry_date < today`.
- No new alert is created if one already exists for the same type, product, warehouse and batch.
- When the problem is gone (stock refilled, or the batch is sold out or written off), the job deletes the matching alert.

### 5.2 `waste_record`

A record of goods that were lost instead of sold.

| Column | Type | Meaning |
| --- | --- | --- |
| `id` | integer, PK | Unique id |
| `product_id` | integer | Link to `product` |
| `warehouse_id` | integer | Link to `warehouse` |
| `batch_id` | integer | Link to `perishable_product_batch` (empty for non-perishable products) |
| `quantity` | decimal(12,2) | How much was lost |
| `reason` | `waste_reason` | `EXPIRED`, `DAMAGED`, `LOST`, `THEFT`, `OTHER` |
| `cost_value` | decimal(12,2) | Money lost = `quantity × cost price` **at that moment** |
| `description` | varchar(250) | Extra detail |
| `recorded_by` | integer | The user (`users.id`) who recorded it |
| `created_at` | timestamp | When it was recorded |

`cost_value` is stored on purpose. If the cost price changes next month, last month's waste report must not change.

### 5.3 `audit_log`

The **accountability trail**: who did what, where and when.

| Column | Type | Meaning |
| --- | --- | --- |
| `id` | bigint, PK | Unique id |
| `user_id` | integer | Who did it |
| `warehouse_id` | integer | Where it happened |
| `action` | `audit_action` | `CREATE`, `UPDATE`, `DELETE`, `LOGIN`, `STATUS_CHANGE` |
| `entity_type` | varchar(100) | Which table or object, e.g. `Product`, `PurchaseOrder` |
| `entity_id` | integer | Which row |
| `description` | varchar(500) | Readable summary, including the old and new value when something was changed |
| `created_at` | timestamp | When it happened |

### `stock_movement` vs `audit_log`

| | `stock_movement` | `audit_log` |
| --- | --- | --- |
| Purpose | Explains **quantity** | Explains **who did what** |
| Records | Only stock changes | Any action, including price edits, logins and cancelled orders |
| Key data | product, batch, quantity, type | user, entity, text description |
| Can it be archived? | No, stock reports depend on it | Yes, after a year or so |

Example: changing a price from ₹50 to ₹40 goes only to `audit_log` (the description says "price changed from 50 to 40"), because no stock changed. Selling 2 items goes to both: `stock_movement` records the −2, and `audit_log` records who made the sale.

---

## 6. Enums reference

| Enum | Values | Used in |
| --- | --- | --- |
| `user_roles` | ADMIN, MANAGER, STAFF | `users.role` |
| `product_unit` | PCS, KG, GRAM, LITRE, ML, METER, BOX, PACK, DOZEN | `product.unit` |
| `movement_type` | PURCHASE, SALE, ADJUSTMENT, TRANSFER_IN, TRANSFER_OUT, RETURN, WASTE | `stock_movement.type` |
| `reference_type` | PURCHASE_ORDER, SALES_ORDER, WASTE_RECORD, MANUAL | `stock_movement.reference_type` |
| `purchase_status` | DRAFT, ORDERED, RECEIVED, CANCELLED | `purchase_order.status` |
| `sales_status` | PENDING, COMPLETED, SHIPPED, RETURNED, CANCELLED | `sales_order.status` |
| `payment_method` | CASH, CARD, UPI, ONLINE, OTHER | `sales_order.payment_method` |
| `alert_type` | LOW_STOCK, NEAR_EXPIRY, EXPIRED | `alert.type` |
| `alert_status` | UNREAD, READ, RESOLVED | **Not used by any table** in this version (`alert` has no status column) |
| `waste_reason` | EXPIRED, DAMAGED, LOST, THEFT, OTHER | `waste_record.reason` |
| `audit_action` | CREATE, UPDATE, DELETE, LOGIN, STATUS_CHANGE | `audit_log.action` |

---

## 7. Example 1: Grocery store (FreshMart)

FreshMart is a grocery store in Virar. It sells packaged milk and loose tomatoes (both **perishable**) and loose rice (**non-perishable**). All dates and ids below are made up for illustration. Today is **31 Oct 2026**. FreshMart's alert setting is `alerts.expiry-days: 7`.

### Step 1: Setup (core tables)

**`users`**

| id | name | role |
|---|---|---|
| 1 | Anil (owner) | ADMIN |
| 2 | Ravi | STAFF |

**`warehouse`**

| id | name | pincode |
|---|---|---|
| 1 | FreshMart Virar Store | 401303 |

**`category`**

| id | name |
| --- | --- |
| 1 | Dairy |
| 2 | Grains |
| 3 | Vegetables |

**`product`** (only the important columns)

| id | name | category_id | unit | cost_price | selling_price | is_perishable |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | Milk 1L pack | 1 | PCS | 48 | 56 | **true** |
| 2 | Basmati rice | 2 | KG | 95 | 120 | **false** |
| 3 | Tomato | 3 | KG | 28 | 40 | **true** |

**Opening stock.** Rice goes into `non_perishable_stock`. Tomatoes go into `perishable_product_batch` because they have an expiry date.

**`non_perishable_stock`**

| id | product_id | warehouse_id | quantity | reorder_level |
|---|---|---|---|---|
| 1 | 2 (Rice) | 1 | 50 | 15 |

**`perishable_product_batch`**

| id | product_id | warehouse_id | batch_no | expiry_date | quantity | reorder_level |
|---|---|---|---|---|---|---|
| 1 | 3 (Tomato) | 1 | T-1029 | 2026-11-02 | 20 | 5 |

**`stock_movement`** records both opening balances as adjustments:

| id | product_id | batch_id | type | quantity | reference_type |
| --- | --- | --- | --- | --- | --- |
| 1 | 2 (Rice) | | ADJUSTMENT | **+50** | MANUAL |
| 2 | 3 (Tomato) | 1 | ADJUSTMENT | **+20** | MANUAL |

> **Link:** `product.is_perishable` decides the table. Rice (`false`) → `non_perishable_stock`, with an empty `batch_id` in the ledger. Tomato (`true`) → `perishable_product_batch`, with `batch_id = 1` in the ledger.

### Step 2: Buying milk (buying side)

Ravi creates a purchase order for 100 packs of milk from a dairy distributor.

**`supplier`**

| id | name | phone |
|---|---|---|
| 1 | Gokul Dairy Distributors | 98xxxxxx21 |

**`purchase_order`** #1

| id | supplier_id | warehouse_id | status | total_amount | order_date | created_by |
|---|---|---|---|---|---|---|
| 1 | 1 | 1 | DRAFT → ORDERED | 4800 | 2026-10-30 | 2 |

**`purchase_order_item`**

| id | purchase_order_id | product_id | quantity | unit_cost |
|---|---|---|---|---|
| 1 | 1 | 1 (Milk) | 100 | 48 |

On **31 Oct** the delivery arrives. On the "receive goods" screen, Ravi marks the order **RECEIVED** and types the batch number `M-1030-A` and expiry date `2026-11-04` from the pack. In one database transaction, the system does three things:

1. `purchase_order.status` becomes `RECEIVED` and `received_date` becomes 2026-10-31.
2. A new **`perishable_product_batch`** row is created:

   | id | product_id | warehouse_id | batch_no | expiry_date | quantity | reorder_level |
   |---|---|---|---|---|---|---|
   | 2 | 1 (Milk) | 1 | M-1030-A | 2026-11-04 | 100 | 20 |

3. A **`stock_movement`** row is written:

   | id | product_id | warehouse_id | batch_id | type | quantity | reference_type | reference_id | created_by |
   |---|---|---|---|---|---|---|---|---|
   | 3 | 1 | 1 | 2 | PURCHASE | **+100** | PURCHASE_ORDER | 1 | 2 |

> **Link:** `supplier` → `purchase_order` → `purchase_order_item` → `product`. Receiving creates the batch and the ledger row. The movement's `reference_id = 1` points back to the purchase order, and its `batch_id = 2` points to the batch, which is how you find "which batch came with this order".

### Step 3: Selling (selling side)

Sunita, a regular customer, buys 2 packs of milk, 1.5 KG rice and 0.75 KG tomatoes and pays by UPI. Ravi sets a bill tax rate of 5%.

**`customer`**

| id | name | phone |
|---|---|---|
| 1 | Sunita Patil | 98xxxxxx10 |

**`sales_order`** #1

| id | customer_id | warehouse_id | status | tax_percent | discount | total_amount | payment_method | created_by |
|---|---|---|---|---|---|---|---|---|
| 1 | 1 | 1 | COMPLETED | 5.00 | 0 | 338.10 | UPI | 2 |

**`sales_order_item`**

| id | sales_order_id | product_id | quantity | unit_price | total_price |
| --- | --- | --- | --- | --- | --- |
| 1 | 1 | 1 (Milk) | 2 | 56 | 112.00 |
| 2 | 1 | 2 (Rice) | 1.5 | 120 | 180.00 |
| 3 | 1 | 3 (Tomato) | 0.75 | 40 | 30.00 |

Subtotal 322.00, plus 5% tax (16.10), minus discount 0, gives **338.10**.

At the same moment, in the same transaction, each line updates the table that matches its product:

- **Milk** (perishable): `perishable_product_batch` #2 goes from 100 to 98.
- **Rice** (non-perishable): `non_perishable_stock` goes from 50 to 48.5.
- **Tomato** (perishable): `perishable_product_batch` #1 goes from 20 to 19.25.
- Three **`stock_movement`** rows are created:

  | id | product_id | batch_id | type | quantity | reference_type | reference_id |
  | --- | --- | --- | --- | --- | --- | --- |
  | 4 | 1 | 2 | SALE | **−2** | SALES_ORDER | 1 |
  | 5 | 2 | | SALE | **−1.5** | SALES_ORDER | 1 |
  | 6 | 3 | 1 | SALE | **−0.75** | SALES_ORDER | 1 |

> **Link:** `customer` → `sales_order` → `sales_order_item` → `product`. A sale fans out into `stock_movement` plus one of the two stock tables. If it were a walk-in bill, `customer_id` would simply be empty.

### Step 4: Nightly alerts (features)

Through the day, more milk is sold and the milk batch falls to **18** packs. That night the alert job runs:

| Check | Result |
| --- | --- |
| Milk: 18 in total, reorder level 20 | **LOW_STOCK** |
| Rice: 48.5 in `non_perishable_stock`, reorder level 15 | OK |
| Milk batch M-1030-A expires 04 Nov (4 days, within 7) | **NEAR_EXPIRY** |
| Tomato batch T-1029 expires 02 Nov (2 days, within 7) | **NEAR_EXPIRY** |

**`alert`**

| id | type | product_id | warehouse_id | batch_id | message |
| --- | --- | --- | --- | --- | --- |
| 1 | LOW_STOCK | 1 | 1 | | Milk 1L pack is low: 18 left (reorder level 20) |
| 2 | NEAR_EXPIRY | 1 | 1 | 2 | Milk batch M-1030-A expires on 04 Nov (18 left) |
| 3 | NEAR_EXPIRY | 3 | 1 | 1 | Tomato batch T-1029 expires on 02 Nov (19.25 kg left) |

Anil sees them in the bell icon in the Angular app. He reorders milk and puts the old milk on offer. Once new stock arrives, the job finds the low-stock problem gone and deletes alert 1.

### Step 5: Waste (features)

On 5 Nov, 6 packs of milk from batch M-1030-A did not sell and are past their expiry date. The job raises an `EXPIRED` alert, and Ravi records the waste:

**`waste_record`**

| id | product_id | warehouse_id | batch_id | quantity | reason | cost_value | recorded_by |
|---|---|---|---|---|---|---|---|
| 1 | 1 | 1 | 2 | 6 | EXPIRED | 288.00 (6 × 48) | 2 |

In one transaction, the system also:

- adds a `stock_movement`: `WASTE`, quantity **−6**, `batch_id = 2`, `reference_type = WASTE_RECORD`, `reference_id = 1`;
- reduces `perishable_product_batch` #2 from 6 to 0 (the row stays as history);
- lets the next alert job delete the `EXPIRED` alert, since the batch is now empty.

The monthly waste report is then a simple query: the sum of `cost_value` from `waste_record`, grouped by `reason`.

### Step 6: Audit trail (features)

Throughout all of the above, `audit_log` quietly collects entries:

| user_id | warehouse_id | action | entity_type | entity_id | description |
| --- | --- | --- | --- | --- | --- |
| 2 | 1 | CREATE | PurchaseOrder | 1 | Ravi created purchase order #1 |
| 2 | 1 | STATUS_CHANGE | PurchaseOrder | 1 | Status changed from ORDERED to RECEIVED |
| 2 | 1 | CREATE | SalesOrder | 1 | Ravi created sales order #1 (₹338.10) |
| 1 | 1 | UPDATE | Product | 3 | Anil changed Tomato selling price from 40 to 45 |
| 2 | 1 | CREATE | WasteRecord | 1 | Ravi recorded 6 expired milk packs |

The price change in the fourth row did **not** touch `stock_movement`, which is why the two tables are separate.

---

## 8. Example 2: Medical store (CareWell Pharmacy)

CareWell is a pharmacy with two outlets, Virar West and Nalasopara. Medicines always have batch numbers and expiry dates, so almost every product is **perishable**. It also sells a few non-perishable items, such as thermometers. This example shows both stock tables, FEFO (first expiry, first out), expiry alerts and a transfer between outlets. CareWell's alert setting is `alerts.expiry-days: 60`, because a pharmacy needs time to return near-expiry stock to the distributor.

### Step 1: Setup (core tables)

**`warehouse`**

| id | name |
| --- | --- |
| 1 | CareWell Virar West |
| 2 | CareWell Nalasopara |

**`category`**

| id | name |
| --- | --- |
| 1 | Tablets |
| 2 | Syrups |
| 3 | Injectables |
| 4 | Devices |

**`product`**

| id | name | category_id | unit | cost_price | selling_price | is_perishable |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | Paracetamol 500mg (strip of 10) | 1 | PCS | 12 | 15 | **true** |
| 2 | Cough syrup 100ml | 2 | PCS | 70 | 95 | **true** |
| 3 | Insulin pen | 3 | PCS | 650 | 780 | **true** |
| 4 | Digital thermometer | 4 | PCS | 180 | 250 | **false** |

**Opening stock at Virar West.** The thermometer goes into `non_perishable_stock`. The medicines go into `perishable_product_batch`.

**`non_perishable_stock`**

| id | product_id | warehouse_id | quantity | reorder_level |
|---|---|---|---|---|
| 1 | 4 (Thermometer) | 1 | 12 | 5 |

**`perishable_product_batch`**

| id | product_id | warehouse_id | batch_no | expiry_date | quantity | reorder_level |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | 1 (Paracetamol) | 1 | PCM-A19 | 2026-12-31 | 40 | 30 |
| 2 | 2 (Syrup) | 1 | SYR-05 | 2027-03-31 | 10 | 5 |

The three opening balances are written to `stock_movement` as `ADJUSTMENT` movements (ids 1 to 3, `reference_type = MANUAL`), with `batch_id` 1, 2 and empty respectively.

### Step 2: Buying (buying side)

**`supplier`**

| id | name | phone |
|---|---|---|
| 1 | MedLine Distributors | 98xxxxxx45 |

**`purchase_order`** #1 (Virar West, total 8900, order date 2026-10-28)

**`purchase_order_item`**

| id | purchase_order_id | product_id | quantity | unit_cost |
| --- | --- | --- | --- | --- |
| 1 | 1 | 1 (Paracetamol) | 200 | 12 |
| 2 | 1 | 3 (Insulin) | 10 | 650 |

When the delivery arrives on 31 Oct, the pharmacist types the batch details from the packs on the receive screen: paracetamol `PCM-A23` expiring 2027-06-30, and insulin `INS-77` expiring 2026-12-15. The system creates two new `perishable_product_batch` rows and two `PURCHASE` movements:

| batch id | product | batch_no | expiry_date | quantity | reorder_level |
| --- | --- | --- | --- | --- | --- |
| 3 | Paracetamol | PCM-A23 | 2027-06-30 | 200 | 30 |
| 4 | Insulin | INS-77 | 2026-12-15 | 10 | 4 |

Paracetamol now has **two batches** in the same warehouse, so the total on hand is 40 + 200 = **240**.

### Step 3: Selling with FEFO (selling side)

A walk-in customer buys 2 strips of paracetamol and 1 cough syrup, paying cash. There is no customer record. The pharmacist sets a bill tax rate of 5%.

**`sales_order`** #1

| id | customer_id | warehouse_id | status | tax_percent | discount | total_amount | payment_method |
|---|---|---|---|---|---|---|---|
| 1 | *(empty)* | 1 | COMPLETED | 5.00 | 0 | 131.25 | CASH |

**`sales_order_item`**

| sales_order_id | product_id | quantity | unit_price | total_price |
| --- | --- | --- | --- | --- |
| 1 | 1 (Paracetamol) | 2 | 15 | 30.00 |
| 1 | 2 (Syrup) | 1 | 95 | 95.00 |

Subtotal 125.00, plus 5% tax (6.25), gives **131.25**.

The system sells paracetamol from the batch with the **nearest expiry** that is still valid, which is `PCM-A19`, not `PCM-A23`:

- Batch 1 (PCM-A19) goes 40 → 38; batch 3 (PCM-A23) stays at 200.
- Syrup batch 2 goes 10 → 9.
- `stock_movement` gets `SALE, −2` with `batch_id = 1` and `SALE, −1` with `batch_id = 2` (ids 6 and 7).

> **Link:** this is why `stock_movement.batch_id` exists. If a distributor recalls batch PCM-A19, you can search the ledger for every sale from that batch.

### Step 4: Nightly alerts (features)

The job runs on 31 Oct 2026. With a 60-day window, it flags batches expiring on or before 30 Dec 2026:

| Batch | Expiry | Within 60 days? | Result |
| --- | --- | --- | --- |
| PCM-A19 (paracetamol) | 2026-12-31 | No (61 days away) | No alert tonight; it will fire tomorrow |
| INS-77 (insulin) | 2026-12-15 | **Yes** (45 days) | `NEAR_EXPIRY` alert created |
| SYR-05 (syrup) | 2027-03-31 | No | None |
| PCM-A23 (paracetamol) | 2027-06-30 | No | None |

The low-stock check also runs: paracetamol totals 238 (reorder level 30), syrup 9 (level 5), thermometer 12 (level 5). All are fine.

**`alert`**

| id | type | product_id | warehouse_id | batch_id | message |
|---|---|---|---|---|---|
| 1 | NEAR_EXPIRY | 3 | 1 | 4 | Insulin pen batch INS-77 expires on 15 Dec (10 left) |

The pharmacist uses this to return the insulin to MedLine before it expires.

### Step 5: Transfer between outlets (core tables)

Virar West has plenty of insulin and Nalasopara has none. The manager moves 4 pens from outlet 1 to outlet 2. A transfer is recorded as **two** stock movements, because the schema has no separate transfer table:

| id | product_id | warehouse_id | batch_id | type | quantity | reference_type | description |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 8 | 3 | 1 | 4 | TRANSFER_OUT | **−4** | MANUAL | Transfer to CareWell Nalasopara |
| 9 | 3 | 2 | 5 | TRANSFER_IN | **+4** | MANUAL | Transfer from CareWell Virar West |

Other changes in the same transaction:

- Batch 4 at warehouse 1 goes from 10 to 6.
- A new `perishable_product_batch` row (id 5) is created at warehouse 2 with the same `batch_no` (INS-77) and expiry (2026-12-15), quantity 4, and the reorder level for that outlet. A batch row belongs to one warehouse, so the receiving outlet needs its own row.

### Step 6: Waste, loss and the audit trail (features)

**Expired stock.** On 1 Jan 2027, 5 strips of PCM-A19 are left and are now expired. A `waste_record` is created (`batch_id = 1`, `reason = EXPIRED`, `quantity = 5`, `cost_value = 5 × 12 = 60`). The linked `stock_movement` is `WASTE, −5`, and batch 1 goes to 0.

**Missing item.** During a monthly stock check, one insulin pen is missing from batch 4. The manager records a `waste_record` with `reason = LOST`, `quantity = 1`, `cost_value = 650`. The ledger shows `WASTE −1` and batch 4 goes from 6 to 5. Monthly reports now show that ₹650 was lost to missing stock, not to expiry.

**Audit trail.** For a pharmacy, accountability matters. The `audit_log` shows who did each of these:

| user_id | warehouse_id | action | entity_type | description |
| --- | --- | --- | --- | --- |
| 3 | 1 | CREATE | WasteRecord | Pharmacist recorded 1 insulin pen as LOST |
| 3 | 1 | CREATE | StockMovement | Transfer of 4 insulin pens to Nalasopara |
| 1 | 1 | UPDATE | Product | Owner changed Cough syrup price from 95 to 99 |
| 3 | 1 | LOGIN | User | Login from 192.168.1.20 |

---

## 9. Rules to follow in the Spring Boot code

1. **Never edit a quantity directly.** Always insert a `stock_movement` and update the matching stock table (`non_perishable_stock` or `perishable_product_batch`) in the **same `@Transactional` method**, so the ledger and the balance can never disagree.
2. **Pick the table from `product.is_perishable`.** `false` → `non_perishable_stock`, with an empty `batch_id` in the movement. `true` → `perishable_product_batch`, with the batch id in the movement.
3. **Never change `is_perishable` once a product has stock.** The quantity would be sitting in the wrong table. Create a new product instead.
4. **Use signed quantities in `stock_movement`.** Positive for `PURCHASE`, `TRANSFER_IN`, `RETURN`. Negative for `SALE`, `TRANSFER_OUT`, `WASTE`. `ADJUSTMENT` can be either.
5. **Sell perishables by FEFO.** Take from the batch with the nearest `expiry_date` that has `quantity > 0` and is not yet expired. If a line needs more than one batch, write one `stock_movement` per batch.
6. **A perishable total is a sum.** The total for a product and warehouse is `SUM(perishable_product_batch.quantity)`. There is no separate total to keep in sync.
7. **Receiving perishables.** The receive request carries `batch_no` and `expiry_date` for each line. If a batch with the same product, warehouse and `batch_no` already exists, add to its quantity instead of creating a duplicate. Copy `reorder_level` from the existing batches of that product and warehouse.
8. **Prevent negative stock.** Check in the service layer, and add `CHECK (quantity >= 0)` on both stock tables in PostgreSQL.
9. **Copy, don't reference, prices.** `sales_order_item.unit_price` and `waste_record.cost_value` are saved as values on purpose, so old reports stay correct.
10. **Soft-delete.** Set `active = false` on `users`, `product`, `supplier` and `warehouse` instead of deleting rows that old orders point to.
11. **Alert job.** Use `@Scheduled` (nightly or hourly). Read the expiry window from configuration. Skip creating an alert if the same type, product, warehouse and batch already has one, and delete alerts whose problem has gone.
12. **Audit automatically.** Use a Spring AOP aspect or a JPA entity listener to write `audit_log` rows, so you don't call it manually in every service. Put the old and new value in the `description` text.

---

## 10. Notes on this DBML version

### Paste-ready fixes

Add an `indexes` block inside each of these tables:

```dbml
// inside non_perishable_stock
indexes {
  (product_id, warehouse_id) [ unique ]
  warehouse_id
 }

// inside perishable_product_batch
indexes {
 (product_id, warehouse_id, batch_no) [ unique ]
 (warehouse_id, expiry_date)
}

// inside stock_movement
indexes {
 (product_id, warehouse_id, created_at)
 (reference_type, reference_id)
 batch_id
}

// inside sales_order
indexes {
 (warehouse_id, created_at)
 customer_id
}

// inside purchase_order
indexes {
 (warehouse_id, status)
 supplier_id
}

// inside alert
indexes {
 (type, product_id, warehouse_id, batch_id)
}

// inside audit_log
indexes {
 (warehouse_id, created_at)
 (entity_type, entity_id)
 (user_id, created_at)
}

// inside waste_record
indexes {
 (warehouse_id, created_at)
 product_id
}
```

In PostgreSQL, also use `jsonb` if you ever re-add JSON columns, and add `CHECK (quantity >= 0)` on `non_perishable_stock` and `perishable_product_batch`.
