# Inventory Management System — Schema Explanation

## 1. Customers

The `customers` table stores information about customers who place orders.

```text
customers
- id
- name
- phone
- email
```

### Example

A customer named Rahul wants to buy some products.

```text
Customer
ID: C101
Name: Rahul
Phone: 9876543210
Email: rahul@gmail.com
```

This customer can place multiple orders.

**Relation:**

```text
customers.id < orders.customerId
```

Meaning:

> One customer can have many orders.

---

## 2. Orders

The `orders` table stores the main information about a customer's order.

```text
orders
- id
- customerId
- warehouseId
- placedAt
- status
```

### Example

Rahul places an order for 5 packets of paneer.

```text
Order
ID: O101
customerId: C101
warehouseId: W1
placedAt: 2026-08-19 10:30
status: "placed"
```

Here:

- `customerId = C101`  → Rahul placed the order.
- `warehouseId = W1`  → Warehouse W1 will fulfill the order.
- `status = placed`  → The order has been placed.
**Relation:**

```text
customers.id < orders.customerId
warehouses.id < orders.warehouseId
```

---

## 3. Order Items

An order can contain multiple products.

The `order_items` table stores the individual products inside an order.

```text
order_items
- id
- orderId
- batchId
- quantity
- price
```

### Example

Rahul's order `O101` contains:

```text
Order Item
ID: I101
orderId: O101
batchId: B501
quantity: 5
price: 80
```

This means:

> Order O101 contains 5 units of the product from batch B501, at ₹80 per unit.

If Rahul also orders milk, there would be another `order_items` record:

```text
I102
orderId: O101
batchId: B502
quantity: 2
price: 50
```

So one order can have many order items.

**Relation:**

```text
orders.id < order_items.orderId
inventory_batches.id < order_items.batchId
```

---

# 4. Inventory Batches

The `inventory_batches` table represents the actual stock stored in a warehouse.

A batch is important because products such as paneer, curd, milk, etc. can have different expiry dates.

### Example

Suppose Warehouse W1 receives 100 packets of paneer.

```text
Batch
ID: B501
productId: P101
warehouseId: W1
batchNumber: PN001
quantityAvailable: 100
expiryDate: 2026-08-25
```

This means:

> Warehouse W1 currently has 100 packets of paneer from batch PN001, which expires on August 25.

If 5 packets are sold:

```text
quantityAvailable:
100 → 95
```

The batch remains the same, but the available quantity decreases.

---

# 5. How an Order Uses a Batch

This is an important part of the system.

Suppose:

```text
Batch B501
Paneer
100 units
Expiry: Aug 25
```

Rahul orders:

```text
5 units
```

The `order_items` table connects Rahul's order to that specific batch:

```text
order_items
-------------------------
orderId = O101
batchId = B501
quantity = 5
price = 80
```

So the flow is:

```text
Customer Rahul
      ↓
Order O101
      ↓
Order Item
      ↓
Batch B501
      ↓
5 units removed from stock
```

This allows the system to know **which batch was used for the sale**.

---

# 6. Users

The `users` table stores employees or system users who operate the warehouse management system.

For example:

```text
User
ID: U101
Name: Amit
Role: Warehouse Manager
```

A user might:

- Receive stock
- Approve a purchase order
- Dispatch stock
- Adjust stock
- Mark damaged stock
- Cancel an order

---

# 7. Audit Logs

The `audit_logs` table records important actions performed by users.

Think of it as the **history of actions in the system**.

```text
audit_logs
- id
- userId
- action
- type
- recordId
- date
- batchId
```

It answers:

> Who did what, to which record, and when?

### Example: Incoming Stock

A warehouse employee receives 100 packets of paneer.

```text
User: U101
Action: "stock_received"
Type: "batch"
RecordId: B501
Date: 2026-08-19
BatchId: B501
```

Meaning:

> User U101 received batch B501 into the warehouse.

---

### Example: Outgoing Stock

Rahul's order takes 5 units from batch B501.

The system can record:

```text
User: U101
Action: "stock_dispatched"
Type: "order"
RecordId: O101
Date: 2026-08-19
BatchId: B501
```

Meaning:

> User U101 dispatched stock from batch B501 for order O101.

Therefore, **audit logs are used for both incoming and outgoing stock**.

They can also record:

```text
stock_received
stock_dispatched
stock_adjusted
stock_wasted
order_cancelled
batch_created
batch_updated
```

---

# 8. Why Do We Need `batchId` in Audit Logs?

Suppose the warehouse has two batches of paneer:

```text
B501 → 100 units → expires Aug 25
B502 → 150 units → expires Aug 30
```

If 5 units are dispatched from B501, the audit log can store:

```text
batchId = B501
```

Now we know exactly which batch was involved.

This is especially useful for products with expiry dates.

---

# 9. What Is `recordId`?

`recordId` tells us which record the action was performed on.

For example:

```text
type = "order"
recordId = O101
```

means:

> The action was performed on order O101.

Another example:

```text
type = "batch"
recordId = B501
```

means:

> The action was performed on batch B501.

So:

```text
type + recordId
```

tells us **what record was affected**.

---

# 10. Complete Example

Let's put everything together.

### Step 1 — Customer

Rahul is registered:

```text
customers
C101 | Rahul | 9876543210 | rahul@gmail.com
```

### Step 2 — Warehouse

Warehouse W1 has paneer.

```text
warehouses
W1 | Mumbai Warehouse
```

### Step 3 — Inventory Batch

The warehouse receives 100 packets:

```text
inventory_batches
B501
productId = P101
warehouseId = W1
quantityAvailable = 100
expiryDate = 2026-08-25
```

### Step 4 — Customer Places Order

Rahul orders 5 packets:

```text
orders
O101
customerId = C101
warehouseId = W1
status = "placed"
```

### Step 5 — Order Item

The 5 packets come from batch B501:

```text
order_items
I101
orderId = O101
batchId = B501
quantity = 5
price = 80
```

### Step 6 — Stock Decreases

The inventory changes:

```text
B501
quantityAvailable: 100 → 95
```

### Step 7 — Audit

The system records who dispatched the stock:

```text
audit_logs
userId = U101
action = "stock_dispatched"
type = "order"
recordId = O101
batchId = B501
```

Now the system can answer:

> **Who dispatched Rahul's order?**

→ User U101

> **Which order?**

→ O101

> **Which batch was used?**

→ B501

> **How many were sold?**

→ 5, from `order_items`

> **How many are left?**

→ 95, from `inventory_batches`

---

# 11. Main Relationships

The important relationships between these tables are:

```text
customers.id < orders.customerId

warehouses.id < orders.warehouseId

orders.id < order_items.orderId

inventory_batches.id < order_items.batchId

users.id < audit_logs.userId

inventory_batches.id < audit_logs.batchId
```

In simple terms:

```text
Customer
   │
   │ places
   ▼
Order
   │
   │ contains
   ▼
Order Item
   │
   │ uses
   ▼
Inventory Batch
```

And:

```text
User
   │
   │ performs action
   ▼
Audit Log
   │
   │ may relate to
   ▼
Inventory Batch
```

# 12. One-Line Explanation of Each Table

| Table | Simple meaning |
| ----- | ----- |
| `customers` | Stores customers |
| `orders` | Stores customer orders |
| `order_items` | Stores products/batches inside an order |
| `inventory_batches` | Stores available stock by batch |
| `users` | Stores warehouse/system users |
| `audit_logs` | Stores the history of important actions |

### The easiest way to remember the system

**Customer places an Order → Order contains Order Items → Order Item takes stock from an Inventory Batch → User performs the operation → Audit Log records the action.**
