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
    private boolean firstScan = true;
    
    public PumpFunScanner(MemeTokenBot bot, long chatId) {
        this.bot = bot;
        this.chatId = chatId;
    }
    
    public void scanNewTokens() {
        System.out.println("🔄 Запуск сканирования DexScreener...");
        
        // Тестовое сообщение (можно потом убрать)
        if (firstScan) {
            firstScan = false;
            bot.sendNotification(chatId, "🧪 Сканер запущен, ищу токены на Solana...");
        }
        
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.dexscreener.com/token-profiles/latest/v1"))
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0")
                .timeout(java.time.Duration.ofSeconds(15))
                .build();
            
            System.out.println("📡 Отправляю запрос к DexScreener API...");
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            System.out.println("✅ Статус ответа: " + response.statusCode());
            
            if (response.statusCode() == 200) {
                parseDexScreenerTokens(response.body());
            } else {
                System.out.println("❌ Ошибка API. Статус: " + response.statusCode());
            }
            
        } catch (Exception e) {
            System.out.println("❌ Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void parseDexScreenerTokens(String json) {
        try {
            JsonArray tokens = JsonParser.parseString(json).getAsJsonArray();
            System.out.println("✅ Получено токенов: " + tokens.size());
            
            int newCount = 0;
            
            for (var element : tokens) {
                JsonObject token = element.getAsJsonObject();
                String chainId = token.get("chainId").getAsString();
                
                // Нас интересует только Solana
                if (!"solana".equals(chainId)) continue;
                
                JsonObject tokenInfo = token.getAsJsonObject("token");
                String address = tokenInfo.get("address").getAsString();
                
                if (!seenTokens.contains(address)) {
                    seenTokens.add(address);
                    
                    String name = tokenInfo.get("name").getAsString();
                    String symbol = tokenInfo.get("symbol").getAsString();
                    
                    String message = String.format(
                        "🆕 **НОВЫЙ ТОКЕН НА SOLANA!**\n\n" +
                        "📛 **Имя:** %s\n" +
                        "🔤 **Символ:** %s\n" +
                        "🔗 **Адрес:** `%s`\n\n" +
                        "📊 [Посмотреть на DexScreener](https://dexscreener.com/solana/%s)\n" +
                        "🛒 [Купить на Jupiter](https://jup.ag/swap/SOL-%s)",
                        name, symbol, address, address, address
                    );
                    
                    bot.sendNotification(chatId, message);
                    newCount++;
                    Thread.sleep(300);
                }
            }
            
            System.out.println("✅ Найдено новых токенов Solana: " + newCount);
            
        } catch (Exception e) {
            System.out.println("❌ Ошибка парсинга: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
