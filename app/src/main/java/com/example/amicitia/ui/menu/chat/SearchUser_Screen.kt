package com.example.amicitia.ui.menu.chat

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private val SearchPrimaryBlue = Color(0xFF3F51B5)
private val SearchBackground = Color(0xFFEFF3FF)

data class SearchUserResult(
    val uid: String,
    val nickname: String?,
    val email: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchUserScreen(
    onBack: () -> Unit,
    onOpenChat: (peerId: String, peerName: String) -> Unit
) {
    val db = remember { Firebase.firestore }
    val scope = rememberCoroutineScope()

    var uidInput by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<SearchUserResult?>(null) }

    fun clearResult() {
        errorText = null
        result = null
    }

    suspend fun searchByUid() {
        val uid = uidInput.trim()
        if (uid.isEmpty()) {
            errorText = "請先輸入 UID"
            result = null
            return
        }

        try {
            loading = true
            clearResult()

            val doc = db.collection("users")
                .document(uid)
                .get()
                .await()

            if (!doc.exists()) {
                errorText = "找不到這個 UID 的使用者"
                return
            }

            val nickname = doc.getString("nickname")
            val email = doc.getString("email")
            result = SearchUserResult(
                uid = doc.id,
                nickname = nickname,
                email = email
            )
        } catch (e: Exception) {
            Log.e("SearchUser", "search failed", e)
            errorText = "搜尋失敗：${e.message ?: "請稍後再試"}"
        } finally {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("搜尋使用者（UID）") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = SearchPrimaryBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SearchBackground)
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uidInput,
                onValueChange = {
                    uidInput = it
                    clearResult()
                },
                label = { Text("使用者 UID") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (!loading) {
                            scope.launch { searchByUid() }
                        }
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { scope.launch { searchByUid() } },
                enabled = !loading && uidInput.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SearchPrimaryBlue,
                    contentColor = Color.White
                )
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("搜尋中…")
                } else {
                    Text("搜尋")
                }
            }

            if (errorText != null) {
                Text(
                    text = errorText ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (result != null) {
                Spacer(Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    tonalElevation = 2.dp,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = result?.nickname ?: "(未設定暱稱)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "UID：${result?.uid}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "Email：${result?.email ?: "未知"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )

                        Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = {
                                val user = result ?: return@Button
                                val name = user.nickname ?: user.email ?: user.uid
                                onOpenChat(user.uid, name)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SearchPrimaryBlue,
                                contentColor = Color.White
                            )
                        ) {
                            Text("開始聊天")
                        }
                    }
                }
            }
        }
    }
}