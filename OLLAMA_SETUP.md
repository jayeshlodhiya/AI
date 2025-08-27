# Ollama Setup and Configuration Guide

## Quick Start

### 1. Install Ollama
```bash
# macOS
brew install ollama

# Linux
curl -fsSL https://ollama.ai/install.sh | sh

# Windows
# Download from https://ollama.ai/download
```

### 2. Start Ollama Service
```bash
ollama serve
```

### 3. Pull a Model
```bash
# Fast, lightweight model (recommended for testing)
ollama pull llama3:8b-instruct

# Better quality, larger model
ollama pull llama3:13b-instruct

# Best quality, largest model (requires more RAM)
ollama pull llama3:70b-instruct
```

## Configuration Profiles

### Fast Profile (Quick Responses)
```bash
# Use this profile for fast responses
./gradlew bootRun --args='--spring.profiles.active=ollama-fast'
```

**Features:**
- 8B model for quick responses
- 30-second timeout
- Good for development and testing

### Large Model Profile (Better Quality)
```bash
# Use this profile for better quality responses
./gradlew bootRun --args='--spring.profiles.active=ollama-large'
```

**Features:**
- 13B model for better quality
- 2-minute timeout
- Good for production use

### Default Profile
```bash
# Use default configuration
./gradlew bootRun
```

**Features:**
- 8B model
- 1-minute timeout
- Balanced approach

## Troubleshooting

### Run Diagnostic Script
```bash
./check-ollama.sh
```

### Common Commands
```bash
# Check Ollama status
ollama list
ollama ps

# Restart Ollama
pkill ollama
ollama serve &

# Test model directly
ollama run llama3:8b-instruct "Hello, how are you?"
```

### Environment Variables
```bash
# Set these in your shell or .env file
export OLLAMA_HOST=127.0.0.1:11434
export OLLAMA_ORIGINS=*
```

## Performance Tips

### 1. Model Selection
- **8B models**: Fast, good for simple tasks
- **13B models**: Better quality, moderate speed
- **70B models**: Best quality, slowest

### 2. Hardware Requirements
- **8B**: 8GB RAM minimum, 16GB recommended
- **13B**: 16GB RAM minimum, 32GB recommended
- **70B**: 32GB RAM minimum, 64GB recommended

### 3. Timeout Settings
```yaml
# Fast responses
read-timeout: 30000      # 30 seconds

# Balanced
read-timeout: 60000      # 1 minute

# Large models
read-timeout: 120000     # 2 minutes
```

## Alternative Models

### Fast Models
```bash
ollama pull phi3:mini           # Very fast, small
ollama pull qwen2.5:0.5b        # Extremely fast
ollama pull llama3:8b-instruct  # Fast, good quality
```

### Quality Models
```bash
ollama pull llama3:13b-instruct # Good quality
ollama pull llama3:70b-instruct # Best quality
ollama pull codellama:13b       # Good for code
```

## Monitoring

### Enable Debug Logging
```yaml
# In application.yml
logging:
  level:
    com.retailai.service.LlmClient: DEBUG
    org.springframework.web.client: DEBUG
```

### Check System Resources
```bash
# Monitor RAM usage
htop
free -h

# Monitor GPU (if using)
nvidia-smi
```

## Switching Between Profiles

### During Development
```bash
# Start with fast profile
./gradlew bootRun --args='--spring.profiles.active=ollama-fast'

# Switch to large profile
./gradlew bootRun --args='--spring.profiles.active=ollama-large'
```

### Production Deployment
```bash
# Set profile in environment
export SPRING_PROFILES_ACTIVE=ollama-large

# Or in docker-compose
environment:
  - SPRING_PROFILES_ACTIVE=ollama-large
```

## Still Having Issues?

1. Check the `OLLAMA_TROUBLESHOOTING.md` file
2. Run `./check-ollama.sh` diagnostic script
3. Check Ollama GitHub issues: https://github.com/ollama/ollama/issues
4. Consider using a hosted LLM provider instead
