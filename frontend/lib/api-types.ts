export interface ApiGenericResponse<T> {
    success: boolean;
    data: T;
    timestamp: string; // ISO 8601
    errorMessage?: string;
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

export interface UserRestaurantPermissions {
    restaurantId: string;
    userId: string;
    roleId: string;
    roleName: string;
    permissions: PermissionMap;
}

export type UserRestaurantPermissionsResponse = UserRestaurantPermissions;

export interface RestaurantSettings {
    prePaymentEnabled: boolean;
    allowTakeaway: boolean;
    allowDelivery: boolean;
    maxTableCapacity: number;
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

export interface Ingredient {
    id: string;
    restaurantId: string;
    name: string;
    unit: string; // 'kg', 'g', 'l', 'unit'
    cost: number;
    currentStock: number;
    // Add other fields as needed
}

export type CreateIngredientRequest = Omit<Ingredient, 'id' | 'restaurantId' | 'currentStock'>;
export type UpdateIngredientRequest = Partial<CreateIngredientRequest>;

