package com.mardous.booming.ui.component.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SmallHeader(
    title: String,
    subtitle: String,
    additionalInfo: String? = null,
    imageModel: Any? = null,
    showIndeterminateIndicator: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        imageModel?.let {
            AsyncImage(
                model = imageModel,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .size(96.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            additionalInfo?.let {
                Spacer(Modifier.heightIn(2.dp))
                ShapedText(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    shape = RoundedCornerShape(4.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = showIndeterminateIndicator,
            modifier = Modifier.padding(start = 8.dp)
        ) {
            CircularWavyProgressIndicator(
                wavelength = 10.dp,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}