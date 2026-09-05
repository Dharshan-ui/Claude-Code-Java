# Claude Code Java

A lightweight AI coding agent built from scratch in Java — connects an LLM (via OpenRouter) to your local filesystem and shell, letting it read files, write code, run commands, and reason through multi-step tasks.

## What It Does

- 🤖 **Agent Loop** — maintains context across multiple tool calls per task
- 📖 **Read Files** · ✍️ **Write Files** · 💻 **Run Shell Commands**
- 🔄 **Tool Calling** — the LLM decides when a tool is needed
- 💬 **Interactive CLI** — chat with it turn by turn, or fire a single prompt

## Architecture

User → CLI → LLM → Tool Call → [Read | Write | Bash] → Tool Result → LLM → Final Response


## Tech Stack
Java 25 · Maven · OpenAI Java SDK · Jackson · OpenRouter · Claude Haiku

## Usage

Interactive mode:

./run.sh

Claude Code Java
Type 'exit' to quit.

create a Java solution for Two Sum
exit


Single-prompt mode:

./run.sh -p "Create a Java solution for Two Sum"


## Configuration
Set `OPENROUTER_API_KEY` as an environment variable. Never commit API keys or `.env` files.

## Status
Core agent loop, tool calling, interactive/single-prompt CLI, and file/shell execution are implemented. No sandboxing yet — tools can read, write, and execute anywhere the process has access. Built as a hands-on exploration of agentic AI systems.