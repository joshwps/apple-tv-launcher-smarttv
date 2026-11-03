package com.kulwant.atvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

final class Weather {

    private static final String TAG = "Weather";
    private Weather() {}

    static final int SCHEMA = 2;

    static final class Data {
        double tempC;
        int code;
        boolean isDay = true;
        String label;
        long fetchedAt;
    }

    static final class Day {
        long dateMs;
        int code;
        double maxC, minC;
        String label;
    }

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences("home", Context.MODE_PRIVATE);
    }

    static String setCity(Context c, String city) {
        if (city == null || city.trim().isEmpty()) return "Cidade vazia";
        try {
            String q = java.net.URLEncoder.encode(city.trim(), "UTF-8");
            JSONObject geo = httpGet("https://geocoding-api.open-meteo.com/v1/search?name="
                    + q + "&count=1&language=pt&format=json");
            if (!geo.has("results") || geo.getJSONArray("results").length() == 0) {
                return "Cidade não encontrada";
            }
            JSONObject r = geo.getJSONArray("results").getJSONObject(0);
            double lat = r.getDouble("latitude");
            double lon = r.getDouble("longitude");
            String name = r.optString("name", city);
            p(c).edit()
                    .putString("weather_city", name)
                    .putLong("weather_lat", Double.doubleToLongBits(lat))
                    .putLong("weather_lon", Double.doubleToLongBits(lon))
                    .putInt("weather_schema", SCHEMA)
                    .putLong("weather_at", 0)
                    .putString("weather_days_json", "")
                    .apply();
            return null;
        } catch (Exception e) {
            return "Sem internet ou cidade inválida";
        }
    }

    static String cityName(Context c) {
        return p(c).getString("weather_city", "");
    }

    static void clear(Context c) {
        p(c).edit()
                .remove("weather_city").remove("weather_lat").remove("weather_lon")
                .remove("weather_at").remove("weather_temp_bits").remove("weather_code")
                .remove("weather_is_day").remove("weather_days_json").remove("weather_schema")
                .apply();
    }

    static Data fetch(Context c) {
        String city = cityName(c);
        if (city.isEmpty()) return null;
        double lat = Double.longBitsToDouble(p(c).getLong("weather_lat", 0));
        double lon = Double.longBitsToDouble(p(c).getLong("weather_lon", 0));
        if (lat == 0 && lon == 0) return null;

        Data d = cached(c);
        long now = System.currentTimeMillis();
        if (d != null && now - d.fetchedAt < 30 * 60_000L) return d;

        try {
            String url = String.format(Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f"
                            + "&current=temperature_2m,weather_code,is_day"
                            + "&daily=weather_code,temperature_2m_max,temperature_2m_min"
                            + "&timezone=auto&forecast_days=3", lat, lon);
            JSONObject json = httpGet(url);
            JSONObject cur = json.getJSONObject("current");
            d = new Data();
            d.tempC = cur.getDouble("temperature_2m");
            d.code = cur.getInt("weather_code");
            d.isDay = cur.optInt("is_day", 1) == 1;
            d.fetchedAt = now;
            d.label = label(d.code, d.isDay);

            String daysJson = "[]";
            if (json.has("daily")) {
                JSONObject daily = json.getJSONObject("daily");
                JSONArray times = daily.getJSONArray("time");
                JSONArray codes = daily.getJSONArray("weather_code");
                JSONArray maxs = daily.getJSONArray("temperature_2m_max");
                JSONArray mins = daily.getJSONArray("temperature_2m_min");
                JSONArray arr = new JSONArray();
                for (int i = 0; i < times.length(); i++) {
                    JSONObject o = new JSONObject();
                    o.put("t", times.getString(i));
                    o.put("c", codes.getInt(i));
                    o.put("x", maxs.getDouble(i));
                    o.put("n", mins.getDouble(i));
                    arr.put(o);
                }
                daysJson = arr.toString();
            }

            p(c).edit()
                    .putLong("weather_temp_bits", Double.doubleToLongBits(d.tempC))
                    .putInt("weather_code", d.code)
                    .putBoolean("weather_is_day", d.isDay)
                    .putLong("weather_at", d.fetchedAt)
                    .putString("weather_days_json", daysJson)
                    .putInt("weather_schema", SCHEMA)
                    .apply();
            return d;
        } catch (Exception e) {
            Log.e(TAG, "fetch failed", e);
            return d;
        }
    }

    static List<Day> fetchDaily(Context c) {
        List<Day> out = new ArrayList<>();
        String json = p(c).getString("weather_days_json", "");
        if (json.isEmpty()) return out;
        try {
            JSONArray arr = new JSONArray(json);
            SimpleDateFormat parse = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Day d = new Day();
                Date dt = parse.parse(o.getString("t"));
                d.dateMs = dt.getTime();
                d.code = o.getInt("c");
                d.maxC = o.getDouble("x");
                d.minC = o.getDouble("n");
                d.label = label(d.code, true);
                out.add(d);
            }
        } catch (Exception ignored) {}
        return out;
    }

    static Data cached(Context c) {
        if (p(c).getInt("weather_schema", 0) < SCHEMA) return null;
        long at = p(c).getLong("weather_at", 0);
        if (at == 0) return null;
        Data d = new Data();
        d.tempC = Double.longBitsToDouble(p(c).getLong("weather_temp_bits", 0));
        d.code = p(c).getInt("weather_code", 0);
        d.isDay = p(c).getBoolean("weather_is_day", true);
        d.fetchedAt = at;
        d.label = label(d.code, d.isDay);
        return d;
    }

    static String iconKey(int code, boolean isDay) {
        if (code == 0) return "clear";
        if (code == 1 || code == 2) return "partly_cloudy";
        if (code == 3) return "cloudy";
        if (code == 45 || code == 48) return "fog";
        if (code >= 51 && code <= 57) return "drizzle";
        if (code >= 61 && code <= 67) return "rain";
        if (code >= 71 && code <= 77) return "snow";
        if (code >= 80 && code <= 82) return "showers";
        if (code >= 85 && code <= 86) return "snow";
        if (code >= 95 && code <= 96) return "thunderstorm_hail";
        if (code >= 95) return "thunderstorm";
        return "clear";
    }

    /** Emoji da condição — sempre retorna um char válido. */
    static String emoji(int code, boolean isDay) {
        if (code == 0) return isDay ? "\u2600\uFE0F" : "\uD83C\uDF19";
        if (code == 1 || code == 2) return isDay ? "\u26C5" : "\u2601\uFE0F";
        if (code == 3) return "\u2601\uFE0F";
        if (code == 45 || code == 48) return "\uD83C\uDF2B";
        if (code >= 51 && code <= 57) return isDay ? "\uD83C\uDF26" : "\uD83C\uDF27";
        if (code >= 61 && code <= 67) return isDay ? "\uD83C\uDF26" : "\uD83C\uDF27";
        if (code >= 71 && code <= 77) return "\uD83C\uDF28";
        if (code >= 80 && code <= 82) return "\uD83C\uDF27";
        if (code >= 85 && code <= 86) return "\u2744\uFE0F";
        if (code >= 95) return "\u26C8";
        return "\uD83C\uDF21";
    }

    static String label(int code, boolean isDay) {
        String key = iconKey(code, isDay);
        switch (key) {
            case "clear":             return isDay ? "Ensolarado" : "C\u00e9u limpo";
            case "partly_cloudy":     return "Parcialmente Nublado";
            case "cloudy":            return "Nublado";
            case "fog":               return "Nevoeiro";
            case "drizzle":           return "Chuva Leve";
            case "rain":              return "Chuva";
            case "freezing_rain":     return "Chuva Congelada";
            case "snow":              return "Neve";
            case "thunderstorm":      return "Tempestade com Raios";
            case "thunderstorm_hail": return "Granizo e Tempestade";
            case "showers":           return "Pancadas de Chuva";
            case "windy":             return "Vento Forte";
            default:                  return "\u2014";
        }
    }

    private static JSONObject httpGet(String urlStr) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
        c.setConnectTimeout(10000);
        c.setReadTimeout(10000);
        c.setRequestMethod("GET");
        try (InputStream in = c.getInputStream();
             Scanner s = new Scanner(in).useDelimiter("\\A")) {
            String body = s.hasNext() ? s.next() : "";
            return new JSONObject(body);
        } finally {
            c.disconnect();
        }
    }
}
