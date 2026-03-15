package com.yourbot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class MemeTokenBot extends TelegramLongPollingBot {
    
    private final String botToken;
    private final String botUsername = "MemTokenScanner_bot"; // Замени на свое!
    
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
        // Проверяем, есть ли сообщение и текст
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
            
            // Отправляем ответ
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
            // Получаем токен из переменной окружения
            String botToken = System.getenv("BOT_TOKEN");
            
            if (botToken == null || botToken.isEmpty()) {
                System.err.println("ОШИБКА: BOT_TOKEN не задан!");
                return;
            }
            
            // Создаем и регистрируем бота
            MemeTokenBot bot = new MemeTokenBot(botToken);
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(bot);
            
            System.out.println("✅ Бот успешно запущен!");
            System.out.println("🤖 Username: @" + bot.getBotUsername());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
