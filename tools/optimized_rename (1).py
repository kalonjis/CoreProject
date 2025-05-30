#!/usr/bin/env python3
"""
Script universel pour renommer CoreProject vers n'importe quel nouveau nom.
Usage: python rename_project.py NouveauNomProjet [--dry-run]
"""

import os
import re
import argparse
from pathlib import Path
from typing import List, Tuple, Dict

class ProjectRenamer:
    def __init__(self, old_name: str = "CoreProject", new_name: str = "", dry_run: bool = False, keep_git: bool = False):
        self.old_name = old_name
        self.new_name = new_name
        self.dry_run = dry_run
        self.keep_git = keep_git
        self.files_modified = 0
        self.occurrences_replaced = 0
        self.errors = []
        
        # Extensions de fichiers à traiter
        self.text_extensions = {
            '.java', '.xml', '.yml', '.yaml', '.properties', 
            '.txt', '.md', '.json', '.html', '.css', '.js',
            '.sql', '.conf', '.cfg', '.ini', '.log'
        }
        
        # Fichiers et dossiers à ignorer
        self.ignore_patterns = {
            '.git', '.idea', 'target', 'node_modules', '.class',
            '__pycache__', '.pyc', '.jar', '.war'
        }

    def should_process_file(self, file_path: Path) -> bool:
        """Détermine si un fichier doit être traité"""
        # Ignorer les fichiers/dossiers dans les patterns d'exclusion
        for part in file_path.parts:
            if any(pattern in part for pattern in self.ignore_patterns):
                return False
        
        # Traiter seulement les fichiers avec les bonnes extensions
        return file_path.suffix.lower() in self.text_extensions or file_path.suffix == ''

    def process_file(self, file_path: Path) -> Tuple[int, bool]:
        """
        Traite un fichier individuel
        Retourne: (nombre d'occurrences remplacées, fichier modifié)
        """
        try:
            # Essayer différents encodages
            encodings = ['utf-8', 'latin-1', 'cp1252']
            content = None
            encoding_used = None
            
            for encoding in encodings:
                try:
                    with open(file_path, 'r', encoding=encoding) as f:
                        content = f.read()
                    encoding_used = encoding
                    break
                except UnicodeDecodeError:
                    continue
            
            if content is None:
                self.errors.append(f"Impossible de lire {file_path} avec les encodages testés")
                return 0, False
            
            # Compter et remplacer les occurrences
            original_content = content
            occurrences = content.count(self.old_name)
            
            if occurrences == 0:
                return 0, False
            
            # Effectuer le remplacement
            new_content = content.replace(self.old_name, self.new_name)
            
            if not self.dry_run:
                # Écrire le fichier modifié
                with open(file_path, 'w', encoding=encoding_used) as f:
                    f.write(new_content)
            
            return occurrences, True
            
        except Exception as e:
            self.errors.append(f"Erreur lors du traitement de {file_path}: {str(e)}")
            return 0, False

    def process_directory(self, directory: Path) -> None:
        """Traite récursivement un dossier"""
        print(f"🔍 Analyse du dossier: {directory}")
        
        for root, dirs, files in os.walk(directory):
            root_path = Path(root)
            
            # Filtrer les dossiers à ignorer
            dirs[:] = [d for d in dirs if not any(pattern in d for pattern in self.ignore_patterns)]
            
            for file in files:
                file_path = root_path / file
                
                if not self.should_process_file(file_path):
                    continue
                
                occurrences, modified = self.process_file(file_path)
                
                if modified:
                    self.files_modified += 1
                    self.occurrences_replaced += occurrences
                    status = "🔄 [DRY RUN]" if self.dry_run else "✅ Modifié"
                    print(f"  {status} {file_path.relative_to(directory)} ({occurrences} occurrences)")

    def rename_directories_and_files(self, base_path: Path) -> None:
        """Renomme les dossiers et fichiers contenant l'ancien nom"""
        if self.dry_run:
            print(f"\n🔄 [DRY RUN] Recherche des dossiers/fichiers à renommer...")
        else:
            print(f"\n📁 Renommage des dossiers et fichiers...")
        
        items_to_rename = []
        
        # Collecter tous les éléments à renommer
        for root, dirs, files in os.walk(base_path, topdown=False):
            root_path = Path(root)
            
            # Fichiers à renommer
            for file in files:
                if self.old_name in file:
                    old_path = root_path / file
                    new_filename = file.replace(self.old_name, self.new_name)
                    new_path = root_path / new_filename
                    items_to_rename.append(('file', old_path, new_path))
            
            # Dossiers à renommer
            for dir_name in dirs:
                if self.old_name in dir_name:
                    old_path = root_path / dir_name
                    new_dirname = dir_name.replace(self.old_name, self.new_name)
                    new_path = root_path / new_dirname
                    items_to_rename.append(('dir', old_path, new_path))
        
        # Effectuer les renommages
        for item_type, old_path, new_path in items_to_rename:
            try:
                if self.dry_run:
                    print(f"  🔄 [DRY RUN] {item_type}: {old_path} → {new_path}")
                else:
                    old_path.rename(new_path)
                    print(f"  ✅ {item_type}: {old_path.name} → {new_path.name}")
            except Exception as e:
                self.errors.append(f"Erreur lors du renommage de {old_path}: {str(e)}")

    def clean_git_history(self, project_path: Path) -> None:
        """Supprime l'historique Git pour éviter les push accidentels vers l'ancien repo"""
        if self.keep_git:
            print(f"\n⚠️  Historique Git conservé (--keep-git activé)")
            print(f"  🚨 ATTENTION: Vous devez changer l'origine Git manuellement !")
            return
            
        if self.dry_run:
            print(f"\n🔄 [DRY RUN] Nettoyage de l'historique Git...")
        else:
            print(f"\n🧹 Nettoyage de l'historique Git...")
        
        git_folder = project_path / '.git'
        gitattributes_file = project_path / '.gitattributes'
        
        # Supprimer le dossier .git
        if git_folder.exists():
            try:
                if self.dry_run:
                    print(f"  🔄 [DRY RUN] Suppression du dossier .git/")
                else:
                    import shutil
                    shutil.rmtree(git_folder)
                    print(f"  ✅ Dossier .git/ supprimé")
            except Exception as e:
                self.errors.append(f"Erreur lors de la suppression de .git/: {str(e)}")
        
        # Supprimer .gitattributes s'il existe
        if gitattributes_file.exists():
            try:
                if self.dry_run:
                    print(f"  🔄 [DRY RUN] Suppression de .gitattributes")
                else:
                    gitattributes_file.unlink()
                    print(f"  ✅ Fichier .gitattributes supprimé")
            except Exception as e:
                self.errors.append(f"Erreur lors de la suppression de .gitattributes: {str(e)}")
        
        # Vérifier si .gitignore existe (on le garde)
        gitignore_file = project_path / '.gitignore'
        if gitignore_file.exists():
            print(f"  ℹ️  Fichier .gitignore conservé pour le nouveau projet")

    def run(self, project_path: Path) -> None:
        """Exécute le processus complet de renommage"""
        if not project_path.exists():
            print(f"❌ Le chemin {project_path} n'existe pas")
            return
        
        if not project_path.is_dir():
            print(f"❌ {project_path} n'est pas un dossier")
            return
        
        print(f"🚀 {'[DRY RUN] ' if self.dry_run else ''}Remplacement de '{self.old_name}' par '{self.new_name}'")
        print(f"📂 Dossier: {project_path.absolute()}")
        print("-" * 60)
        
        # Nettoyer l'historique Git en premier (sécurité)
        self.clean_git_history(project_path)
        
        # Traiter les fichiers
        self.process_directory(project_path)
        
        # Renommer les dossiers et fichiers
        self.rename_directories_and_files(project_path)
        
        # Afficher le résumé
        self.show_summary()

    def show_summary(self) -> None:
        """Affiche un résumé des modifications"""
        print("\n" + "=" * 60)
        print("📊 RÉSUMÉ")
        print("=" * 60)
        
        if self.dry_run:
            print("🔄 MODE DRY RUN - Aucune modification effectuée")
        
        print(f"📄 Fichiers traités: {self.files_modified}")
        print(f"🔄 Occurrences remplacées: {self.occurrences_replaced}")
        
        if self.errors:
            print(f"\n❌ Erreurs ({len(self.errors)}):")
            for error in self.errors[:10]:  # Limiter à 10 erreurs
                print(f"  • {error}")
            if len(self.errors) > 10:
                print(f"  • ... et {len(self.errors) - 10} autres erreurs")
        
        if not self.dry_run and self.files_modified > 0:
            print(f"\n✅ Remplacement terminé avec succès!")
            print(f"🎉 Le projet '{self.old_name}' est maintenant '{self.new_name}'")
            print(f"\n🎯 PROCHAINES ÉTAPES pour {self.new_name}:")
            print("1. 📝 Adaptez votre fichier .env")
            print("2. 📝 Mettez à jour README.md") 
            print("3. ⚡ Testez la compilation: mvn clean compile")
            print("4. 🚀 Créez le repo GitHub et poussez le code")
        elif self.dry_run:
            print(f"\n💡 Exécutez sans --dry-run pour appliquer les modifications")

def validate_project_name(name: str) -> bool:
    """Valide le nom du projet"""
    if not name or not name.strip():
        return False
    
    # Autoriser lettres, chiffres, underscore et tiret
    if not re.match(r'^[a-zA-Z0-9_-]+$', name):
        return False
    
    # Le nom doit commencer par une lettre
    if not name[0].isalpha():
        return False
        
    return True

def main():
    parser = argparse.ArgumentParser(
        description="Renomme CoreProject vers un nouveau nom de projet",
        epilog="Exemples:\n"
               "  python rename_project.py TaskManager --dry-run\n"
               "  python rename_project.py EcommerceApp\n"
               "  python rename_project.py BlogEngine --path /autre/chemin",
        formatter_class=argparse.RawDescriptionHelpFormatter
    )
    
    parser.add_argument(
        'new_name',
        nargs='?',  # Optionnel pour compatibilité
        help="Nouveau nom du projet (ex: TaskManager, BlogEngine, etc.)"
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
    parser.add_argument(
        '--old-name',
        type=str,
        default='CoreProject',
        help="Ancien nom à remplacer (défaut: CoreProject)"
    )
    
    args = parser.parse_args()
    
    # Si pas de nouveau nom fourni, demander interactivement
    if not args.new_name:
        print("🏷️  Renommage de projet CoreProject")
        print("-" * 40)
        while True:
            new_name = input("📝 Nouveau nom du projet: ").strip()
            if validate_project_name(new_name):
                args.new_name = new_name
                break
            else:
                print("❌ Nom invalide. Utilisez uniquement lettres, chiffres, _ et - (doit commencer par une lettre)")
    
    # Validation du nom
    if not validate_project_name(args.new_name):
        print("❌ Nom de projet invalide. Utilisez uniquement lettres, chiffres, _ et - (doit commencer par une lettre)")
        return
    
    project_path = Path(args.path).resolve()
    
    # Confirmation si ce n'est pas un dry run
    if not args.dry_run:
        print("⚠️  ATTENTION: Cette opération va modifier tous les fichiers du projet!")
        print(f"📂 Dossier: {project_path}")
        print(f"🔄 {args.old_name} → {args.new_name}")
        
        response = input("\n❓ Continuer? (oui/non): ").strip().lower()
        if response not in ['oui', 'o', 'yes', 'y']:
            print("❌ Opération annulée")
            return
    
    # Exécuter le renommage
    renamer = ProjectRenamer(
        old_name=args.old_name,
        new_name=args.new_name,
        dry_run=args.dry_run
    )
    renamer.run(project_path)

if __name__ == "__main__":
    main()
