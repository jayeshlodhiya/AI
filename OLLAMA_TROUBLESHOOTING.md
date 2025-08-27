# Ollama Troubleshooting Guide

## Common Issues and Solutions

### 1. Read Timeout Errors

**Problem**: `[LLM] Ollama call failed: I/O error on POST request for "http://localhost:11434/api/chat": Read timed out`

**Causes**:
- Model is too large for your hardware
- Insufficient RAM/VRAM
- Slow CPU/GPU
- Model is still loading

**Solutions**:

#### A. Increase Timeouts
```yaml
# In application.yml
app:
  llm:
    read-timeout: 120000    # 2 minutes
    connect-timeout: 15000  # 15 seconds
```

#### B. Use Smaller Models
```yaml
app:
  llm:
    model: llama3:8b-instruct    # Smaller, faster model
    # Instead of: llama3:70b-instruct (much larger, slower)
```

#### C. Check Ollama Status
```bash
# Check if Ollama is running
ollama list

# Check model status
ollama ps

# Restart Ollama service
ollama serve
```

### 2. Connection Refused

**Problem**: `Connection refused: localhost:11434`

**Solutions**:
```bash
# Start Ollama service
ollama serve

# Check if port is in use
lsof -i :11434

# Test Ollama API
curl http://localhost:11434/api/tags
```

### 3. Model Not Found

**Problem**: `Model 'llama3' not found`

**Solutions**:
```bash
# Pull the model
ollama pull llama3:8b-instruct

# List available models
ollama list

# Check model info
ollama show llama3:8b-instruct
```

### 4. Performance Optimization

#### A. Hardware Requirements
- **8B models**: Minimum 8GB RAM, recommended 16GB+
- **13B models**: Minimum 16GB RAM, recommended 32GB+
- **70B models**: Minimum 32GB RAM, recommended 64GB+

#### B. Model Selection
```yaml
# Fast, lightweight models
app:
  llm:
    model: llama3:8b-instruct      # Fast, good quality
    model: phi3:mini               # Very fast, smaller
    model: qwen2.5:0.5b            # Extremely fast

# Better quality, slower models
app:
  llm:
    model: llama3:13b-instruct     # Better quality, slower
    model: llama3:70b-instruct     # Best quality, slowest
```

### 5. Alternative LLM Providers

If Ollama continues to have issues, consider switching to hosted providers:

```yaml
# Groq (very fast)
app:
  llm:
    provider: openai
    model: llama3.1-8b-instant
    base-url: https://api.groq.com/openai/v1
    api-key: ${GROQ_API_KEY}

# OpenAI
app:
  llm:
    provider: openai
    model: gpt-3.5-turbo
    base-url: https://api.openai.com/v1
    api-key: ${OPENAI_API_KEY}

# Anthropic
app:
  llm:
    provider: openai
    model: claude-3-haiku-20240307
    base-url: https://api.anthropic.com/v1
    api-key: ${ANTHROPIC_API_KEY}
```

### 6. Environment Variables

Set these in your environment or `.env` file:
```bash
export GROQ_API_KEY="your-groq-key"
export OPENAI_API_KEY="your-openai-key"
export ANTHROPIC_API_KEY="your-anthropic-key"
```

### 7. Testing Ollama

Test Ollama directly before using it in the app:
```bash
# Test basic functionality
ollama run llama3:8b-instruct "Hello, how are you?"

# Test API endpoint
curl -X POST http://localhost:11434/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "model": "llama3:8b-instruct",
    "messages": [{"role": "user", "content": "Hello"}],
    "stream": false
  }'
```

### 8. Monitoring and Logs

Enable detailed logging in your application:
```yaml
# In application.yml
logging:
  level:
    com.retailai.service.LlmClient: DEBUG
    org.springframework.web.client: DEBUG
```

### 9. Quick Fix Commands

```bash
# Restart everything
pkill ollama
ollama serve &
./gradlew bootRun

# Check system resources
htop
free -h
nvidia-smi  # if using GPU
```

## Still Having Issues?

1. Check Ollama GitHub issues: https://github.com/ollama/ollama/issues
2. Verify your hardware meets model requirements
3. Try a different model size
4. Consider using a hosted LLM provider instead
