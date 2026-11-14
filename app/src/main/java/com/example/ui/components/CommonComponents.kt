package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.SyncMetadataState
import com.example.ui.theme.LeafGreenSecondary
import com.example.ui.theme.StatusPaidBg
import com.example.ui.theme.StatusPaidText
import com.example.ui.theme.StatusPendingBg
import com.example.ui.theme.StatusPendingText
import com.example.ui.theme.StatusUnpaidBg
import com.example.ui.theme.StatusUnpaidText
import com.example.ui.theme.WaterBluePrimary

@Composable
fun SyncStatusBanner(
  syncState: SyncMetadataState,
  onSyncClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val bgColor by animateColorAsState(
    targetValue = when {
      !syncState.isOnline -> MaterialTheme.colorScheme.surfaceVariant
      syncState.hasPendingWrites -> StatusPendingBg
      else -> StatusPaidBg
    },
    label = "sync_bg"
  )
  val contentColor by animateColorAsState(
    targetValue = when {
      !syncState.isOnline -> MaterialTheme.colorScheme.onSurfaceVariant
      syncState.hasPendingWrites -> StatusPendingText
      else -> StatusPaidText
    },
    label = "sync_fg"
  )
  val icon = when {
    !syncState.isOnline -> Icons.Rounded.CloudOff
    syncState.hasPendingWrites -> Icons.Rounded.CloudSync
    else -> Icons.Rounded.CloudDone
  }
  val label = when {
    !syncState.isOnline && syncState.hasPendingWrites -> "Offline • Perubahan tersimpan lokal, tunggu koneksi"
    !syncState.isOnline -> "Mode Offline • Menampilkan data tersimpan lokal"
    syncState.hasPendingWrites -> "Menyinkronkan perubahan ke Firebase..."
    syncState.isFromCache -> "Online • Memperbarui dari cache lokal"
    else -> "Online • Tersinkron dengan Firebase"
  }

  Surface(
    color = bgColor,
    shape = RoundedCornerShape(12.dp),
    modifier = modifier
      .fillMaxWidth()
      .testTag("sync_status_banner")
      .clickable(onClick = onSyncClick)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        Icon(
          imageVector = icon,
          contentDescription = "Status sinkronisasi",
          tint = contentColor,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = label,
          style = MaterialTheme.typography.labelMedium,
          color = contentColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Icon(
        imageVector = Icons.Rounded.Refresh,
        contentDescription = "Sinkronkan sekarang",
        tint = contentColor,
        modifier = Modifier.size(16.dp)
      )
    }
  }
}

@Composable
fun BillStatusPill(status: String, modifier: Modifier = Modifier) {
  val (bg, fg, text) = when (status) {
    "lunas" -> Triple(StatusPaidBg, StatusPaidText, "Lunas")
    "menunggu_verifikasi" -> Triple(StatusPendingBg, StatusPendingText, "Menunggu Verifikasi")
    "ditolak" -> Triple(StatusUnpaidBg, StatusUnpaidText, "Ditolak")
    else -> Triple(StatusUnpaidBg, StatusUnpaidText, "Belum Bayar")
  }
  Surface(
    color = bg,
    shape = RoundedCornerShape(50),
    modifier = modifier
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
      color = fg,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
    )
  }
}

@Composable
fun PamsimasHeroHeader(
  title: String,
  subtitle: String,
  badgeText: String,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit = {}
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(22.dp))
      .background(
        Brush.linearGradient(
          colors = listOf(WaterBluePrimary, Color(0xFF0673A8), LeafGreenSecondary)
        )
      )
      .padding(18.dp)
  ) {
    Column {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Rounded.WaterDrop,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(22.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = title,
              style = MaterialTheme.typography.titleLarge,
              color = Color.White
            )
            Text(
              text = subtitle,
              style = MaterialTheme.typography.bodySmall,
              color = Color.White.copy(alpha = 0.88f)
            )
          }
        }
        Surface(
          color = Color.White.copy(alpha = 0.2f),
          shape = RoundedCornerShape(50)
        ) {
          Text(
            text = badgeText,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
          )
        }
      }
      content()
    }
  }
}

@Composable
fun StatMetricCard(
  label: String,
  value: String,
  icon: ImageVector,
  accentColor: Color,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Box(
        modifier = Modifier
          .size(34.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(accentColor.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = label,
          tint = accentColor,
          modifier = Modifier.size(20.dp)
        )
      }
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
fun EmptyStateCard(
  icon: ImageVector,
  title: String,
  description: String,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier
          .size(56.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(28.dp)
        )
      }
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}
