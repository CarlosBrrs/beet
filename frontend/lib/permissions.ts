export enum PermissionModule {
    KDS = "KDS",
    CASH = "CASH",
    ORDERS = "ORDERS",
    TABLES = "TABLES",
    CATALOG = "CATALOG",
    FINANCE = "FINANCE",
    KITCHEN = "KITCHEN",
    RECIPES = "RECIPES",
    PAYMENTS = "PAYMENTS",
    INVENTORY = "INVENTORY",
    OPERATIONS = "OPERATIONS",
    RESTAURANTS = "RESTAURANTS",
    SUBSCRIPTION = "SUBSCRIPTION"
}

export enum PermissionAction {
    VIEW = "VIEW",
    VIEW_ALL = "VIEW_ALL",
    CREATE = "CREATE",
    EDIT = "EDIT",
    DELETE = "DELETE",
    MANAGE = "MANAGE",
    OPEN = "OPEN",
    CLOSE = "CLOSE",
    VOID = "VOID",
    COMMENT = "COMMENT",
    PROCESS = "PROCESS"
}

export type PermissionMap = Partial<Record<PermissionModule, PermissionAction[]>>
