# يقين — خادم وسيط لمحتوى القرآن

هذا Worker اختياري ومخصص لتمرير طلبات Quran Foundation Content API من تطبيق Android دون وضع `client_secret` داخله.

## مهم قبل النشر

- أنشئ تطبيقًا من نوع **Backend/server** في Quran Foundation Developer Console.
- اطلب صلاحيات Content التي تحتاجها فقط. الإعداد الافتراضي هنا يطلب:
  - `content.quran.read`
  - `content.audio.read`
  - `content.tafsirs.read`
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
5. نفّذ:
   `npx wrangler deploy`
6. اختبر `/health`، ثم بعد تهيئة الاعتمادات اختبر `/api/quran/chapters`.
7. بعد نجاح النشر، أضف عنوان Worker كمتغير مستودع GitHub باسم `QF_PROXY_BASE_URL` (ليس سرًا)، أو مرره محليًا كخاصية Gradle بالاسم نفسه. سيُضمَّن العنوان فقط في BuildConfig؛ لا تُضمَّن بيانات الاعتماد.

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

يحفظ Worker رمز OAuth2 في ذاكرة الـisolate حتى يقترب من انتهاء صلاحيته، ويعيد طلبه مرة واحدة عند استجابة 401. لا يسجل الرمز ولا يعيده للعميل.
- قبل مشاركة عنوان Worker مع التطبيق، فعّل Cloudflare Rate Limiting على المسارات `/api/quran/*` (مثل حدّ طلبات مناسب لكل IP)، وراقب استهلاك API. الواجهة للقراءة فقط ولا تقبل POST/PUT/DELETE، لكن عنوان Worker عام وليس سرًا.

## ما لم يُنجز بعد

- لم يُنشر Worker إلى حساب Cloudflare.
- لم تُضف أي مفاتيح أو أسرار إلى المستودع.
- لم يُربط تطبيق Android بعنوان Worker بعد.
- لم تُختبر صلاحيات production أو بياناتها.
- يلزم التحقق من endpoint الفعلي لتوقيت الآيات/الكلمات ومن تغطية كل تسجيل قبل تشغيل التظليل المتزامن.

لا تعتبر هذه الملفات خادمًا منشورًا أو تكاملًا مكتملًا قبل تنفيذ خطوات النشر والاختبار.
