package com.autoparkour.learning;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;

public class AutoparkourLearning {
    private static final String LEARNING_FILE = "autoparkour_learning.json";
    private final Path learningPath;
    private final Gson gson;

    // Параметры обучения
    private float jumpBoost = 0.18f;
    private float sprintReliability = 1.0f;
    private int optimalJumpWindow = 7;
    private float diagonalPenalty = 3.0f;
    private float airControl = 0.8f;
    private int successfulJumps = 0;
    private int failedJumps = 0;
    private float averageJumpDistance = 1.5f;

    // Статистика
    private int totalJumps = 0;
    private int totalOvershoots = 0;
    private int totalUndershoots = 0;

    // Одиночный экземпляр (синглтон)
    private static AutoparkourLearning instance;

    private AutoparkourLearning() {
        this.learningPath = FabricLoader.getInstance().getConfigDir().resolve(LEARNING_FILE);
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .create();
        load();
    }

    public static AutoparkourLearning getInstance() {
        if (instance == null) {
            instance = new AutoparkourLearning();
        }
        return instance;
    }

    public void save() {
    try (Writer writer = new FileWriter(learningPath.toFile())) {
        // Создаём простой JSON вручную, без Gson
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"jumpBoost\": ").append(jumpBoost).append(",\n");
        json.append("  \"sprintReliability\": ").append(sprintReliability).append(",\n");
        json.append("  \"optimalJumpWindow\": ").append(optimalJumpWindow).append(",\n");
        json.append("  \"diagonalPenalty\": ").append(diagonalPenalty).append(",\n");
        json.append("  \"airControl\": ").append(airControl).append(",\n");
        json.append("  \"successfulJumps\": ").append(successfulJumps).append(",\n");
        json.append("  \"failedJumps\": ").append(failedJumps).append(",\n");
        json.append("  \"averageJumpDistance\": ").append(averageJumpDistance).append(",\n");
        json.append("  \"totalJumps\": ").append(totalJumps).append(",\n");
        json.append("  \"totalOvershoots\": ").append(totalOvershoots).append(",\n");
        json.append("  \"totalUndershoots\": ").append(totalUndershoots).append("\n");
        json.append("}");

        writer.write(json.toString());
    } catch (IOException e) {
        e.printStackTrace();
    }
}

public void load() {
    if (!learningPath.toFile().exists()) {
        save();
        return;
    }

    try (Reader reader = new FileReader(learningPath.toFile())) {
        // Простой парсинг вручную
        String content = new String(java.nio.file.Files.readAllBytes(learningPath));

        // Очень простой парсинг (для продакшена лучше использовать JsonReader, но сейчас главное - убрать ошибку)
        jumpBoost = getFloatValue(content, "jumpBoost", jumpBoost);
        sprintReliability = getFloatValue(content, "sprintReliability", sprintReliability);
        optimalJumpWindow = getIntValue(content, "optimalJumpWindow", optimalJumpWindow);
        diagonalPenalty = getFloatValue(content, "diagonalPenalty", diagonalPenalty);
        airControl = getFloatValue(content, "airControl", airControl);
        successfulJumps = getIntValue(content, "successfulJumps", successfulJumps);
        failedJumps = getIntValue(content, "failedJumps", failedJumps);
        averageJumpDistance = getFloatValue(content, "averageJumpDistance", averageJumpDistance);
        totalJumps = getIntValue(content, "totalJumps", totalJumps);
        totalOvershoots = getIntValue(content, "totalOvershoots", totalOvershoots);
        totalUndershoots = getIntValue(content, "totalUndershoots", totalUndershoots);

    } catch (IOException e) {
        e.printStackTrace();
    }
}

private float getFloatValue(String json, String key, float defaultValue) {
    try {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return defaultValue;
        start += search.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        return Float.parseFloat(json.substring(start, end).trim());
    } catch (Exception e) {
        return defaultValue;
    }
}

private int getIntValue(String json, String key, int defaultValue) {
    try {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return defaultValue;
        start += search.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        return Integer.parseInt(json.substring(start, end).trim());
    } catch (Exception e) {
        return defaultValue;
    }
}

    // Методы для обновления параметров на основе результатов
    public void onSuccessfulJump(double distance) {
        successfulJumps++;
        totalJumps++;
        averageJumpDistance = (float) ((averageJumpDistance * (totalJumps - 1) + distance) / totalJumps);

        // Улучшаем параметры при успехе
        if (distance > 2.5) {
            jumpBoost = Math.min(0.28f, jumpBoost + 0.005f);
        } else if (distance < 1.5) {
            jumpBoost = Math.max(0.14f, jumpBoost - 0.002f);
        }

        save();
    }

    public void onFailedJump(boolean overshoot) {
        failedJumps++;
        totalJumps++;

        if (overshoot) {
            totalOvershoots++;
            jumpBoost = Math.max(0.12f, jumpBoost - 0.01f);
            sprintReliability = Math.max(0.6f, sprintReliability - 0.05f);
        } else {
            totalUndershoots++;
            jumpBoost = Math.min(0.25f, jumpBoost + 0.008f);
        }

        save();
    }

    public void onTeleport() {
        // При телепорте сбрасываем статистику, но сохраняем параметры
        successfulJumps = 0;
        failedJumps = 0;
        totalJumps = 0;
        totalOvershoots = 0;
        totalUndershoots = 0;
        save();
    }

    // Геттеры и сеттеры
    public float getJumpBoost() { return jumpBoost; }
    public void setJumpBoost(float jumpBoost) {
        this.jumpBoost = Math.max(0.1f, Math.min(0.3f, jumpBoost));
        save();
    }

    public float getSprintReliability() { return sprintReliability; }
    public void setSprintReliability(float sprintReliability) {
        this.sprintReliability = Math.max(0.5f, Math.min(2.0f, sprintReliability));
        save();
    }

    public int getOptimalJumpWindow() { return optimalJumpWindow; }
    public void setOptimalJumpWindow(int optimalJumpWindow) {
        this.optimalJumpWindow = Math.max(3, Math.min(12, optimalJumpWindow));
        save();
    }

    public float getDiagonalPenalty() { return diagonalPenalty; }
    public void setDiagonalPenalty(float diagonalPenalty) {
        this.diagonalPenalty = Math.max(1.5f, Math.min(10.0f, diagonalPenalty));
        save();
    }

    public float getAirControl() { return airControl; }
    public void setAirControl(float airControl) {
        this.airControl = Math.max(0.5f, Math.min(1.0f, airControl));
        save();
    }

    public int getSuccessfulJumps() { return successfulJumps; }
    public int getFailedJumps() { return failedJumps; }
    public float getAverageJumpDistance() { return averageJumpDistance; }
    public int getTotalJumps() { return totalJumps; }
    public int getTotalOvershoots() { return totalOvershoots; }
    public int getTotalUndershoots() { return totalUndershoots; }

    public float getSuccessRate() {
        if (totalJumps == 0) return 0;
        return (float) successfulJumps / totalJumps * 100;
    }

    public void resetStats() {
        successfulJumps = 0;
        failedJumps = 0;
        totalJumps = 0;
        totalOvershoots = 0;
        totalUndershoots = 0;
        save();
    }

    public void resetAll() {
        jumpBoost = 0.18f;
        sprintReliability = 1.0f;
        optimalJumpWindow = 7;
        diagonalPenalty = 3.0f;
        airControl = 0.8f;
        successfulJumps = 0;
        failedJumps = 0;
        averageJumpDistance = 1.5f;
        totalJumps = 0;
        totalOvershoots = 0;
        totalUndershoots = 0;
        save();
    }
}
