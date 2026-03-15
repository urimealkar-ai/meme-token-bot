package com.yourbot;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import com.google.gson.*;
import java.net.URI;
import java.util.*;

public class RaydiumWebSocketScanner {
    
    private WebSocketClient wsClient;
    private final MemeTokenBot bot;
    private final long chatId;
    private final Set<String> seenPools = new HashSet<>();
    private static final String RAYDIUM_PROGRAM_ID = "675kPX9MHTjS2zt1qfr1NYHuzeLXfQM9H24wFSUt1Mp8";
    private static final String HELIUS_WSS = "wss://atlas-mainnet.helius-rpc.com?api-key=";
    private final String apiKey;
    
    public RaydiumWebSocketScanner(MemeTokenBot bot, long chatId, String heliusApiKey) {
        this.bot = bot;
        this.chatId = chatId;
        this.apiKey = heliusApiKey;
    }
    
    public void start() {
        try {
            // Подключаемся к Helius WebSocket (нужен API ключ)
            String wssUrl = HELIUS_WSS + apiKey;
            wsClient = new WebSocketClient(new URI(wssUrl)) {
                
                @Override
                public void onOpen(ServerHandshake handshake) {
                    System.out.println("✅ WebSocket подключен к Solana");
                    sendSubscriptionRequest();
                }
                
                @Override
                public void onMessage(String message) {
                    handleIncomingMessage(message);
                }
                
                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("❌ WebSocket закрыт: " + reason);
                    // Переподключаемся через 5 секунд
                    try {
                        Thread.sleep(5000);
                        reconnect();
                    } catch (InterruptedException e) {}
                }
                
                @Override
                public void onError(Exception ex) {
                    System.out.println("❌ WebSocket ошибка: " + ex.getMessage());
                }
            };
            
            wsClient.connect();
            
        } catch (Exception e) {
            System.out.println("❌ Ошибка подключения WebSocket: " + e.getMessage());
        }
    }
    
    private void sendSubscriptionRequest() {
        // Подписываемся на транзакции программы Raydium
        JsonObject request = new JsonObject();
        request.addProperty("jsonrpc", "2.0");
        request.addProperty("id", 1);
        request.addProperty("method", "transactionSubscribe");
        
        JsonArray params = new JsonArray();
        
        // Параметры фильтрации
        JsonObject filter = new JsonObject();
        filter.addProperty("failed", false);
        JsonArray accounts = new JsonArray();
        accounts.add(RAYDIUM_PROGRAM_ID);
        filter.add("accountInclude", accounts);
        params.add(filter);
        
        // Опции подписки
        JsonObject options = new JsonObject();
        options.addProperty("commitment", "confirmed");
        options.addProperty("encoding", "jsonParsed");
        options.addProperty("maxSupportedTransactionVersion", 0);
        params.add(options);
        
        request.add("params", params);
        
        wsClient.send(request.toString());
        System.out.println("📡 Подписка на Raydium отправлена");
    }
    
    private void handleIncomingMessage(String message) {
        try {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();
            
            // Проверяем, что это результат транзакции
            if (!json.has("params")) return;
            
            JsonObject result = json.getAsJsonObject("params").getAsJsonObject("result");
            
            // Получаем логи транзакции
            JsonArray logs = result.getAsJsonObject("transaction")
                                   .getAsJsonObject("meta")
                                   .getAsJsonArray("logMessages");
            
            // Ищем лог создания нового пула
            boolean isNewPool = false;
            for (int i = 0; i < logs.size(); i++) {
                String log = logs.get(i).getAsString();
                if (log.contains("initialize2: InitializeInstruction2")) {
                    isNewPool = true;
                    break;
                }
            }
            
            if (!isNewPool) return;
            
            // Получаем подписи и ключи аккаунтов
            String signature = result.get("signature").getAsString();
            JsonArray accountKeys = result.getAsJsonObject("transaction")
                                          .getAsJsonObject("transaction")
                                          .getAsJsonObject("message")
                                          .getAsJsonArray("accountKeys");
            
            // Извлекаем адреса токенов
            String tokenA = accountKeys.get(8).getAsJsonObject().get("pubkey").getAsString();
            String tokenB = accountKeys.get(9).getAsJsonObject().get("pubkey").getAsString();
            String ammId = accountKeys.get(2).getAsJsonObject().get("pubkey").getAsString();
            
            // Формируем уникальный ID пула
            String poolId = signature + "_" + ammId;
            
            // Проверяем, не видели ли мы этот пул раньше
            if (!seenPools.contains(poolId)) {
                seenPools.add(poolId);
                
                // Определяем, какой токен новый (не SOL)
                String SOL_MINT = "So11111111111111111111111111111111111111112";
                String newToken = tokenA.equals(SOL_MINT) ? tokenB : tokenA;
                String pairToken = tokenA.equals(SOL_MINT) ? tokenA : tokenB;
                
                // Отправляем уведомление
                sendNewTokenNotification(newToken, pairToken, ammId, signature);
                
                System.out.println("🆕 Новый токен найден! " + newToken);
            }
            
        } catch (Exception e) {
            // Игнорируем ошибки парсинга (часто приходят пустые сообщения)
        }
    }
    
    private void sendNewTokenNotification(String tokenAddress, String pairToken, String ammId, String signature) {
        String message = String.format(
            "🆕 **НОВЫЙ ТОКЕН НА SOLANA!**\n\n" +
            "🔗 **Адрес токена:** `%s`\n" +
            "💧 **Пара с:** %s\n" +
            "🏊 **ID пула:** `%s`\n" +
            "🔍 **Tx:** `%s`\n\n" +
            "📊 **Ссылки:**\n" +
            "• [DexScreener](https://dexscreener.com/solana/%s)\n" +
            "• [Jupiter](https://jup.ag/swap/SOL-%s)\n" +
            "• [Birdeye](https://birdeye.so/token/%s?chain=solana)\n" +
            "• [Solscan](https://solscan.io/token/%s)",
            tokenAddress, 
            pairToken.equals("So11111111111111111111111111111111111111112") ? "SOL" : pairToken,
            ammId,
            signature,
            tokenAddress, tokenAddress, tokenAddress, tokenAddress
        );
        
        bot.sendNotification(chatId, message);
    }
    
    public void stop() {
        if (wsClient != null) {
            wsClient.close();
        }
    }
}
