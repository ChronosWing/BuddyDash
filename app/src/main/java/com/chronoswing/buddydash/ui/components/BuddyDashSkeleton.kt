package com.chronoswing.buddydash.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chronoswing.buddydash.ui.motion.rememberPrefersReducedMotion
import com.chronoswing.buddydash.ui.motion.skeletonShimmerBrush

@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 6.dp,
) {
    val reduced = rememberPrefersReducedMotion()
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val brush = skeletonShimmerBrush(reduced)
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (brush != null) {
                    Modifier.background(brush = brush, shape = shape)
                } else {
                    Modifier.background(color = base, shape = shape)
                },
            ),
    )
}

@Composable
fun PrinterListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 4,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 12.dp,
            vertical = 8.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(itemCount) {
            PrinterCardSkeleton()
        }
    }
}

@Composable
fun PrinterCardSkeleton(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(18.dp),
                cornerRadius = 6.dp,
            )
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth(0.35f)
                    .height(12.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(2) {
                    SkeletonBlock(
                        modifier = Modifier
                            .width(72.dp)
                            .height(22.dp),
                        cornerRadius = 8.dp,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SkeletonBlock(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        cornerRadius = 2.dp,
                    )
                    SkeletonBlock(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(14.dp),
                    )
                }
                ThumbnailSkeleton(size = 64.dp, cornerRadius = 10.dp)
            }
        }
    }
}

@Composable
fun ThumbnailSkeleton(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    cornerRadius: Dp = 8.dp,
) {
    SkeletonBlock(
        modifier = modifier.size(size),
        cornerRadius = cornerRadius,
    )
}

@Composable
fun ArchiveRowSkeleton(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            ThumbnailSkeleton(size = 56.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(14.dp),
                )
                SkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.45f)
                        .height(12.dp),
                )
                SkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(11.dp),
                )
            }
        }
    }
}

@Composable
fun ArchiveListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 6,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 12.dp,
            end = 12.dp,
            bottom = 12.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(itemCount) {
            ArchiveRowSkeleton()
        }
    }
}

@Composable
fun SpoolRowSkeleton(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            SkeletonBlock(
                modifier = Modifier.size(40.dp),
                cornerRadius = 20.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(14.dp),
                )
                SkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.45f)
                        .height(11.dp),
                )
                SkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    cornerRadius = 2.dp,
                )
            }
        }
    }
}

@Composable
fun SpoolListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 6,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 12.dp,
            end = 12.dp,
            bottom = 12.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(itemCount) {
            SpoolRowSkeleton()
        }
    }
}

@Composable
fun QueueRowSkeleton(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            ThumbnailSkeleton(size = 44.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SkeletonBlock(
                    modifier = Modifier
                        .width(28.dp)
                        .height(10.dp),
                )
                SkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(14.dp),
                )
                SkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(11.dp),
                )
            }
        }
    }
}

@Composable
fun QueueListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 4,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 12.dp,
            vertical = 8.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(itemCount) {
            QueueRowSkeleton()
        }
    }
}

@Composable
fun PrinterDetailSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SkeletonBlock(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            cornerRadius = 10.dp,
        )
        DetailCardSkeleton(lineCount = 5)
        DetailCardSkeleton(lineCount = 3)
    }
}

@Composable
fun DetailCardSkeleton(
    modifier: Modifier = Modifier,
    lineCount: Int = 4,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(16.dp),
            )
            repeat(lineCount) { index ->
                SkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth(if (index % 2 == 0) 0.9f else 0.65f)
                        .height(12.dp),
                )
            }
        }
    }
}
