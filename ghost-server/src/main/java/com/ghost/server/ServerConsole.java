package com.ghost.server;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Model;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;
import picocli.shell.jline3.PicocliJLineCompleter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * サーバーのコンソール入力を管理するクラス。
 * JLine3 と Picocli を組み合わせた対話型コンソールを提供します。
 * Tab 補完・入力履歴をサポートし、コマンドが増えても容易に拡張できます。
 */
public class ServerConsole {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerConsole.class);
    private static final String PROMPT = "ghost> ";

    private final GhostModServer server;
    // コンソールループの継続フラグ（StopCommand から false にセットされる）
    private final AtomicBoolean running = new AtomicBoolean(true);

    public ServerConsole(GhostModServer server) {
        this.server = server;
    }

    // =========================================================================
    // コマンド定義（Picocli アノテーション）
    // =========================================================================

    /**
     * コンソールのルートコマンド。
     * 入力が空またはサブコマンドに一致しない場合にヘルプを表示します。
     */
    @Command(
            name = "",
            mixinStandardHelpOptions = false,
            description = "Ghost Server Console",
            subcommands = {
                    StopCommand.class,
                    HelpCommand.class
            }
    )
    static class RootCommand implements Runnable {

        @Spec
        Model.CommandSpec spec;

        // サーバー参照と終了フラグは start() 内でセットされる
        GhostModServer server;
        AtomicBoolean running;

        @Override
        public void run() {
            // サブコマンド未入力時はヘルプを表示
            spec.commandLine().usage(spec.commandLine().getOut());
        }
    }

    /**
     * stop コマンド: 全クライアントへ切断を通知してサーバーを停止します。
     */
    @Command(
            name = "stop",
            description = "Notify all clients and safely stop the server."
    )
    static class StopCommand implements Runnable {

        @ParentCommand
        RootCommand parent;

        @Override
        public void run() {
            parent.server.shutdown();
            parent.running.set(false);
        }
    }

    // =========================================================================
    // コンソールループ
    // =========================================================================

    /**
     * コンソールを起動し、入力ループを開始します（ブロッキング）。
     * stop コマンド実行または EOF（Ctrl+D）/ Ctrl+C で返ります。
     */
    public void start() {
        try (Terminal terminal = TerminalBuilder.builder().system(true).build()) {

            // ルートコマンドのインスタンスにサーバー参照と終了フラグをセット
            RootCommand root = new RootCommand();
            root.server = server;
            root.running = running;

            CommandLine commandLine = new CommandLine(root);
            PrintWriter writer = terminal.writer();
            commandLine.setOut(writer);
            commandLine.setErr(writer);

            // Picocli のコマンド定義から JLine3 の Tab 補完機能を構築
            PicocliJLineCompleter completer = new PicocliJLineCompleter(commandLine.getCommandSpec());

            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .completer(completer)
                    .parser(new DefaultParser())
                    .build();

            LOGGER.info("Console started: type 'stop' to shutdown, 'help' for commands, Tab for completion.");

            while (running.get()) {
                try {
                    String line = reader.readLine(PROMPT);
                    if (line == null || line.isBlank()) continue;
                    // 入力をスペース区切りで分割し Picocli に渡す
                    commandLine.execute(line.trim().split("\\s+"));
                } catch (UserInterruptException e) {
                    LOGGER.info("Ctrl+C detected. Shutting down server...");
                    server.shutdown();
                    running.set(false);
                } catch (EndOfFileException e) {
                    LOGGER.info("EOF received. Shutting down server...");
                    server.shutdown();
                    running.set(false);
                }
            }

        } catch (IOException e) {
            LOGGER.warn("Failed to initialize JLine3 terminal. Falling back to standard input mode.", e);
            fallbackConsole();
        }
    }

    /**
     * JLine3 が使用できない環境でのフォールバックコンソール。
     * 単純な BufferedReader で "stop" コマンドのみを受け付けます。
     */
    private void fallbackConsole() {
        LOGGER.info("Fallback console mode: type 'stop' to shutdown.");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while (running.get() && (line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if ("stop".equalsIgnoreCase(trimmed)) {
                    server.shutdown();
                    running.set(false);
                } else if (!trimmed.isEmpty()) {
                    LOGGER.warn("Unknown command: '{}'. Type 'stop' or 'help'.", trimmed);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to read standard input", e);
        }
    }
}
