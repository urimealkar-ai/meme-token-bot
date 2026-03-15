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
    private MemeTokenBot bot;
    private long chatId;
    private boolean firstScan = true; // Для первого теста
    
    public PumpFunScanner(MemeTokenBot bot, long chatId) {
        this.bot = bot;
        this.chatId = chatId;
    }
    
    public void scanNewTokens() {
        System.out.println("🔄 Запуск сканирования Pump.fun...");
        
        // ТЕСТ: при первом запуске отправляем тестовое сообщение
        if (firstScan) {
            firstScan = false;
            bot.sendNotification(chatId, "🧪 Тестовое сообщение от сканера! Если ты это видишь, значит бот может отправлять уведомления.");
        }
        
        try {
            // Формируем запрос к API Pump.fun
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://frontend-api.pump.fun/coins?limit=10&offset=0&sort=created&order=DESC"))
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0") // Добавим User-Agent для надежности
                .timeout(java.time.Duration.ofSeconds(10))
                .build();
            
            System.out.println("📡 Отправляю запрос к API Pump.fun...");
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            System.out.println("✅ Статус ответа: " + response.statusCode());
            
            if (response.statusCode() == 200) {
                String body = response.body();
                System.out.println("📦 Получено данных: " + body.length() + " символов");
                
                if (body.length() < 10) {
                    System.out.println("❌ Ответ слишком короткий: " + body);
                    return;
                }
                
                parseTokens(body);
            } else {
                System.out.println("❌ Ошибка API Pump.fun. Статус: " + response.statusCode());
                System.out.println("Тело ответа: " + response.body());
            }
            
        } catch (java.net.http.HttpTimeoutException e) {
            System.out.println("⏰ Таймаут соединения: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ Ошибка сканирования: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void parseTokens(String json) {
        try {
            System.out.println("🔍 Парсинг JSON ответа...");
            JsonArray tokens = JsonParser.parseString(json).getAsJsonArray();
            System.out.println("✅ Получено токенов в ответе: " + tokens.size());
            
            int newTokensCount = 0;
            
            for (int i = 0; i < tokens.size(); i++) {
                JsonObject token = tokens.get(i).getAsJsonObject();
                
                String mint = token.get("mint").getAsString();
                String name = token.get("name").getAsString();
                String symbol = token.get("symbol").getAsString();
                
                // Проверяем, видели ли мы этот токен
                if (!seenTokens.contains(mint)) {
                    seenTokens.add(mint);
                    
                    // Получаем остальные поля (могут отсутствовать)
                    String creator = token.has("creator") ? token.get("creator").getAsString() : "Неизвестно";
                    String createdAt = token.has("createdAt") ? token.get("createdAt").getAsString() : "Неизвестно";
                    
                    // Формируем красивое сообщение
                    String message = String.format(
                        "🆕 **НАЙДЕН НОВЫЙ ТОКЕН!**\n\n" +
                        "📛 **Имя:** %s\n" +
                        "🔤 **Символ:** %s\n" +
                        "👤 **Создатель:** %s\n" +
                        "⏰ **Создан:** %s\n" +
                        "🔗 **Адрес:** `%s`\n\n" +
                        "🌐 [Посмотреть на Pump.fun](https://pump.fun/coin/%s)\n" +
                        "📊 [DexScreener](https://dexscreener.com/solana/%s)",
                        name, symbol, creator, createdAt, mint, mint, mint
                    );
                    
                    bot.sendNotification(chatId, message);
                    newTokensCount++;
                    
                    System.out.println("✅ Новый токен: " + name + " (" + symbol + ")");
                    
                    // Небольшая задержка, чтобы не заспамить
                    Thread.sleep(300);
                }
            }
            
            if (newTokensCount > 0) {
                System.out.println("🎉 Всего новых токенов: " + newTokensCount);
            } else {
                System.out.println("⏳ Новых токенов не найдено");
            }
            
        } catch (Exception e) {
            System.out.println("❌ Ошибка парсинга JSON: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
