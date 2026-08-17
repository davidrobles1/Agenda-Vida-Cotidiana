package com.vidacotidiana.app.core.mock

/**
 * UX-006: mock data for the 6 scaffolding-only modules authorized by the
 * user (Documentos, Inventario, Garantías, Mantenimiento, Suscripciones,
 * Familia) — presentation only, no backend, no persistence. Kept in its own
 * file, referenced only from each module's ViewModel/State, never inlined
 * into a composable, so it's obvious at a glance what's mock vs. real when
 * reading a screen file.
 */

data class MockDocument(val id: String, val name: String, val category: String, val date: String, val sizeLabel: String)
data class MockDocumentCategory(val name: String, val count: Int)

data class MockWarranty(val id: String, val product: String, val category: String, val expiresLabel: String, val status: WarrantyStatus)
enum class WarrantyStatus { VIGENTE, POR_VENCER, VENCIDA }

data class MockInventoryItem(val id: String, val name: String, val category: String, val status: String)

data class MockMaintenanceRecord(val id: String, val item: String, val task: String, val lastDoneLabel: String, val nextDueLabel: String, val status: MaintenanceStatus)
enum class MaintenanceStatus { AL_DIA, PROXIMO, VENCIDO }

data class MockSubscription(val id: String, val name: String, val category: String, val priceLabel: String, val renewsLabel: String)

data class MockFamilyMember(val id: String, val name: String, val relationship: String)

object MockData {
    val documentCategories = listOf(
        MockDocumentCategory("Identificación", 5),
        MockDocumentCategory("Comprobantes", 8),
        MockDocumentCategory("Seguros", 4),
        MockDocumentCategory("Contratos", 3),
        MockDocumentCategory("Otros", 4),
    )

    val documents = listOf(
        MockDocument("doc-1", "INE_David.pdf", "Identificación", "02 May 2024", "1.2 MB"),
        MockDocument("doc-2", "Pasaporte.pdf", "Identificación", "15 Abr 2024", "2.1 MB"),
        MockDocument("doc-3", "Poliza_Seguro_Auto.pdf", "Seguros", "10 Abr 2024", "1.8 MB"),
        MockDocument("doc-4", "Recibo_Luz_Abril.pdf", "Comprobantes", "05 Abr 2024", "1.1 MB"),
    )

    val warranties = listOf(
        MockWarranty("war-1", "Laptop Dell XPS 13", "Electrónicos", "12 Mayo 2026", WarrantyStatus.POR_VENCER),
        MockWarranty("war-2", "Lavadora Samsung", "Hogar", "20 Ago 2026", WarrantyStatus.VIGENTE),
        MockWarranty("war-3", "Refrigerador LG", "Hogar", "01 Ene 2026", WarrantyStatus.VENCIDA),
        MockWarranty("war-4", "Auto Toyota Corolla", "Vehículos", "30 Sep 2027", WarrantyStatus.VIGENTE),
    )

    val inventoryCategories = listOf("Todos", "Hogar", "Electrónicos", "Vehículos")

    val inventoryItems = listOf(
        MockInventoryItem("inv-1", "Laptop Dell XPS 13", "Electrónicos", "En uso"),
        MockInventoryItem("inv-2", "Lavadora Samsung", "Hogar", "En uso"),
        MockInventoryItem("inv-3", "Sofá Sala", "Hogar", "En uso"),
        MockInventoryItem("inv-4", "Auto Toyota Corolla", "Vehículos", "En uso"),
    )

    val maintenanceRecords = listOf(
        MockMaintenanceRecord("mnt-1", "Auto Toyota Corolla", "Cambio de aceite", "15 Feb 2026", "15 Ago 2026", MaintenanceStatus.PROXIMO),
        MockMaintenanceRecord("mnt-2", "Lavadora Samsung", "Limpieza de filtro", "01 Ene 2026", "01 Jul 2026", MaintenanceStatus.AL_DIA),
        MockMaintenanceRecord("mnt-3", "Refrigerador LG", "Revisión general", "10 Nov 2025", "10 May 2026", MaintenanceStatus.VENCIDO),
    )

    val subscriptions = listOf(
        MockSubscription("sub-1", "Netflix", "Entretenimiento", "$219/mes", "20 Ago 2026"),
        MockSubscription("sub-2", "Spotify", "Entretenimiento", "$115/mes", "05 Ago 2026"),
        MockSubscription("sub-3", "Almacenamiento en la nube", "Servicios", "$99/mes", "12 Ago 2026"),
    )

    val familyMembers = listOf(
        MockFamilyMember("fam-1", "Ana García", "Esposa"),
        MockFamilyMember("fam-2", "María García", "Hija"),
        MockFamilyMember("fam-3", "Carlos García", "Hijo"),
    )
}
