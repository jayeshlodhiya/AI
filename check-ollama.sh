#!/bin/bash

# Ollama Diagnostic Script
# This script checks Ollama status and helps troubleshoot common issues

echo "🔍 Ollama Diagnostic Tool"
echo "=========================="

# Check if Ollama is installed
if ! command -v ollama &> /dev/null; then
    echo "❌ Ollama is not installed!"
    echo "   Install from: https://ollama.ai/download"
    exit 1
fi

echo "✅ Ollama is installed"

# Check if Ollama service is running
if ! pgrep -x "ollama" > /dev/null; then
    echo "❌ Ollama service is not running"
    echo "   Starting Ollama service..."
    ollama serve &
    sleep 3
else
    echo "✅ Ollama service is running"
fi

# Check port availability
if lsof -i :11434 > /dev/null 2>&1; then
    echo "✅ Port 11434 is available and in use by Ollama"
else
    echo "❌ Port 11434 is not in use"
    echo "   Ollama might not be running properly"
fi

# Test API endpoint
echo "🌐 Testing Ollama API endpoint..."
if curl -s http://localhost:11434/api/tags > /dev/null; then
    echo "✅ Ollama API is responding"
else
    echo "❌ Ollama API is not responding"
    echo "   Service might be starting up or having issues"
fi

# Check available models
echo "📚 Checking available models..."
MODELS=$(ollama list 2>/dev/null)
if [ $? -eq 0 ]; then
    echo "✅ Models found:"
    echo "$MODELS"
else
    echo "❌ Failed to list models"
fi

# Check system resources (macOS compatible)
echo "💻 System Resources:"
if command -v sysctl &> /dev/null; then
    # macOS
    RAM_TOTAL=$(sysctl -n hw.memsize | awk '{print $0/1024/1024/1024 " GB"}')
    CPU_CORES=$(sysctl -n hw.ncpu)
    echo "   RAM: $RAM_TOTAL"
    echo "   CPU: $CPU_CORES cores"
else
    # Linux
    if command -v free &> /dev/null; then
        echo "   RAM: $(free -h | grep Mem | awk '{print $3 "/" $2}')"
    fi
    if command -v nproc &> /dev/null; then
        echo "   CPU: $(nproc) cores"
    fi
fi

# Check if specific model exists
MODEL_NAME="llama3"
if ollama list | grep -q "$MODEL_NAME"; then
    echo "✅ Model '$MODEL_NAME' is available"
else
    echo "❌ Model '$MODEL_NAME' not found"
    echo "   Pull it with: ollama pull $MODEL_NAME"
fi

# Test model response
echo "🧪 Testing model response..."
if ollama list | grep -q "$MODEL_NAME"; then
    echo "   Testing with simple query..."
    # Use timeout command if available, otherwise just run
    if command -v timeout &> /dev/null; then
        RESPONSE=$(timeout 30s ollama run "$MODEL_NAME" "Hi" 2>&1)
    else
        RESPONSE=$(ollama run "$MODEL_NAME" "Hi" 2>&1)
    fi
    
    if [ $? -eq 0 ] || echo "$RESPONSE" | grep -q "Hi"; then
        echo "✅ Model responded successfully"
        echo "   Response preview: ${RESPONSE:0:100}..."
    else
        echo "❌ Model test failed: $RESPONSE"
    fi
else
    echo "   Skipping model test (model not available)"
fi

echo ""
echo "🎯 Quick Fixes:"
echo "   1. Restart Ollama: pkill ollama && ollama serve &"
echo "   2. Pull model: ollama pull llama3:8b-instruct"
echo "   3. Check logs: ollama serve --verbose"
echo "   4. Increase timeouts in application.yml"
echo ""
echo "📖 See OLLAMA_TROUBLESHOOTING.md for detailed solutions"
