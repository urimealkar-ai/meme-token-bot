package com.yourbot;

import com.sun.net.httpserver.HttpServer;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class MemeTokenBot extends TelegramLongPollingBot { // <<< НАЧАЛО КЛАССА

    private final String botToken;
    private final String botUsername = "MemTokenScanner_bot";
    private final long MY_CHAT_ID = 911691327; // ЗДЕСЬ ТВОЙ ID

    public MemeTokenBot(String botToken) {
        this.botToken = botToken;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        System.out.println("🔥 ПОЛУЧЕНО ОБНОВЛЕНИЕ: " + update);

        if (!update.hasMessage() || !update.getMessage().hasText()) {
            System.out.println("⏭️ Нет текстового сообщения");
            return;
        }

        long chatId = update.getMessage().getChatId();
        System.out.println("👤 ChatID: " + chatId + " (мой: " + MY_CHAT_ID + ")");

        if (chatId != MY_CHAT_ID) {
            System.out.println("⚠️ Чужой пользователь: " + chatId);
            return;
        }

        String messageText = update.getMessage().getText();
        String responseText;

        switch (messageText) {
            case "/start":
                responseText = "👋 Привет! Я твой личный бот для мем-токенов на Solana!\n\n"
                        + "Доступные команды:\n"
                        + "/start - это меню\n"
                        + "/status - проверить работу";
                break;
            case "/status":
                responseText = "✅ Бот работает!\n"
                        + "⏰ Время: " + new java.util.Date();
                break;
            default:
                responseText = "Я пока понимаю только /start и /status 😅";
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(responseText);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            String portEnv = System.getenv("PORT");
            if (portEnv == null) {
                portEnv = "10000";
            }
            int port = Integer.parseInt(portEnv);

            HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
            server.createContext("/", exchange -> {
                String response = "Bot is running!";
                exchange.getResponseHeaders().set("Content-Type", "text/plain");

                if ("HEAD".equals(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(200, -1);
                } else {
                    exchange.sendResponseHeaders(200, response.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                }
                exchange.close();
            });
            server.setExecutor(null);
            server.start();
            System.out.println("✅ HTTP-сервер запущен на порту " + port);

            String botToken = System.getenv("BOT_TOKEN");
            if (botToken == null || botToken.isEmpty()) {
                System.err.println("❌ ОШИБКА: BOT_TOKEN не задан!");
                return;
            }

            MemeTokenBot bot = new MemeTokenBot(botToken);
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(bot);

            System.out.println("✅ Telegram бот успешно запущен!");
            System.out.println("🤖 Username: @" + bot.getBotUsername());

        } catch (Exception e) {
            System.err.println("❌ КРИТИЧЕСКАЯ ОШИБКА:");
            e.printStackTrace();
        }
    }
} 
