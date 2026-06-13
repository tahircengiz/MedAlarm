# MedAlarm

> Açık kaynaklı, %100 offline, reklamsız bir ilaç hatırlatma uygulaması.
> _An open-source, 100% offline, ad-free medication reminder app._

**Android · Kotlin · Jetpack Compose · Room**

---

## 📥 İndir / Download

En güncel sürümü ve önceki tüm sürümleri buradan bulabilirsiniz:

### ➡️ **[Tüm sürümler / All releases](https://github.com/tahircengiz/MedAlarm/releases)**

**Nasıl kurulur (Türkçe):**
1. Yukarıdaki bağlantıyı açın; en üstteki sürüm en yenisidir.
2. O sürümün **Assets** bölümündeki `MedAlarm-vX.Y.Z.apk` dosyasını telefonunuza indirin.
3. Dosyayı açın ve "bilinmeyen kaynaklardan kuruluma izin ver" isteğini onaylayın.
4. İlk açılışta kurulum sihirbazını tamamlayın — özellikle **pil optimizasyonu muafiyeti** ve (Xiaomi/Samsung/Huawei vb.) **arka planda çalışma** izinlerini verin, yoksa hatırlatmalar gecikebilir.

**How to install (English):** Open the [releases page](https://github.com/tahircengiz/MedAlarm/releases), download the `MedAlarm-vX.Y.Z.apk` from the **Assets** of the topmost (newest) release, open it on your Android phone, and allow installation from this source. Complete the on-boarding permission wizard on first launch.

> ℹ️ Sürümler şu an **beta** olarak işaretlidir. Her sürümün açıklamasında o sürümdeki yenilikler **Türkçe** olarak listelenir — değişiklik geçmişi için [CHANGELOG.md](CHANGELOG.md) dosyasına da bakabilirsiniz.

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
- ✅ Renkli, modern Material 3 arayüz — yaşlı kullanıcılar için kolay kullanım
- ✅ İlaç kutusu fotoğrafı ekleme (kamera/galeri) + kırpma — hangi ilaç olduğu bir bakışta belli
- ✅ Sağa/sola kaydırarak hızlı işlem (alındı / ertele / atla), ayarlardan değiştirilebilir
- ✅ Büyük yazı modu (düşük görme / yaşlı kullanıcılar için)
- ✅ Akıllı saate düşen bildirimler + yanıtlanmazsa tekrar uyarı
- ✅ Açık / koyu tema + sistem teması
- ✅ Türkçe + İngilizce
- ✅ Sistem durumu kontrolü (izinler, pil optimizasyonu, autostart)

### Tasarım belgeleri

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — teknik mimari, modüller, alarm akışı
- [docs/DATA_MODEL.md](docs/DATA_MODEL.md) — Room veritabanı şeması
- [docs/PERMISSIONS.md](docs/PERMISSIONS.md) — izinler, sistem durumu sihirbazı
- [docs/DISCLAIMER.md](docs/DISCLAIMER.md) — sorumluluk metni (uygulama içinde de gösterilir)

### Geliştirme durumu

🚀 **Beta yayında.** Uygulama çalışır durumda ve aktif geliştiriliyor.
APK'yı yukarıdaki [İndir / Download](#-i̇ndir--download) bölümünden alabilirsiniz.

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

🚀 **Beta released.** The app is functional and under active development.
Get the APK from the [Download](#-i̇ndir--download) section above.

---

## License

Licensed under the **GNU General Public License v3.0** — see [LICENSE](LICENSE).

This copyleft license keeps the project open and prevents closed-source
commercial forks (e.g. re-releases with ads). You are free to use, study,
share, and modify the app, provided derivative works remain under GPL-3.0.
