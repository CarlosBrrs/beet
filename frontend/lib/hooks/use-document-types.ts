import { useQuery } from "@tanstack/react-query"
import { apiClient } from "@/lib/api-client"
import { ApiGenericResponse, DocumentTypeResponse } from "@/lib/api-types"

export const DocumentTypeKeys = {
    all: ["document-types"] as const,
    byCountry: (countryCode: string) => [...DocumentTypeKeys.all, { countryCode }] as const,
}

async function fetchDocumentTypes(countryCode: string): Promise<DocumentTypeResponse[]> {
    if (!countryCode) return [] // Safety check

    // We expect the apiClient to throw if !res.ok, and we expect it to return the JSON body.
    // Need to cast or unwrap depending on how apiClient works here.
    const data = await apiClient<ApiGenericResponse<DocumentTypeResponse[]>>(
        `/document-types?countryCode=${countryCode}`
    )

    if (!data.success) throw new Error(data.errorMessage || "Failed to fetch document types")
    return data.data
}

export function useDocumentTypes(countryCode: string) {
    return useQuery({
        queryKey: DocumentTypeKeys.byCountry(countryCode),
        queryFn: () => fetchDocumentTypes(countryCode),
        enabled: !!countryCode, // Only run the query if countryCode is truthy
    })
}
