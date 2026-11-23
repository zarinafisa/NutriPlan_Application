package com.example.nutriplan.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.nutriplan.ui.theme.GreenDark
import com.example.nutriplan.ui.theme.RedError

@Composable
private fun nutriFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = GreenDark,
    focusedLabelColor = GreenDark,
    cursorColor = GreenDark,
    errorBorderColor = RedError,
    errorLabelColor = RedError,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    errorContainerColor = Color.White
)

@Composable
fun NutriTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    helper: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    leadingIcon: ImageVector? = null
) {
    var tampil by remember { mutableStateOf(false) }
    val teks = error ?: helper
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        isError = error != null,
        leadingIcon = if (leadingIcon != null) { { Icon(leadingIcon, null) } } else null,
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { tampil = !tampil }) {
                    Icon(
                        if (tampil) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = "Tampilkan kata sandi"
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !tampil) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else keyboardType),
        supportingText = if (teks != null) { { Text(teks) } } else null,
        shape = RoundedCornerShape(16.dp),
        colors = nutriFieldColors()
    )
}

/** Field baca-saja yang bisa diketuk (untuk dropdown dan bottom sheet). */
@Composable
fun PickerField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    helper: String? = null,
    trailingIcon: ImageVector = Icons.Filled.ArrowDropDown
) {
    val teks = error ?: helper
    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            trailingIcon = { Icon(trailingIcon, null) },
            isError = error != null,
            supportingText = if (teks != null) { { Text(teks) } } else null,
            shape = RoundedCornerShape(16.dp),
            colors = nutriFieldColors()
        )
        Box(Modifier.matchParentSize().clickable(onClick = onClick))
    }
}

@Composable
fun <T> DropdownPickerField(
    label: String,
    value: String,
    options: List<Pair<T, String>>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null
) {
    var buka by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        PickerField(label = label, value = value, onClick = { buka = true }, error = error)
        DropdownMenu(expanded = buka, onDismissRequest = { buka = false }) {
            options.forEach { (kunci, teks) ->
                DropdownMenuItem(
                    text = { Text(teks) },
                    onClick = { onSelect(kunci); buka = false }
                )
            }
        }
    }
}