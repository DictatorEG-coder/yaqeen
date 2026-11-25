package com.yaqeen.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SunnahInk = Color(0xFF090E0B)
private val SunnahPanel = Color(0xFF141D18)
private val SunnahGold = Color(0xFFD7C18A)
private val SunnahMuted = Color(0xFFAAB7AE)
private val SunnahText = Color(0xFFF1F3EF)

private data class SunnahPractice(
    val title: String,
    val description: String,
    val evidence: String,
    val reference: String,
    val url: String
)

private val reviewedSunnahPractices = listOf(
    SunnahPractice(
        "السواك عند الصلاة",
        "يُستحب استعمال السواك عند القيام إلى الصلاة؛ وهي سنة ثابتة في الصحيحين.",
        "لولا أن أشق على أمتي لأمرتهم بالسواك عند كل صلاة.",
        "صحيح البخاري 887 · صحيح مسلم 252",
        "https://sunnah.com/bukhari:887"
    ),
    SunnahPractice(
        "ركعتا سنة الفجر",
        "المحافظة على ركعتي الفجر الخفيفتين قبل صلاة الصبح.",
        "ركعتا الفجر خير من الدنيا وما فيها.",
        "صحيح مسلم 725",
        "https://sunnah.com/muslim:725"
    ),
    SunnahPractice(
        "صلاة الضحى",
        "صلاة ركعتين أو أكثر من الضحى، وهي من الوصايا النبوية.",
        "ويجزئ من ذلك ركعتان يركعهما من الضحى.",
        "صحيح مسلم 720",
        "https://sunnah.com/muslim:720"
    ),
    SunnahPractice(
        "صيام ثلاثة أيام من كل شهر",
        "صيام ثلاثة أيام شهريًا من الأعمال التي أوصى بها النبي ﷺ.",
        "صيام ثلاثة أيام من كل شهر صيام الدهر كله.",
        "صحيح البخاري 1976 · صحيح مسلم 1159",
        "https://sunnah.com/bukhari:1976"
    ),
    SunnahPractice(
        "الوتر قبل النوم لمن خاف ألا يستيقظ",
        "من خشي ألا يقوم آخر الليل فليوتر قبل أن ينام.",
        "من خاف أن لا يقوم من آخر الليل فليوتر أوله، ومن طمع أن يقوم آخره فليوتر آخر الليل.",
        "صحيح مسلم 755",
        "https://sunnah.com/muslim:755"
    )
)

@Composable
internal fun SunnahMahjooraScreen(modifier: Modifier = Modifier) {
    var search by remember { mutableStateOf("") }
    val results = reviewedSunnahPractices.filter {
        it.title.contains(search.trim(), true) || it.description.contains(search.trim(), true)
    }
    Column(
        modifier = modifier.fillMaxSize().background(SunnahInk)
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Text("سنن يُغفل عنها", color = SunnahGold, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text("تذكير بسنن ثابتة مع نص الدليل ومصدره.", color = SunnahMuted, fontSize = 12.sp)
        Spacer(Modifier.height(18.dp))
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("ابحث عن سنة") }
        )
        Spacer(Modifier.height(12.dp))
        results.forEach { sunnah ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SunnahPanel)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(sunnah.title, color = SunnahGold, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(7.dp))
                    Text(sunnah.description, color = SunnahMuted, fontSize = 12.sp, lineHeight = 21.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("الدليل", color = SunnahGold, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("«" + sunnah.evidence + "»", color = SunnahText, fontSize = 16.sp, lineHeight = 29.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(sunnah.reference, color = SunnahMuted, fontSize = 10.sp)
                    Text(
                        "فتح المرجع",
                        color = SunnahGold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 8.dp).clickable {
                            // The reference URL is opened by the system browser through the user's tap.
                            androidx.compose.ui.platform.LocalUriHandler.current.openUri(sunnah.url)
                        }
                    )
                }
            }
        }
        if (results.isEmpty()) Text("لا توجد نتائج مطابقة.", color = SunnahMuted)
        Spacer(Modifier.height(24.dp))
    }
}
