import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionTool;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionToolMessageParam;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.core.JsonValue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Scanner;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

void main(String[] args) {

    boolean interactive = args.length == 0;

    // Support both:
    // claude
    // claude -p "your prompt"
    if (!interactive && (args.length < 2 || !"-p".equals(args[0]))) {
        System.err.println("Usage: claude -p <prompt>");
        System.exit(1);
    }

    String prompt = interactive ? "" : args[1];
    Scanner scanner = new Scanner(System.in);

    if (interactive) {
        System.out.println("Claude Code Java");
        System.out.println("Type 'exit' to quit.");
    }

    // Get API configuration
    String apiKey = System.getenv("OPENROUTER_API_KEY");
    String baseUrl = System.getenv("OPENROUTER_BASE_URL");

    if (baseUrl == null || baseUrl.isEmpty()) {
        baseUrl = "https://openrouter.ai/api/v1";
    }

    if (apiKey == null || apiKey.isEmpty()) {
        throw new RuntimeException("OPENROUTER_API_KEY is not set");
    }

    // Create OpenRouter client
    OpenAIClient client = OpenAIOkHttpClient.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .build();

    // ---------------------------------------------------------
    // 1. READ FILE TOOL
    // ---------------------------------------------------------

    FunctionParameters readParameters = FunctionParameters.builder()
            .putAdditionalProperty("type", JsonValue.from("object"))
            .putAdditionalProperty("properties", JsonValue.from(Map.of(
                    "file_path", Map.of(
                            "type", "string",
                            "description", "The path to the file to read"
                    )
            )))
            .putAdditionalProperty(
                    "required",
                    JsonValue.from(List.of("file_path"))
            )
            .build();

    FunctionDefinition readFunction = FunctionDefinition.builder()
            .name("read_file")
            .description("Read and return the contents of a file")
            .parameters(readParameters)
            .build();

    ChatCompletionTool readFileTool = ChatCompletionTool.builder()
            .function(readFunction)
            .build();

    // ---------------------------------------------------------
    // 2. WRITE FILE TOOL
    // ---------------------------------------------------------

    FunctionParameters writeParameters = FunctionParameters.builder()
            .putAdditionalProperty("type", JsonValue.from("object"))
            .putAdditionalProperty("properties", JsonValue.from(Map.of(
                    "file_path", Map.of(
                            "type", "string",
                            "description", "The path to the file to write to"
                    ),
                    "content", Map.of(
                            "type", "string",
                            "description", "The content to write to the file"
                    )
            )))
            .putAdditionalProperty(
                    "required",
                    JsonValue.from(List.of("file_path", "content"))
            )
            .build();

    FunctionDefinition writeFunction = FunctionDefinition.builder()
            .name("write_file")
            .description(
                    "Write content to a file, creating it if it doesn't exist or overwriting it if it does"
            )
            .parameters(writeParameters)
            .build();

    ChatCompletionTool writeFileTool = ChatCompletionTool.builder()
            .function(writeFunction)
            .build();

    // ---------------------------------------------------------
    // 3. BASH TOOL
    // ---------------------------------------------------------

    FunctionParameters bashParameters = FunctionParameters.builder()
            .putAdditionalProperty("type", JsonValue.from("object"))
            .putAdditionalProperty("properties", JsonValue.from(Map.of(
                    "command", Map.of(
                            "type", "string",
                            "description", "The shell command to execute"
                    )
            )))
            .putAdditionalProperty(
                    "required",
                    JsonValue.from(List.of("command"))
            )
            .build();

    FunctionDefinition bashFunction = FunctionDefinition.builder()
            .name("bash")
            .description("Execute a shell command")
            .parameters(bashParameters)
            .build();

    ChatCompletionTool bashTool = ChatCompletionTool.builder()
            .function(bashFunction)
            .build();

    ObjectMapper mapper = new ObjectMapper();

    // ---------------------------------------------------------
    // 4. CONVERSATION HISTORY
    // ---------------------------------------------------------

    List<ChatCompletionMessageParam> messages = new ArrayList<>();

    // ---------------------------------------------------------
    // 5. CLI LOOP
    // ---------------------------------------------------------

    while (true) {

        // Interactive mode asks for a new prompt every time.
        // -p mode already has its prompt from the command line.
        if (interactive) {

            System.out.print("> ");

            if (!scanner.hasNextLine()) {
                break;
            }

            prompt = scanner.nextLine();

            // Exit the CLI
            if ("exit".equalsIgnoreCase(prompt.trim())) {
                break;
            }

            // Ignore empty prompts
            if (prompt.trim().isEmpty()) {
                continue;
            }
        }

        // Add user's message to conversation history
        messages.add(
                ChatCompletionMessageParam.ofUser(
                        ChatCompletionUserMessageParam.builder()
                                .content(prompt)
                                .build()
                )
        );

        // -----------------------------------------------------
        // 6. AGENT LOOP
        // -----------------------------------------------------

        while (true) {

            ChatCompletion response = client.chat()
                    .completions()
                    .create(
                            ChatCompletionCreateParams.builder()
                                    .model("anthropic/claude-haiku-4.5")
                                    .messages(messages)
                                    .addTool(readFileTool)
                                    .addTool(writeFileTool)
                                    .addTool(bashTool)
                                    .maxTokens(1024)
                                    .build()
                    );

            if (response.choices().isEmpty()) {
                throw new RuntimeException("No choices in response");
            }

            var message = response.choices().get(0).message();

            // -------------------------------------------------
            // Add Claude's response to conversation history
            // -------------------------------------------------

            var assistantMessageBuilder =
                    ChatCompletionAssistantMessageParam.builder();

            message.content()
                    .ifPresent(assistantMessageBuilder::content);

            message.toolCalls()
                    .ifPresent(assistantMessageBuilder::toolCalls);

            messages.add(
                    ChatCompletionMessageParam.ofAssistant(
                            assistantMessageBuilder.build()
                    )
            );

            // -------------------------------------------------
            // No tools requested -> Claude is finished
            // -------------------------------------------------

            if (message.toolCalls().isEmpty()
                    || message.toolCalls().get().isEmpty()) {

                System.out.println();
                System.out.println(message.content().orElse(""));
                System.out.println();

                break;
            }

            // -------------------------------------------------
            // 7. EXECUTE TOOL CALLS
            // -------------------------------------------------

            for (var toolCall : message.toolCalls().get()) {

                String toolCallId = toolCall.id();
                String functionName = toolCall.function().name();
                String argumentsJson = toolCall.function().arguments();

                String toolResultContent = "";

                try {

                    JsonNode argsNode =
                            mapper.readTree(argumentsJson);

                    // -----------------------------------------
                    // READ FILE
                    // -----------------------------------------

                    if ("read_file".equals(functionName)) {

                        String filePath =
                                argsNode.get("file_path").asText();

                        toolResultContent =
                                Files.readString(
                                        Paths.get(filePath)
                                );

                    }

                    // -----------------------------------------
                    // WRITE FILE
                    // -----------------------------------------

                    else if ("write_file".equals(functionName)) {

                        String filePath =
                                argsNode.get("file_path").asText();

                        String content =
                                argsNode.get("content").asText();

                        Path path = Paths.get(filePath);

                        if (path.getParent() != null) {
                            Files.createDirectories(
                                    path.getParent()
                            );
                        }

                        Files.writeString(path, content);

                        toolResultContent =
                                "File written successfully.";

                    }

                    // -----------------------------------------
                    // BASH
                    // -----------------------------------------

                    else if ("bash".equals(functionName)) {

                        String command =
                                argsNode.get("command").asText();

                        ProcessBuilder processBuilder;

                        String os =
                                System.getProperty("os.name")
                                        .toLowerCase();

                        // Windows
                        if (os.contains("win")) {

                            processBuilder =
                                    new ProcessBuilder(
                                            "cmd.exe",
                                            "/c",
                                            command
                                    );

                        }

                        // Linux / macOS / CodeCrafters
                        else {

                            processBuilder =
                                    new ProcessBuilder(
                                            "sh",
                                            "-c",
                                            command
                                    );
                        }

                        // Combine stderr with stdout
                        processBuilder.redirectErrorStream(true);

                        Process process =
                                processBuilder.start();

                        byte[] outputBytes =
                                process.getInputStream()
                                        .readAllBytes();

                        String output =
                                new String(outputBytes);

                        int exitCode =
                                process.waitFor();

                        if (output.trim().isEmpty()) {

                            toolResultContent =
                                    exitCode == 0
                                            ? "Command executed successfully with no output."
                                            : "Command failed with exit code "
                                                    + exitCode;

                        } else {

                            toolResultContent = output;
                        }

                    }

                    // -----------------------------------------
                    // UNKNOWN TOOL
                    // -----------------------------------------

                    else {

                        toolResultContent =
                                "Unknown tool called: "
                                        + functionName;
                    }

                } catch (Exception e) {

                    System.err.println(
                            "Error executing tool ("
                                    + functionName
                                    + "): "
                                    + e.getMessage()
                    );

                    toolResultContent =
                            "Error: " + e.getMessage();
                }

                // -------------------------------------------------
                // Send tool result back to Claude
                // -------------------------------------------------

                messages.add(
                        ChatCompletionMessageParam.ofTool(
                                ChatCompletionToolMessageParam.builder()
                                        .toolCallId(toolCallId)
                                        .content(toolResultContent)
                                        .build()
                        )
                );
            }
        }

        // In -p mode, execute only once and exit.
        if (!interactive) {
            break;
        }
    }

    scanner.close();
}