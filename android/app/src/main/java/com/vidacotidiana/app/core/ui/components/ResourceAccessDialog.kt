package com.vidacotidiana.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vidacotidiana.app.core.data.FamilyMember
import com.vidacotidiana.app.core.data.ShareableKind
import com.vidacotidiana.app.core.data.SharedResource
import com.vidacotidiana.app.core.data.UserSearchResult
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme

/**
 * QUIÉN TIENE ACCESO A ESTE RECURSO — el diálogo que faltaba.
 *
 * EL HUECO QUE CIERRA. El backend sabe compartir seis tipos de recurso con
 * gente de la familia desde que existe ADR-025, pero el móvil solo declaraba
 * las dos llamadas de LECTURA: se veía lo que a uno le habían compartido y no
 * había manera de compartir nada ni de retirárselo a nadie. Lo único que
 * existía era invitar por correo a una tarea, que es otra mecánica y solo
 * sirve para tareas.
 *
 * NO SE ESCRIBE UN DESTINATARIO A CIEGAS. Un campo de texto libre admite
 * correos que no existen, se equivoca por una letra y no dice nada hasta que
 * el servidor falla. Aquí se ELIGE de una lista:
 *
 *  · arriba, los integrantes de la familia —que son con quien se comparte casi
 *    siempre y ya están confirmados por ambas partes—;
 *  · debajo, una búsqueda en el padrón para alguien que aún no es familia. El
 *    backend exige cinco caracteres y no devuelve nada por debajo de eso
 *    (SEC-001, para que no se pueda enumerar a los usuarios letra a letra), y
 *    el diálogo lo dice en vez de parecer roto.
 *
 * Quien ya tiene acceso NO vuelve a aparecer en el selector: ofrecerlo otra
 * vez solo lleva al 409 del servidor.
 *
 * «SU PARTE» NO ES UN PERMISO. Es un compromiso, y hay recursos que no lo
 * admiten —un artículo se posee, un documento es de solo ver—. En esos tipos
 * el interruptor no se dibuja, porque el servidor rechazaría la petición
 * ([ShareableKind.supportsResponsibility]).
 */
@Composable
fun ResourceAccessDialog(
    kind: ShareableKind,
    resourceTitle: String,
    shares: List<SharedResource>,
    loading: Boolean,
    family: List<FamilyMember>,
    searchResults: List<UserSearchResult>,
    searching: Boolean,
    onSearch: (String) -> Unit,
    onGrant: (userId: String, responsibility: Boolean) -> Unit,
    onRevoke: (shareId: String) -> Unit,
    onSetResponsibility: (shareId: String, responsibility: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = VidaTheme.colors
    val t = VidaTheme.type
    var query by remember { mutableStateOf("") }
    var conParte by remember { mutableStateOf(kind.supportsResponsibility) }

    // La búsqueda se limpia al cerrar: dejarla viva enseñaría los resultados
    // de otro recurso la próxima vez que se abra.
    LaunchedEffect(Unit) { onSearch("") }

    val yaTienen = shares.mapNotNull { it.counterpartUserId }.toSet()
    val candidatosFamilia = family.filter { it.userId !in yaTienen }
    val candidatosBusqueda = searchResults.filter { u ->
        u.userId !in yaTienen && candidatosFamilia.none { it.userId == u.userId }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surfaceVariant,
        title = { Text("Quién tiene acceso", style = t.cardTitle, color = c.text) },
        text = {
            Column(
                Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(VidaSpacing.sm),
            ) {
                Text(
                    "«$resourceTitle» · ${kind.label}",
                    style = t.micro,
                    color = c.textTertiary,
                )

                /* ── QUIÉN LO TIENE YA ──────────────────────────────────── */
                when {
                    loading && shares.isEmpty() ->
                        Text("Consultando…", style = t.caption, color = c.textSecondary)
                    shares.isEmpty() ->
                        Text(
                            "Todavía no lo has compartido con nadie.",
                            style = t.caption,
                            color = c.textSecondary,
                        )
                    else -> {
                        Text("CON ACCESO", style = t.eyebrow, color = c.textTertiary)
                        shares.forEach { share ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .background(c.surface, RoundedCornerShape(14.dp))
                                    .border(1.dp, c.line, RoundedCornerShape(14.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(9.dp),
                            ) {
                                Avatar(c.primary)
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        share.counterpart,
                                        style = t.caption.copy(fontWeight = FontWeight.SemiBold),
                                        color = c.text,
                                        maxLines = 1,
                                    )
                                    Text(
                                        when {
                                            !kind.supportsResponsibility -> "Puede verlo"
                                            share.partDone -> "Ya hizo su parte"
                                            share.responsibility -> "Le toca su parte"
                                            else -> "Solo lo ve"
                                        },
                                        style = t.micro,
                                        color = c.textTertiary,
                                        maxLines = 1,
                                    )
                                }
                                // Cambiar el compromiso SIN tener que quitar el
                                // acceso y volverlo a dar: es un PATCH que el
                                // backend ya tenía y nadie llamaba.
                                if (kind.supportsResponsibility) {
                                    TextButton(
                                        onClick = { onSetResponsibility(share.id, !share.responsibility) },
                                    ) {
                                        Text(
                                            if (share.responsibility) "Solo ver" else "Darle parte",
                                            style = t.micro,
                                        )
                                    }
                                }
                                Box(
                                    Modifier
                                        .size(30.dp)
                                        .background(c.error.copy(alpha = 0.10f), CircleShape)
                                        .vidaClickable(onClick = { onRevoke(share.id) }),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Outlined.Close,
                                        contentDescription = "Quitar el acceso a ${share.counterpart}",
                                        modifier = Modifier.size(15.dp),
                                        tint = c.error,
                                    )
                                }
                            }
                        }
                    }
                }

                /* ── A QUIÉN DÁRSELO ────────────────────────────────────── */
                Text("DAR ACCESO A", style = t.eyebrow, color = c.textTertiary)

                if (kind.supportsResponsibility) {
                    // Se marca ANTES de elegir a la persona, porque el gesto de
                    // elegirla es el que ya comparte: pedir a quién y luego con
                    // qué condición serían dos pasos donde basta uno.
                    VidaCheckRow(
                        label = "Se compromete a hacer su parte",
                        checked = conParte,
                        onToggle = { conParte = !conParte },
                    )
                }

                if (candidatosFamilia.isEmpty() && yaTienen.isEmpty() && family.isEmpty()) {
                    Text(
                        "Todavía no tienes a nadie en Familia. Invita a los tuyos desde Familia y podrás compartir con ellos.",
                        style = t.micro,
                        color = c.textTertiary,
                    )
                }

                candidatosFamilia.forEach { member ->
                    CandidateRow(
                        name = member.username,
                        hint = "De tu familia",
                        icon = true,
                        onClick = { onGrant(member.userId, conParte) },
                    )
                }

                // SOLO FAMILIA (ADR-025). La búsqueda en el padrón ofrecía a
                // cualquier cuenta, pero el servidor rechaza compartir con quien
                // no es de tu familia («Solo puedes compartir con integrantes de
                // tu familia»): era un camino que siempre terminaba en error, y
                // mezclaba la regla de Personal con gente de fuera. A alguien
                // nuevo se le invita primero a Familia.
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Listo") } },
    )
}

@Composable
private fun CandidateRow(name: String, hint: String, icon: Boolean, onClick: () -> Unit) {
    val c = VidaTheme.colors
    val t = VidaTheme.type
    Row(
        Modifier
            .fillMaxWidth()
            .background(c.surface, RoundedCornerShape(14.dp))
            .border(1.dp, c.line, RoundedCornerShape(14.dp))
            .vidaClickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Avatar(if (icon) c.second else c.textTertiary, family = icon)
        Column(Modifier.weight(1f)) {
            Text(name, style = t.caption.copy(fontWeight = FontWeight.SemiBold), color = c.text, maxLines = 1)
            Text(hint, style = t.micro, color = c.textTertiary, maxLines = 1)
        }
        Text("Dar acceso", style = t.micro.copy(fontWeight = FontWeight.SemiBold), color = c.primary)
    }
}

/** El disco con inicial — la gramática de «AVATARES Y PERSONAS» de la lámina. */
@Composable
private fun Avatar(tint: androidx.compose.ui.graphics.Color, family: Boolean = false) {
    Box(
        Modifier.size(30.dp).background(tint.copy(alpha = 0.14f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (family) Icons.Outlined.Group else Icons.Outlined.Person,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = tint,
        )
    }
}
