package com.grf.helper;

public class TokenManager {

    private static TokenManager instance;
    private String token;

    private TokenManager() {}

    public static TokenManager getInstance() {
        try {
            if (instance == null) instance = new TokenManager();
            return instance;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setToken(String token) { try { this.token = token; } catch (Exception e) { e.printStackTrace(); } }

    public String getToken() { try { return token; } catch (Exception e) { e.printStackTrace(); return null; } }

    public boolean isLoggedIn() { try { return token != null && !token.isEmpty(); } catch (Exception e) { e.printStackTrace(); return false; } }

    public void clear() { try { token = null; } catch (Exception e) { e.printStackTrace(); } }
}