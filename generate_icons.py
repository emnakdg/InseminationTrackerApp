from PIL import Image
import os

SRC = r"C:\Users\emin_\Desktop\InseminationTrackerApp\Design\Icon3.png"
BASE = r"C:\Users\emin_\Desktop\InseminationTrackerApp\app\src\main\res"

# Android mipmap sizes (square + round same source)
MIPMAP_SIZES = {
    "mipmap-mdpi":    48,
    "mipmap-hdpi":    72,
    "mipmap-xhdpi":   96,
    "mipmap-xxhdpi":  144,
    "mipmap-xxxhdpi": 192,
}

# Google Play store icon
PLAY_OUT = r"C:\Users\emin_\Desktop\InseminationTrackerApp\Design\ic_launcher_play_store_512.png"

img = Image.open(SRC).convert("RGBA")

# Generate all mipmap sizes
for folder, size in MIPMAP_SIZES.items():
    out_dir = os.path.join(BASE, folder)
    resized = img.resize((size, size), Image.LANCZOS)
    for name in ["ic_launcher.png", "ic_launcher_round.png"]:
        out_path = os.path.join(out_dir, name)
        resized.save(out_path, "PNG")
        print(f"OK {folder}/{name} ({size}x{size})")

# Google Play 512x512
play_icon = img.resize((512, 512), Image.LANCZOS)
play_icon.save(PLAY_OUT, "PNG")
print(f"OK Play Store icon: ic_launcher_play_store_512.png (512x512)")

print("\nTüm ikonlar oluşturuldu!")
