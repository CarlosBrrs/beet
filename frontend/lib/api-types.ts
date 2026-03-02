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

// ── Mock Ingredient types (used by list, detail, delete, adjust — still mocked) ──

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
