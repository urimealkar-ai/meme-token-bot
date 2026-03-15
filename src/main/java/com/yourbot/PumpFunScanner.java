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
        System.out.println("✅ Получено токенов в ответе: " + tokens.size());

        int newCount = 0;

        for (int i = 0; i < tokens.size(); i++) {
            try {
                JsonObject item = tokens.get(i).getAsJsonObject();

                // 1. Проверяем, что это Solana
                if (!item.has("chainId")) continue;
                String chainId = item.get("chainId").getAsString();
                if (!"solana".equals(chainId)) continue;

                // 2. Ищем информацию о токене (в разных полях)
                JsonObject tokenInfo = null;
                String address = null;
                String name = null;
                String symbol = null;

                // Поле может называться "token", "baseToken" или информация может быть на верхнем уровне
                if (item.has("token") && item.get("token").isJsonObject()) {
                    tokenInfo = item.getAsJsonObject("token");
                } else if (item.has("baseToken") && item.get("baseToken").isJsonObject()) {
                    tokenInfo = item.getAsJsonObject("baseToken");
                } else {
                    // Если нет вложенного объекта, пробуем взять поля напрямую из item
                    if (item.has("address")) address = item.get("address").getAsString();
                    if (item.has("name")) name = item.get("name").getAsString();
                    if (item.has("symbol")) symbol = item.get("symbol").getAsString();
                }

                // Если нашли tokenInfo, извлекаем данные из него
                if (tokenInfo != null) {
                    if (tokenInfo.has("address")) address = tokenInfo.get("address").getAsString();
                    if (tokenInfo.has("name")) name = tokenInfo.get("name").getAsString();
                    if (tokenInfo.has("symbol")) symbol = tokenInfo.get("symbol").getAsString();
                }

                // 3. Если мы не смогли найти адрес — пропускаем токен
                if (address == null || address.isEmpty()) {
                    System.out.println("⚠️ Пропускаю токен #" + i + ": нет адреса");
                    continue;
                }

                // 4. Проверяем, не видели ли мы этот токен раньше
                if (!seenTokens.contains(address)) {
                    seenTokens.add(address);

                    // Подставляем заглушки для имени и символа, если их нет
                    if (name == null || name.isEmpty()) name = "Unknown";
                    if (symbol == null || symbol.isEmpty()) symbol = "???";

                    // Формируем красивое сообщение
                    String message = String.format(
                        "🆕 **НАЙДЕН НОВЫЙ ТОКЕН НА SOLANA!**\n\n" +
                        "📛 **Имя:** %s\n" +
                        "🔤 **Символ:** %s\n" +
                        "🔗 **Адрес:** `%s`\n\n" +
                        "📊 [DexScreener](https://dexscreener.com/solana/%s)\n" +
                        "🛒 [Jupiter](https://jup.ag/swap/SOL-%s)\n" +
                        "📈 [Birdeye](https://birdeye.so/token/%s?chain=solana)",
                        name, symbol, address, address, address, address
                    );

                    bot.sendNotification(chatId, message);
                    newCount++;
                    Thread.sleep(300); // Небольшая задержка, чтобы не заспамить
                }

            } catch (Exception e) {
                System.out.println("⚠️ Ошибка при обработке токена #" + i + ": " + e.getMessage());
                // Продолжаем со следующим токеном, даже если этот сломался
            }
        }

        System.out.println("✅ Найдено НОВЫХ токенов Solana: " + newCount);

    } catch (Exception e) {
        System.out.println("❌ Критическая ошибка парсинга JSON: " + e.getMessage());
        e.printStackTrace();
    }
}
}
