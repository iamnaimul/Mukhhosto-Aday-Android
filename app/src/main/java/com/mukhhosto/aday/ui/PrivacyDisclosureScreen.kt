
package com.mukhhosto.aday.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mukhhosto.aday.ui.theme.*

@Composable
fun PrivacyDisclosureScreen(onAccept: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(OffWhite)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("গোপনীয়তা নোটিশ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Bullet("এই অ্যাপ কোনো শিক্ষার্থীর তথ্য বা ফলাফল Database-এ সংরক্ষণ করে না।")
                Bullet("পরীক্ষার কোনো ইতিহাস, র‍্যাঙ্কিং বা প্রোফাইল তৈরি হয় না।")
                Bullet("শিক্ষার্থীর নাম অ্যাপের স্ক্রিনে দেখানো হয়; Speech-to-Text অনুরোধে নাম পাঠানো হয় না।")
                Bullet("অ্যাপ নিজে অডিও কোনো সার্ভারে আপলোড করে না। তবে ফোনের সাধারণ Speech Recognition সার্ভিস ইন্টারনেট ব্যবহার করতে পারে; শুধু On-device recognizer ব্যবহৃত হলে অডিও ডিভাইসেই প্রক্রিয়াকরণ করা হয়।")
                Bullet("এই সংস্করণে অ্যাপ নিজে অডিও ফাইল রেকর্ড বা সংরক্ষণ করে না। শনাক্ত করা বক্তব্য ও ফলাফল শুধু বর্তমান স্ক্রিনের অস্থায়ী মেমোরিতে থাকে।")
                Bullet("মাইক্রোফোন শোনা শুধু শিক্ষক ‘বলা শুরু’ চাপলে শুরু হয়; ব্যাকগ্রাউন্ডে চলবে না।")
            }
        }
        Text(
            "অ্যাপটি ব্যবহার চালিয়ে যেতে আপনি উপরের শর্তগুলো গ্রহণ করছেন।",
            style = MaterialTheme.typography.bodyMedium
        )
        Button(
            onClick = onAccept,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
        ) {
            Text("একমত ও শুরু করুন", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Bullet(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Text("•", color = BluePrimary, fontWeight = FontWeight.Bold)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
