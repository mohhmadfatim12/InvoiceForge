#!/bin/sh
# InvoiceForge - Push to GitHub & Trigger Cloud APK Build

echo "================================================="
echo "  InvoiceForge - Cloud APK Build & Download"
echo "================================================="
echo ""
echo "1. Create a new repository on https://github.com/new"
echo "   (Name it 'InvoiceForge', leave it empty without README)"
echo ""
printf "Enter your GitHub Repo URL (e.g. https://github.com/USERNAME/InvoiceForge.git): "
read REPO_URL

if [ -z "$REPO_URL" ]; then
    echo "No URL provided. Exiting."
    exit 1
fi

git remote remove origin 2>/dev/null
git remote add origin "$REPO_URL"
git branch -M main

echo ""
echo "Pushing code to GitHub..."
git push -u origin main

if [ $? -eq 0 ]; then
    echo ""
    echo "================================================="
    echo "✅ Success! Code pushed to GitHub."
    echo ""
    echo "GitHub Actions is now compiling your APK in the cloud!"
    echo "Go to:"
    echo "  $REPO_URL/actions"
    echo ""
    echo "Once finished (~2 minutes), tap on the latest run"
    echo "and download 'InvoiceForge-v1.0.0-debug.apk' directly to your mobile phone!"
    echo "================================================="
else
    echo ""
    echo "❌ Push failed. Check your GitHub credentials / Personal Access Token."
fi
