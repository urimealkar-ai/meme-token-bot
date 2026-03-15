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
            
            for (int i = 0; i < tokens.size(); i++) {
                try {
                    JsonObject token = tokens.get(i).getAsJsonObject();
                    
                    // Проверяем chainId
                    if (!token.has("chainId")) continue;
                    String chainId = token.get("chainId").getAsString();
                    if (!"solana".equals(chainId)) continue;
                    
                    // Поле может называться по-разному: "token" или "baseToken"
                    JsonObject tokenInfo = null;
                    if (token.has("token")) {
                        tokenInfo = token.getAsJsonObject("token");
                    } else if (token.has("baseToken")) {
                        tokenInfo = token.getAsJsonObject("baseToken");
                    } else {
                        System.out.println("⚠️ Пропускаю: нет информации о токене");
                        continue;
                    }
                    
                    // Проверяем наличие обязательных полей
                    if (!tokenInfo.has("address")) continue;
                    
                    String address = tokenInfo.get("address").getAsString();
                    
                    if (!seenTokens.contains(address)) {
                        seenTokens.add(address);
                        
                        String name = tokenInfo.has("name") ? tokenInfo.get("name").getAsString() : "Unknown";
                        String symbol = tokenInfo.has("symbol") ? tokenInfo.get("symbol").getAsString() : "???";
                        
                        // Формируем сообщение
                        StringBuilder message = new StringBuilder();
                        message.append("🆕 **НОВЫЙ ТОКЕН НА SOLANA!**\n\n");
                        message.append("📛 **Имя:** ").append(name).append("\n");
                        message.append("🔤 **Символ:** ").append(symbol).append("\n");
                        message.append("🔗 **Адрес:** `").append(address).append("`\n\n");
                        
                        // Добавляем ссылки
                        message.append("📊 [DexScreener](https://dexscreener.com/solana/").append(address).append(")\n");
                        message.append("🛒 [Jupiter](https://jup.ag/swap/SOL-").append(address).append(")\n");
                        message.append("📈 [Birdeye](https://birdeye.so/token/").append(address).append("?chain=solana)");
                        
                        bot.sendNotification(chatId, message.toString());
                        newCount++;
                        
                        Thread.sleep(300);
                    }
                    
                } catch (Exception e) {
                    System.out.println("⚠️ Ошибка обработки токена #" + i + ": " + e.getMessage());
                }
            }
            
            System.out.println("✅ Найдено новых токенов Solana: " + newCount);
            
        } catch (Exception e) {
            System.out.println("❌ Ошибка парсинга JSON: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
