# MedAlarm

> Açık kaynaklı, %100 offline, reklamsız bir ilaç hatırlatma uygulaması.
> _An open-source, 100% offline, ad-free medication reminder app._

**Android · Kotlin · Jetpack Compose · Room**

---

## 🇹🇷 Türkçe

### Vizyon

MedAlarm bir **sosyal sorumluluk projesidir**. İnsanların ilaçlarını
zamanında almasına yardımcı olmak için yazılmıştır. Hiçbir ticari amacı,
kullanıcıdan veri toplama hedefi veya kâr beklentisi yoktur.

### Sorumluluk Reddi

> **MedAlarm bir tıbbi cihaz değildir, tıbbi tavsiye vermez.**
> Uygulama yalnızca hatırlatma aracıdır. İlaçlarınızı düzenli almak,
> doğru dozda kullanmak ve doktorunuzun talimatlarına uymak tamamen
> sizin sorumluluğunuzdadır. Bildirimlerin ulaşmaması durumunda
> oluşabilecek sağlık sorunlarından geliştirici sorumlu tutulamaz.

Detay: [docs/DISCLAIMER.md](docs/DISCLAIMER.md)

### Bloatware-Free Manifesto

Bu uygulamada **hiçbir zaman** şunlar olmayacak:

- ❌ Reklam (AdMob vb.)
- ❌ Analytics / takip kütüphanesi (Firebase, Mixpanel vb.)
- ❌ Crash reporting bulut servisi (Crashlytics vb.)
- ❌ Hesap / login sistemi
- ❌ In-app purchase / "premium" özellik
- ❌ Push notification servisi (FCM yok — tüm bildirimler cihaz lokalinde)
- ❌ İnternet izni (`android.permission.INTERNET` istenmeyecek)

Uygulamanın işlevini yerine getirmesi için **internet bağlantısı asla
gerekmeyecek**. Verileriniz cihazınızdan dışarı çıkmaz.

### MVP Özellikleri

- ✅ İlaç ekleme/düzenleme/silme — ad, doz, ölçü birimi (tablet/ml/mg/damla/sprey/saşe), not, renk
- ✅ Esnek programlama:
  - Günde belirli saatlerde (örn. 08:00, 14:00, 20:00)
  - X saatte bir
  - Haftanın belirli günlerinde
  - Yemekten önce/sonra/ile etiketi
- ✅ Tedavi süresi (başlangıç/bitiş tarihi)
- ✅ Bildirimle hatırlatma + butonlar: **Aldım** · **Ertele** · **Atla**
- ✅ Ayarlanabilir erteleme süresi ve maksimum erteleme sayısı
- ✅ Stok takibi — kalan miktar düşünce bildirim
- ✅ Geçmiş günlüğü — hangi ilaç ne zaman alındı/atlandı
- ✅ TTS — bildirimde ilaç adının sesli okunması
- ✅ JSON yedekleme (manuel export/import)
- ✅ PDF rapor (doktora göstermek için)
- ✅ Modern, minimalist Material 3 arayüz
- ✅ Açık / koyu tema + sistem teması
- ✅ Türkçe + İngilizce
- ✅ Sistem durumu kontrolü (izinler, pil optimizasyonu, autostart)

### Tasarım belgeleri

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — teknik mimari, modüller, alarm akışı
- [docs/DATA_MODEL.md](docs/DATA_MODEL.md) — Room veritabanı şeması
- [docs/PERMISSIONS.md](docs/PERMISSIONS.md) — izinler, sistem durumu sihirbazı
- [docs/DISCLAIMER.md](docs/DISCLAIMER.md) — sorumluluk metni (uygulama içinde de gösterilir)

### Geliştirme durumu

🚧 Erken tasarım aşaması. Henüz kod yok — sadece dokümantasyon.

---

## 🇬🇧 English

### Vision

MedAlarm is a **social responsibility project**. It exists to help
people take their medication on time. It has no commercial purpose,
no data collection goals, and no profit motive.

### Disclaimer

> **MedAlarm is not a medical device and does not provide medical advice.**
> The app is only a reminder tool. Taking your medication on time,
> using the correct dose, and following your doctor's instructions are
> entirely your responsibility. The developer cannot be held liable for
> any health issues arising from missed or undelivered notifications.

Details: [docs/DISCLAIMER.md](docs/DISCLAIMER.md)

### Bloatware-Free Manifesto

This app will **never** include:

- ❌ Advertisements
- ❌ Analytics / tracking SDKs
- ❌ Cloud crash reporting
- ❌ User accounts or login
- ❌ In-app purchases or "premium" features
- ❌ Push notification services (no FCM — all notifications are device-local)
- ❌ Internet permission

The app will **never require an internet connection** to function.
Your data does not leave your device.

### MVP Features

See the Turkish section above; the feature set is identical.

### Design docs

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- [docs/DATA_MODEL.md](docs/DATA_MODEL.md)
- [docs/PERMISSIONS.md](docs/PERMISSIONS.md)
- [docs/DISCLAIMER.md](docs/DISCLAIMER.md)

### Status

🚧 Early design phase. No code yet — documentation only.

---

## License

To be decided. Likely GPL-3.0 or Apache-2.0 to keep the project open and
prevent commercial forks with ads.
