package com.yourbot;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class MemeTokenBot extends TelegramLongPollingBot {

    private final String botToken;
    private final String botUsername = "MemTokenScanner_bot"; // ЗДЕСЬ ТВОЁ ИМЯ БОТА

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
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            String responseText;

            switch (messageText) {
                case "/start":
                    responseText = "👋 Привет! Я бот для отслеживания мем-токенов на Solana!\n\n"
                                 + "Доступные команды:\n"
                                 + "/start - показать это меню\n"
                                 + "/status - проверить работу бота";
                    break;
                case "/status":
                    responseText = "✅ Бот работает!\n"
                                 + "⏰ Время: " + new java.util.Date();
                    break;
                default:
                    responseText = "Я пока понимаю только команды /start и /status 😅";
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
    }

    public static void main(String[] args) {
    try {
        // HTTP-сервер для Render (исправленная версия)
        String portEnv = System.getenv("PORT");
        if (portEnv == null) {
            portEnv = "10000";
        }
        int port = Integer.parseInt(portEnv);
        
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/", exchange -> {
            String response = "Bot is running!";
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            
            // Правильная обработка HEAD-запросов
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
        
        // Запуск Telegram бота
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
        System.out.println("🔒 Режим: только для владельца (chatId = " + bot.MY_CHAT_ID + ")");
        
    } catch (Exception e) {
        System.err.println("❌ КРИТИЧЕСКАЯ ОШИБКА:");
        e.printStackTrace();
    }
}
}
