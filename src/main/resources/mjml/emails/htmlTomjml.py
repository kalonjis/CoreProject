"""
Parcourt récursivement un dossier et renomme chaque fichier .html en .mjml.

Usage : python html_to_mjml.py <dossier_source>
"""

import os
import sys
def convert(src_root):
    for dirpath, _, filenames in os.walk(src_root):
        for filename in filenames:
            if not filename.endswith(".html"):
                continue

            src_file = os.path.join(dirpath, filename)
            dst_file = os.path.join(dirpath, filename.replace(".html", ".mjml"))

            os.rename(src_file, dst_file)
            print(f"  {src_file}  →  {dst_file}")


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage : python html_to_mjml.py <dossier_source> [dossier_destination]")
        sys.exit(1)

    src = sys.argv[1]

    if not os.path.isdir(src):
        print(f"Erreur : '{src}' n'est pas un dossier valide.")
        sys.exit(1)

    print(f"Dossier : {src}\n")
    convert(src)
    print("\nTerminé ✓")