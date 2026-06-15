package com.example.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class HubSection(val label: String)

@Composable
fun HubScreen(
  sections: List<HubSection>,
  selectedIndex: Int,
  onSelect: (Int) -> Unit,
  content: @Composable () -> Unit,
) {
  Column(modifier = Modifier.fillMaxSize()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      sections.forEachIndexed { index, section ->
        FilterChip(
          selected = selectedIndex == index,
          onClick = { onSelect(index) },
          label = { Text(section.label) },
        )
      }
    }
    content()
  }
}
