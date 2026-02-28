package com.example.rpg.game;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class GameService {
    private record NodeDef(String id, String name, String diff, String icon, boolean boss, int order, String[] enemyIds) {}
    private record EnemyDef(String id, String name, String icon, int level, int hp, int atk, int def, int exp, int gold) {}
    private record ShopDef(String id, String name, String desc, String icon, int cost, int max) {}
    private record SpellDef(String id, String name, String icon, int mp, String effect, double power) {}

    private enum EnemyActionType {
        BASIC,
        COUNTER_STANCE,
        CHARGE,
        CHARGED_STRIKE,
        DRAIN,
        MULTI_STRIKE
    }

    private static class Player {
        String name = "アストラ";
        String icon = "🧙";
        int level = 1;
        int hp = 120;
        int maxHp = 120;
        int mp = 50;
        int maxMp = 50;
        int atk = 22;
        int def = 10;
        int exp = 0;
        int expNext = 100;
        int gold = 100;
        int defenseBuff = 0;
        boolean poisoned = false;
        Map<String, Integer> items = new LinkedHashMap<>();
        List<String> spells = new ArrayList<>();
        List<String> completedNodes = new ArrayList<>();
    }

    private static class EnemyState {
        String id;
        String name;
        String icon;
        int level;
        int hp;
        int maxHp;
        int atk;
        int def;
        int exp;
        int gold;
        boolean counterStance;
        int chargeTurns;
    }

    private static final List<NodeDef> NODES = List.of(
            new NodeDef("grass", "始まりの草原", "易しい", "🌿", false, 0, new String[] {"slime", "wolf"}),
            new NodeDef("forest", "ささやきの森", "易しい", "🌲", false, 1, new String[] {"goblin", "wolf"}),
            new NodeDef("cave", "残響の洞窟", "普通", "🪨", false, 2, new String[] {"skeleton", "shadow"}),
            new NodeDef("ruins", "古代遺跡", "普通", "🏛", false, 3, new String[] {"golem", "skeleton"}),
            new NodeDef("volcano", "火山外縁", "難しい", "🌋", false, 4, new String[] {"salamander", "golem"}),
            new NodeDef("midboss", "深淵の門", "ボス", "⚔", true, 5, new String[] {"demonKnight"}),
            new NodeDef("tower", "闇の塔", "ボス", "🗼", true, 6, new String[] {"lichLord"}),
            new NodeDef("final", "魔王の玉座", "最終ボス", "👑", true, 7, new String[] {"demonKing"})
    );

    private static final Map<String, EnemyDef> ENEMIES = Map.of(
            "slime", new EnemyDef("slime", "スライム", "🟢", 1, 44, 14, 3, 25, 20),
            "wolf", new EnemyDef("wolf", "ダイアウルフ", "🐺", 1, 48, 16, 4, 30, 24),
            "goblin", new EnemyDef("goblin", "ゴブリン", "👺", 2, 55, 18, 5, 45, 35),
            "shadow", new EnemyDef("shadow", "シャドウ", "👤", 2, 58, 20, 5, 55, 38),
            "skeleton", new EnemyDef("skeleton", "スケルトン", "💀", 3, 66, 22, 6, 70, 52),
            "golem", new EnemyDef("golem", "ゴーレム", "🪵", 4, 84, 24, 9, 95, 65),
            "salamander", new EnemyDef("salamander", "サラマンダー", "🦎", 5, 88, 27, 9, 120, 80),
            "demonKnight", new EnemyDef("demonKnight", "魔騎士", "🛡", 6, 120, 31, 11, 180, 130),
            "lichLord", new EnemyDef("lichLord", "リッチロード", "🧛", 8, 145, 36, 13, 240, 180),
            "demonKing", new EnemyDef("demonKing", "魔王アルタナ", "😈", 10, 210, 42, 15, 420, 420)
    );

    private static final Map<String, ShopDef> SHOP_ITEMS = Map.of(
            "potion", new ShopDef("potion", "ポーション", "HP +60回復", "🧪", 50, 5),
            "hiPotion", new ShopDef("hiPotion", "ハイポーション", "HP +150回復", "🧴", 120, 3),
            "ether", new ShopDef("ether", "エーテル", "MP +30回復", "💧", 80, 3),
            "elixir", new ShopDef("elixir", "エリクサー", "HP/MP全回復", "🌟", 400, 2),
            "antidote", new ShopDef("antidote", "どくけし草", "毒を治す", "🌿", 30, 3)
    );

    private static final Map<String, SpellDef> SPELLS = Map.of(
            "fire", new SpellDef("fire", "ファイア", "🔥", 10, "damage", 1.45),
            "heal", new SpellDef("heal", "ヒール", "✨", 12, "heal", 0.35),
            "thunder", new SpellDef("thunder", "サンダー", "⚡", 18, "damage", 1.85),
            "shield", new SpellDef("shield", "シールド", "🛡", 14, "buff", 0.0)
    );

    private final Random random = new Random();

    private Player player;
    private EnemyState enemy;
    private String screen;
    private String battleStatus;
    private String battleMenu;
    private String currentNodeId;
    private int turn;
    private List<String> battleLogs;
    private String notice;
    private int worldTier;

    public GameService() {
        resetGame();
    }

    public synchronized String start() {
        resetGame();
        return currentStateJson();
    }

    public synchronized String begin() {
        if (player == null) {
            resetGame();
        }
        screen = "map";
        notice = "行き先を選んでください。";
        return currentStateJson();
    }

    public synchronized String currentStateJson() {
        return toJson(snapshot());
    }

    public synchronized String enterNode(String nodeId) {
        NodeDef node = findNode(nodeId);
        if (node == null) {
            notice = "そのエリアは存在しません。";
            return currentStateJson();
        }
        if (!isUnlocked(node)) {
            notice = "まだこのエリアには入れません。";
            return currentStateJson();
        }

        currentNodeId = node.id();
        battleMenu = "main";
        battleStatus = "ONGOING";
        turn = 1;
        battleLogs = new ArrayList<>();
        enemy = createEnemy(node);
        screen = "battle";
        addLog(enemy.name + " があらわれた。");
        if (worldTier > 0) {
            addLog("脅威階層 " + worldTier + "：すべての敵が強化されている。");
        }
        notice = "";
        return currentStateJson();
    }

    public synchronized String openShop() {
        screen = "shop";
        notice = "商店へようこそ。";
        return currentStateJson();
    }

    public synchronized String closeShop() {
        screen = "map";
        notice = "地図に戻りました。";
        return currentStateJson();
    }

    public synchronized String buyItem(String itemId) {
        ShopDef item = SHOP_ITEMS.get(itemId);
        if (item == null) {
            notice = "そのアイテムは存在しません。";
            return currentStateJson();
        }

        int owned = player.items.getOrDefault(item.id(), 0);
        if (owned >= item.max()) {
            notice = item.name + " はこれ以上持てません。";
            return currentStateJson();
        }
        if (player.gold < item.cost()) {
            notice = "所持金が足りません。";
            return currentStateJson();
        }

        player.gold -= item.cost();
        player.items.put(item.id(), owned + 1);
        notice = item.name + " を購入した。";
        return currentStateJson();
    }

    public synchronized String useMapItem(String itemId) {
        if (!"map".equals(screen)) {
            notice = "地図画面でのみ使用できます。";
            return currentStateJson();
        }

        notice = consumeItem(itemId);
        return currentStateJson();
    }

    public synchronized String setMenu(String menu) {
        if (!"battle".equals(screen) || enemy == null || !"ONGOING".equals(battleStatus)) {
            return currentStateJson();
        }
        if ("main".equals(menu) || "magic".equals(menu) || "item".equals(menu)) {
            battleMenu = menu;
        }
        return currentStateJson();
    }

    public synchronized String action(String type, String arg) {
        if (!"battle".equals(screen) || enemy == null || !"ONGOING".equals(battleStatus)) {
            notice = "戦闘中ではありません。";
            return currentStateJson();
        }

        switch (type) {
            case "attack" -> playerAttack();
            case "magic" -> playerMagic(arg);
            case "item" -> playerItem(arg);
            case "run" -> playerRun();
            default -> addLog("不明な行動です。");
        }

        return currentStateJson();
    }

    public synchronized String continueBattleResult() {
        if (!"battle".equals(screen) || "ONGOING".equals(battleStatus)) {
            return currentStateJson();
        }
        if ("LOST".equals(battleStatus)) {
            resetGame();
            notice = "あなたは倒れた……タイトルへ戻ります。";
            return currentStateJson();
        }

        screen = "map";
        enemy = null;
        battleMenu = "main";
        if (currentNodeId != null && !player.completedNodes.contains(currentNodeId)) {
            player.completedNodes.add(currentNodeId);
        }
        currentNodeId = null;
        notice = "地図へ戻った。";
        return currentStateJson();
    }

    private void playerAttack() {
        int damage = Math.max(1, player.atk - enemy.def + randomRange(-2, 8));
        if (applyPlayerDamage(damage, "⚔ " + player.name + " の攻撃！ " + enemy.name + " に ")) {
            return;
        }
        enemyTurn();
    }

    private void playerMagic(String spellId) {
        SpellDef spell = SPELLS.get(spellId);
        if (spell == null || !player.spells.contains(spell.id())) {
            addLog("その魔法は使えない。");
            return;
        }
        if (player.mp < spell.mp()) {
            addLog("MPが足りない。");
            return;
        }

        player.mp -= spell.mp();
        if ("damage".equals(spell.effect())) {
            int damage = Math.max(5, (int) Math.floor(player.atk * spell.power() - enemy.def * 0.4 + randomRange(0, 10)));
            if (applyPlayerDamage(damage, spell.icon() + " " + spell.name() + "！ " + enemy.name + " に ")) {
                return;
            }
            enemyTurn();
            return;
        }

        if ("heal".equals(spell.effect())) {
            int heal = Math.max(20, (int) Math.floor(player.maxHp * spell.power() + randomRange(0, 18)));
            int actual = Math.min(heal, player.maxHp - player.hp);
            player.hp = Math.min(player.maxHp, player.hp + heal);
            addLog(spell.icon() + " " + spell.name() + " でHPを " + actual + " 回復した。");
            enemyTurn();
            return;
        }

        if ("buff".equals(spell.effect())) {
            player.defenseBuff += 9;
            addLog(spell.icon() + " 防御の結界が勇者を包んだ。");
            enemyTurn();
        }
    }

    private void playerItem(String itemId) {
        addLog(consumeItem(itemId));
        enemyTurn();
    }

    private String consumeItem(String itemId) {
        int count = player.items.getOrDefault(itemId, 0);
        if (count <= 0) {
            return "そのアイテムは持っていない。";
        }

        return switch (itemId) {
            case "potion" -> {
                if (player.hp >= player.maxHp) {
                    yield "HPはすでに最大だ。";
                }
                player.items.put(itemId, count - 1);
                int healed = applyHeal(60);
                yield "🧪 ポーションでHPを " + healed + " 回復した。";
            }
            case "hiPotion" -> {
                if (player.hp >= player.maxHp) {
                    yield "HPはすでに最大だ。";
                }
                player.items.put(itemId, count - 1);
                int healed = applyHeal(150);
                yield "🧴 ハイポーションでHPを " + healed + " 回復した。";
            }
            case "ether" -> {
                if (player.mp >= player.maxMp) {
                    yield "MPはすでに最大だ。";
                }
                player.items.put(itemId, count - 1);
                int before = player.mp;
                player.mp = Math.min(player.maxMp, player.mp + 30);
                yield "💧 エーテルでMPを " + (player.mp - before) + " 回復した。";
            }
            case "elixir" -> {
                if (player.hp >= player.maxHp && player.mp >= player.maxMp) {
                    yield "HP/MPはすでに最大だ。";
                }
                player.items.put(itemId, count - 1);
                int hpGain = player.maxHp - player.hp;
                int mpGain = player.maxMp - player.mp;
                player.hp = player.maxHp;
                player.mp = player.maxMp;
                yield "🌟 エリクサーでHP " + hpGain + " / MP " + mpGain + " 回復した。";
            }
            case "antidote" -> {
                if (!player.poisoned) {
                    yield "毒状態ではない。";
                }
                player.items.put(itemId, count - 1);
                player.poisoned = false;
                yield "🌿 どくけし草で毒を治した。";
            }
            default -> "使えないアイテムだ。";
        };
    }

    private void playerRun() {
        boolean success = random.nextInt(100) < 45;
        if (success) {
            battleStatus = "ESCAPED";
            addLog("💨 うまく逃げ切った。");
            return;
        }
        addLog("💨 逃走に失敗した。");
        enemyTurn();
    }

    private void enemyTurn() {
        if (!"ONGOING".equals(battleStatus) || enemy == null || enemy.hp <= 0) {
            return;
        }

        EnemyActionType action = chooseEnemyAction();
        switch (action) {
            case COUNTER_STANCE -> startCounterStance();
            case CHARGE -> startChargeAttack();
            case CHARGED_STRIKE -> executeChargedStrike();
            case DRAIN -> executeDrainAttack();
            case MULTI_STRIKE -> executeMultiStrike();
            case BASIC -> executeBasicEnemyAttack();
        }

        concludeEnemyTurn();
    }

    private void onWin() {
        battleStatus = "WON";
        int gainExp = enemy.exp;
        int gainGold = enemy.gold + randomRange(0, 18);
        player.exp += gainExp;
        player.gold += gainGold;

        addLog("🎉 " + enemy.name + " を倒した。");
        addLog("報酬: EXP +" + gainExp + " / GOLD +" + gainGold);

        boolean leveled = false;
        while (player.exp >= player.expNext) {
            player.exp -= player.expNext;
            player.level += 1;
            player.expNext = (int) Math.floor(player.expNext * 1.55);
            player.maxHp = (int) Math.floor(player.maxHp * 1.14 + 10);
            player.hp = player.maxHp;
            player.maxMp = (int) Math.floor(player.maxMp * 1.12 + 6);
            player.mp = player.maxMp;
            player.atk = (int) Math.floor(player.atk * 1.11 + 3);
            player.def = (int) Math.floor(player.def * 1.10 + 2);
            unlockSpellsByLevel();
            leveled = true;
        }

        if (leveled) {
            addLog("🔺 レベルアップ！ Lv." + player.level);
        }

        if ("final".equals(currentNodeId)) {
            worldTier += 1;
            addLog("👑 魔王を討伐した。脅威階層が " + worldTier + " に上昇。");
            addLog("すべての敵がさらに強く、危険な行動を取るようになった。");
        }
    }

    private boolean applyPlayerDamage(int rawDamage, String logPrefix) {
        int damage = Math.max(1, rawDamage);
        int actualDamage = Math.min(enemy.hp, damage);
        enemy.hp = Math.max(0, enemy.hp - damage);
        addLog(logPrefix + actualDamage + " ダメージ。");

        if (enemy.hp <= 0) {
            onWin();
            return true;
        }

        return triggerFullCounter(actualDamage);
    }

    private boolean triggerFullCounter(int dealtDamage) {
        if (enemy == null || !enemy.counterStance || dealtDamage <= 0 || !hasFullCounterTrait(enemy.id)) {
            return false;
        }

        enemy.counterStance = false;
        int reflected = Math.max(2, dealtDamage * 2);
        player.hp = Math.max(0, player.hp - reflected);
        addLog(enemy.name + " のフルカウンター！ " + reflected + " ダメージを反射！");

        if (player.hp <= 0) {
            markPlayerDefeated();
            return true;
        }

        return false;
    }

    private EnemyActionType chooseEnemyAction() {
        if (enemy.chargeTurns > 0) {
            return EnemyActionType.CHARGED_STRIKE;
        }

        int hpRate = (int) Math.floor((enemy.hp * 100.0) / Math.max(1, enemy.maxHp));

        if (hasLifeDrainTrait(enemy.id) && hpRate <= 62 && random.nextInt(100) < 32) {
            return EnemyActionType.DRAIN;
        }

        if (hasFullCounterTrait(enemy.id) && !enemy.counterStance && hpRate <= 78 && random.nextInt(100) < 30) {
            return EnemyActionType.COUNTER_STANCE;
        }

        if (hasChargeTrait(enemy.id) && random.nextInt(100) < 24) {
            return EnemyActionType.CHARGE;
        }

        if (hasMultiStrikeTrait(enemy.id) && random.nextInt(100) < 30) {
            return EnemyActionType.MULTI_STRIKE;
        }

        return EnemyActionType.BASIC;
    }

    private void startCounterStance() {
        enemy.counterStance = true;
        addLog(enemy.name + " はカウンターの構えを取った。");
    }

    private void startChargeAttack() {
        enemy.chargeTurns = 1;
        addLog(enemy.name + " は強烈な一撃を溜めている。");
    }

    private void executeChargedStrike() {
        enemy.chargeTurns = 0;
        int damage = calculateEnemyDamage(1.85, randomRange(2, 9));
        applyEnemyDamage(damage, enemy.name + " の溜め攻撃！ ");
        maybeInflictPoison(18);
    }

    private void executeDrainAttack() {
        int damage = calculateEnemyDamage(1.15, randomRange(0, 6));
        int dealt = applyEnemyDamage(damage, enemy.name + " は生命吸収を放った。 ");
        if (dealt > 0) {
            int heal = Math.max(1, (int) Math.floor(dealt * 0.6));
            int before = enemy.hp;
            enemy.hp = Math.min(enemy.maxHp, enemy.hp + heal);
            addLog(enemy.name + " は " + (enemy.hp - before) + " HPを吸収した。");
        }
    }

    private void executeMultiStrike() {
        int hits = "demonKing".equals(enemy.id) ? randomRange(2, 4) : randomRange(2, 3);
        int total = 0;
        for (int i = 1; i <= hits; i++) {
            int damage = calculateEnemyDamage(0.72, randomRange(-2, 4));
            int dealt = applyEnemyDamage(damage, enemy.name + " の連撃 " + i + " 発目！ ");
            total += dealt;
            if (player.hp <= 0) {
                return;
            }
        }
        addLog(enemy.name + " の連撃合計 " + total + " ダメージ。");
        maybeInflictPoison(24);
    }

    private void executeBasicEnemyAttack() {
        int damage = calculateEnemyDamage(1.0, randomRange(-1, 7));
        applyEnemyDamage(damage, enemy.name + " の攻撃！ ");
        maybeInflictPoison(poisonChanceForEnemy());
    }

    private int calculateEnemyDamage(double atkRate, int randomOffset) {
        int defense = player.def + player.defenseBuff;
        int scaledAtk = (int) Math.floor(enemy.atk * atkRate);
        return Math.max(1, scaledAtk - defense + randomOffset);
    }

    private int applyEnemyDamage(int damage, String logPrefix) {
        int actualDamage = Math.max(0, Math.min(player.hp, damage));
        player.hp = Math.max(0, player.hp - Math.max(1, damage));
        addLog(logPrefix + actualDamage + " ダメージ。");
        if (player.hp <= 0) {
            markPlayerDefeated();
        }
        return actualDamage;
    }

    private void maybeInflictPoison(int chance) {
        if (player.hp <= 0 || player.poisoned) {
            return;
        }
        if (random.nextInt(100) < chance) {
            player.poisoned = true;
            addLog(enemy.name + " の攻撃で毒になった。");
        }
    }

    private int poisonChanceForEnemy() {
        if ("shadow".equals(enemy.id) || "salamander".equals(enemy.id) || "demonKing".equals(enemy.id)) {
            return 32;
        }
        if ("lichLord".equals(enemy.id) || "skeleton".equals(enemy.id)) {
            return 22;
        }
        return 14 + Math.min(10, worldTier * 2);
    }

    private void concludeEnemyTurn() {
        if (!"ONGOING".equals(battleStatus)) {
            return;
        }

        if (player.poisoned) {
            int poisonDamage = Math.max(2, (int) Math.floor(player.maxHp * (0.05 + worldTier * 0.01)));
            player.hp = Math.max(0, player.hp - poisonDamage);
            addLog("毒で " + poisonDamage + " ダメージ。");
            if (player.hp <= 0) {
                markPlayerDefeated();
                return;
            }
        }

        if (player.defenseBuff > 0) {
            player.defenseBuff = Math.max(0, player.defenseBuff - 3);
        }

        turn += 1;
    }

    private void markPlayerDefeated() {
        if (!"LOST".equals(battleStatus)) {
            battleStatus = "LOST";
            addLog("あなたは倒れた……");
        }
    }

    private boolean hasFullCounterTrait(String enemyId) {
        return "demonKnight".equals(enemyId) || "lichLord".equals(enemyId) || "demonKing".equals(enemyId);
    }

    private boolean hasChargeTrait(String enemyId) {
        return "golem".equals(enemyId) || "demonKnight".equals(enemyId) || "salamander".equals(enemyId) || "demonKing".equals(enemyId);
    }

    private boolean hasLifeDrainTrait(String enemyId) {
        return "lichLord".equals(enemyId) || "demonKing".equals(enemyId);
    }

    private boolean hasMultiStrikeTrait(String enemyId) {
        return "wolf".equals(enemyId) || "shadow".equals(enemyId) || "demonKing".equals(enemyId);
    }

    private int applyHeal(int amount) {
        int before = player.hp;
        player.hp = Math.min(player.maxHp, player.hp + amount);
        return player.hp - before;
    }

    private EnemyState createEnemy(NodeDef node) {
        String[] ids = node.enemyIds();
        String picked = ids[random.nextInt(ids.length)];
        EnemyDef def = ENEMIES.get(picked);

        int tier = Math.max(0, worldTier);
        double hpScale = 1.0 + tier * 0.24 + (node.boss() ? tier * 0.08 : 0.0);
        double atkScale = 1.0 + tier * 0.17 + (node.boss() ? tier * 0.05 : 0.0);
        double defScale = 1.0 + tier * 0.13 + (node.boss() ? tier * 0.05 : 0.0);

        EnemyState e = new EnemyState();
        e.id = def.id();
        e.name = def.name();
        e.icon = def.icon();
        e.level = def.level() + tier * 2 + (node.boss() ? tier : 0);
        e.maxHp = Math.max(1, (int) Math.round(def.hp() * hpScale + tier * 6));
        e.hp = e.maxHp;
        e.atk = Math.max(1, (int) Math.round(def.atk() * atkScale + tier * 2));
        e.def = Math.max(1, (int) Math.round(def.def() * defScale + tier));
        e.exp = Math.max(1, (int) Math.round(def.exp() * (1.0 + tier * 0.20)));
        e.gold = Math.max(1, (int) Math.round(def.gold() * (1.0 + tier * 0.16)));
        e.counterStance = false;
        e.chargeTurns = 0;
        return e;
    }

    private NodeDef findNode(String nodeId) {
        for (NodeDef node : NODES) {
            if (node.id().equals(nodeId)) {
                return node;
            }
        }
        return null;
    }

    private boolean isUnlocked(NodeDef node) {
        if (node.order() == 0) {
            return true;
        }
        NodeDef previous = NODES.get(node.order() - 1);
        return player.completedNodes.contains(previous.id());
    }

    private void unlockSpellsByLevel() {
        if (player.level >= 1 && !player.spells.contains("fire")) player.spells.add("fire");
        if (player.level >= 1 && !player.spells.contains("heal")) player.spells.add("heal");
        if (player.level >= 3 && !player.spells.contains("thunder")) player.spells.add("thunder");
        if (player.level >= 5 && !player.spells.contains("shield")) player.spells.add("shield");
    }

    private void addLog(String text) {
        battleLogs.add(text);
        while (battleLogs.size() > 8) {
            battleLogs.remove(0);
        }
    }

    private Map<String, Object> snapshot() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("screen", screen);
        root.put("notice", notice == null ? "" : notice);
        root.put("player", playerMap());
        root.put("map", mapMap());
        root.put("shop", shopMap());
        root.put("battle", battleMap());
        return root;
    }

    private Map<String, Object> playerMap() {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("name", player.name);
        p.put("icon", player.icon);
        p.put("level", player.level);
        p.put("hp", player.hp);
        p.put("maxHp", player.maxHp);
        p.put("mp", player.mp);
        p.put("maxMp", player.maxMp);
        p.put("atk", player.atk);
        p.put("def", player.def);
        p.put("exp", player.exp);
        p.put("expNext", player.expNext);
        p.put("gold", player.gold);
        p.put("defenseBuff", player.defenseBuff);
        p.put("poisoned", player.poisoned);
        p.put("items", new LinkedHashMap<>(player.items));
        p.put("spells", new ArrayList<>(player.spells));
        p.put("completedNodes", new ArrayList<>(player.completedNodes));
        return p;
    }

    private Map<String, Object> mapMap() {
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (NodeDef node : NODES) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", node.id());
            row.put("name", node.name());
            row.put("diff", node.diff());
            row.put("icon", node.icon());
            row.put("boss", node.boss());
            row.put("order", node.order());
            row.put("locked", !isUnlocked(node));
            row.put("completed", player.completedNodes.contains(node.id()));
            nodes.add(row);
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("nodes", nodes);
        map.put("worldTier", worldTier);
        return map;
    }

    private Map<String, Object> shopMap() {
        List<Map<String, Object>> items = new ArrayList<>();
        for (ShopDef def : List.of(
                SHOP_ITEMS.get("potion"),
                SHOP_ITEMS.get("hiPotion"),
                SHOP_ITEMS.get("ether"),
                SHOP_ITEMS.get("elixir"),
                SHOP_ITEMS.get("antidote")
        )) {
            int owned = player.items.getOrDefault(def.id(), 0);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", def.id());
            item.put("name", def.name());
            item.put("desc", def.desc());
            item.put("icon", def.icon());
            item.put("cost", def.cost());
            item.put("max", def.max());
            item.put("owned", owned);
            item.put("canBuy", player.gold >= def.cost() && owned < def.max());
            items.add(item);
        }

        Map<String, Object> shop = new LinkedHashMap<>();
        shop.put("items", items);
        return shop;
    }

    private Map<String, Object> battleMap() {
        Map<String, Object> battle = new LinkedHashMap<>();
        battle.put("menu", battleMenu);
        battle.put("status", battleStatus);
        battle.put("turn", turn);
        battle.put("currentNodeId", currentNodeId == null ? "" : currentNodeId);
        battle.put("logs", new ArrayList<>(battleLogs));
        battle.put("worldTier", worldTier);

        List<Map<String, Object>> spells = new ArrayList<>();
        for (String spellId : player.spells) {
            SpellDef spellDef = SPELLS.get(spellId);
            if (spellDef == null) continue;
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("id", spellDef.id());
            s.put("name", spellDef.name());
            s.put("icon", spellDef.icon());
            s.put("mp", spellDef.mp());
            s.put("usable", player.mp >= spellDef.mp());
            spells.add(s);
        }
        battle.put("spells", spells);

        List<Map<String, Object>> items = new ArrayList<>();
        for (String itemId : List.of("potion", "hiPotion", "ether", "elixir", "antidote")) {
            ShopDef def = SHOP_ITEMS.get(itemId);
            Map<String, Object> i = new LinkedHashMap<>();
            i.put("id", itemId);
            i.put("name", def.name());
            i.put("icon", def.icon());
            i.put("count", player.items.getOrDefault(itemId, 0));
            items.add(i);
        }
        battle.put("items", items);

        if (enemy == null) {
            battle.put("enemy", null);
        } else {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("id", enemy.id);
            e.put("name", enemy.name);
            e.put("icon", enemy.icon);
            e.put("level", enemy.level);
            e.put("hp", enemy.hp);
            e.put("maxHp", enemy.maxHp);
            e.put("counterStance", enemy.counterStance);
            e.put("chargeTurns", enemy.chargeTurns);
            battle.put("enemy", e);
        }

        return battle;
    }

    private void resetGame() {
        player = new Player();
        player.items.put("potion", 2);
        player.items.put("hiPotion", 0);
        player.items.put("ether", 1);
        player.items.put("elixir", 0);
        player.items.put("antidote", 0);

        player.spells.add("fire");
        player.spells.add("heal");

        enemy = null;
        screen = "title";
        battleStatus = "IDLE";
        battleMenu = "main";
        currentNodeId = null;
        turn = 1;
        worldTier = 0;
        battleLogs = new ArrayList<>();
        notice = "";
    }

    private int randomRange(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }

    private String toJson(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String s) {
            return "\"" + escape(s) + "\"";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append(toJson(list.get(i)));
            }
            sb.append(']');
            return sb.toString();
        }
        if (value instanceof Map<?, ?> map) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append("\"").append(escape(String.valueOf(entry.getKey()))).append("\":");
                sb.append(toJson(entry.getValue()));
            }
            sb.append('}');
            return sb.toString();
        }
        return "\"" + escape(String.valueOf(value)) + "\"";
    }

    private String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
