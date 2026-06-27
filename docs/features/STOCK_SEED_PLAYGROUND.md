# Stock Seed Playground

Este documento describe la data local creada por `scripts/local/reset_catalog_inventory_playground.sql` para probar inventario, recetas, productos y armables.

La data usa sufijo `PRUEBA STOCK` y esta pensada para `Brunchea`. No crea órdenes. Las órdenes se crearán posteriormente desde POS para validar reservas, consumos y movimientos.

## Menu y publicación

Menu creado:

- `Menu pruebas de stock`

Submenus:

- `Productos planos y directos`
- `Productos con preparaciones`
- `Armables de prueba`
- `Agotados y bajo stock`

Publicados individualmente:

- `Agua botella plano PRUEBA STOCK`
- `Ensalada directa PRUEBA STOCK`
- `Arroz porcion preparacion PRUEBA STOCK`
- `Bowl carne mixto PRUEBA STOCK`
- `Gaseosa lata PRUEBA STOCK`
- `Postre fresa agotado PRUEBA STOCK`
- `Bowl armable PRUEBA STOCK`

No publicado individualmente:

- `Queso adicional opcion PRUEBA STOCK`: solo debe aparecer como opcion dentro de armables.

## Ingredientes y stock inicial

| Ingrediente | Unidad base | Costo base | Stock inicial | Min stock | Lectura esperada |
|---|---:|---:|---:|---:|---|
| Carne molida PRUEBA STOCK | g | 24 | 1000 | 300 | Stock suficiente para bowls de carne |
| Tomate PRUEBA STOCK | g | 4 | 200 | 80 | Puede agotarse al probar ensalada/bowl |
| Cebolla roja PRUEBA STOCK | g | 3 | 300 | 100 | Usada indirectamente por guiso |
| Arroz crudo PRUEBA STOCK | g | 5 | 1000 | 300 | Usado indirectamente por arroz cocido |
| Aceite PRUEBA STOCK | ml | 20 | 100 | 20 | Usado indirectamente por arroz cocido |
| Lechuga PRUEBA STOCK | g | 6 | 600 | 200 | Usada por ensalada |
| Queso PRUEBA STOCK | g | 30 | 400 | 100 | Usado por ensalada y extra de armable |
| Gaseosa lata PRUEBA STOCK | pcs | 2500 | 2 | 3 | Disponible, pero bajo stock |
| Fresa PRUEBA STOCK | g | 10 | 0 | 100 | Agotado |

## Preparaciones internas

### Guiso tomate y cebolla PRUEBA STOCK

Uso: validar expansion recursiva de una preparacion.

- Rendimiento físico: `300 g`
- Receta del lote:
  - `100 g` Tomate PRUEBA STOCK
  - `200 g` Cebolla roja PRUEBA STOCK
- Costo lote: `1000`
- Costo por g: `3.333333`

### Arroz cocido PRUEBA STOCK

Uso: validar producto basado solo en preparacion.

- Rendimiento físico: `1000 g`
- Receta del lote:
  - `500 g` Arroz crudo PRUEBA STOCK
  - `20 ml` Aceite PRUEBA STOCK
- Costo lote: `2900`
- Costo por g: `2.9`

## Productos y consumo esperado

### Agua botella plano PRUEBA STOCK

Producto plano, no rastreado.

- Precio: `4000`
- No tiene receta.
- No debe generar `order_item_ingredient_requirements`.
- No debe generar reservas ni consumos.

| Venta | Consumo esperado |
|---:|---|
| 1 unidad | Ningún ingrediente |
| 2 unidades | Ningún ingrediente |

### Ensalada directa PRUEBA STOCK

Producto rastreado con ingredientes directos.

- Precio: `12000`
- Rendimiento físico: `1000 g`
- Porciones vendibles: `5`
- Costo por porcion: `1400`

Receta lote:

- `500 g` Lechuga
- `250 g` Tomate
- `100 g` Queso

| Ingrediente | 1 unidad | 2 unidades |
|---|---:|---:|
| Lechuga PRUEBA STOCK | 100 g | 200 g |
| Tomate PRUEBA STOCK | 50 g | 100 g |
| Queso PRUEBA STOCK | 20 g | 40 g |

### Arroz porcion preparacion PRUEBA STOCK

Producto rastreado basado unicamente en preparacion.

- Precio: `5000`
- Rendimiento físico: `1000 g`
- Porciones vendibles: `10`
- Costo por porcion: `290`

Receta lote:

- `1000 g` Arroz cocido PRUEBA STOCK

Expansión por unidad vendida:

| Ingrediente | 1 unidad | 2 unidades |
|---|---:|---:|
| Arroz crudo PRUEBA STOCK | 50 g | 100 g |
| Aceite PRUEBA STOCK | 2 ml | 4 ml |

### Bowl carne mixto PRUEBA STOCK

Producto rastreado mixto: ingrediente directo + dos preparaciones.

- Precio: `16000`
- Rendimiento físico: `2500 g`
- Porciones vendibles: `10`
- Costo por porcion esperado: `3956.6667`

Receta lote:

- `1500 g` Carne molida PRUEBA STOCK
- `200 g` Guiso tomate y cebolla PRUEBA STOCK
- `1000 g` Arroz cocido PRUEBA STOCK

Expansión por unidad vendida:

| Ingrediente | 1 unidad | 2 unidades |
|---|---:|---:|
| Carne molida PRUEBA STOCK | 150 g | 300 g |
| Tomate PRUEBA STOCK | 6.666667 g | 13.333334 g |
| Cebolla roja PRUEBA STOCK | 13.333333 g | 26.666666 g |
| Arroz crudo PRUEBA STOCK | 50 g | 100 g |
| Aceite PRUEBA STOCK | 2 ml | 4 ml |

### Queso adicional opcion PRUEBA STOCK

Producto rastreado option-only.

- No publicado individualmente.
- Habilitado como opcion para armables.
- Rendimiento físico: `500 g`
- Porciones vendibles: `10`
- Costo por unidad: `1500`

| Ingrediente | 1 unidad | 2 unidades |
|---|---:|---:|
| Queso PRUEBA STOCK | 50 g | 100 g |

### Gaseosa lata PRUEBA STOCK

Producto rastreado unitario.

- Precio: `6000`
- Rendimiento físico: `1 pcs`
- Porciones vendibles: `1`
- Stock inicial: `2 pcs`
- Min stock: `3 pcs`
- Debe aparecer disponible, pero con bajo stock.

| Ingrediente | 1 unidad | 2 unidades |
|---|---:|---:|
| Gaseosa lata PRUEBA STOCK | 1 pcs | 2 pcs |

### Postre fresa agotado PRUEBA STOCK

Producto rastreado agotado.

- Precio: `9000`
- Rendimiento físico: `500 g`
- Porciones vendibles: `10`
- Stock inicial de fresa: `0 g`
- Debe aparecer visible en POS, pero deshabilitado.

| Ingrediente | 1 unidad | 2 unidades |
|---|---:|---:|
| Fresa PRUEBA STOCK | 50 g | 100 g |

## Armable: Bowl armable PRUEBA STOCK

Precio base: `18000`.

Slots:

| Slot | Min | Max | Opciones |
|---|---:|---:|---|
| Base fija | 1 | 1 | Arroz porcion preparacion, default, +0 |
| Principal | 1 | 1 | Bowl carne mixto +0, Ensalada directa +2000 |
| Bebidas | 0 | 2 | Gaseosa lata +3000, maxQuantity 2 |
| Extras | 0 | 3 | Queso adicional opcion +1500, maxQuantity 2 |

### Combinaciones esperadas

#### Base + Bowl carne

| Ingrediente | Consumo |
|---|---:|
| Arroz crudo PRUEBA STOCK | 100 g |
| Aceite PRUEBA STOCK | 4 ml |
| Carne molida PRUEBA STOCK | 150 g |
| Tomate PRUEBA STOCK | 6.666667 g |
| Cebolla roja PRUEBA STOCK | 13.333333 g |

Explicación: base consume una porcion de arroz; bowl carne también incluye otra porcion equivalente de arroz cocido.

#### Base + Ensalada

| Ingrediente | Consumo |
|---|---:|
| Arroz crudo PRUEBA STOCK | 50 g |
| Aceite PRUEBA STOCK | 2 ml |
| Lechuga PRUEBA STOCK | 100 g |
| Tomate PRUEBA STOCK | 50 g |
| Queso PRUEBA STOCK | 20 g |

#### Base + Bowl carne + Gaseosa

| Ingrediente | Consumo |
|---|---:|
| Arroz crudo PRUEBA STOCK | 100 g |
| Aceite PRUEBA STOCK | 4 ml |
| Carne molida PRUEBA STOCK | 150 g |
| Tomate PRUEBA STOCK | 6.666667 g |
| Cebolla roja PRUEBA STOCK | 13.333333 g |
| Gaseosa lata PRUEBA STOCK | 1 pcs |

#### Base + Bowl carne + Queso x2

| Ingrediente | Consumo |
|---|---:|
| Arroz crudo PRUEBA STOCK | 100 g |
| Aceite PRUEBA STOCK | 4 ml |
| Carne molida PRUEBA STOCK | 150 g |
| Tomate PRUEBA STOCK | 6.666667 g |
| Cebolla roja PRUEBA STOCK | 13.333333 g |
| Queso PRUEBA STOCK | 100 g |

#### Base + Bowl carne + Gaseosa + Queso

| Ingrediente | Consumo |
|---|---:|
| Arroz crudo PRUEBA STOCK | 100 g |
| Aceite PRUEBA STOCK | 4 ml |
| Carne molida PRUEBA STOCK | 150 g |
| Tomate PRUEBA STOCK | 6.666667 g |
| Cebolla roja PRUEBA STOCK | 13.333333 g |
| Gaseosa lata PRUEBA STOCK | 1 pcs |
| Queso PRUEBA STOCK | 50 g |

## Qué revisar cuando se venda algo

### Al crear draft

Debe congelar requerimientos por unidad vendida:

```sql
SELECT oi.item_name_snapshot,
       mi.name AS ingredient,
       r.quantity_base_per_sale_unit,
       r.unit_cost_snapshot
FROM order_item_ingredient_requirements r
JOIN order_items oi ON oi.id = r.order_item_id
JOIN master_ingredients mi ON mi.id = r.master_ingredient_id
WHERE r.order_item_id = '<order-item-id>'
ORDER BY mi.name;
```

### Al confirmar orden

Debe crear reservas activas multiplicadas por cantidad vendida:

```sql
SELECT mi.name, r.quantity_base, r.status, r.unit_cost_snapshot
FROM inventory_reservations r
JOIN master_ingredients mi ON mi.id = r.master_ingredient_id
WHERE r.order_id = '<order-id>'
ORDER BY mi.name;
```

### Al pasar ticket a PREPARING

Debe consumir reservas, descontar stock y crear transacciones `SALE`:

```sql
SELECT mi.name, s.current_stock, c.quantity_base, t.delta, t.reason
FROM order_item_consumptions c
JOIN master_ingredients mi ON mi.id = c.master_ingredient_id
JOIN ingredient_stocks s ON s.id = c.ingredient_stock_id
JOIN inventory_transactions t ON t.id = c.inventory_transaction_id
WHERE c.order_id = '<order-id>'
ORDER BY mi.name;
```

### Stock actual de la data de prueba

```sql
SELECT mi.name, s.current_stock, s.min_stock, u.abbreviation
FROM ingredient_stocks s
JOIN master_ingredients mi ON mi.id = s.master_ingredient_id
JOIN units u ON u.id = mi.base_unit_id
WHERE mi.name LIKE '%PRUEBA STOCK'
ORDER BY mi.name;
```

### Catálogo publicado

```sql
SELECT m.name AS menu,
       sm.name AS submenu,
       n.node_type,
       COALESCE(i.name, t.name) AS item
FROM submenu_nodes n
JOIN submenus sm ON sm.id = n.submenu_id
JOIN menus m ON m.id = sm.menu_id
LEFT JOIN items i ON i.id = n.item_id
LEFT JOIN templates t ON t.id = n.template_id
WHERE m.name = 'Menu pruebas de stock'
ORDER BY sm.sort_order, n.sort_order;
```