# Sürüm Notları

Bu dosya her sürümde son kullanıcıya yönelik "Yenilikler" notlarını tutar.
Yayın iş akışı (`.github/workflows/release.yml`), bir `vX.Y.Z` etiketi gönderildiğinde
o etikete karşılık gelen bölümü otomatik olarak GitHub Release açıklamasına koyar.

> Biçim: her sürüm için `## vX.Y.Z-...` başlığı, altında madde işaretli notlar.
> Başlık, etiket adıyla birebir aynı olmalı (örn. etiket `v0.1.0-beta12` → başlık `## v0.1.0-beta12`).
> Notları sade ve kullanıcı diliyle yazın; commit/PR referansı vermeyin.

## v0.1.0-beta15
- **İlaç ekleme artık önce soruyor:** "İlaç ekle"ye dokununca ilacın **düzenli mi yoksa tek seferlik mi** kullanılacağını seçiyorsun.
  - Düzenli: alışılmış tam form (program, tedavi süresi, stok…).
  - Tek seferlik: yalnızca ilaç bilgisi, doz, ne zaman alınacağı (gün + saat) ve isteğe bağlı fotoğraf — sade ve hızlı.

## v0.1.0-beta14
- **Stok düzenleme** eklendi: ilaç detayında "Düzenle" ile mevcut stok miktarını doğrudan istediğin değere ayarlayabilirsin (önceden yalnızca üzerine ekleme vardı).
- **Tek seferlik / tek gün ilaç** seçeneği: tedavi süresinde "Tek gün" modunu seçip ilacın yalnızca belirli bir günde, belirlediğin saatlerde hatırlatılmasını sağlayabilirsin.
- Ayarlar → Hakkında bölümüne geliştirici ve kaynak kodu (GitHub) bağlantıları eklendi.

## v0.1.0-beta13
- Kırpma ekranındaki **onayla (✓), döndür ve iptal** düğmeleri görünmüyordu; artık üst çubukta görünüyor, böylece kırpmayı tamamlayıp ekrandan çıkabilirsin.

## v0.1.0-beta12
- İlaç fotoğrafı eklerken artık **kırpma ekranı** geliyor: kamerayla çektiğin ya da galeriden seçtiğin fotoğrafı yakınlaştırıp, kaydırıp, döndürerek sadece ilaç kutusunu çerçeveleyebilirsin.

## v0.1.0-beta11
- **Fotoğraf ekleme düzeltildi.** Kamera ve galeri seçimi görünüyordu ama fotoğraf bir türlü eklenmiyordu; artık doğru çalışıyor. Bir sorun olursa ekranda uyarı gösteriliyor.

## v0.1.0-beta10
- Kamerayla çekilen fotoğrafın bazı telefonlarda kaybolması düzeltildi.

## v0.1.0-beta9
- İlaç kutusu fotoğrafı artık ana ekrandaki doz penceresinde ve ilaç detayında görünüyor — hangi ilacı alacağın bir bakışta belli.
- Fotoğrafı görüntüleme (büyütme) ve silme seçenekleri eklendi. Fotoğraf ekranı kaplamıyor, orantılı gösteriliyor.

## v0.1.0-beta8
- **Hatırlatmalar artık akıllı saate de düşüyor.**
- Yanıtlanmayan hatırlatma, belirlenen aralıkta yeniden ses/titreşimle uyarıyor (önceden bazı telefonlarda tek sefer geliyordu).

## v0.1.0-beta7
- İlaçlara kutu fotoğrafı ekleme ve stok genel bakış ekranı eklendi.

## v0.1.0-beta6
- Daha renkli ve modern arayüz; yaşlı kullanıcılar için daha kolay kullanım.
- Doz kartlarını sağa/sola kaydırarak hızlı işlem (alındı / ertele / atla) — Ayarlar'dan değiştirilebilir.
- Büyük yazı modu.
