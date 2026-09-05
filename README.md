# Claude Code Java

A lightweight AI coding agent built from scratch in Java.

The project uses an LLM through the OpenRouter API and gives the model access to local tools for reading files, writing files, and executing shell commands.

The goal is to build an agent that can understand a natural-language coding task, inspect a workspace, make changes, run commands, and return the results to the user.

## Features

- Interactive command-line interface
- Multi-turn conversation history
- LLM-powered agent loop
- File reading
- File creation and modification
- Shell command execution
- Cross-platform command execution
- Tool calling through the OpenAI-compatible API
- OpenRouter integration
- Single-prompt mode using `-p`

## Architecture

```text
User
  |
  v
Java CLI
  |
  v
LLM via OpenRouter
  |
  v
Tool decision
  |
  +------------------+
  |                  |
  v                  v
Read File         Write File
  |                  |
  +--------+---------+
           |
           v
       Bash / Shell
           |
           v
      Tool Result
           |
           v
          LLM
           |
           v
      Final Response