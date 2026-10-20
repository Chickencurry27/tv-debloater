"""Download 12 nature wallpapers (1920px wide) from Wikimedia Commons featured landscape pictures
into launcher/assets/wallpapers/ and write WALLPAPER-CREDITS.md with author and licence."""
import json, re, sys, time, urllib.parse, urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "launcher" / "assets" / "wallpapers"
UA = "tv-debloater/1.0 (personal project; contact via GitHub)"
TITLES = [
    "File:2013 Cogden Bridge.jpg",
    "File:2013 Rainbow over Washfold.jpg",
    "File:2015 Swaledale from Kisdon Hill.jpg",
    "File:2019 - Parc national des Pyrenees - Vallée de Gavarnie.jpg",
    "File:2909 vallon moy res.jpg",
    "File:Alone in the unspoilt wilderness (Unsplash).jpg",
    "File:Amanecer en el lago Titicaca, Puno, Perú, 2015-08-01, DD 01.JPG",
    "File:Amanhecer no Hercules --.jpg",
    "File:Andiast-Breil-Brigels. 24-09-2025. (d.j.b.) 08.jpg",
    "File:Anfiteatro, Valle de la Luna, San Pedro de Atacama, Chile, 2016-02-01, DD 149.JPG",
    "File:Araucárias ao fundo Parque Nacional da Serra da Bocaina - denoise.jpg",
    "File:Aguarales de Valpalmas, Zaragoza, España, 2015-01-06, DD 26.JPG",
]


def get(url):
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=60) as r:
        return r.read()


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    q = urllib.parse.urlencode({
        "action": "query", "format": "json", "titles": "|".join(TITLES), "prop": "imageinfo",
        "iiprop": "url|extmetadata", "iiurlwidth": 1920,
        "iiextmetadatafilter": "LicenseShortName|Artist",
    })
    data = json.loads(get("https://commons.wikimedia.org/w/api.php?" + q))
    info = {p["title"]: p["imageinfo"][0] for p in data["query"]["pages"].values() if "imageinfo" in p}
    credits = ["# Wallpaper credits", "",
               "Photos from Wikimedia Commons featured pictures, scaled to 1920 px wide. Each keeps its own licence.", ""]
    for n, title in enumerate(TITLES, 1):
        ii = info.get(title)
        if not ii:
            print("missing", title, file=sys.stderr)
            continue
        (OUT / f"wp{n:02d}.jpg").write_bytes(get(ii["thumburl"]))
        m = ii["extmetadata"]
        artist = re.sub(r"<[^>]+>", "", m.get("Artist", {}).get("value", "unknown")).strip()
        lic = m.get("LicenseShortName", {}).get("value", "unknown")
        credits.append(f"- wp{n:02d}.jpg: {title[5:]} by {artist}, {lic}. https://commons.wikimedia.org/wiki/{urllib.parse.quote(title.replace(' ', '_'))}")
        print("ok", n, title)
        time.sleep(3)  # be polite to Wikimedia
    (ROOT / "WALLPAPER-CREDITS.md").write_text("\n".join(credits) + "\n")


main()
