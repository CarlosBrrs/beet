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
    INVOICES = "INVOICES",
    OPERATIONS = "OPERATIONS",
    RESTAURANTS = "RESTAURANTS",
    SUBSCRIPTION = "SUBSCRIPTION",
    MENUS = "MENUS",
    PREPARATIONS = "PREPARATIONS",
    PRODUCTS = "PRODUCTS",
    TEMPLATES = "TEMPLATES",
    STAFF = "STAFF"
}


export enum PermissionAction {
    VIEW = "VIEW",
    VIEW_ALL = "VIEW_ALL",
    CREATE = "CREATE",
    EDIT = "EDIT",
    DELETE = "DELETE",
    MANAGE = "MANAGE",
    ACTIVATE = "ACTIVATE",
    OPEN = "OPEN",
    CLOSE = "CLOSE",
    VOID = "VOID",
    COMMENT = "COMMENT",
    PROCESS = "PROCESS",
    CANCEL = "CANCEL",
    COMPLETE = "COMPLETE",
    REFUND = "REFUND",
    UPDATE_STATUS = "UPDATE_STATUS"
}

export type PermissionMap = Partial<Record<PermissionModule, PermissionAction[]>>
