package com.example.amicitia.ui.menu.home.run

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.amicitia.R

@Composable
fun RunModeScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "選擇跑步模式",
            style = MaterialTheme.typography.headlineSmall,
            color = Color(0xFF111827)
        )

        Spacer(modifier = Modifier.height(36.dp))

        RunModeCard(
            title = "單人模式 SOLO",
            iconRes = R.drawable.ic_run,
            onClick = { navController.navigate("run_solo") }
        )

        Spacer(modifier = Modifier.height(20.dp))

        RunModeCard(
            title = "多人模式 MULTI",
            iconRes = R.drawable.ic_run,   // 若之後有多人跑步 icon 再換掉
            onClick = { navController.navigate("run_multi") }
        )
    }
}

@Composable
private fun RunModeCard(
    title: String,
    iconRes: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                tint = Color(0xFF4B5563),
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.width(20.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF111827)
            )
        }
    }
}