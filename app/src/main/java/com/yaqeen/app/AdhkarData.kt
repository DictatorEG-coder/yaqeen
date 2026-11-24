package com.yaqeen.app

internal data class AdhkarEntry(
    val id: String,
    val category: String,
    val text: String,
    val targetCount: Int,
    val sourceLabel: String,
    val sourceUrl: String,
    val note: String? = null
)

internal val verifiedAdhkarEntries = listOf(
    AdhkarEntry(
        id = "morning_sayyid_al_istighfar",
        category = "morning",
        text = "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ، وَأَبُوءُ بِذَنْبِي، فَاغْفِرْ لِي، فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ.",
        targetCount = 1,
        sourceLabel = "صحيح البخاري · 6323",
        sourceUrl = "https://sunnah.com/bukhari:6323"
    ),
    AdhkarEntry(
        id = "evening_sayyid_al_istighfar",
        category = "evening",
        text = "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ، وَأَبُوءُ بِذَنْبِي، فَاغْفِرْ لِي، فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ.",
        targetCount = 1,
        sourceLabel = "صحيح البخاري · 6323",
        sourceUrl = "https://sunnah.com/bukhari:6323"
    ),
    AdhkarEntry(
        id = "morning_waking",
        category = "morning",
        text = "الْحَمْدُ لِلَّهِ الَّذِي أَحْيَانَا بَعْدَ مَا أَمَاتَنَا، وَإِلَيْهِ النُّشُورُ.",
        targetCount = 1,
        sourceLabel = "صحيح البخاري · 6324",
        sourceUrl = "https://sunnah.com/bukhari:6324"
    ),
    AdhkarEntry(
        id = "sleep_dua",
        category = "sleep",
        text = "بِاسْمِكَ اللَّهُمَّ أَمُوتُ وَأَحْيَا.",
        targetCount = 1,
        sourceLabel = "صحيح البخاري · 6324",
        sourceUrl = "https://sunnah.com/bukhari:6324"
    ),
    AdhkarEntry(
        id = "sleep_takbir",
        category = "sleep",
        text = "اللَّهُ أَكْبَرُ",
        targetCount = 33,
        sourceLabel = "صحيح البخاري · 6318",
        sourceUrl = "https://sunnah.com/bukhari:6318",
        note = "من الذكر الوارد قبل النوم في حديث علي وفاطمة رضي الله عنهما."
    ),
    AdhkarEntry(
        id = "sleep_tasbih",
        category = "sleep",
        text = "سُبْحَانَ اللَّهِ",
        targetCount = 33,
        sourceLabel = "صحيح البخاري · 6318",
        sourceUrl = "https://sunnah.com/bukhari:6318",
        note = "من الذكر الوارد قبل النوم في حديث علي وفاطمة رضي الله عنهما."
    ),
    AdhkarEntry(
        id = "sleep_tahmid",
        category = "sleep",
        text = "الْحَمْدُ لِلَّهِ",
        targetCount = 33,
        sourceLabel = "صحيح البخاري · 6318",
        sourceUrl = "https://sunnah.com/bukhari:6318",
        note = "من الذكر الوارد قبل النوم في حديث علي وفاطمة رضي الله عنهما."
    )
)

internal fun adhkarStorageKey(dateKey: String, entryId: String): String =
    "adhkar:$dateKey:$entryId"
