package io.github.bntheq.qanopy

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PikselYukleme() {
    LoadingIndicator(polygons = listOf(MaterialShapes.PixelCircle, MaterialShapes.PixelTriangle))
}
