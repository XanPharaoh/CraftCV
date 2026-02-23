import urllib.request
import os

fonts = {
    "inter_regular.ttf":   "https://github.com/google/fonts/raw/main/ofl/inter/static/Inter-Regular.ttf",
    "inter_medium.ttf":    "https://github.com/google/fonts/raw/main/ofl/inter/static/Inter-Medium.ttf",
    "inter_semibold.ttf":  "https://github.com/google/fonts/raw/main/ofl/inter/static/Inter-SemiBold.ttf",
    "inter_bold.ttf":      "https://github.com/google/fonts/raw/main/ofl/inter/static/Inter-Bold.ttf"
}

dest_dir = r"d:\Resume Tailer\resume-tailor-android\app\src\main\res\font"
if not os.path.exists(dest_dir):
    os.makedirs(dest_dir)

for name, url in fonts.items():
    dest_path = os.path.join(dest_dir, name)
    print(f"Downloading {name}...")
    try:
        urllib.request.urlretrieve(url, dest_path)
        print(f"  Saved to {dest_path}")
    except Exception as e:
        print(f"  Error: {e}")
