# يقين — خادم وسيط لمحتوى القرآن

هذا Worker اختياري ومخصص لتمرير طلبات Quran Foundation Content API من تطبيق Android دون وضع `client_secret` داخله.

## مهم قبل النشر

- أنشئ تطبيقًا من نوع **Backend/server** في Quran Foundation Developer Console.
- اطلب صلاحيات Content التي تحتاجها فقط. الإعداد الافتراضي هنا يطلب:
  - `content.audio.read` (required for the current reciter and timing endpoints)
- Add `content.quran.read` or `content.tafsirs.read` only when those API features are enabled and those scopes are approved.
- لا تضع بيانات الاعتماد في GitHub أو في كود Android أو في المحادثة.
- بيئة pre-live لا تحتوي إلا على الفاتحة والبقرة. لا تربطها بنسخة الإنتاج؛ يلزم اعتماد الوصول إلى production من Quran Foundation أولًا.
- هذا الخادم لا يخزن بيانات المستخدم، ولا ينفذ طلبات كتابة.

## النشر إلى Cloudflare Workers

1. ثبّت Wrangler وسجّل الدخول إلى حساب Cloudflare.
2. من هذا المجلد نفّذ:
   `npx wrangler secret put QF_CLIENT_ID`
3. ثم:
   `npx wrangler secret put QF_CLIENT_SECRET`
4. اضبط `QF_SCOPE` و`QF_ENV` وفق الصلاحيات والبيئة التي اعتمدها Quran Foundation. لا تغيّر `QF_ENV` إلى production قبل موافقة الوصول الإنتاجي.
5. عند الحصول على مفتاح Sunnah.com الرسمي، أضفه اختياريًا عبر `npx wrangler secret put SUNNAH_API_KEY`. لا تضعه في التطبيق أو المستودع.
6. نفّذ:
   `npx wrangler deploy`
7. اختبر `/health`، ثم بعد تهيئة الاعتمادات اختبر `/api/quran/chapters`.
8. بعد نجاح النشر، أضف عنوان Worker كمتغير مستودع GitHub باسم `QF_PROXY_BASE_URL` (ليس سرًا)، أو مرره محليًا كخاصية Gradle بالاسم نفسه. سيُضمَّن العنوان فقط في BuildConfig؛ لا تُضمَّن بيانات الاعتماد.

## المسارات المتاحة

كلها GET فقط، وتُمرر إلى Quran Foundation Content API v4:

- `/api/quran/chapters`
- `/api/quran/verses/...`
- `/api/quran/recitations/...`
- `/api/quran/resources/chapter_reciters`
- `/api/quran/chapter_recitations/{reciter_id}`
- `/api/quran/chapter_recitations/{reciter_id}/{chapter_number}?segments=true`
- `/api/quran/audio/...`
- `/api/quran/tafsirs/...`
- `/api/quran/translations/...`
- `/api/quran/search/...`
- `/api/sunnah/collections` و`/api/sunnah/collections/{name}/books/...`
- `/api/sunnah/hadiths/...`

يحفظ Worker رمز OAuth2 في ذاكرة الـisolate حتى يقترب من انتهاء صلاحيته، ويعيد طلبه مرة واحدة عند استجابة 401. لا يسجل الرمز ولا يعيده للعميل.
- قبل مشاركة عنوان Worker مع التطبيق، فعّل Cloudflare Rate Limiting على المسارات `/api/quran/*` (مثل حدّ طلبات مناسب لكل IP)، وراقب استهلاك API. الواجهة للقراءة فقط ولا تقبل POST/PUT/DELETE، لكن عنوان Worker عام وليس سرًا.

## ما لم يُنجز بعد

- لم يُنشر Worker إلى حساب Cloudflare.
- لم تُضف أي مفاتيح أو أسرار إلى المستودع.
- لم يُربط تطبيق Android بعنوان Worker بعد.
- لم تُختبر صلاحيات production أو بياناتها.
- يلزم التحقق من endpoint الفعلي لتوقيت الآيات/الكلمات ومن تغطية كل تسجيل قبل تشغيل التظليل المتزامن.

Sunnah.com API يحتاج مفتاحًا رسميًا يُطلب من [مستودعهم](https://github.com/sunnah-com/api/issues/new?template=request-for-api-access.md&title=Request+for+API+access). واجهتهم الرسمية تنص على أن البيانات المتاحة جزء من corpus ويُضاف المزيد بعد المراجعة اليدوية؛ يجب مقارنة `totalAvailableHadith` مع `totalHadith` لكل مجموعة/كتاب قبل وصف البخاري أو مسلم بأنه كامل. لا تُعرض المجموعة كاملة إذا كانت الأعداد غير متطابقة.

لا تعتبر هذه الملفات خادمًا منشورًا أو تكاملًا مكتملًا قبل تنفيذ خطوات النشر والاختبار.
