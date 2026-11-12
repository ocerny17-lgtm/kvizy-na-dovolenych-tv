# Kvízy na dovolených – Android TV

Aplikace pro Android TV (set-top box Homatics Box Q), která po spuštění
otevře přes celou obrazovku webovou hru <https://dovolena-krkonose.vercel.app>.
Ovládá se šipkami, OK a Zpět na ovladači – zvýraznění prvků řeší samotný web.

- Kotlin, jedna Activity s WebView, minSdk 24, targetSdk 36
- ikona na hlavní obrazovce Android TV (banner 320×180) i v běžném launcheru
- JavaScript + localStorage (správce zůstává přihlášen), obrazovka nezhasíná
- tlačítko Zpět = zpět v historii webu, na úvodní stránce zavře aplikaci
- odkazy mimo `dovolena-krkonose.vercel.app` se ve WebView neotevřou
- bez internetu se ukáže „Není připojení“ s tlačítkem **Zkusit znovu**
- web pozná aplikaci podle `KvizyTV` na konci User-Agentu

## Přímý odkaz ke stažení

```
https://github.com/<owner>/kvizy-na-dovolenych-tv/releases/latest/download/kvizy-na-dovolenych.apk
```

Každý push na `main` spustí workflow `.github/workflows/release.yml`, které sestaví
podepsané APK a vydá ho jako nový GitHub Release (`v1.<číslo běhu>`, označený jako „Latest“).
Odkaz výše proto vždy vede na nejnovější verzi. `versionCode` = číslo běhu workflow,
takže každá nová verze jde nainstalovat přes starou.

## Podpisový klíč (GitHub Secrets)

Klíč musí být **pořád stejný** – jinak Android aktualizaci odmítne a starou verzi by
bylo nutné nejdřív odinstalovat (a tím by se smazalo i přihlášení správce).

V repozitáři na GitHubu: **Settings → Secrets and variables → Actions → New repository secret**
a založte čtyři secrets:

| Název | Hodnota |
|---|---|
| `SIGNING_KEYSTORE_BASE64` | keystore (`.jks`) zakódovaný do base64, jeden řádek |
| `SIGNING_STORE_PASSWORD` | heslo k úložišti |
| `SIGNING_KEY_ALIAS` | `kvizy` |
| `SIGNING_KEY_PASSWORD` | heslo ke klíči |

Nový klíč (jen pokud byste ho někdy potřebovali vytvořit znovu – pak už staré instalace
nepůjde aktualizovat):

```bash
keytool -genkeypair -v -keystore kvizy-release.jks -storetype PKCS12 -alias kvizy -keyalg RSA -keysize 4096 -validity 36500
base64 -w0 kvizy-release.jks > keystore-base64.txt
```

(`keytool` je součástí JDK, např. `C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe`.)

Složka `keystore/` je v `.gitignore` – klíč se nikdy necommituje. Zálohujte ho mimo počítač.

## Lokální sestavení

```bash
export SIGNING_STORE_FILE=/cesta/kvizy-release.jks SIGNING_STORE_PASSWORD=... SIGNING_KEY_ALIAS=kvizy SIGNING_KEY_PASSWORD=...
./gradlew assembleRelease
```

Bez proměnných `SIGNING_*` vznikne nepodepsané APK (nejde nainstalovat).

Ikonu a banner generuje `java tools/GenerateIcons.java` (z kořene projektu).

## Instalace do boxu přes Downloader

1. Na boxu otevřete Google Play, najděte a nainstalujte aplikaci **Downloader** (od AFTVnews).
2. **Nastavení → Systém → Informace → Sestavení (Build)** – stiskněte 7× OK, tím zapnete
   možnosti pro vývojáře (na některých verzích není potřeba).
3. **Nastavení → Aplikace → Zabezpečení a omezení → Neznámé zdroje** (případně
   *Instalovat neznámé aplikace*) a povolte **Downloader**.
4. Spusťte Downloader, do pole URL zadejte přímý odkaz výše (s vaším GitHub jménem
   místo `<owner>`) a dejte **Go**.
5. Po stažení zvolte **Instalovat**. Po instalaci můžete v Downloaderu APK smazat (**Delete**).
6. Aplikace „Kvízy na dovolených“ se objeví v řadě aplikací na hlavní obrazovce.

Aktualizace: stejný postup se stejným odkazem – nová verze se nainstaluje přes starou
a přihlášení správce zůstane zachováno.

Tip: dlouhý odkaz můžete zkrátit (např. přes Downloader kód z aftv.news nebo bit.ly),
aby se na ovladači psal snáz.
