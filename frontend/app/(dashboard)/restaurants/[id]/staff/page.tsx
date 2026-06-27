"use client"

import { Can } from "@/components/shared/can"
import { StaffWorkspace } from "@/components/modules/staff/staff-workspace"
import { PermissionAction, PermissionModule } from "@/lib/permissions"

export default function RestaurantStaffPage() {
    return (
        <Can I={PermissionAction.VIEW} a={PermissionModule.STAFF}>
            <StaffWorkspace mode="restaurant" />
        </Can>
    )
}
