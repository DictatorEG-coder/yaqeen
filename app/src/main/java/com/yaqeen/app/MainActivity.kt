package com.yaqeen.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF090E0B)
private val Panel = Color(0xFF141D18)
private val PanelSoft = Color(0xFF1B2821)
private val Emerald = Color(0xFF287A59)
private val Gold = Color(0xFFD7C18A)
private val Muted = Color(0xFFAAB7AE)
private val MainText = Color(0xFFF1F3EF)
private const val TELEGRAM_CHANNEL = "https://t.me/Dictator_channel2"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { YaqeenApp() }
    }
}

@Composable
private fun YaqeenApp() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var openBookmarksRequested by remember { mutableStateOf(false) }
    MaterialTheme {
        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Scaffold(
                containerColor = Ink,
                bottomBar = {
                    NavigationBar(
                        modifier = Modifier.navigationBarsPadding(),
                        containerColor = Panel,
                        contentColor = MainText
                    ) {
                        val tabs = listOf(
                            Triple("الرئيسية", Icons.Outlined.Home, 0),
                            Triple("القرآن", Icons.Outlined.MenuBook, 1),
                            Triple("السنة", Icons.Outlined.LibraryBooks, 2),
                            Triple("الأذكار", Icons.Outlined.Bookmarks, 3),
                            Triple("المزيد", Icons.Outlined.MoreHoriz, 4)
                        )
                        tabs.forEach { (label, icon, index) ->
                            NavigationBarItem(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label, fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Gold,
                                    selectedTextColor = Gold,
                                    indicatorColor = Emerald.copy(alpha = 0.22f),
                                    unselectedIconColor = Muted,
                                    unselectedTextColor = Muted
                                )
                            )
                        }
                    }
                }
            ) { padding ->
                when (selectedTab) {
                    0 -> HomeScreen(
                        modifier = Modifier.padding(padding),
                        onNavigate = { selectedTab = it }
                    )
                    1 -> QuranReaderScreen(
                        modifier = Modifier.padding(padding),
                        showBookmarksInitially = openBookmarksRequested,
                        onBookmarksOpened = { openBookmarksRequested = false }
                    )
                    2 -> SunnahScreen(Modifier.padding(padding))
                    3 -> AdhkarScreen(Modifier.padding(padding))
                    else -> MoreScreen(
                        modifier = Modifier.padding(padding),
                        onOpenBookmarks = { selectedTab = 1; openBookmarksRequested = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun PageColumn(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Ink)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) { content() }
}

@Composable
private fun PageHeader(title: String, subtitle: String) {
    Spacer(Modifier.height(22.dp))
    Text(title, color = Gold, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(5.dp))
    Text(subtitle, color = Muted, fontSize = 12.sp)
    Spacer(Modifier.height(22.dp))
}

@Composable
private fun HomeScreen(modifier: Modifier = Modifier, onNavigate: (Int) -> Unit) {
    PageColumn(modifier) {
        Spacer(Modifier.height(22.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("يَقِين", color = Gold, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                Text("رفيقك في القرآن والسنة", color = Muted, fontSize = 12.sp)
            }
            Surface(shape = CircleShape, color = Panel) {
                Icon(
                    Icons.Outlined.NotificationsNone,
                    contentDescription = "التنبيهات",
                    tint = Gold,
                    modifier = Modifier.padding(12.dp).size(23.dp)
                )
            }
        }

        Spacer(Modifier.height(26.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(25.dp),
            colors = CardDefaults.cardColors(containerColor = Panel)
        ) {
            Column(Modifier.padding(22.dp)) {
                Text("الْقُرْآنُ الْكَرِيم", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Text(
                    "﴿ وَرَتِّلِ الْقُرْآنَ تَرْتِيلًا ﴾",
                    color = MainText,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(8.dp))
                Text("سورة المزمل · الآية ٤", color = Muted, fontSize = 11.sp)
                Spacer(Modifier.height(17.dp))
                Text("اقرأ المصحف أو استمع إلى تلاوات القراء", color = Muted, fontSize = 12.sp)
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = { onNavigate(1) },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                    shape = RoundedCornerShape(13.dp)
                ) {
                    Text("افتح القرآن الكريم", color = MainText)
                }
            }
        }

        Spacer(Modifier.height(25.dp))
        SectionHeading("استكشف يقين")
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FeatureCard(
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Outlined.MenuBook, null, tint = Gold) },
                title = "القرآن الكريم",
                subtitle = "قراءة واستماع",
                onClick = { onNavigate(1) }
            )
            FeatureCard(
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Outlined.LibraryBooks, null, tint = Gold) },
                title = "السنة النبوية",
                subtitle = "حديث ومصدر",
                onClick = { onNavigate(2) }
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FeatureCard(
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Outlined.Bookmarks, null, tint = Gold) },
                title = "الأذكار",
                subtitle = "أذكار مأثورة",
                onClick = { onNavigate(3) }
            )
            FeatureCard(
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Outlined.MoreHoriz, null, tint = Gold) },
                title = "المزيد",
                subtitle = "عن يقين والتواصل",
                onClick = { onNavigate(4) }
            )
        }

        Spacer(Modifier.height(25.dp))
        SectionHeading("من هدي النبوة")
        Spacer(Modifier.height(10.dp))
        InfoCard(
            title = "حديث اليوم",
            body = "ستُعرض هنا أحاديث موثقة مع مصادرها وتخريجها بعد تجهيز قاعدة المحتوى ومراجعتها."
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "يقين · نعمل ليكون المحتوى موثوقًا والتجربة هادئة",
            modifier = Modifier.fillMaxWidth().padding(bottom = 22.dp),
            color = Muted.copy(alpha = 0.7f),
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun QuranScreen(modifier: Modifier = Modifier) {
    PageColumn(modifier) {
        PageHeader("القرآن الكريم", "اقرأ، استمع، واحفظ موضعك")
        InfoCard(
            title = "ثلاث طرق للتفاعل مع القرآن",
            body = "القراءة فقط، أو الاستماع فقط، أو متابعة المصحف مع تظليل الآية أثناء التلاوة عند توفر توقيت موثوق."
        )
        Spacer(Modifier.height(18.dp))
        SectionHeading("القراء المستهدفون")
        Spacer(Modifier.height(10.dp))
        listOf(
            "محمد صديق المنشاوي",
            "عبد الباسط عبد الصمد",
            "محمود خليل الحصري",
            "مصطفى إسماعيل",
            "محمود علي البنا",
            "محمد رفعت",
            "عبد الرحمن السديس",
            "سعود الشريم",
            "ماهر المعيقلي",
            "مشاري العفاسي"
        ).forEach { name ->
            SimpleRow(Icons.Outlined.Headphones, name, "مرتل ومجود بحسب التسجيلات المتاحة")
        }
        Spacer(Modifier.height(14.dp))
        SimpleRow(Icons.Outlined.PlayCircleOutline, "مكتبة المحافل والتسجيلات", "تسجيلات موثقة ومصرح باستخدامها")
        SimpleRow(Icons.Outlined.MenuBook, "الحفظ والتسميع", "تكرار المقاطع وتسجيل صوتك للمراجعة")
        SimpleRow(Icons.Outlined.LibraryBooks, "التجويد والتفسير والبحث", "أحكام ملونة وتفسير موثق وبحث في الآيات")
        SimpleRow(Icons.Outlined.Bookmarks, "العلامات والختمة", "حفظ مواضعك وختمة اختيارية")
        Spacer(Modifier.height(16.dp))
        InfoCard(
            title = "مكتبة التلاوات قيد التجهيز",
            body = "لن نعرض أزرار تشغيل وهمية. ستُربط التلاوات بعد تحديد مصادر موثوقة والتأكد من حقوق استخدامها."
        )
        Spacer(Modifier.height(22.dp))
    }
}

@Composable
private fun SunnahScreen(modifier: Modifier = Modifier) {
    PageColumn(modifier) {
        PageHeader("السنة النبوية", "مصادر الحديث والشرح والتخريج")
        SimpleRow(Icons.Outlined.LibraryBooks, "صحيح البخاري", "الأبواب والأحاديث والبحث")
        SimpleRow(Icons.Outlined.LibraryBooks, "صحيح مسلم", "الأبواب والأحاديث والبحث")
        SimpleRow(Icons.Outlined.PlayCircleOutline, "حديث اليوم", "اختياري مع المصدر والتخريج")
        SimpleRow(Icons.Outlined.Bookmarks, "السنن المهجورة", "سنن ثابتة مع الدليل والشرح المختصر")
        SimpleRow(Icons.Outlined.Bookmarks, "سنة اليوم", "تذكير اختياري بسنة موثقة")
        SimpleRow(Icons.Outlined.Bookmarks, "شرح الحديث", "من شروح معتمدة مع ذكر المرجع")
        Spacer(Modifier.height(12.dp))
        InfoCard(
            title = "المحتوى قيد الإعداد",
            body = "لن نضع أحاديث أو درجات أو شروحًا غير متحقق منها. سيُربط كل نص بمصدره وبيانات تخريجه."
        )
        Spacer(Modifier.height(22.dp))
    }
}

@Composable
private fun AdhkarScreen(modifier: Modifier = Modifier) {
    PageColumn(modifier) {
        PageHeader("الأذكار", "أذكار مأثورة وتنبيهات تختارها بنفسك")
        SimpleRow(Icons.Outlined.Bookmarks, "أذكار الصباح", "تنبيه اختياري في الوقت الذي تحدده")
        SimpleRow(Icons.Outlined.Bookmarks, "أذكار المساء", "تنبيه اختياري في الوقت الذي تحدده")
        SimpleRow(Icons.Outlined.Bookmarks, "أذكار النوم والاستيقاظ", "نصوص مأثورة ومصادرها")
        SimpleRow(Icons.Outlined.NotificationsNone, "التنبيهات", "تفعيل كل تذكير أو إيقافه بصورة مستقلة")
        SimpleRow(Icons.Outlined.PlayCircleOutline, "عداد الأذكار", "عداد لكل ذكر مع إمكانية ضبط العدد")
        Spacer(Modifier.height(12.dp))
        InfoCard(
            title = "التذكيرات اختيارية",
            body = "يمكنك اختيار أذكار الصباح والمساء وقيام الليل والورد القرآني وحديث اليوم وسنة اليوم، دون إشعارات مزعجة."
        )
        Spacer(Modifier.height(22.dp))
    }
}

@Composable
private fun MoreScreen(modifier: Modifier = Modifier, onOpenBookmarks: () -> Unit) {
    val context = LocalContext.current
    PageColumn(modifier) {
        PageHeader("المزيد", "إعدادات يقين ومعلومات التطبيق")
        SimpleRow(Icons.Outlined.Bookmarks, "المحفوظات", "الآيات والأحاديث والمواضع المفضلة", onClick = onOpenBookmarks)
        SimpleRow(Icons.Outlined.NotificationsNone, "إعدادات التنبيهات", "اختيار أنواع التذكيرات وأوقاتها")
        SimpleRow(Icons.Outlined.MenuBook, "حول تطبيق يقين", "الرؤية والمصادر وحقوق المحتوى")
        Spacer(Modifier.height(14.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Panel)
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("تواصل معنا", color = Gold, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "إذا وجدت خطأً أو لديك ملاحظة أو اقتراح، يسعدنا أن تخبرنا عبر قناة يقين على تيليجرام.",
                    color = Muted,
                    fontSize = 13.sp,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(TELEGRAM_CHANNEL))
                        try {
                            context.startActivity(intent)
                        } catch (_: ActivityNotFoundException) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(TELEGRAM_CHANNEL)))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                    shape = RoundedCornerShape(13.dp)
                ) {
                    Icon(Icons.Outlined.Send, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("قناة يقين على تيليجرام")
                }
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(TELEGRAM_CHANNEL)))
                }) {
                    Text("t.me/Dictator_channel2", color = Gold, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = PanelSoft)
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("دعوة طيبة", color = Gold, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(9.dp))
                Text(
                    "إن أعجبك تطبيق يقين، فلا تنسَ الدعاء لصاحبه بالتوفيق والنجاح في دراسته، وأن يجعل الله هذا العمل نافعًا وخالصًا لوجهه.",
                    color = MainText,
                    fontSize = 13.sp,
                    lineHeight = 24.sp
                )
            }
        }
        Spacer(Modifier.height(22.dp))
        Text(
            "يقين · إصدار تأسيسي",
            modifier = Modifier.fillMaxWidth().padding(bottom = 22.dp),
            color = Muted.copy(alpha = 0.7f),
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SectionHeading(title: String) {
    Text(title, color = MainText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun FeatureCard(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(containerColor = Panel)
    ) {
        Column(Modifier.fillMaxWidth().padding(15.dp)) {
            icon()
            Spacer(Modifier.height(15.dp))
            Text(title, color = MainText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(containerColor = Panel)
    ) {
        Column(Modifier.padding(17.dp)) {
            Text(title, color = Gold, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(body, color = Muted, fontSize = 12.sp, lineHeight = 21.sp)
        }
    }
}

@Composable
private fun SimpleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 5.dp)
            .background(Panel, RoundedCornerShape(16.dp))
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(Emerald.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Gold, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.size(12.dp))
        Column {
            Text(title, color = MainText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = Muted, fontSize = 10.sp)
        }
    }
}
