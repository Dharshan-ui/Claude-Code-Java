# Custom Claude Code Agent in Java

A lightweight, autonomous AI coding assistant built from scratch in Java. It interacts with LLMs via OpenRouter, parses tool calls in a continuous agent loop, and executes local system actions.

## Features
- **Conversational Agent Loop**: Maintains conversation history and reasons through multi-step tasks.
- **File Operations**: Safely reads files and writes/creates new files dynamically.
- **Cross-Platform Bash Execution**: Executes native shell commands directly on your OS to run tests, inspect directories, and manage your workspace.

## Tech Stack
- Java
- OpenAI Java SDK & OkHttp
- Jackson (JSON parsing)
- OpenRouter API (`anthropic/claude-haiku-4.5`)

## Usage
Run the program with a prompt:
```bash
java -cp "target/classes;target/dependency/*" Main -p "Your prompt here"

### Step 4: Wipe Git History (Crucial for Zero Evidence)
Right now, your past commit messages might contain references to the challenge. To completely wipe your commit history and start fresh with a single clean commit, run:

```powershell
# Create a fresh temporary branch with all your current files
git checkout --orphan fresh-start

# Stage everything
git add .

# Make a brand new, clean initial commit
git commit -m "Initial commit: Custom Java Claude Agent"

# Replace the old master branch with your fresh one
git branch -D master
git branch -m master