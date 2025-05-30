#!/usr/bin/env python3
"""
Script adaptatif pour renommer CoreProject vers n'importe quel nouveau nom.
Usage: python tools/adaptive_rename.py NouveauNomProjet [--dry-run]
"""

import sys
import os
import argparse
from pathlib import Path

# Import du script principal (doit être dans le même dossier)
from rename_project import ProjectRenamer

def main():
    parser = argparse.ArgumentParser(
        description="Renomme CoreProject vers un nouveau nom de projet"
    )
    parser.add_argument(
        'new_name',
        help="Nouveau nom du projet (ex: BaladesEnSabots, TaskManager, etc.)"
    )
    parser.add_argument(
        '--dry-run', 
        action='store_true',
        help="Affiche les modifications sans les appliquer"
    )
    parser.add_argument(
        '--path',
        type=str,
        default='.',
        help="Chemin vers le dossier du projet (défaut: dossier courant)"
    )
    
    args = parser.parse_args()
    
    # Validation du nom
    new_name = args.new_name.strip()
    if not new_name:
        print("❌ Le nom du projet ne peut pas être vide")
        return
    
    if not new_name.replace('_', '').replace('-', '').isalnum():
        print("❌ Le nom du projet doit contenir seulement des lettres, chiffres, _ et -")
        return
    
    # Créer une instance du renommeur avec le nouveau nom
    renamer = ProjectRenamer(dry_run=args.dry_run)
    renamer.old_name = "CoreProject"
    renamer.new_name = new_name
    
    project_path = Path(args.path).resolve()
    
    # Confirmation si ce n'est pas un dry run
    if not args.dry_run:
        print("⚠️  ATTENTION: Cette opération va modifier tous les fichiers du projet!")
        print(f"📂 Dossier: {project_path}")
        print(f"🔄 CoreProject → {new_name}")
        
        response = input("\n❓ Continuer? (oui/non): ").strip().lower()
        if response not in ['oui', 'o', 'yes', 'y']:
            print("❌ Opération annulée")
            return
    
    # Exécuter le renommage
    renamer.run(project_path)
    
    # Instructions post-renommage
    if not args.dry_run and renamer.files_modified > 0:
        print(f"\n🎯 PROCHAINES ÉTAPES pour {new_name}:")
        print("1. 📝 Adaptez votre fichier .env")
        print("2. 📝 Mettez à jour README.md") 
        print("3. ⚡ Testez la compilation: mvn clean compile")
        print("4. 🚀 Créez le repo GitHub et poussez le code")

if __name__ == "__main__":
    main()
