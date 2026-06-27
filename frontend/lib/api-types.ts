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

export type StaffAccountStatus = "ACTIVE" | "SUSPENDED";
export type StaffAssignmentStatus = "ACTIVE" | "SUSPENDED" | "REMOVED";
export type StaffInvitationStatus = "PENDING" | "EXPIRED" | "ACCEPTED" | "REVOKED";

export interface PermissionCatalogResponse {
    module: string;
    label: string;
    actions: string[];
}

export interface StaffRoleResponse {
    id: string;
    name: string;
    permissions: PermissionMap;
    presetKey: string | null;
    active: boolean;
    assignedUsers: number;
}


export interface StaffAssignment {
    restaurantId: string;
    restaurantName: string;
    roleId: string;
    roleName: string;
    assignmentStatus: StaffAssignmentStatus;
}

export interface StaffMemberResponse {
    userId: string;
    email: string;
    fullName: string;
    accountStatus: StaffAccountStatus;
    lastLoginAt: string | null;
    assignments: StaffAssignment[];
}

export interface StaffInvitationResponse {
    id: string;
    restaurantId: string;
    restaurantName: string;
    roleId: string;
    roleName: string;
    email: string;
    status: StaffInvitationStatus;
    expiresAt: string;
    acceptedAt: string | null;
    revokedAt: string | null;
    invitationPath: string | null;
    createdAt: string;
}

export interface StaffInvitationRequest {
    email: string;
    roleId: string;
}

export interface StaffAssignmentRequest {
    roleId?: string;
    status: Exclude<StaffAssignmentStatus, "REMOVED">;
}

export interface AcceptStaffInvitationRequest {
    firstName: string;
    secondName?: string;
    firstLastname: string;
    secondLastname?: string;
    phoneNumber?: string;
    username?: string;
    password: string;
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
    timeZone: string;
    prepaidOrderExpirationMinutes: number;
    cashCountMode: "BLIND" | "VISIBLE";
}

export interface ReportServiceTypeTotal {
    serviceType: string
    orderCount: number
    grossSales: number
}

export interface ReportOverview {
    dateFrom: string
    dateTo: string
    provisional: boolean
    grossSales: number
    completedSales: number
    openOrderValue: number
    completedOrders: number
    averageTicket: number
    collected: number
    tips: number
    refunds: number
    netCollected: number
    serviceTypes: ReportServiceTypeTotal[]
    restaurants: Array<{
        restaurantId: string
        restaurantName: string
        grossSales: number
        collected: number
        completedOrders: number
    }>
}

export interface ReportSeriesPoint {
    period: string
    grossSales: number
    completedSales: number
    collected: number
    refunds: number
    orderCount: number
}

export interface SalesSeriesReport {
    provisional: boolean
    points: ReportSeriesPoint[]
}

export interface PaymentMethodReportRow {
    restaurantId: string
    restaurantName: string
    paymentMethodId: string
    methodCode: string
    methodName: string
    methodType: string
    paymentCount: number
    collected: number
    tips: number
    refunds: number
    netCollected: number
}

export interface BusinessDayReportRow {
    id: string
    restaurantId: string
    restaurantName: string
    businessDate: string
    timeZone: string
    status: "OPEN" | "CLOSED"
    provisional: boolean
    closureSequence: number
    paymentsTotal: number
    tipsTotal: number
    refundsTotal: number
    expectedCash: number | null
    countedCash: number | null
    difference: number | null
    openedAt: string
    closedAt: string | null
}

export interface BusinessDayDetailReport {
    id: string
    restaurantId: string
    restaurantName: string
    businessDate: string
    timeZone: string
    status: "OPEN" | "CLOSED"
    provisional: boolean
    events: Array<{
        type: string
        reason: string | null
        occurredAt: string
        occurredBy: string
    }>
    closures: Array<{
        id: string
        sequence: number
        payments: number
        tips: number
        refunds: number
        cashIn: number
        cashOut: number
        expectedCash: number
        countedCash: number
        difference: number
        notes: string | null
        closedAt: string
    }>
    sessions: Array<{
        id: string
        registerName: string
        status: string
        openingAmount: number
        expectedCash: number | null
        countedCash: number | null
        difference: number | null
        differenceReason: string | null
        openedAt: string
        closedAt: string | null
    }>
}

export interface CatalogReportRow {
    restaurantId: string
    restaurantName: string
    referenceId: string
    name: string
    lineType: "PRODUCT" | "TEMPLATE"
    soldQuantity: number
    canceledQuantity: number
    grossSales: number
    theoreticalCost: number | null
    theoreticalMargin: number | null
    costComplete: boolean
    optionBreakdown: string | null
}

export interface InventoryConsumptionReportRow {
    restaurantId: string
    restaurantName: string
    ingredientId: string
    ingredientName: string
    quantityBase: number
    historicalCost: number | null
    costComplete: boolean
}

export interface MissingCostIngredient {
    restaurantId: string
    restaurantName: string
    ingredientId: string
    ingredientName: string
    currentStock: number
}

export interface InventoryValuationReport {
    knownValue: number
    valuedIngredientCount: number
    missingCostIngredientCount: number
    missingCostIngredients: MissingCostIngredient[]
}

export interface LowStockReportRow {
    restaurantId: string
    restaurantName: string
    ingredientId: string
    ingredientName: string
    currentStock: number
    minStock: number
    shortage: number
}

export interface RestaurantRequest {
    name: string;
    address?: string;
    email?: string;
    phoneNumber?: string;
    operationMode: 'PREPAID' | 'POSTPAID';
    isActive?: boolean;
    settings: RestaurantSettings;
}

export interface RestaurantUpdateRequest {
    name?: string;
    address?: string;
    email?: string;
    phoneNumber?: string;
    operationMode?: 'PREPAID' | 'POSTPAID';
    isActive?: boolean;
    settings?: Partial<RestaurantSettings>;
}

export type RestaurantResponse = {
    id: string;
    name: string;
    address?: string | null;
    email?: string | null;
    phoneNumber?: string | null;
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
    countedCash: number;
    differenceReason?: string;
    notes?: string;
}

export interface CashSessionListResponse extends CashSessionResponse {
    restaurantName: string;
    cashRegisterName: string;
    expectedCash: number | null;
    differenceAmount: number | null;
    differenceReason: string | null;
}

export type BusinessDayStatus = "OPEN" | "CLOSED";

export interface BusinessDayResponse {
    id: string;
    restaurantId: string;
    businessDate: string;
    timeZone: string;
    status: BusinessDayStatus;
    openedAt: string;
    openedBy: string;
    closedAt: string | null;
    closedBy: string | null;
    openSessionCount: number;
    pendingOrderCount: number;
    missingReconciliationCount: number;
    unexplainedDifferenceCount: number;
}

export type CashMovementDirection = "IN" | "OUT";
export type CashMovementReason = "CHANGE_FUND" | "SAFE_DROP" | "PETTY_EXPENSE" | "CORRECTION" | "OTHER";
export type CashMovementStatus = "RECORDED" | "VOIDED";

export interface CashMovementRequest {
    direction: CashMovementDirection;
    reason: CashMovementReason;
    amount: number;
    notes?: string;
}

export interface CashMovementResponse extends CashMovementRequest {
    id: string;
    restaurantId: string;
    businessDayId: string;
    cashSessionId: string;
    status: CashMovementStatus;
    createdAt: string;
    createdBy: string;
    createdDeviceId: string;
    voidedAt: string | null;
    voidedBy: string | null;
    voidReason: string | null;
}

export interface PaymentTotalResponse {
    paymentMethodId: string | null;
    methodCode: string;
    methodName: string;
    methodType: PaymentMethodType;
    paymentCount: number;
    paymentAmount: number;
    tipAmount: number;
    refundAmount: number;
    netAmount: number;
}

export interface CashSessionReconciliationResponse {
    id: string | null;
    restaurantId: string;
    businessDayId: string;
    cashSessionId: string;
    openingAmount: number;
    paymentsTotal: number | null;
    tipsTotal: number | null;
    refundsTotal: number | null;
    cashInTotal: number | null;
    cashOutTotal: number | null;
    expectedCash: number | null;
    countedCash: number | null;
    differenceAmount: number | null;
    differenceReason: string | null;
    notes: string | null;
    closedAt: string | null;
    closedBy: string | null;
    closedDeviceId: string | null;
    blind: boolean;
    closed: boolean;
    paymentTotals: PaymentTotalResponse[];
}

export interface BusinessDayClosureResponse {
    id: string;
    restaurantId: string;
    businessDayId: string;
    closureSequence: number;
    paymentsTotal: number;
    tipsTotal: number;
    refundsTotal: number;
    cashInTotal: number;
    cashOutTotal: number;
    expectedCashTotal: number;
    countedCashTotal: number;
    differenceTotal: number;
    notes: string | null;
    closedAt: string;
    closedBy: string;
    paymentTotals: PaymentTotalResponse[];
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
    brandName: string | null;
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
    currentStock: number;
    costComplete: boolean;
    activeSupplier: ActiveSupplierInfo | null;
}

// ── Suppliers ──

export interface SupplierResponse {
    id: string;
    name: string;
    documentTypeId: string;
    documentNumber: string;
    contactName: string | null;
    email: string | null;
    phone: string | null;
    address: string | null;
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
    sourceName: string | null;
    unitName: string | null;
    unitAbbreviation: string | null;
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
    sellableUnitsPerBatch: number | null;
    portionSize: number | null;
    portionUnitId: string | null;
    portionUnitAbbreviation: string | null;
    batchTheoreticalCost: number | null;
    salePrice: number | null;
    theoreticalCost: number | null;
    costComplete: boolean;
    missingCostIngredients: string[];
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
    sellableUnitsPerBatch?: number;
}

export interface UpdateItemRequest {
    name?: string;
    description?: string;
    salePrice?: number;
    yieldQty?: number;
    yieldUnitId?: string;
    sellableUnitsPerBatch?: number;
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
export type PaymentStatus = "UNPAID" | "PARTIALLY_PAID" | "PAID" | "REFUND_PENDING" | "REFUNDED";
export type ServiceType = "DINE_IN" | "TAKEOUT" | "DELIVERY";
export type DeliveryStatus = "NOT_APPLICABLE" | "PENDING_DISPATCH" | "OUT_FOR_DELIVERY" | "DELIVERED" | "CANCELED";
export type OrderLineType = "PRODUCT" | "TEMPLATE";
export type CatalogReferenceType = "PRODUCT" | "TEMPLATE";
export type PaymentMethodType = "CASH" | "DEBIT_CARD" | "CREDIT_CARD" | "NEQUI" | "DAVIPLATA" | "INTERNAL_CREDIT" | "OTHER";
export type PaymentRecordStatus = "RECORDED" | "VOIDED" | "REFUNDED";
export type PaymentRefundStatus = "RECORDED" | "VOIDED";
export type OrderItemInventoryDisposition = "RELEASE_RESERVED" | "NO_RESTOCK" | "RESTOCK" | "WASTE";
export type BillPaymentStatus = "UNPAID" | "PARTIALLY_PAID" | "PAID";
export type BillSplitMode = "ITEM_QUANTITY" | "AMOUNT" | "PERCENTAGE" | "EVENLY";
export type PaymentPendingState = "ACTIVE" | "EXPIRED";

export interface PosTemplateOptionResponse {
    slotOptionId: string;
    itemId: string;
    itemName: string;
    surcharge: number;
    maxQuantity: number;
    isDefault: boolean;
    available: boolean;
    lowStock: boolean;
    maxAvailableUnits: number | null;
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
    maxAvailableUnits: number | null;
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
    refundDueSnapshot: number;
    refundedTotalSnapshot: number;
    paidTotal: number;
    remainingBalance: number;
    paymentExpiresAt: string | null;
    paymentExpiredAt: string | null;
    paymentExpired: boolean;
    prepaidOrderExpirationMinutes: number;
    createdAt: string;
    updatedAt: string;
}

export interface TemplateOptionSnapshotResponse {
    id: string;
    slotOptionId: string;
    itemId: string;
    itemNameSnapshot: string;
    quantity: number;
    surchargeSnapshot: number;
    theoreticalCostSnapshot: number | null;
}

export interface TemplateSlotSnapshotResponse {
    id: string;
    templateSlotId: string;
    slotNameSnapshot: string;
    minSelectionSnapshot: number;
    maxSelectionSnapshot: number;
    sortOrder: number;
    options: TemplateOptionSnapshotResponse[];
}

export interface OrderItemDetailResponse {
    id: string;
    lineType: OrderLineType;
    itemId: string | null;
    templateId: string | null;
    submenuNodeId: string | null;
    itemNameSnapshot: string;
    unitPriceSnapshot: number;
    theoreticalCostSnapshot: number | null;
    quantity: number;
    canceledQuantity: number;
    activeQuantity: number;
    subtotalGrossSnapshot: number;
    notes: string | null;
    templateSlots: TemplateSlotSnapshotResponse[];
    taxes: unknown[];
    cancellations: OrderItemCancellationResponse[];
}

export interface OrderItemCancellationResponse {
    id: string;
    quantity: number;
    grossAmount: number;
    reason: string;
    kitchenStatusSnapshot: KitchenStatus;
    inventoryDisposition: OrderItemInventoryDisposition;
    createdAt: string;
    createdBy: string | null;
    systemGenerated: boolean;
}

export interface KitchenTicketLineResponse {
    id: string;
    orderItemId: string;
    lineType: OrderLineType;
    quantity: number;
    canceledQuantity: number;
    activeQuantity: number;
    itemNameSnapshot: string;
    notes: string | null;
    templateSlots: TemplateSlotSnapshotResponse[];
}

export interface KitchenTicketResponse {
    id: string;
    orderId: string;
    orderNumber: string | null;
    orderPublicCode: string | null;
    orderDisplayCode: string | null;
    customerName: string | null;
    status: KitchenTicketStatus;
    sentAt: string;
    startedAt: string | null;
    readyAt: string | null;
    canceledAt: string | null;
    lines: KitchenTicketLineResponse[];
}


export interface KitchenTicketStreamEvent {
    eventType: "kitchen.ticket.created" | "kitchen.ticket.updated" | "kitchen.ticket.status_changed";
    restaurantId: string;
    ticketId: string;
    orderId: string;
    orderDisplayCode: string | null;
    customerName: string | null;
    status: KitchenTicketStatus;
    occurredAt: string;
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
    refunds: OrderRefundResponse[];
}

export interface OrderRefundResponse {
    id: string;
    paymentId: string;
    cashSessionId: string;
    amount: number;
    status: PaymentRefundStatus;
    reason: string;
    externalReference: string | null;
    createdAt: string;
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

export interface CancelOrderItemRequest {
    quantity: number;
    reason: string;
    inventoryDisposition?: OrderItemInventoryDisposition;
}

export interface CancelOrderRequest {
    reason: string;
    lineDecisions: {
        orderItemId: string;
        inventoryDisposition?: OrderItemInventoryDisposition;
    }[];
}

export interface PaymentRefundRequest {
    paymentId: string;
    amount: number;
    reason: string;
    reference?: string;
}

export interface PaymentRefundResponse {
    id: string;
    restaurantId: string;
    orderId: string;
    paymentId: string;
    cashSessionId: string;
    amount: number;
    status: PaymentRefundStatus;
    reason: string;
    externalReference: string | null;
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






