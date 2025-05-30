# 🚀 Guide d'utilisation : Script de renommage de projet

Ce guide vous explique comment utiliser le script `rename.py` pour transformer CoreProject en votre nouveau projet.

## 📋 Prérequis
- Python 3.6+ installé
- Git installé
- Terminal/invite de commande

---

## 🎯 Workflow complet (recommandé)

### Étape 1 : Préparation de l'environnement

```bash
# 1. Créer un répertoire pour votre nouveau projet
mkdir MonNouveauProjet-workspace
cd MonNouveauProjet-workspace

# 2. Cloner CoreProject
git clone https://github.com/kalonjis/CoreProject.git

# 3. Déplacer le script de renommage au niveau parent
cp ./CoreProject/tools/rename.py ./

# 4. Vérifier la structure (optionnel)
ls -la
# Doit afficher :
# - CoreProject/     (le projet cloné)
# - rename.py        (le script de renommage)
```

### Étape 2 : Exécution du renommage

```bash
# Mode interactif (recommandé pour débutants)
python rename.py

# OU mode direct
python rename.py MonNouveauProjet
```

### Étape 3 : Test et finalisation

```bash
# Entrer dans le nouveau dossier (après renommage)
cd MonNouveauProjet

# Tester la compilation
mvn clean compile

# Initialiser le nouveau repo Git
git init
git add .
git commit -m "Initial commit - MonNouveauProjet"

# Créer le repo sur GitHub et pousser
git remote add origin https://github.com/USER/MonNouveauProjet.git
git push -u origin main
```

### Étape 4 : Nettoyage (optionnel)

```bash
# Revenir au dossier parent
cd ..

# Supprimer le script de renommage si désiré
rm rename.py

# Optionnel : renommer le dossier workspace
cd ..
mv MonNouveauProjet-workspace MonNouveauProjet-final
```

---

## 🧪 Mode dry-run (Test sécurisé)

**⚠️ TOUJOURS tester d'abord avec --dry-run !**

```bash
# Après avoir suivi l'étape 1 de préparation
python rename.py MonNouveauProjet --dry-run

# Si le résultat vous convient, exécution réelle
python rename.py MonNouveauProjet
```

---

## 🔧 Structure de travail

### Structure avant exécution
```
MonNouveauProjet-workspace/
├── rename.py              # Script de renommage (copié depuis CoreProject/tools/)
└── CoreProject/           # Projet cloné depuis GitHub
    ├── src/
    ├── pom.xml
    ├── tools/
    │   └── rename.py      # Script original (peut être supprimé après copie)
    └── ...
```

### Structure après exécution
```
MonNouveauProjet-workspace/
├── rename.py              # Script de renommage (peut être supprimé)
└── MonNouveauProjet/      # Projet renommé
    ├── src/
    ├── pom.xml
    └── ...
```

---

## 🔧 Options avancées

### Syntaxe complète
```bash
python rename.py [NOUVEAU_NOM] [OPTIONS]
```

### Options disponibles

| Option | Description | Exemple |
|--------|-------------|---------|
| `--dry-run` | **🧪 Mode test** - Affiche les modifications sans les appliquer | `--dry-run` |
| `--old-name` | **🏷️ Ancien nom** à remplacer (défaut: CoreProject) | `--old-name MonAncienProjet` |
| `--keep-git` | **⚠️ DANGEREUX** - Conserve l'historique Git | `--keep-git` |

---

## 📝 Exemples d'utilisation complète

### Cas d'usage : Projet E-commerce
```bash
# 1. Préparation
mkdir EcommerceApp-workspace
cd EcommerceApp-workspace
git clone https://github.com/kalonjis/CoreProject.git
cp ./CoreProject/tools/rename.py ./

# 2. Test et exécution
python rename.py EcommerceApp --dry-run
python rename.py EcommerceApp

# 3. Finalisation
cd EcommerceApp
mvn clean compile
git init
git add .
git commit -m "Initial commit - EcommerceApp"
git remote add origin https://github.com/USER/EcommerceApp.git
git push -u origin main
```

### Cas d'usage : Blog Engine
```bash
# 1. Préparation
mkdir BlogEngine-workspace
cd BlogEngine-workspace
git clone https://github.com/kalonjis/CoreProject.git
cp ./CoreProject/tools/rename.py ./

# 2. Test et exécution
python rename.py BlogEngine --dry-run
python rename.py BlogEngine

# 3. Finalisation
cd BlogEngine
mvn clean compile
git init
git add .
git commit -m "Initial commit - BlogEngine"
git remote add origin https://github.com/USER/BlogEngine.git
git push -u origin main
```

### Cas d'usage : API REST
```bash
# 1. Préparation
mkdir RestApiProject-workspace
cd RestApiProject-workspace
git clone https://github.com/kalonjis/CoreProject.git
cp ./CoreProject/tools/rename.py ./

# 2. Test et exécution
python rename.py RestApiProject --dry-run
python rename.py RestApiProject

# 3. Finalisation
cd RestApiProject
mvn clean compile
git init
git add .
git commit -m "Initial commit - RestApiProject"
git remote add origin https://github.com/USER/RestApiProject.git
git push -u origin main
```

---

## ⚡ Processus automatique exécuté

Quand vous lancez le script, il effectue automatiquement :

### 1. 🛡️ **Sécurité Git** (par défaut)
- ✅ Supprime le dossier `.git/` de CoreProject
- ✅ Supprime `.gitattributes`
- ✅ Conserve `.gitignore` pour le nouveau projet

### 2. 🔄 **Remplacement dans les fichiers**
- ✅ Tous les fichiers texte (.java, .xml, .yml, .md, etc.)
- ✅ Remplace "CoreProject" par votre nouveau nom
- ✅ Gère plusieurs encodages automatiquement

### 3. 📁 **Renommage des dossiers/fichiers**
- ✅ Renomme les dossiers contenant "CoreProject"
- ✅ Renomme les fichiers contenant "CoreProject"
- ✅ Renomme le dossier principal "CoreProject/" vers "VotreNouveauProjet/"

### 4. 📋 **Instructions post-exécution**
- ✅ Guide pour créer le nouveau repo GitHub
- ✅ Commandes Git prêtes à copier/coller

---

## 🔒 Sécurité et confirmations

### Confirmations automatiques
Le script demande **confirmation** avant les modifications importantes :

```bash
⚠️  CONFIRMATION REQUISE
📂 Dossier: /chemin/vers/CoreProject
🔄 CoreProject → MonNouveauProjet
🧹 L'historique Git sera supprimé (sécurité)
✨ 45 fichiers vont être modifiés
✨ 127 occurrences vont être remplacées

❓ Voulez-vous appliquer ces modifications?
   Tapez 'oui' pour continuer, ou 'non' pour annuler:
```

### Option --keep-git (DANGEREUX)
```bash
# ATTENTION: Risque de push vers l'ancien repo !
python rename.py MonProjet --keep-git
```
⚠️ **Utiliser seulement si vous savez ce que vous faites !**

---

## 📊 Résumé des modifications

Après exécution, le script affiche un résumé :

```bash
📊 RÉSUMÉ FINAL
==========================================
📄 Fichiers traités: 45
🔄 Occurrences remplacées: 127
📂 Dossier principal: CoreProject/ → MonNouveauProjet/
✅ Remplacement terminé avec succès!
🎉 Le projet 'CoreProject' est maintenant 'MonNouveauProjet'

🎯 PROCHAINES ÉTAPES pour MonNouveauProjet:
1. 📂 Entrez dans le nouveau dossier: cd MonNouveauProjet
2. 📝 Adaptez votre fichier .env
3. 📝 Mettez à jour README.md
4. ⚡ Testez la compilation: mvn clean compile
5. 🚀 Créez le repo GitHub et poussez le code:
   git init
   git add .
   git commit -m 'Initial commit - MonNouveauProjet'
   git remote add origin https://github.com/USER/MonNouveauProjet.git
   git push -u origin main
```

---

## 🛠️ Règles de nommage

### ✅ Noms valides
- Commence par une lettre
- Lettres, chiffres, `_` et `-` autorisés
- Entre 2 et 50 caractères

```bash
✅ TaskManager
✅ EcommerceApp
✅ Blog_Engine
✅ Rest-API
✅ MyProject2024
```

### ❌ Noms invalides
```bash
❌ 123Project        # Commence par un chiffre
❌ Mon Projet        # Contient un espace
❌ Project@Home      # Contient @
❌ A                 # Trop court
```

---

## 🚨 Dépannage

### Erreur "No module named..."
```bash
# Vérifier que Python est installé
python --version

# Sur certains systèmes
python3 rename.py MonProjet --dry-run
```

### Le script ne trouve pas CoreProject
```bash
# Vérifier que vous êtes dans le bon dossier et que la structure est correcte
ls -la
# Doit afficher : CoreProject/ et rename.py

# Vérifier que CoreProject contient bien pom.xml et src/
ls -la CoreProject/
```

### Erreur "CoreProject non trouvé"
```bash
# S'assurer que le dossier CoreProject existe
ls -la CoreProject/

# Vérifier qu'on a bien copié le script au bon endroit
ls -la rename.py

# Structure correcte :
# votre-workspace/
# ├── rename.py
# └── CoreProject/
```

### Erreur de permissions
```bash
# Sur Linux/Mac, donner les permissions si nécessaire
chmod +x rename.py
```

### Annuler si erreur
Si le script plante en cours d'exécution :
1. 🔄 **Supprimer le workspace** et recommencer depuis l'étape 1
2. 🧪 **Utiliser --dry-run** pour identifier le problème
3. 🔍 **Vérifier la structure** des dossiers

---

## ⏱️ Workflow complet recommandé - Résumé

```bash
# ===== ÉTAPE 1: PRÉPARATION =====
mkdir MonNouveauProjet-workspace
cd MonNouveauProjet-workspace
git clone https://github.com/kalonjis/CoreProject.git
cp ./CoreProject/tools/rename.py ./

# ===== ÉTAPE 2: TEST =====
python rename.py MonNouveauProjet --dry-run

# ===== ÉTAPE 3: EXÉCUTION =====
python rename.py MonNouveauProjet

# ===== ÉTAPE 4: FINALISATION =====
cd MonNouveauProjet
mvn clean compile
# Adapter .env et README.md

# ===== ÉTAPE 5: NOUVEAU REPO GIT =====
git init
git add .
git commit -m "Initial commit - MonNouveauProjet"
git remote add origin https://github.com/USER/MonNouveauProjet.git
git push -u origin main

# ===== ÉTAPE 6: NETTOYAGE (optionnel) =====
cd ..
rm rename.py  # Supprimer le script si désiré
```

**⏱️ Temps total estimé : 5-10 minutes**

---

## 💡 Conseils

- 🧪 **Toujours** utiliser `--dry-run` en premier
- 📁 **Créer un workspace dédié** pour chaque nouveau projet
- 🎯 **Choisir un nom** descriptif et unique
- 📝 **Suivre les étapes** post-exécution dans l'ordre
- 🔄 **Tester la compilation** avant de pousser sur GitHub
- 🧹 **Nettoyer le workspace** après finalisation si désiré

---

## 🚀 Avantages de cette approche

### ✅ **Isolation complète**
- Chaque nouveau projet a son propre workspace
- Pas de risque de conflit entre projets

### ✅ **Reproductibilité**
- Workflow identique pour tous les projets
- Facile à documenter et partager en équipe

### ✅ **Flexibilité**
- Possibilité de garder plusieurs workspaces en parallèle
- Facilite les tests et comparaisons

### ✅ **Sécurité**
- Historique Git supprimé par défaut
- Nouveau repo Git propre pour chaque projet

---

## 📚 Ressources utiles

- [Documentation Git](https://git-scm.com/doc)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Maven Documentation](https://maven.apache.org/guides/)

---

## 🆘 Support

En cas de problème :
1. Vérifiez que Python 3.6+ est installé
2. Assurez-vous d'avoir la bonne structure de dossiers
3. Utilisez `--dry-run` pour diagnostiquer
4. Consultez la section dépannage ci-dessus
5. Recommencez depuis l'étape 1 si nécessaire

---

*Version du guide : 2.0*  
*Compatible avec : rename.py (workflow avec workspace)*  
*Dernière mise à jour : mai 2025*