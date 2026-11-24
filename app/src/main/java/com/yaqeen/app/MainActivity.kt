package com.yaqeen.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF0B100E)
private val Panel = Color(0xFF141D18)
private val Emerald = Color(0xFF2C8A64)
private val Gold = Color(0xFFD5BC7A)
private val Muted = Color(0xFFAAB7AE)
private val MainText = Color(0xFFF1F3EF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { YaqeenApp() }
    }
}

@Composable
private fun YaqeenApp() {
    var selectedTab by remember { mutableIntStateOf(0) }

    MaterialTheme {
        Scaffold(
            containerColor = Ink,
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.navigationBarsPadding(),
                    containerColor = Panel,
                    contentColor = MainText
                ) {
                    val tabs = listOf(
                        Triple("الرئيسية", Icons.Default.Home, 0),
                        Triple("القرآن", Icons.Default.MenuBook, 1),
                        Triple("السنة", Icons.Default.LibraryBooks, 2),
                        Triple("المحفوظات", Icons.Default.BookmarkBorder, 3)
                    )
                    tabs.forEach { (label, icon, index) ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        ) { padding ->
            when (selectedTab) {
                0 -> HomeScreen(Modifier.padding(padding))
                else -> PlaceholderScreen(
                    title = listOf("", "القرآن الكريم", "السنة النبوية", "المحفوظات")[selectedTab],
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Ink)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(22.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("يَقِين", color = Gold, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                Text("رفيقك في القرآن والسنة", color = Muted, fontSize = 12.sp)
            }
            Surface(shape = CircleShape, color = Panel) {
                Icon(
                    Icons.Default.NotificationsNone,
                    contentDescription = "التنبيهات",
                    tint = Gold,
                    modifier = Modifier.padding(12.dp).size(22.dp)
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Panel)
        ) {
            Column(Modifier.padding(22.dp)) {
                Text("وَرْدُكَ مِنَ الْقُرْآن", color = Gold, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    "﴿ وَرَتِّلِ الْقُرْآنَ تَرْتِيلًا ﴾",
                    color = MainText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Start
                )
                Spacer(Modifier.height(8.dp))
                Text("المزمل · ٤", color = Muted, fontSize = 12.sp)
                Spacer(Modifier.height(18.dp))
                Text("ابدأ القراءة أو تابع من آخر موضع وصلت إليه", color = Muted, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(26.dp))
        SectionHeading("القرآن الكريم", "استمع أو اقرأ")
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureCard(
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Default.MenuBook, null, tint = Gold) },
                title = "المصحف",
                subtitle = "قراءة وتفسير"
            )
            FeatureCard(
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Default.Headphones, null, tint = Gold) },
                title = "التلاوات",
                subtitle = "أشهر القراء"
            )
        }

        Spacer(Modifier.height(26.dp))
        SectionHeading("من هدي النبوة", "محتوى موثوق")
        Spacer(Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Panel)
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("حديث اليوم", color = Gold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Text(
                    "سيظهر هنا حديث موثق مع مصدره ودرجته وشرح مختصر.",
                    color = MainText,
                    fontSize = 15.sp,
                    lineHeight = 25.sp
                )
                Spacer(Modifier.height(8.dp))
                Text("المحتوى التجريبي — بانتظار ربط قاعدة الأحاديث المراجعة", color = Muted, fontSize = 10.sp)
            }
        }

        Spacer(Modifier.height(26.dp))
        SectionHeading("الأذكار", "اذكر الله")
        Spacer(Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Panel)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("أذكار الصباح والمساء", color = MainText, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(5.dp))
                    Text("أذكار مأثورة مع عداد تفاعلي", color = Muted, fontSize = 12.sp)
                }
                Box(
                    modifier = Modifier.size(42.dp).background(Emerald.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.LibraryBooks, null, tint = Gold)
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        Text(
            "يقين · إصدار تأسيسي",
            modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
            color = Muted.copy(alpha = 0.65f),
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SectionHeading(title: String, action: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = MainText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(action, color = Gold, fontSize = 11.sp)
    }
}

@Composable
private fun FeatureCard(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Panel)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            icon()
            Spacer(Modifier.height(18.dp))
            Text(title, color = MainText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().background(Ink).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = Gold, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(
            "هذا القسم ضمن خطة التطوير، وسيُبنى بوظائفه الفعلية في المراحل التالية.",
            color = Muted,
            textAlign = TextAlign.Center,
            lineHeight = 25.sp
        )
    }
}
