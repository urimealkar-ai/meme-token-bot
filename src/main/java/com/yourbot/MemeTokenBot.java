package com.yourbot;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import java.util.List;
import java.util.ArrayList;
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

public class MemeTokenBot extends TelegramLongPollingBot {

    private final String botToken;
    private final String botUsername = "MemTokenScanner_bot";
    private final long MY_CHAT_ID = 911691327;

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
        String messageText = update.getMessage().getText();

        if (chatId != MY_CHAT_ID) {
            System.out.println("⚠️ Чужой пользователь: " + chatId);
            return;
        }

        System.out.println("👤 Команда: " + messageText);

        String responseText;
        SendMessage message = new SendMessage();
        message.setChatId(chatId);

        switch (messageText) {
            case "/start":
                responseText = "👋 Добро пожаловать! Я бот для отслеживания мем-токенов на Solana.\n\nВыбери действие:";
                message.setText(responseText);
                message.setReplyMarkup(createMainKeyboard());
                break;

                 case "🔍 За неделю":
                    responseText = "🔍 WebSocket сканер уже работает в фоне!\n\nОн будет присылать новые токены автоматически.";
                    message.setText(responseText);
                    break;

            case "📅 За сегодня":
                responseText = "⏳ Список мем-коинов за СЕГОДНЯ (скоро будет)";
                message.setText(responseText);
                break;

            case "📊 Мой портфель":
                responseText = "💼 Здесь будет твой портфель (в разработке)";
                message.setText(responseText);
                break;

            case "⚙️ Настройки":
                responseText = "⚙️ Настройки (пока пусто)";
                message.setText(responseText);
                break;

            case "/status":
                responseText = "✅ Бот работает!\n⏰ Время: " + new java.util.Date();
                message.setText(responseText);
                break;

            default:
                responseText = "Я пока не знаю такой команды. Используй меню 👇";
                message.setText(responseText);
                message.setReplyMarkup(createMainKeyboard());
        }

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private ReplyKeyboardMarkup createMainKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("🔍 За неделю");
        row1.add("📅 За сегодня");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("📊 Мой портфель");
        row2.add("⚙️ Настройки");

        keyboard.add(row1);
        keyboard.add(row2);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    public void sendNotification(long chatId, String messageText) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(messageText);
        message.setParseMode("Markdown");

        try {
            execute(message);
            System.out.println("✅ Уведомление отправлено");
        } catch (TelegramApiException e) {
            System.out.println("❌ Ошибка отправки уведомления: " + e.getMessage());
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

            // После botsApi.registerBot(bot);
System.out.println("✅ Telegram бот успешно запущен!");

// Запускаем WebSocket сканер
bot.startRaydiumScanner();

            System.out.println("✅ Telegram бот успешно запущен!");

            new Thread(() -> {
                PumpFunScanner scanner = new PumpFunScanner(bot, bot.MY_CHAT_ID);
                while (true) {
                    try {
                        Thread.sleep(60000);
                        scanner.scanNewTokens();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            System.out.println("🤖 Username: @" + bot.getBotUsername());

        } catch (Exception e) {
            System.err.println("❌ КРИТИЧЕСКАЯ ОШИБКА:");
            e.printStackTrace();
        }
    }
    private RaydiumWebSocketScanner raydiumScanner;

// Добавь этот метод для запуска WebSocket сканера
public void startRaydiumScanner() {
    String heliusApiKey = System.getenv("HELIUS_API_KEY");
    if (heliusApiKey == null || heliusApiKey.isEmpty()) {
        System.out.println("⚠️ HELIUS_API_KEY не задан, WebSocket сканер не запущен");
        return;
    }
    
    // Извлекаем API ключ из полного URL
    if (heliusApiKey.contains("api-key=")) {
        heliusApiKey = heliusApiKey.split("api-key=")[1];
    }
    
    raydiumScanner = new RaydiumWebSocketScanner(this, MY_CHAT_ID, heliusApiKey);
    raydiumScanner.start();
    System.out.println("✅ Raydium WebSocket сканер запущен");
}
}
