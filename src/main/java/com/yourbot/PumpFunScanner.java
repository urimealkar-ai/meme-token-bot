package com.yourbot;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.HashSet;
import java.util.Set;

public class PumpFunScanner {
    
    private final HttpClient client = HttpClient.newHttpClient();
    private final Set<String> seenTokens = new HashSet<>();
    private MemeTokenBot bot;  // Ссылка на бота для отправки сообщений
    private long chatId;        // Твой Chat ID
    
    public PumpFunScanner(MemeTokenBot bot, long chatId) {
        this.bot = bot;
        this.chatId = chatId;
    }
    
    public void scanNewTokens() {
        try {
            // Запрос к API Pump.fun для получения новых токенов
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://frontend-api.pump.fun/coins?limit=20&offset=0&sort=created&order=DESC"))
                .header("Accept", "application/json")
                .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                parseTokens(response.body());
            } else {
                System.out.println("❌ Ошибка API Pump.fun: " + response.statusCode());
            }
            
        } catch (Exception e) {
            System.out.println("❌ Ошибка сканирования: " + e.getMessage());
        }
    }
    
    private void parseTokens(String json) {
        try {
            JsonArray tokens = JsonParser.parseString(json).getAsJsonArray();
            int newTokensCount = 0;
            
            for (var element : tokens) {
                JsonObject token = element.getAsJsonObject();
                String mint = token.get("mint").getAsString();
                
                // Проверяем, новый ли токен
                if (!seenTokens.contains(mint)) {
                    seenTokens.add(mint);
                    
                    String name = token.get("name").getAsString();
                    String symbol = token.get("symbol").getAsString();
                    String creator = token.get("creator").getAsString();
                    String createdAt = token.get("createdAt").getAsString();
                    
                    // Отправляем уведомление в Telegram
                    String message = String.format(
                        "🆕 **НОВЫЙ ТОКЕН НА PUMP.FUN!**\n\n" +
                        "📛 Имя: %s\n" +
                        "🔤 Символ: %s\n" +
                        "👤 Создатель: %s\n" +
                        "⏰ Создан: %s\n" +
                        "🔗 Адрес: `%s`\n\n" +
                        "🌐 Посмотреть: https://pump.fun/coin/%s",
                        name, symbol, creator, createdAt, mint, mint
                    );
                    
                    bot.sendNotification(chatId, message);
                    newTokensCount++;
                    
                    // Небольшая задержка между сообщениями
                    Thread.sleep(500);
                }
            }
            
            if (newTokensCount > 0) {
                System.out.println("✅ Найдено новых токенов: " + newTokensCount);
            }
            
        } catch (Exception e) {
            System.out.println("❌ Ошибка парсинга: " + e.getMessage());
        }
    }
}
