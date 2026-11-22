package com.knightmstr.onyxtv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knightmstr.onyxtv.data.AppModel
import com.knightmstr.onyxtv.data.PrefsCodec
import com.knightmstr.onyxtv.ui.theme.DarkSurfaceVariant
import com.knightmstr.onyxtv.ui.theme.FireAmber
import com.knightmstr.onyxtv.ui.theme.FireOrange
import com.knightmstr.onyxtv.ui.theme.TextPrimary
import com.knightmstr.onyxtv.ui.theme.TextSecondary

/** Lets the user give an app a custom name, or restore the original one. */
@Composable
fun RenameAppDialog(
    app: AppModel,
    onSave: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember(app.packageName) { mutableStateOf(app.label) }
    val fieldFocus = rememberInitialFocus()
    val fieldShape = RoundedCornerShape(12.dp)

    val save = {
        onSave(text)
        onDismiss()
    }

    TvDialogFrame(
        title = "Rename ${app.originalLabel}",
        subtitle = "Up to ${PrefsCodec.MAX_LABEL_LENGTH} characters",
        accentColor = Color(app.accentColor),
        onDismiss = onDismiss
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurfaceVariant, fieldShape)
                .border(BorderStroke(1.5.dp, FireOrange.copy(alpha = 0.7f)), fieldShape)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            BasicTextField(
                value = text,
                onValueChange = { text = it.take(PrefsCodec.MAX_LABEL_LENGTH) },
                singleLine = true,
                textStyle = TextStyle(color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(FireOrange),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { save() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(fieldFocus)
            )
        }

        TvMenuItem(
            title = "Save",
            icon = Icons.Rounded.Check,
            iconTint = FireAmber,
            enabled = text.isNotBlank(),
            onClick = save
        )

        if (app.isRenamed) {
            TvMenuItem(
                title = "Restore original name",
                subtitle = app.originalLabel,
                icon = Icons.Rounded.Restore,
                iconTint = TextSecondary,
                onClick = { onSave(null); onDismiss() }
            )
        }
    }
}
