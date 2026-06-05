export interface ApiGenericResponse<T> {
    success: boolean;
    data: T;
    timestamp: string; // ISO 8601
    errorMessage?: string;
}

export interface PageResponse<T> {
    content: T[];
    pageable: {
        pageNumber: number;
        pageSize: number;
        sort: {
            empty: boolean;
            sorted: boolean;
            unsorted: boolean;
        };
        offset: number;
        paged: boolean;
        unpaged: boolean;
    };
    last: boolean;
    totalElements: number;
    totalPages: number;
    first: boolean;
    size: number;
    number: number;
    sort: {
        empty: boolean;
        sorted: boolean;
        unsorted: boolean;
    };
    numberOfElements: number;
    empty: boolean;
}

export interface SubscriptionPlanFeatures {
    max_restaurants: number;
    max_employees: number;
    advanced_reporting: boolean;
    priority_support: boolean;
    multi_user_access: boolean;
}

export type BillingCycle = 'MONTHLY' | 'YEARLY'; // Adjust based on backend enum if needed

export interface SubscriptionPlan {
    id: string; // UUID
    name: string;
    description: string;
    price: number;
    currency: string;
    interval: BillingCycle;
    features: SubscriptionPlanFeatures;
}

export interface RegisterUserRequest {
    email: string;
    password: string;
    firstName: string;
    secondName?: string;
    firstLastname: string;
    secondLastname?: string;
    phoneNumber: string;
    username: string;
    subscriptionPlanId: string; // UUID
}

export interface UserResponse {
    id: string;
    email: string;
    username: string;
    firstName: string;
    secondName?: string;
    firstLastname: string;
    secondLastname?: string;
    phoneNumber?: string;
    role?: string;
    // Add other fields from UserResponse java class if needed
}

export interface LoginRequest {
    email: string;
    password: string;
}

export interface LoginResponse {
    token: string;
    user: UserResponse;
}

// Permissions Types
import { PermissionMap } from "./permissions";

/**
 * A single permission scope returned by GET /auth/my-permissions.
 * - restaurantId === null  → global role (e.g. OWNER)
 * - restaurantId is set    → role scoped to a specific restaurant
 */
export interface UserPermissionEntry {
    restaurantId: string | null;
    role: string;
    permissions: PermissionMap;
}

/** @deprecated Use UserPermissionEntry from /auth/my-permissions instead */
export interface UserRestaurantPermissions {
    restaurantId: string;
    userId: string;
    roleId: string;
    roleName: string;
    permissions: PermissionMap;
}

/** @deprecated Use UserPermissionEntry from /auth/my-permissions instead */
export type UserRestaurantPermissionsResponse = UserRestaurantPermissions;

export interface RestaurantSettings {
    prePaymentEnabled: boolean;
    allowTakeaway: boolean;
    allowDelivery: boolean;
    maxTableCapacity: number;
    taxApplyMode: 'PER_INVOICE' | 'PER_ITEM';
    defaultTaxPercentage: number;
    timeZone?: string;
}

export interface RestaurantRequest {
    name: string;
    address?: string;
    email?: string;
    phoneNumber?: string;
    operationMode: 'PREPAID' | 'POSTPAID';
    settings: RestaurantSettings;
}

export type RestaurantResponse = {
    id: string;
    name: string;
    operationMode: 'PREPAID' | 'POSTPAID';
    isActive: boolean;
    ownerId: string;
    settings: RestaurantSettings;
    role: string; // "Owner", "Manager", etc.
}

// Cash registers

export interface CashRegisterResponse {
    id: string;
    restaurantId: string;
    name: string;
    deviceId: string | null;
    isActive: boolean;
    notes: string | null;
    createdAt: string;
    updatedAt: string;
}

export interface CreateCashRegisterRequest {
    name: string;
    notes?: string;
}

export interface UpdateCashRegisterRequest {
    name?: string;
    isActive?: boolean;
    notes?: string;
}

export type CashSessionStatus = "OPEN" | "CLOSED";

export interface CashSessionResponse {
    id: string;
    restaurantId: string;
    cashRegisterId: string;
    status: CashSessionStatus;
    openedAt: string;
    openedBy: string;
    openedDeviceId: string;
    openingAmount: number;
    closedAt: string | null;
    closedBy: string | null;
    closedDeviceId: string | null;
    closingAmount: number | null;
    notes: string | null;
}

export interface OpenCashSessionRequest {
    cashRegisterId: string;
    openingAmount: number;
    notes?: string;
}

export interface CloseCashSessionRequest {
    closingAmount: number;
    notes?: string;
}

export interface CashSessionListResponse extends CashSessionResponse {
    restaurantName: string;
    cashRegisterName: string;
}

// ── Mock Ingredient types (used by list, detail, delete, adjust — still mocked) ──

export type TableAvailabilityStatus = "AVAILABLE" | "OCCUPIED" | "INACTIVE";

export interface RestaurantTableResponse {
    id: string;
    restaurantId: string;
    name: string;
    capacity: number;
    area: string | null;
    sortOrder: number;
    isActive: boolean;
    notes: string | null;
    availabilityStatus: TableAvailabilityStatus;
    openOrderId: string | null;
    createdAt: string;
    updatedAt: string;
}

export interface CreateRestaurantTableRequest {
    name: string;
    capacity: number;
    area?: string;
    sortOrder?: number;
    notes?: string;
}

export interface UpdateRestaurantTableRequest {
    name?: string;
    capacity?: number;
    area?: string;
    sortOrder?: number;
    isActive?: boolean;
    notes?: string;
}

export interface MockIngredient {
    id: string;
    restaurantId: string;
    name: string;
    unit: string; // 'kg', 'g', 'l', 'unit'
    cost: number;
    currentStock: number;
}

export type MockCreateIngredientRequest = Omit<MockIngredient, 'id' | 'restaurantId' | 'currentStock'>;
export type MockUpdateIngredientRequest = Partial<MockCreateIngredientRequest>;

// ── Units (real, from GET /units) ──

export type UnitType = "MASS" | "VOLUME" | "UNIT";

export interface UnitResponse {
    id: string;
    name: string;
    abbreviation: string;
    type: UnitType;
    factorToBase: number;
    isBase: boolean;
}

// ── Ingredient Creation (real, matches backend DTOs) ──

export interface DocumentTypeResponse {
    id: string;
    name: string;
    description: string | null;
}

export interface MasterIngredientPayload {
    name: string;
    baseUnitId: string;
}

export interface SupplierPayload {
    id: string | null; // null = quick-add new supplier
    name?: string;
    documentTypeId?: string;
    documentNumber?: string;
}

export interface SupplierItemPayload {
    brandName?: string;
    purchaseUnitName: string;
    conversionFactor: number;
    conversionUnitId: string;
    totalPrice: number;
}

export interface CreateIngredientRequest {
    masterIngredient: MasterIngredientPayload;
    supplier: SupplierPayload;
    supplierItem: SupplierItemPayload;
}

export interface SupplierItemInfo {
    id: string;
    supplierId: string;
    brandName: string;
    purchaseUnitName: string;
    conversionFactor: number;
    lastCostBase: number;
}

export interface IngredientResponse {
    id: string;
    name: string;
    baseUnitId: string;
    activeSupplierItemId: string;
    supplierItem: SupplierItemInfo;
}

// ── Ingredient Read Endpoints (Real) ──

export interface IngredientListResponse {
    id: string;
    name: string;
    baseUnitId: string;
    unitAbbreviation: string;
    costPerBaseUnit: number | null;
}

export interface ActiveSupplierInfo {
    supplierId: string;
    supplierName: string;
    supplierItemId: string;
    brandName: string;
    purchaseUnitName: string;
    conversionFactor: number;
    lastCostBase: number;
}

export interface IngredientDetailResponse {
    id: string;
    name: string;
    baseUnitId: string;
    unitName: string;
    unitAbbreviation: string;
    costPerBaseUnit: number | null;
    activeSupplier: ActiveSupplierInfo | null;
}

// ── Suppliers ──

export interface SupplierResponse {
    id: string;
    name: string;
    documentTypeId: string;
    documentNumber: string;
    contactName: string;
    email: string;
    phone: string;
    address: string;
    isActive: boolean;
}

// ── Inventory (real, matches backend DTOs) ──

export interface InventoryStockResponse {
    id: string;
    masterIngredientId: string;
    ingredientName: string;
    unitAbbreviation: string;
    currentStock: number;
    minStock: number;
    lowStock: boolean;
}

export interface ActivateIngredientInventoryRequest {
    masterIngredientId: string;
    initialStock: number;
    minStock?: number;
}

export type AdjustmentMode = "REPLACE" | "DELTA";
export type TransactionReason = "ADJUSTMENT" | "WASTE" | "CORRECTION" | "INITIAL" | "PURCHASE" | "SALE";

export interface AdjustStockRequest {
    mode: AdjustmentMode;
    value: number;
    reason: TransactionReason;
    notes?: string;
}

export interface InventoryTransactionResponse {
    id: string;
    delta: number;
    reason: TransactionReason;
    invoiceId: string | null;
    previousStock: number;
    resultingStock: number;
    notes: string | null;
    createdAt: string; // ISO 8601
}

// ── Invoice types ──

export interface RegisterInvoiceRequest {
    supplierId: string;
    supplierInvoiceNumber: string;
    emissionDate: string; // YYYY-MM-DD
    notes?: string;
    taxPercentage?: number;       // For PER_INVOICE mode
    items: RegisterInvoiceItemRequest[];
}

export interface RegisterInvoiceItemRequest {
    supplierItemId: string;
    quantityPurchased: number;
    unitPricePurchased: number;
    taxPercentage?: number;       // For PER_ITEM mode
    conversionFactorUsed: number;
}

export interface InvoiceResponse {
    id: string;
    supplierName: string | null;
    supplierInvoiceNumber: string;
    emissionDate: string;
    receivedAt: string;
    totalAmount: number;
    itemCount: number;
    status: string;
}

export interface InvoiceDetailResponse {
    id: string;
    supplierName: string;
    supplierInvoiceNumber: string;
    emissionDate: string;
    receivedAt: string;
    subtotal: number;
    totalTax: number;
    totalAmount: number;
    notes: string | null;
    status: string;
    items: InvoiceItemDetailResponse[];
}

export interface InvoiceItemDetailResponse {
    id: string;
    ingredientName: string;
    purchaseUnitName: string;
    conversionFactorUsed: number;
    baseUnitAbbreviation: string;
    quantityPurchased: number;
    unitPricePurchased: number;
    taxPercentage: number;
    subtotal: number;
    taxAmount: number;
    costPerBaseUnit: number;
}

export interface SupplierItemForInvoiceResponse {
    id: string;
    brandName: string;
    purchaseUnitName: string;
    conversionFactor: number;
    lastCostBase: number | null;
    masterIngredientId: string;
    ingredientName: string;
    baseUnitAbbreviation: string;
}

// ── Menus ──

export interface SubmenuResponse {
    id: string;
    menuId: string;
    name: string;
    description: string;
    sortOrder: number;
    createdAt: string;
    updatedAt: string;
}

export interface MenuResponse {
    id: string;
    restaurantId: string;
    name: string;
    description: string;
    createdAt: string;
    updatedAt: string;
    submenus: SubmenuResponse[];
}

export interface CreateMenuRequest {
    name: string;
    description?: string;
}

export interface UpdateMenuRequest {
    name: string;
    description?: string;
}

export interface CreateSubmenuRequest {
    name: string;
    description?: string;
    sortOrder?: number;
}

export interface UpdateSubmenuRequest {
    name: string;
    description?: string;
    sortOrder?: number;
}

// ── Items — Preparations (Layer 1) & Products (Layer 2) ──

export type ItemClass = "PREPARATION" | "PRODUCT";
export type RecipeLineSource = "INGREDIENT" | "PREPARATION";

export interface RecipeLineResponse {
    id: string;
    source: RecipeLineSource;
    masterIngredientId: string | null;
    childItemId: string | null;
    quantity: number;
    unitId: string;
    sortOrder: number;
}

export interface ItemResponse {
    id: string;
    restaurantId: string;
    itemClass: ItemClass;
    name: string;
    description: string | null;
    isInventoryTracked: boolean;
    yieldQty: number | null;
    yieldUnitId: string | null;
    salePrice: number | null;
    theoreticalCost: number | null;
    isActive: boolean;
    isAvailableAsTemplateOption: boolean;
    isPublished: boolean;
    isUsedAsTemplateOption: boolean;
    recipeLines: RecipeLineResponse[];
    createdAt: string;
    updatedAt: string;
}

// Recipe lines: include source for both preparations and products
export interface RecipeLineRequest {
    source: RecipeLineSource;
    masterIngredientId?: string;   // when source = INGREDIENT
    childItemId?: string;          // when source = PREPARATION
    quantity: number;
    unitId: string;
}

// Backward-compatible alias for product-specific usages
export type ProductRecipeLineRequest = RecipeLineRequest;

export interface CreatePreparationRequest {
    name: string;
    description?: string;
    yieldQty: number;
    yieldUnitId: string;
    lines: RecipeLineRequest[];
}

export interface CreateProductRequest {
    name: string;
    description?: string;
    salePrice?: number;
    isInventoryTracked: boolean;
    isAvailableAsTemplateOption?: boolean;
    userDefinedCost?: number;              // Only for flat products
    lines?: RecipeLineRequest[];           // Only for products with recipe
    yieldQty?: number;
    yieldUnitId?: string;
}

export interface UpdateItemRequest {
    name?: string;
    description?: string;
    salePrice?: number;
    yieldQty?: number;
    yieldUnitId?: string;
    userDefinedCost?: number;
    isAvailableAsTemplateOption?: boolean;
    lines?: RecipeLineRequest[];
}

// ── Templates (Layer 3) ──

export interface SlotOptionRequest {
    itemId: string;
    surcharge?: number;
    isDefault?: boolean;
    maxQuantity: number;
    sortOrder?: number;
}

export interface SlotRequest {
    name: string;
    minSelection: number;
    maxSelection: number;
    sortOrder?: number;
    options: SlotOptionRequest[];
}

export interface CreateTemplateRequest {
    name: string;
    description?: string;
    basePrice: number;
    slots: SlotRequest[];
}

export interface SlotOptionResponse {
    id: string;
    itemId: string;
    surcharge: number;
    isDefault: boolean;
    maxQuantity: number;
    sortOrder: number;
}

export interface SlotResponse {
    id: string;
    name: string;
    minSelection: number;
    maxSelection: number;
    sortOrder: number;
    options: SlotOptionResponse[];
}

export interface TemplateResponse {
    id: string;
    restaurantId: string;
    name: string;
    description: string | null;
    basePrice: number;
    isActive: boolean;
    isPublished: boolean;
    slots: SlotResponse[];
    createdAt: string;
    updatedAt: string;
}

export type SubmenuNodeType = "PRODUCT" | "TEMPLATE";

export interface PublishSubmenuNodeRequest {
    nodeType: SubmenuNodeType;
    referenceId: string;
    sortOrder?: number;
}

export interface ProductDependenciesResponse {
    publications: {
        menuId: string;
        menuName: string;
        submenuId: string;
        submenuName: string;
    }[];
    templateUsages: {
        templateId: string;
        templateName: string;
        slotId: string;
        slotName: string;
    }[];
}

export interface SubmenuNodeResponse {
    id: string;
    submenuId: string;
    nodeType: SubmenuNodeType;
    itemId: string | null;
    templateId: string | null;
    sortOrder: number;
    item: ItemResponse | null;
    template: TemplateResponse | null;
}

// Orders / POS

export type OrderStatus = "DRAFT" | "AWAITING_PAYMENT" | "OPEN" | "COMPLETED" | "CANCELED";
export type KitchenStatus = "NOT_SENT" | "PENDING" | "PREPARING" | "PARTIALLY_READY" | "READY" | "ACCEPTED" | "SERVED";
export type KitchenTicketStatus = "PENDING" | "PREPARING" | "READY" | "CANCELED";
export type PaymentStatus = "UNPAID" | "PARTIALLY_PAID" | "PAID" | "REFUNDED";
export type ServiceType = "DINE_IN" | "TAKEOUT" | "DELIVERY";
export type DeliveryStatus = "NOT_APPLICABLE" | "PENDING_DISPATCH" | "OUT_FOR_DELIVERY" | "DELIVERED" | "CANCELED";
export type OrderLineType = "PRODUCT" | "TEMPLATE";
export type CatalogReferenceType = "PRODUCT" | "TEMPLATE";
export type PaymentMethodType = "CASH" | "DEBIT_CARD" | "CREDIT_CARD" | "NEQUI" | "DAVIPLATA" | "INTERNAL_CREDIT" | "OTHER";
export type PaymentRecordStatus = "RECORDED" | "VOIDED" | "REFUNDED";
export type BillPaymentStatus = "UNPAID" | "PARTIALLY_PAID" | "PAID";
export type BillSplitMode = "ITEM_QUANTITY" | "AMOUNT" | "PERCENTAGE" | "EVENLY";

export interface PosTemplateOptionResponse {
    slotOptionId: string;
    itemId: string;
    itemName: string;
    surcharge: number;
    maxQuantity: number;
    isDefault: boolean;
    available: boolean;
    lowStock: boolean;
    unavailableReason: string | null;
    sortOrder: number;
    insufficientIngredients: string[];
}

export interface PosTemplateSlotResponse {
    slotId: string;
    name: string;
    minSelection: number;
    maxSelection: number;
    sortOrder: number;
    options: PosTemplateOptionResponse[];
}

export interface PosCatalogResponse {
    nodeId: string;
    menuId: string;
    menuName: string;
    submenuId: string;
    submenuName: string;
    referenceType: CatalogReferenceType;
    referenceId: string;
    name: string;
    description: string | null;
    price: number;
    available: boolean;
    lowStock: boolean;
    unavailableReason: string | null;
    sortOrder: number;
    insufficientIngredients: string[];
    slots: PosTemplateSlotResponse[];
}

export interface TemplateOptionSelectionRequest {
    slotOptionId: string;
    itemId: string;
    quantity: number;
}

export interface TemplateSlotSelectionRequest {
    slotId: string;
    options: TemplateOptionSelectionRequest[];
}

export interface OrderItemRequest {
    lineType: OrderLineType;
    itemId?: string;
    templateId?: string;
    submenuNodeId: string;
    quantity: number;
    notes?: string;
    slots?: TemplateSlotSelectionRequest[];
}

export interface OrderCreateRequest {
    serviceType: ServiceType;
    tableId?: string;
    customerName?: string;
    customerPhone?: string;
    deliveryContactName?: string;
    deliveryPhone?: string;
    deliveryAddress?: string;
    deliveryNotes?: string;
    deliveryFee?: number;
    notes?: string;
    items: OrderItemRequest[];
}

export interface OrderResponse {
    id: string;
    restaurantId: string;
    cashSessionId: string | null;
    businessDate: string;
    dailySequence: number;
    orderNumber: string;
    publicCode: string;
    displayCode: string;
    orderStatus: OrderStatus;
    kitchenStatus: KitchenStatus;
    paymentStatus: PaymentStatus;
    serviceType: ServiceType;
    tableId: string | null;
    customerName: string | null;
    customerPhone: string | null;
    deliveryStatus: DeliveryStatus;
    subtotalGrossSnapshot: number;
    taxAmountSnapshot: number;
    totalGrossSnapshot: number;
    tipTotalSnapshot: number;
    createdAt: string;
    updatedAt: string;
}

export interface OrderItemDetailResponse {
    id: string;
    lineType: OrderLineType;
    itemId: string | null;
    templateId: string | null;
    submenuNodeId: string | null;
    itemNameSnapshot: string;
    unitPriceSnapshot: number;
    theoreticalCostSnapshot: number;
    quantity: number;
    subtotalGrossSnapshot: number;
    notes: string | null;
    templateSlots: {
        id: string;
        templateSlotId: string;
        slotNameSnapshot: string;
        minSelectionSnapshot: number;
        maxSelectionSnapshot: number;
        sortOrder: number;
        options: {
            id: string;
            slotOptionId: string;
            itemId: string;
            itemNameSnapshot: string;
            quantity: number;
            surchargeSnapshot: number;
            theoreticalCostSnapshot: number;
        }[];
    }[];
    taxes: unknown[];
}

export interface KitchenTicketLineResponse {
    id: string;
    orderItemId: string;
    quantity: number;
    itemNameSnapshot: string;
    notes: string | null;
}

export interface KitchenTicketResponse {
    id: string;
    orderId: string;
    orderNumber: string | null;
    orderPublicCode: string | null;
    orderDisplayCode: string | null;
    status: KitchenTicketStatus;
    sentAt: string;
    startedAt: string | null;
    readyAt: string | null;
    canceledAt: string | null;
    lines: KitchenTicketLineResponse[];
}

export interface OrderPaymentResponse {
    id: string;
    paymentMethodId: string;
    cashSessionId: string;
    amount: number;
    tipAmount: number;
    status: PaymentRecordStatus;
    externalReference: string | null;
    createdAt: string;
}

export interface OrderDetailResponse extends OrderResponse {
    originCashSessionId: string | null;
    originDeviceId: string | null;
    operationModeSnapshot: "PREPAID" | "POSTPAID";
    deliveryContactName: string | null;
    deliveryPhone: string | null;
    deliveryAddress: string | null;
    deliveryNotes: string | null;
    deliveryFee: number | null;
    prepaymentRequiredSnapshot: boolean;
    taxRateSnapshot: number;
    notes: string | null;
    items: OrderItemDetailResponse[];
    taxes: unknown[];
    kitchenTickets: KitchenTicketResponse[];
    payments: OrderPaymentResponse[];
}

export interface PaymentMethodResponse {
    id: string;
    restaurantId: string;
    code: string;
    name: string;
    type: PaymentMethodType;
    isActive: boolean;
    requiresReference: boolean;
    sortOrder: number;
    createdAt: string;
    updatedAt: string;
}

export interface PaymentMethodRequest {
    code?: string;
    name?: string;
    type?: PaymentMethodType;
    isActive?: boolean;
    requiresReference?: boolean;
    sortOrder?: number;
}

export interface PaymentRequest {
    paymentMethodId: string;
    orderBillId?: string;
    amount: number;
    tipAmount?: number;
    externalReference?: string;
    notes?: string;
}

export interface PaymentResponse {
    id: string;
    restaurantId: string;
    orderId: string;
    orderBillId: string | null;
    paymentMethodId: string;
    cashSessionId: string;
    amount: number;
    tipAmount: number;
    status: PaymentRecordStatus;
    externalReference: string | null;
    notes: string | null;
    createdAt: string;
}

export interface SplitBillsRequest {
    mode: BillSplitMode;
    bills: {
        label?: string;
        amount?: number;
        percentage?: number;
        items?: { orderItemId: string; quantity: number }[];
    }[];
}

export interface OrderBillResponse {
    id: string;
    orderId: string;
    label: string;
    splitMode: BillSplitMode;
    subtotalGrossSnapshot: number;
    tipTotalSnapshot: number;
    totalPaidSnapshot: number;
    paymentStatus: BillPaymentStatus;
    createdAt: string;
    updatedAt: string;
    allocations: {
        id: string;
        orderItemId: string;
        quantity: number;
        amount: number;
    }[];
}

