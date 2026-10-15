# FlexPad: Sanal Gamepad Sadeleştirme, Kesilme Düzeltmesi ve Tam Kontrol Özelleştirme Planı

Bu plan; test alanının kaldırılması, ekran kontrollerinin yarım/kesilmiş görünme hatasının kökten çözülmesi, hem joysticklerin hem de tüm tuşların serbestçe silinip eklenebilmesi, ana ekranın sade ve rahat bir arayüze kavuşturulması ve uygulamanın en yüksek akıcılıkta optimize edilmesi adımlarını belirler.

---

## Kullanıcı İncelemesi ve Kritik Kararlar

> [!IMPORTANT]
> Aşağıdaki kritik mimari kararlar kullanıcı talepleri doğrultusunda belirlenmiştir:

- **Test Alanının Kaldırılması:** Ana sayfadaki karmaşık test alanı tamamen kaldırılacak; ana ekran doğrudan katman başlatma, profil seçimi, tek tıkla özelleştirme ve izin durumlarına odaklanacaktır.
- **Ekran Kontrollerinin Yarım/Kesik Görünme Hatası (Kritik Düzeltme):**
  - *Kök Neden:* `GamepadOverlayService` içindeki `WindowManager.LayoutParams` (x, y) koordinatları pencereyi ekrana yerleştirirken, Composable içindeki `Modifier.offset` formülü ekran oranlarını tekrar hesaba katarak kontrolleri pencere sınırlarının dışına taşıyor ve kontrollerin yarısının kesilmesine neden oluyordu.
  - *Çözüm:* Modüler yüzen pencereler için `VirtualJoystick` ve `VirtualButton` bileşenlerine pencere içi merkezleme parametresi eklenerek çift offset hatası sıfırlanacak; kontroller %100 tam ve yuvarlak olarak ekranda görünecektir.
- **Joystick ve Tuş Silme/Ekleme Özgürlüğü:**
  - `GamepadProfile` içindeki `leftJoystick` ve `rightJoystick` yapılandırmalarına `enabled: Boolean` ve silinebilirlik desteği eklenecektir.
  - Özelleştirme ekranında joystickler için "Joystick'i Sil / Gizle" butonu bulunacak; silinmişse üst çubukta "+ Sol Joystick Ekle" ve "+ Sağ Joystick Ekle" seçenekleri çıkacaktır.
  - Her buton (A, B, X, Y, Tetikler, Yön tuşları dahil) istisnasız silinebilecek ve yeni butonlar eklenebilecektir.

---

## 1. Genel Bakış ve Temel Konsept

FlexPad; mobil oyuncuların diğer oyunların üzerinde şeffaf, modüler ve oyun içi dokunmaları engellemeyen sanal gamepad kontrolleri kullanmasını sağlayan yüksek performanslı bir araçtır.
Uygulama; karmaşık menülerden arındırılarak sadece gerekli fonksiyonları içeren, tek tıkla özelleştirilebilen ve anında çalışan sade bir yapıya dönüştürülecektir.

---

## 2. Kullanıcı Deneyimi ve Görsel Tasarım

```
┌──────────────────────────────────────────────────────────────┐
│                    FLEXPAD GAMEPAD                           │
│           Modüler Kontrol & Gerçek Giriş Motoru             │
├──────────────────────────────────────────────────────────────┤
│ ┌──────────────────────────────────────────────────────────┐ │
│ │  YÜZEN KATMAN KONTROLÜ                [ AÇIK / KAPALI ]  │ │
│ │  Oyunun üzerinde kontrolleri başlat                      │ │
│ │                                                          │ │
│ │  [ 1. Profil (Standart) ]   [ 2. Profil (FPS/Nişancı) ]  │ │
│ └──────────────────────────────────────────────────────────┘ │
│                                                              │
│ ┌──────────────────────────────────────────────────────────┐ │
│ │  🎮 KONTROLLERİ ÖZELLEŞTİR (TAM EKRAN STÜDYO)            │ │
│ │  Tuşları taşıyın, büyütün, şeffaflığı ayarlayın, silin   │ │
│ └──────────────────────────────────────────────────────────┘ │
│                                                              │
│ ┌──────────────────────────────────────────────────────────┐ │
│ │  GİRİŞ MOTORU & İZİNLER                                  │ │
│ │  ✓ Overlay İzni           ✓ Accessibility (Dokunma)     │ │
│ │  [ Accessibility ]        [ Shizuku ADB ]                │ │
│ └──────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────┘
```

### Kullanıcı Akışı:
1. **Ana Ekran:** Kullanıcı uygulamayı açtığında karışık test alanları görmez; doğrudan büyük başlatma anahtarı, Profil 1/Profil 2 seçici ve belirgin sarı "🎮 Kontrolleri Özelleştir" butonu yer alır.
2. **Özelleştirme:** "Özelleştir" butonuna basıldığında ekranı tam kaplayan stüdyo açılır:
   - Joystick veya tuş sürüklenirken sıfır titreme ile akıcı hareket eder.
   - Joystick'e tıklanarak boyutu, deadzone'u ayarlanabilir veya istenirse tamamen silinebilir.
   - Herhangi bir tuşa tıklanarak adı, boyutu, rengi, tuş kodu değiştirilebilir veya silinebilir.
   - "+ Tuş Ekle", "+ Sol Stick", "+ Sağ Stick" butonlarıyla istenen kontroller geri getirilebilir.
3. **Katman Başlatıldığında:** Ekranda yarım kesilmiş tuşlar olmaz; joystickler ve tuşlar tam boyutlarıyla ekrana oturur ve dokunulduğu an anında tepki verir.

---

## 3. Ürün Kararları ve Mimari Tercihler

1. **Test Alanının Kaldırılması:**
   - *Gerekçe:* Test alanı ana sayfayı uzatıyor, karmaşıklaştırıyor ve kullanıcı odağını dağıtıyordu. Kontroller zaten yüzen pencerede veya tam ekran stüdyoda gerçek zamanlı test edilebilmektedir.
2. **Modüler Yüzen Pencerelerde Bağımsız Koordinat Sistemi:**
   - *Gerekçe:* `WindowManager` zaten pencerenin (x, y) konumunu ekrana göre belirlediği için, içerideki Compose View'ın tekrar `Modifier.offset` uygulaması görüntüyü pencere dışına itiyordu. WindowManager pencerelerinde `isStandaloneWindow = true` parametresiyle offset sıfırlanacak ve kesilme tamamen giderilecektir.
3. **Joystick Opsiyonelliği:**
   - *Gerekçe:* Bazı oyunlarda yalnızca tek joystick (örneğin sadece hareket) veya yalnızca tuşlar gerekebilir. `leftJoystick` ve `rightJoystick` için aktiflik/silme bayrağı eklenerek kullanıcıya tam özgürlük sunulacaktır.

---

## 4. Teknik Mimari ve Bileşen Haritası

```
┌──────────────────────────────────────────────────────────────┐
│                     HomeScreen.kt                            │
│  - Sadeleştirilmiş Dashboard                                │
│  - Overlay Başlatıcı Switch                                 │
│  - Profil 1 / Profil 2 Hızlı Seçici                         │
│  - "Özelleştir" Butonu -> FullScreenCustomizeStudio          │
└───────────────────────┬──────────────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────────┐
│            FullScreenCustomizeStudio.kt                      │
│  - Tam Ekran Boyut Uyumlu Canvas                             │
│  - Titremesiz Sürükleme (Local Drag State)                   │
│  - Joystick Düzenleme / Silme Menüsü                         │
│  - Tuş Düzenleme / Silme (ButtonEditDialog)                  │
│  - "+ Tuş Ekle" / "+ Joystick Ekle" Butonları                │
└───────────────────────┬──────────────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────────┐
│               GamepadOverlayService.kt                       │
│  - Kesilmesiz Modüler WindowManager Pencereleri              │
│  - LeftJoy Window (Sıfır iç offset, tam daire)               │
│  - RightJoy Window (Sıfır iç offset, tam daire)              │
│  - Tuş Pencereleri (Sıfır iç offset, tam şekil)              │
│  - Ekranın geri kalanında %0 görünmez engelleyici katman     │
└──────────────────────────────────────────────────────────────┘
```

### Yapılacak Somut Değişiklikler:
1. `TestArenaScreen.kt` dosyasını ve `HomeScreen.kt` içindeki test alanı bileşenlerini kaldırmak. [TAMAMLANDI]
2. `VirtualJoystick.kt` ve `VirtualButton.kt` bileşenlerine `applyScreenOffset: Boolean = true` parametresi eklemek (overlay içinde `false` olacak, böylece pencereler kesilmeyecek). [TAMAMLANDI]
3. `GamepadProfile.kt` ve `DefaultProfiles.kt` modellerinde joystick'leri etkinleştirilebilir / silinebilir (`enabled = true/false`) hale getirmek. [TAMAMLANDI]
4. `FullScreenCustomizeStudio.kt` içine Joystick Düzenleme & Silme diyaloğu eklemek (`JoystickEditDialog`). [TAMAMLANDI]
5. `HomeScreen.kt` arayüzünü minimalist, ferah ve modern bir karta dönüştürmek. [TAMAMLANDI]
6. Derleme ve testleri (`compile_applet`, `gradle testDebugUnitTest`) koşturup doğrulamak. [TAMAMLANDI - BUILD SUCCESSFUL]

