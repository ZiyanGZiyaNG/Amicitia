package com.example.amicitia.ui.menu.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.amicitia.R
import com.example.amicitia.SportStats
import com.example.amicitia.SportsRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

data class SportMeta(
    val key: String,
    val name: String,
    val icon: Painter
)

@Composable
fun HomeRoute(
    onSportSelected: (String) -> Unit
) {
    val repo = remember { SportsRepository() }
    val uid = Firebase.auth.currentUser?.uid

    // sportKey -> SportStats（從 Firebase 抓回來塞這裡）
    var statsMap by remember { mutableStateOf<Map<String, SportStats>>(emptyMap()) }

    val sports = listOf(
        SportMeta("tennis", "網球", painterResource(R.drawable.ic_tennis)),
        SportMeta("run", "跑步", painterResource(R.drawable.ic_run)),
        SportMeta("basketball", "籃球", painterResource(R.drawable.ic_basketball)),
        SportMeta("football", "足球", painterResource(R.drawable.ic_football)),
        SportMeta("volleyball", "排球", painterResource(R.drawable.ic_volleyball)),
        SportMeta("badminton", "羽球", painterResource(R.drawable.ic_badminton))
    )

    LaunchedEffect(uid) {
        uid ?: return@LaunchedEffect

        try {
            // 保證 /users/{uid}/sports/ 底下一定有六個運動
            repo.ensureAllSportsExist(uid)

            val result = mutableMapOf<String, SportStats>()
            for (sport in sports) {
                val stats = repo.getSportStats(uid, sport.key)
                result[sport.key] = stats
            }
            statsMap = result
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp),
    ) {
        Text(
            text = "選擇你的運動",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sports) { sport ->
                val score = statsMap[sport.key]?.totalScore?.toInt() ?: 1000

                SportCard(
                    icon = sport.icon,
                    name = sport.name,
                    score = score,
                    onClick = { onSportSelected(sport.key) }
                )
            }
        }
    }
}

@Composable
fun SportCard(
    icon: Painter,
    name: String,
    score: Int,
    onClick: () -> Unit
) {
    Surface(
        color = Color.White,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(96.dp),
        onClick = onClick
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = icon,
                contentDescription = name,
                tint = Color.Black,
                modifier = Modifier.height(48.dp)
            )

            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp),
                color = Color(0xFF111827)
            )

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "分數 $score",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6B7280)
            )
        }
    }
}