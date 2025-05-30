# 🚀 Guide Complet : CoreProject → Nouveau Projet

Ce guide vous permet de créer un nouveau projet à partir de CoreProject en quelques étapes simples.

## 📋 Prérequis
- Git installé
- Python 3.6+ installé
- Compte GitHub
- VSCode (recommandé)

---

## 🔄 Étape 1 : Cloner CoreProject

```bash
# Cloner dans un nouveau dossier avec le nom de votre futur projet
git clone https://github.com/VOTRE-USERNAME/CoreProject.git MonNouveauProjet

# Aller dans le dossier
cd MonNouveauProjet
```

---

## 🗑️ Étape 2 : Nettoyer les fichiers Git

```bash
# Supprimer l'historique Git de CoreProject
rm -rf .git
rm -f .gitattributes

# Sur Windows :
# rmdir /s .git
# del .gitattributes
```

**Fichiers à conserver :**
✅ `.gitignore` - Règles d'exclusion Git  
✅ `README.md` - À adapter plus tard  
✅ `tools/` - Contient vos scripts utiles  

---

## 🏷️ Étape 3 : Renommer le projet

### 3.1 Ouvrir le projet dans VSCode
```bash
code .
```

### 3.2 Lancer le script de renommage
```bash
# Test en mode dry-run (recommandé)
python tools/rename_project.py --dry-run

# Si le résultat vous convient, exécution réelle
python tools/rename_project.py
```

### 3.3 Adapter le script pour votre projet
Ouvrez `tools/rename_project.py` et modifiez ces lignes :
```python
self.old_name = "CoreProject" 
self.new_name = "MonNouveauProjet"  # ← Changez ici
```

---

## ⚙️ Étape 4 : Configuration spécifique

### 4.1 Fichier .env
Adaptez votre fichier `.env` avec les nouvelles valeurs :
```env
# Exemples à adapter
APP_DB_URL=jdbc:postgresql://localhost:5432/monNouveauProjet
APP_MAIL_USERNAME=contact@monNouveauProjet.com
# ... autres variables
```

### 4.2 README.md
Mettez à jour le README avec :
- Nouveau nom du projet
- Description adaptée
- Instructions spécifiques

### 4.3 Vérification
```bash
# Test de compilation
mvn clean compile

# Test de l'application
mvn spring-boot:run
```

---

## 🌐 Étape 5 : Créer le repo GitHub

### 5.1 Sur GitHub.com
1. Cliquez sur **"New repository"**
2. **Repository name** : `MonNouveauProjet`
3. **Description** : Description de votre projet
4. ❌ **Décochez** "Add a README file"
5. ❌ **Décochez** "Add .gitignore"
6. **Cliquez** "Create repository"

### 5.2 Initialiser et pousser
```bash
# Initialiser Git
git init

# Ajouter tous les fichiers
git add .

# Premier commit
git commit -m "Initial commit - MonNouveauProjet based on CoreProject"

# Configurer la branche principale
git branch -M main

# Ajouter le remote (remplacez l'URL)
git remote add origin https://github.com/VOTRE-USERNAME/MonNouveauProjet.git

# Pousser vers GitHub
git push -u origin main
```

---

## ✅ Étape 6 : Vérifications finales

### Checklist post-création :
- [ ] Application démarre correctement
- [ ] Tests passent : `mvn test`
- [ ] Nouveau repo GitHub créé et poussé
- [ ] README adapté au nouveau projet
- [ ] Variables d'environnement configurées
- [ ] Base de données adaptée si nécessaire

---

## 🛠️ Dépannage

### Erreur "remote origin already exists"
```bash
git remote remove origin
git remote add origin https://github.com/VOTRE-USERNAME/MonNouveauProjet.git
```

### Erreur de compilation après renommage
```bash
mvn clean
mvn compile
```

### Le script ne trouve pas certains fichiers
Vérifiez que vous êtes à la racine du projet :
```bash
ls -la  # Doit montrer pom.xml, src/, tools/
```

---

## 🎯 Templates de noms courants

Voici des exemples de renommage selon le type de projet :

```python
# E-commerce
self.new_name = "EcommerceApp"

# Gestion
self.new_name = "TaskManager"

# API
self.new_name = "RestApiProject"

# Blog
self.new_name = "BlogEngine"
```

---

## 📚 Ressources utiles

- [Git Documentation](https://git-scm.com/doc)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Maven Documentation](https://maven.apache.org/guides/)

---

## 🆘 Besoin d'aide ?

En cas de problème :
1. Vérifiez que Python 3.6+ est installé
2. Assurez-vous d'être dans le bon dossier
3. Relancez en mode `--dry-run` pour diagnostiquer
4. Consultez les logs d'erreur du script

**Temps estimé total : 10-15 minutes** ⏱️