package com.example.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import com.example.core.i18n.AppLanguage
import com.example.ui.theme.Apricot
import com.example.ui.theme.Cocoa
import com.example.ui.theme.Cream
import com.example.ui.theme.Sage
import com.example.ui.theme.Surface
import com.example.ui.theme.Taupe

@Composable
fun LanguageButton(language: AppLanguage, onClick: () -> Unit, modifier: Modifier = Modifier) {
  OutlinedButton(onClick = onClick, shape = RoundedCornerShape(8.dp), modifier = modifier) {
    Icon(Icons.Rounded.Translate, contentDescription = null, tint = Cocoa, modifier = Modifier.size(18.dp))
    Spacer(Modifier.width(6.dp))
    Text(if (language == AppLanguage.English) "Tiếng Việt" else "English", color = Cocoa, fontWeight = FontWeight.Bold)
  }
}

@Composable
fun ScreenList(title: String, subtitle: String, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 18.dp),
    contentPadding = PaddingValues(top = 28.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Text(title, style = MaterialTheme.typography.headlineSmall, color = Cocoa, fontWeight = FontWeight.Black)
      Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Taupe)
      Spacer(Modifier.height(8.dp))
    }
    content()
  }
}

@Composable
fun InfoCard(icon: ImageVector, title: String, body: String) {
  CardBlock {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
      Icon(icon, contentDescription = null, tint = Apricot, modifier = Modifier.size(30.dp))
      Column {
        Text(title, color = Cocoa, fontWeight = FontWeight.Black)
        Text(body, color = Taupe, style = MaterialTheme.typography.bodyMedium)
      }
    }
  }
}

@Composable
fun CardBlock(border: BorderStroke? = null, content: @Composable ColumnScope.() -> Unit) {
  Card(
    colors = CardDefaults.cardColors(containerColor = Surface),
    shape = RoundedCornerShape(8.dp),
    border = border,
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
  }
}

@Composable
fun EsmeryTextField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  modifier: Modifier = Modifier,
  keyboardType: KeyboardType = KeyboardType.Text,
  password: Boolean = false,
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label) },
    modifier = modifier.fillMaxWidth(),
    singleLine = true,
    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
  )
}

@Composable
fun PrimaryButton(
  text: String,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  loading: Boolean = false,
  onClick: () -> Unit,
) {
  Button(
    onClick = onClick,
    enabled = !loading,
    colors = ButtonDefaults.buttonColors(containerColor = Apricot),
    shape = RoundedCornerShape(8.dp),
    modifier = modifier
      .fillMaxWidth()
      .height(54.dp),
  ) {
    if (loading) {
      CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
    } else {
      icon?.let {
        Icon(it, contentDescription = null, tint = Color.White)
        Spacer(Modifier.width(8.dp))
      }
      Text(text, color = Color.White, fontWeight = FontWeight.Black)
    }
  }
}

@Composable
fun InlineMessage(text: String) {
  Surface(color = Sage.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
    Text(text, color = Cocoa, modifier = Modifier.padding(12.dp))
  }
}

@Composable
fun AvatarInitial(name: String, modifier: Modifier = Modifier) {
  Surface(shape = CircleShape, color = Sage, modifier = modifier.size(44.dp)) {
    Box(contentAlignment = Alignment.Center) {
      Text(name.take(1), color = Cocoa, fontWeight = FontWeight.Black)
    }
  }
}

fun Modifier.esmeryBackground(): Modifier = this.then(Modifier)
