# Git Commands for BusinessPRO+
# Run these in the Android Studio Terminal (do not type 'sh')

# 1. Add all project files
git add .

# 2. Create a commit
git commit -m "Initial commit with OTA and Splash fixes"

# 3. Push the code to the 'main' branch
git branch -M main
git push -u origin main --force
