package com.example.midespensa.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.midespensa.ui.theme.GreenBack

@Composable
fun HeaderSection(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(GreenBack)
            .padding(40.dp, 60.dp, 40.dp, 30.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}

