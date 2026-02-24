package com.craftcv.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.craftcv.app.ui.theme.CraftColors
import com.craftcv.app.ui.theme.InterFamily

// ── Primary Button ──────────────────────────────────────────────────────────
@Composable
fun CraftButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: String = "",
) {
    Button(
        onClick  = onClick,
        enabled  = enabled && !isLoading,
        modifier = modifier.height(52.dp),
        shape    = RoundedCornerShape(10.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor         = CraftColors.InkPrimary,
            disabledContainerColor = CraftColors.Border,
            contentColor           = Color.White,
            disabledContentColor   = CraftColors.InkTertiary,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            if (icon.isNotEmpty()) {
                Text(icon, fontSize = 16.sp, color = LocalContentColor.current)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text, fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = LocalContentColor.current)
        }
    }
}

// ── Outline Button ──────────────────────────────────────────────────────────
@Composable
fun CraftOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: String = "",
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick  = onClick,
        modifier = modifier.height(52.dp),
        shape    = RoundedCornerShape(10.dp),
        border   = BorderStroke(1.dp, if (enabled) CraftColors.Border else CraftColors.Border.copy(alpha = 0.4f)),
        colors   = ButtonDefaults.outlinedButtonColors(contentColor = CraftColors.InkPrimary),
        enabled  = enabled,
    ) {
        if (icon.isNotEmpty()) {
            Text(icon, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text, fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp)
    }
}

// ── Accent Button ───────────────────────────────────────────────────────────
@Composable
fun CraftAccentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: String = "",
) {
    Button(
        onClick   = onClick,
        enabled   = enabled && !isLoading,
        modifier  = modifier.height(52.dp),
        shape     = RoundedCornerShape(10.dp),
        colors    = ButtonDefaults.buttonColors(
            containerColor         = CraftColors.Accent, 
            contentColor           = Color.White,
            disabledContainerColor = CraftColors.Border,
            disabledContentColor   = CraftColors.InkTertiary
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp),
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            if (icon.isNotEmpty()) {
                Text(icon, fontSize = 16.sp, color = LocalContentColor.current)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text, fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = LocalContentColor.current)
        }
    }
}

// ── Input Field ─────────────────────────────────────────────────────────────
@Composable
fun CraftTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    label: String = "",
    minLines: Int = 1,
    maxLines: Int = 1,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label.isNotEmpty()) {
            Text(
                text       = label,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Medium,
                fontSize   = 13.sp,
                color      = CraftColors.InkSecondary,
            )
        }
        OutlinedTextField(
            value         = value,
            onValueChange = onValueChange,
            placeholder   = {
                Text(placeholder, color = CraftColors.InkTertiary, fontFamily = InterFamily, fontSize = 14.sp)
            },
            modifier      = modifier.fillMaxWidth(),
            shape         = RoundedCornerShape(8.dp),
            minLines      = minLines,
            maxLines      = maxLines,
            trailingIcon  = trailingIcon,
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = CraftColors.Accent,
                unfocusedBorderColor    = CraftColors.Border,
                focusedContainerColor   = CraftColors.Surface,
                unfocusedContainerColor = CraftColors.Surface,
                focusedTextColor        = CraftColors.InkPrimary,
                unfocusedTextColor      = CraftColors.InkPrimary,
                cursorColor             = CraftColors.Accent,
            ),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = InterFamily,
                fontSize   = 14.sp,
                color      = CraftColors.InkPrimary,
            ),
        )
    }
}

// ── Card ────────────────────────────────────────────────────────────────────
@Composable
fun CraftCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier        = modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(12.dp),
        color           = CraftColors.Surface,
        border          = BorderStroke(1.dp, CraftColors.Border),
        shadowElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

// ── Section Header ──────────────────────────────────────────────────────────
@Composable
fun SectionHeader(step: String, title: String, subtitle: String = "") {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text          = step,
            fontFamily    = InterFamily,
            fontWeight    = FontWeight.SemiBold,
            fontSize      = 11.sp,
            letterSpacing = 1.sp,
            color         = CraftColors.Accent,
        )
        Text(text = title, fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = CraftColors.InkPrimary)
        if (subtitle.isNotEmpty()) {
            Text(text = subtitle, fontFamily = InterFamily, fontSize = 13.sp, color = CraftColors.InkSecondary)
        }
    }
}

// ── Chip ────────────────────────────────────────────────────────────────────
@Composable
fun CraftChip(
    text: String,
    onRemove: (() -> Unit)? = null,
    color: Color = CraftColors.SurfaceVariant,
    textColor: Color = CraftColors.InkSecondary,
) {
    Surface(shape = RoundedCornerShape(6.dp), color = color, border = BorderStroke(1.dp, CraftColors.Border)) {
        Row(
            modifier              = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text, fontFamily = InterFamily, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textColor)
            if (onRemove != null) {
                Text("×", fontSize = 14.sp, color = CraftColors.InkTertiary, modifier = Modifier.clickable { onRemove() })
            }
        }
    }
}

// ── Labeled Divider ─────────────────────────────────────────────────────────
@Composable
fun LabeledDivider(label: String) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = CraftColors.Border)
        Text(label, fontFamily = InterFamily, fontSize = 12.sp, color = CraftColors.InkTertiary)
        HorizontalDivider(modifier = Modifier.weight(1f), color = CraftColors.Border)
    }
}

// ── Status Pill ─────────────────────────────────────────────────────────────
@Composable
fun StatusPill(text: String, color: Color, textColor: Color, icon: String = "") {
    Surface(shape = RoundedCornerShape(20.dp), color = color) {
        Row(
            modifier              = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (icon.isNotEmpty()) Text(icon, fontSize = 12.sp)
            Text(text, fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = textColor)
        }
    }
}

// ── Pro Gate Card ───────────────────────────────────────────────────────────
@Composable
fun ProGateCard(message: String, onUpgrade: () -> Unit) {
    Surface(
        shape  = RoundedCornerShape(10.dp),
        color  = CraftColors.ProGoldSoft,
        border = BorderStroke(1.dp, CraftColors.ProGold.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("★", fontSize = 18.sp, color = CraftColors.ProGold)
            Column(modifier = Modifier.weight(1f)) {
                Text("Pro feature", fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = CraftColors.InkPrimary)
                Text(message, fontFamily = InterFamily, fontSize = 12.sp, color = CraftColors.InkSecondary)
            }
            Surface(
                shape    = RoundedCornerShape(6.dp),
                color    = CraftColors.InkPrimary,
                modifier = Modifier.clickable(onClick = onUpgrade),
            ) {
                Text(
                    "Upgrade",
                    modifier   = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 12.sp,
                    color      = Color.White,
                )
            }
        }
    }
}
