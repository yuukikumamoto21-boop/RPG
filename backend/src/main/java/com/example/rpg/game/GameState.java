package com.example.rpg.game;

public class GameState {
    private int playerHp;
    private int maxPlayerHp;
    private int enemyHp;
    private int maxEnemyHp;
    private int potions;
    private boolean guardActive;
    private int turn;
    private GameStatus status;
    private String log;
    private String enemyName;

    public int getPlayerHp() {
        return playerHp;
    }

    public void setPlayerHp(int playerHp) {
        this.playerHp = playerHp;
    }

    public int getMaxPlayerHp() {
        return maxPlayerHp;
    }

    public void setMaxPlayerHp(int maxPlayerHp) {
        this.maxPlayerHp = maxPlayerHp;
    }

    public int getEnemyHp() {
        return enemyHp;
    }

    public void setEnemyHp(int enemyHp) {
        this.enemyHp = enemyHp;
    }

    public int getMaxEnemyHp() {
        return maxEnemyHp;
    }

    public void setMaxEnemyHp(int maxEnemyHp) {
        this.maxEnemyHp = maxEnemyHp;
    }

    public int getPotions() {
        return potions;
    }

    public void setPotions(int potions) {
        this.potions = potions;
    }

    public boolean isGuardActive() {
        return guardActive;
    }

    public void setGuardActive(boolean guardActive) {
        this.guardActive = guardActive;
    }

    public int getTurn() {
        return turn;
    }

    public void setTurn(int turn) {
        this.turn = turn;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
    }

    public String getEnemyName() {
        return enemyName;
    }

    public void setEnemyName(String enemyName) {
        this.enemyName = enemyName;
    }
}
