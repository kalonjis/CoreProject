#!/usr/bin/env python3
"""
Script universel pour renommer CoreProject vers n'importe quel nouveau nom.
Usage: python rename.py NouveauNomProjet

IMPORTANT: Ce script doit être placé au même niveau que le dossier "CoreProject"
Structure requise:
  votre_dossier/
  ├── rename.py          (ce script)
  └── CoreProject/       (le projet à renommer)

Le script exécute automatiquement un dry-run, affiche le résumé, puis demande confirmation.
"""

import os
import re
import argparse
import shutil
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
        self.directories_renamed = 0
        self.main_directory_renamed = False
        self.errors = []

        # Extensions de fichiers à traiter
        self.text_extensions = {
            '.java', '.xml', '.yml', '.yaml', '.properties',
            '.txt', '.md', '.json', '.html', '.css', '.js',
            '.sql', '.conf', '.cfg', '.ini', '.log', '.sh'
        }

        # Fichiers et dossiers à ignorer
        self.ignore_patterns = {
            '.git', '.idea', 'target', 'node_modules', '.class',
            '__pycache__', '.pyc', '.jar', '.war', '.DS_Store'
        }

    def detect_and_validate_project(self) -> Path:
        """Détecte et valide le projet CoreProject dans le répertoire du script"""
        # Utiliser le répertoire où se trouve le script, pas le répertoire courant d'exécution
        script_dir = Path(__file__).parent.resolve()
        project_path = script_dir / self.old_name

        print(f"🔍 Recherche du projet {self.old_name} dans: {script_dir}")
        print(f"📂 Chemin attendu: {project_path}")

        # Vérifier que le dossier CoreProject existe
        if not project_path.exists():
            print(f"❌ Dossier '{self.old_name}' non trouvé dans le répertoire du script")
            print(f"💡 Structure attendue:")
            print(f"   {script_dir}/")
            print(f"   ├── rename.py          (ce script)")
            print(f"   └── {self.old_name}/       (le projet à renommer)")
            raise FileNotFoundError(f"Dossier {self.old_name} non trouvé")

        if not project_path.is_dir():
            print(f"❌ '{self.old_name}' existe mais n'est pas un dossier")
            raise NotADirectoryError(f"{self.old_name} n'est pas un dossier")

        print(f"✅ Dossier '{self.old_name}' trouvé")

        # Valider que c'est bien un projet CoreProject
        if not self.validate_project(project_path):
            raise ValueError(f"Le dossier {self.old_name} ne semble pas être un projet valide")

        return project_path

    def validate_project(self, project_path: Path) -> bool:
        """Valide que c'est bien un projet CoreProject"""
        print(f"🔍 Validation du projet dans: {project_path}")

        # Vérifier pom.xml
        pom_file = project_path / "pom.xml"
        if not pom_file.exists():
            print(f"❌ Fichier pom.xml non trouvé dans {project_path}")
            return False

        # Vérifier src/
        src_dir = project_path / "src"
        if not src_dir.exists():
            print(f"❌ Dossier src/ non trouvé dans {project_path}")
            return False

        # Vérifier la présence de CoreProject dans pom.xml
        try:
            with open(pom_file, 'r', encoding='utf-8') as f:
                content = f.read()
                if self.old_name not in content:
                    print(f"❌ '{self.old_name}' non trouvé dans pom.xml")
                    print(f"💡 Contenu du pom.xml (100 premiers caractères):")
                    print(f"   {content[:100]}...")
                    return False
        except Exception as e:
            print(f"❌ Erreur lors de la lecture de pom.xml: {e}")
            return False

        print(f"✅ Projet {self.old_name} validé dans: {project_path}")
        return True

    def should_process_file(self, file_path: Path) -> bool:
        """Détermine si un fichier doit être traité"""
        # Ignorer les fichiers/dossiers dans les patterns d'exclusion
        for part in file_path.parts:
            if any(pattern in part for pattern in self.ignore_patterns):
                return False

        # Traiter seulement les fichiers avec les bonnes extensions ou sans extension
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
                    if not self.dry_run:  # Afficher le détail seulement lors de l'exécution réelle
                        status = "✅ Modifié"
                        print(f"  {status} {file_path.relative_to(directory)} ({occurrences} occurrences)")

    def rename_project_directory(self, project_path: Path) -> Path:
        """Renomme le dossier principal du projet"""
        parent_dir = project_path.parent
        new_project_path = parent_dir / self.new_name

        # Marquer que le dossier principal sera renommé
        self.main_directory_renamed = True

        if not self.dry_run:
            print(f"\n📁 Renommage du dossier principal...")
            try:
                project_path.rename(new_project_path)
                print(f"  ✅ {self.old_name}/ → {self.new_name}/")
            except Exception as e:
                self.errors.append(f"Erreur lors du renommage du dossier principal: {str(e)}")
                return project_path

        return new_project_path

    def rename_directories_and_files(self, base_path: Path) -> None:
        """Renomme les dossiers et fichiers contenant l'ancien nom"""
        if not self.dry_run:
            print(f"\n📁 Renommage des dossiers et fichiers internes...")

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
                if not self.dry_run:
                    old_path.rename(new_path)
                    print(f"  ✅ {item_type}: {old_path.name} → {new_path.name}")

                # Compter les dossiers renommés
                if item_type == 'dir':
                    self.directories_renamed += 1

            except Exception as e:
                self.errors.append(f"Erreur lors du renommage de {old_path}: {str(e)}")

    def clean_git_history(self, project_path: Path) -> None:
        """Supprime l'historique Git pour éviter les push accidentels vers l'ancien repo"""
        if self.keep_git:
            if not self.dry_run:
                print(f"\n⚠️  Historique Git conservé (--keep-git activé)")
                print(f"  🚨 ATTENTION: Vous devez changer l'origine Git manuellement !")
                print(f"  💡 Commandes suggérées:")
                print(f"     cd {self.new_name}")
                print(f"     git remote remove origin")
                print(f"     git remote add origin https://github.com/USER/{self.new_name}.git")
            return

        if not self.dry_run:
            print(f"\n🧹 Nettoyage de l'historique Git...")

        git_folder = project_path / '.git'
        gitattributes_file = project_path / '.gitattributes'

        # Supprimer le dossier .git
        if git_folder.exists():
            try:
                if not self.dry_run:
                    shutil.rmtree(git_folder)
                    print(f"  ✅ Dossier .git/ supprimé")
            except Exception as e:
                self.errors.append(f"Erreur lors de la suppression de .git/: {str(e)}")

        # Supprimer .gitattributes s'il existe
        if gitattributes_file.exists():
            try:
                if not self.dry_run:
                    gitattributes_file.unlink()
                    print(f"  ✅ Fichier .gitattributes supprimé")
            except Exception as e:
                self.errors.append(f"Erreur lors de la suppression de .gitattributes: {str(e)}")

        # Vérifier si .gitignore existe (on le garde)
        gitignore_file = project_path / '.gitignore'
        if gitignore_file.exists() and not self.dry_run:
            print(f"  ℹ️  Fichier .gitignore conservé pour le nouveau projet")

    def run(self) -> bool:
        """Exécute le processus complet de renommage"""
        try:
            # Détecter et valider le projet dans le répertoire courant
            project_path = self.detect_and_validate_project()
        except (FileNotFoundError, NotADirectoryError, ValueError) as e:
            print(f"❌ {str(e)}")
            return False

        if not self.dry_run:
            print(f"🚀 Remplacement de '{self.old_name}' par '{self.new_name}'")
            print(f"📂 Dossier: {project_path.absolute()}")
            print("-" * 60)

        # Nettoyer l'historique Git en premier (sécurité)
        self.clean_git_history(project_path)

        # Traiter les fichiers
        self.process_directory(project_path)

        # Renommer les dossiers et fichiers internes
        self.rename_directories_and_files(project_path)

        # Renommer le dossier principal
        new_project_path = self.rename_project_directory(project_path)

        return True

    def show_summary(self, is_dry_run_summary: bool = False) -> None:
        """Affiche un résumé des modifications"""
        print("\n" + "=" * 60)
        if is_dry_run_summary:
            print("🔍 APERÇU DES MODIFICATIONS")
        else:
            print("📊 RÉSUMÉ FINAL")
        print("=" * 60)

        if self.dry_run and is_dry_run_summary:
            print("🔄 Simulation - Aucune modification effectuée")

        print(f"📄 Fichiers à traiter: {self.files_modified}")
        print(f"🔄 Occurrences à remplacer: {self.occurrences_replaced}")
        if self.directories_renamed > 0:
            print(f"📁 Dossiers internes à renommer: {self.directories_renamed}")
        if self.main_directory_renamed:
            print(f"📂 Dossier principal: {self.old_name}/ → {self.new_name}/")

        if self.errors:
            print(f"\n❌ Erreurs détectées ({len(self.errors)}):")
            for error in self.errors[:5]:  # Limiter à 5 erreurs pour l'aperçu
                print(f"  • {error}")
            if len(self.errors) > 5:
                print(f"  • ... et {len(self.errors) - 5} autres erreurs")

        if is_dry_run_summary:
            if self.files_modified > 0 or self.main_directory_renamed:
                print(f"\n📋 Résumé des changements prévus:")
                if self.files_modified > 0:
                    print(f"   • {self.files_modified} fichiers seront modifiés")
                    print(f"   • {self.occurrences_replaced} occurrences de '{self.old_name}' seront remplacées par '{self.new_name}'")
                if self.directories_renamed > 0:
                    print(f"   • {self.directories_renamed} dossiers internes seront renommés")
                if self.main_directory_renamed:
                    print(f"   • Le dossier principal '{self.old_name}/' sera renommé en '{self.new_name}/'")
                if not self.keep_git:
                    print(f"   • L'historique Git sera supprimé (sécurité)")
            else:
                print(f"\n⚠️  Aucune occurrence de '{self.old_name}' trouvée!")
                print(f"   Vérifiez que le projet contient bien du code {self.old_name}")
        else:
            if self.files_modified > 0:
                print(f"\n✅ Remplacement terminé avec succès!")
                print(f"🎉 Le projet '{self.old_name}' est maintenant '{self.new_name}'")
                print(f"\n🎯 PROCHAINES ÉTAPES pour {self.new_name}:")
                print(f"1. 📂 Entrez dans le nouveau dossier: cd {self.new_name}")
                print("2. 📝 Adaptez votre fichier .env")
                print("3. 📝 Mettez à jour README.md")
                print("4. ⚡ Testez la compilation: mvn clean compile")
                print("5. 🚀 Créez le repo GitHub et poussez le code:")
                print(f"   git init")
                print(f"   git add .")
                print(f"   git commit -m 'Initial commit - {self.new_name}'")
                print(f"   git remote add origin https://github.com/USER/{self.new_name}.git")
                print(f"   git push -u origin main")

def validate_project_name(name: str) -> bool:
    """Valide le nom du projet selon les bonnes pratiques Java"""
    if not name or not name.strip():
        return False

    # Autoriser lettres, chiffres, underscore et tiret
    if not re.match(r'^[a-zA-Z][a-zA-Z0-9_-]*$', name):
        return False

    # Éviter les noms trop courts ou trop longs
    if len(name) < 2 or len(name) > 50:
        return False

    return True

def main():
    parser = argparse.ArgumentParser(
        description="Renomme CoreProject vers un nouveau nom de projet.\n"
                    "IMPORTANT: Ce script doit être placé au même niveau que le dossier 'CoreProject'\n"
                    "Le script exécute automatiquement un aperçu puis demande confirmation.",
        epilog="Exemples:\n"
               "  python rename.py TaskManager\n"
               "  python rename.py EcommerceApp\n"
               "  python rename.py BlogEngine\n"
               "  python rename.py NewProject --keep-git\n\n"
               "Structure requise:\n"
               "  votre_dossier/\n"
               "  ├── rename.py          (ce script)\n"
               "  └── CoreProject/       (le projet à renommer)",
        formatter_class=argparse.RawDescriptionHelpFormatter
    )

    parser.add_argument(
        'new_name',
        nargs='?',  # Optionnel pour compatibilité
        help="Nouveau nom du projet (ex: TaskManager, BlogEngine, etc.)"
    )
    parser.add_argument(
        '--old-name',
        type=str,
        default='CoreProject',
        help="Ancien nom à remplacer (défaut: CoreProject)"
    )
    parser.add_argument(
        '--keep-git',
        action='store_true',
        help="Conserve l'historique Git (ATTENTION: risque de push vers l'ancien repo)"
    )

    args = parser.parse_args()

    # Si pas de nouveau nom fourni, demander interactivement
    if not args.new_name:
        print("🏷️  Renommage de projet CoreProject")
        print("-" * 40)
        print("IMPORTANT: Ce script doit être au même niveau que le dossier 'CoreProject'")
        print("")
        while True:
            new_name = input("📝 Nouveau nom du projet: ").strip()
            if validate_project_name(new_name):
                args.new_name = new_name
                break
            else:
                print("❌ Nom invalide. Le nom doit:")
                print("   • Commencer par une lettre")
                print("   • Contenir uniquement lettres, chiffres, _ et -")
                print("   • Faire entre 2 et 50 caractères")

    # Validation du nom
    if not validate_project_name(args.new_name):
        print("❌ Nom de projet invalide.")
        print("Le nom doit commencer par une lettre et contenir uniquement lettres, chiffres, _ et -")
        return

    print("🔍 ÉTAPE 1: Analyse et aperçu des modifications")
    print("=" * 60)

    # ÉTAPE 1: Exécuter un dry-run automatique
    dry_renamer = ProjectRenamer(
        old_name=args.old_name,
        new_name=args.new_name,
        dry_run=True,
        keep_git=args.keep_git
    )

    success = dry_renamer.run()
    if not success:
        print("\n❌ Impossible de continuer à cause des erreurs ci-dessus.")
        return

    # Afficher l'aperçu
    dry_renamer.show_summary(is_dry_run_summary=True)

    # Si aucune modification à faire, arrêter ici
    if dry_renamer.files_modified == 0 and not dry_renamer.main_directory_renamed:
        return

    # ÉTAPE 2: Demander confirmation
    print("\n" + "⚠️ " * 20)
    print("CONFIRMATION REQUISE")
    print("⚠️ " * 20)

    if args.keep_git:
        print("🚨 ATTENTION: --keep-git activé!")
        print("   Vous devrez manuellement changer l'origine Git pour éviter les push accidentels")

    print(f"📂 Dossier: {Path(__file__).parent.resolve() / args.old_name}")
    print(f"🔄 {args.old_name} → {args.new_name}")
    if not args.keep_git:
        print("🧹 L'historique Git sera supprimé (sécurité)")

    print(f"\n✨ {dry_renamer.files_modified} fichiers vont être modifiés")
    print(f"✨ {dry_renamer.occurrences_replaced} occurrences vont être remplacées")
    if dry_renamer.directories_renamed > 0:
        print(f"✨ {dry_renamer.directories_renamed} dossiers internes vont être renommés")
    if dry_renamer.main_directory_renamed:
        print(f"✨ Le dossier principal '{args.old_name}/' sera renommé en '{args.new_name}/'")

    print("\n❓ Voulez-vous appliquer ces modifications?")
    response = input("   Tapez 'oui' pour continuer, ou 'non' pour annuler: ").strip().lower()

    if response not in ['oui', 'o', 'yes', 'y']:
        print("❌ Opération annulée par l'utilisateur")
        return

    # ÉTAPE 3: Exécution réelle
    print("\n🚀 ÉTAPE 2: Application des modifications")
    print("=" * 60)

    real_renamer = ProjectRenamer(
        old_name=args.old_name,
        new_name=args.new_name,
        dry_run=False,
        keep_git=args.keep_git
    )

    success = real_renamer.run()
    if success:
        real_renamer.show_summary(is_dry_run_summary=False)

if __name__ == "__main__":
    main()