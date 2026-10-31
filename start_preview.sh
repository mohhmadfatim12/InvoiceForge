#!/bin/sh
# InvoiceForge - Mobile Local Server
PORT=8080
echo "================================================="
echo "  InvoiceForge — Mobile Interactive App Server"
echo "================================================="
echo ""
echo "Starting local offline server on port $PORT..."
echo ""
echo "👉 Open your mobile browser (Chrome/Brave/Firefox) and visit:"
echo "   http://localhost:$PORT"
echo ""
echo "Press Ctrl+C to stop the server at any time."
echo "================================================="

cd "$(dirname "$0")/preview" && python3 -m http.server $PORT
