# 🚀 Guide d'utilisation : Script de renommage de projet

Ce guide vous explique comment utiliser le script `rename.py` pour transformer CoreProject en votre nouveau projet.

## 📋 Prérequis
- Python 3.6+ installé
- Projet CoreProject cloné localement
- Terminal/invite de commande

---

## 🎯 Utilisation rapide

### Option 1 : Mode interactif (recommandé pour débutants)
```bash
cd MonProjetCloné
python tools/rename.py
```
Le script vous demandera le nouveau nom interactivement.

### Option 2 : Mode direct
```bash
cd MonProjetCloné
python tools/rename.py MonNouveauProjet
```

---

## 🧪 Mode dry-run (Test sécurisé)

**⚠️ TOUJOURS tester d'abord avec --dry-run !**

```bash
# Test pour voir ce qui sera modifié
python tools/rename.py MonNouveauProjet --dry-run

# Si le résultat vous convient, exécution réelle
python tools/rename.py MonNouveauProjet
```

---

## 🔧 Options avancées

### Syntaxe complète
```bash
python tools/rename.py [NOUVEAU_NOM] [OPTIONS]
```

### Options disponibles

| Option | Description | Exemple |
|--------|-------------|---------|
| `--dry-run` | **🧪 Mode test** - Affiche les modifications sans les appliquer | `--dry-run` |
| `--path` | **📂 Chemin personnalisé** vers le projet | `--path /autre/dossier` |
| `--old-name` | **🏷️ Ancien nom** à remplacer (défaut: CoreProject) | `--old-name MonAncienProjet` |
| `--keep-git` | **⚠️ DANGEREUX** - Conserve l'historique Git | `--keep-git` |

---

## 📝 Exemples d'utilisation

### Cas d'usage courants
```bash
# E-commerce
python tools/rename.py EcommerceApp --dry-run
python tools/rename.py EcommerceApp

# Blog
python tools/rename.py BlogEngine --dry-run
python tools/rename.py BlogEngine

# Gestion de tâches
python tools/rename.py TaskManager --dry-run
python tools/rename.py TaskManager

# API REST
python tools/rename.py RestApiProject --dry-run
python tools/rename.py RestApiProject
```

### Avec chemin personnalisé
```bash
# Si le projet est dans un autre dossier
python tools/rename.py MonProjet --path /chemin/vers/projet --dry-run
python tools/rename.py MonProjet --path /chemin/vers/projet
```

### Renommer depuis un autre nom
```bash
# Si votre projet s'appelle "AutreNom" au lieu de "CoreProject"
python tools/rename.py NouveauNom --old-name AutreNom --dry-run
python tools/rename.py NouveauNom --old-name AutreNom
```

---

## ⚡ Processus automatique exécuté

Quand vous lancez le script, il effectue automatiquement :

### 1. 🛡️ **Sécurité Git** (par défaut)
- ✅ Supprime le dossier `.git/` 
- ✅ Supprime `.gitattributes`
- ✅ Conserve `.gitignore` pour le nouveau projet

### 2. 🔄 **Remplacement dans les fichiers**
- ✅ Tous les fichiers texte (.java, .xml, .yml, .md, etc.)
- ✅ Remplace "CoreProject" par votre nouveau nom
- ✅ Gère plusieurs encodages automatiquement

### 3. 📁 **Renommage des dossiers/fichiers**
- ✅ Renomme les dossiers contenant "CoreProject"
- ✅ Renomme les fichiers contenant "CoreProject"

### 4. 📋 **Instructions post-exécution**
- ✅ Guide pour créer le nouveau repo GitHub
- ✅ Commandes Git prêtes à copier/coller

---

## 🔒 Sécurité et confirmations

### Confirmations automatiques
Le script demande **confirmation** avant les modifications importantes :

```bash
⚠️  ATTENTION: Cette opération va modifier tous les fichiers du projet!
📂 Dossier: /chemin/vers/projet
🔄 CoreProject → MonNouveauProjet
🧹 L'historique Git sera supprimé (sécurité)

❓ Continuer? (oui/non):
```

### Option --keep-git (DANGEREUX)
```bash
# ATTENTION: Risque de push vers l'ancien repo !
python tools/rename.py MonProjet --keep-git
```
⚠️ **Utiliser seulement si vous savez ce que vous faites !**

---

## 📊 Résumé des modifications

Après exécution, le script affiche un résumé :

```bash
📊 RÉSUMÉ
==========================================
📄 Fichiers traités: 45
🔄 Occurrences remplacées: 127
✅ Remplacement terminé avec succès!
🎉 Le projet 'CoreProject' est maintenant 'MonNouveauProjet'

🎯 PROCHAINES ÉTAPES pour MonNouveauProjet:
1. 📝 Adaptez votre fichier .env
2. 📝 Mettez à jour README.md
3. ⚡ Testez la compilation: mvn clean compile
4. 🚀 Créez le repo GitHub et poussez le code:
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
python3 tools/rename.py MonProjet --dry-run
```

### Le script ne trouve pas le projet
```bash
# Vérifier que vous êtes dans le bon dossier
ls -la
# Doit afficher : pom.xml, src/, tools/, etc.

# Ou spécifier le chemin
python tools/rename.py MonProjet --path /chemin/correct
```

### Erreur de permissions
```bash
# Sur Linux/Mac, donner les permissions
chmod +x tools/rename.py
```

### Annuler si erreur
Si le script plante en cours d'exécution :
1. 🔄 **Restaurez depuis Git** (si vous avez fait un commit avant)
2. 🔄 **Re-clonez** CoreProject et recommencez
3. 🧪 **Utilisez --dry-run** pour identifier le problème

---

## ⏱️ Workflow complet recommandé

```bash
# 1. Cloner CoreProject
git clone https://github.com/USER/CoreProject.git MonNouveauProjet
cd MonNouveauProjet

# 2. Test avec dry-run
python tools/rename.py MonNouveauProjet --dry-run

# 3. Si OK, exécution réelle
python tools/rename.py MonNouveauProjet

# 4. Tester la compilation
mvn clean compile

# 5. Adapter .env et README.md

# 6. Créer le nouveau repo GitHub et pousser
git init
git add .
git commit -m "Initial commit - MonNouveauProjet"
git remote add origin https://github.com/USER/MonNouveauProjet.git
git push -u origin main
```

**⏱️ Temps total estimé : 5-10 minutes**

---

## 💡 Conseils

- 🧪 **Toujours** utiliser `--dry-run` en premier
- 💾 **Faire un backup** du projet avant renommage si important
- 🎯 **Choisir un nom** descriptif et unique
- 📝 **Suivre les étapes** post-exécution dans l'ordre
- 🔄 **Tester la compilation** avant de pousser sur GitHub

---

## 📚 Ressources utiles

- [Documentation Git](https://git-scm.com/doc)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Maven Documentation](https://maven.apache.org/guides/)

---

## 🆘 Support

En cas de problème :
1. Vérifiez que Python 3.6+ est installé
2. Assurez-vous d'être dans le bon dossier du projet
3. Utilisez `--dry-run` pour diagnostiquer
4. Consultez la section dépannage ci-dessus

---

*Version du guide : 1.0*
*Compatible avec : rename.py (optimized_rename_final)*