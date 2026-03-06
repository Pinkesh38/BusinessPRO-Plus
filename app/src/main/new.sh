sh
# 1. Initialize Git in your project
git init

# 2. Add all your project files
git add .

# 3. Create a starting point (Commit)
git commit -m "Initial commit with OTA and Splash fixes"

# 4. Link to your GitHub (Use YOUR actual URL here)
git remote add origin https://github.com/Pinkesh38/BusinessPRO-Plus.git

# 5. Push the code to the 'main' branch
git branch -M main
git push -u origin main