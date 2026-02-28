const API_BASE = (() => {
  const meta = document
    .querySelector('meta[name="api-base"]')
    ?.getAttribute("content")
    ?.trim();
  if (meta) return meta.replace(/\/$/, "");

  const queryBase = new URLSearchParams(location.search).get("apiBase");
  if (queryBase) return queryBase.replace(/\/$/, "");

  const isLocal =
    location.hostname === "localhost" || location.hostname === "127.0.0.1";
  if (isLocal) {
    return `${location.protocol}//${location.hostname}:8080/api/game`;
  }

  return `${location.origin}/api/game`;
})();

const screens = {
  title: document.getElementById("title-screen"),
  map: document.getElementById("map-screen"),
  shop: document.getElementById("shop-screen"),
  battle: document.getElementById("battle-screen"),
};

const mapStatsEl = document.getElementById("mapStats");
const mapGridEl = document.getElementById("mapGrid");
const mapRecoverGridEl = document.getElementById("mapRecoverGrid");
const recoverPanelEl = document.getElementById("recoverPanel");
const recoverToggleBtnEl = document.getElementById("recoverToggleBtn");
const recoverContentEl = document.getElementById("recoverContent");
const shopGoldEl = document.getElementById("shopGold");
const shopGridEl = document.getElementById("shopGrid");

const playerCardEl = document.getElementById("playerCard");
const enemyCardEl = document.getElementById("enemyCard");
const playerFxEl = document.getElementById("playerFx");
const enemyFxEl = document.getElementById("enemyFx");
const playerSpriteEl = document.getElementById("playerSprite");
const enemySpriteEl = document.getElementById("enemySprite");
const playerNameEl = document.getElementById("playerName");
const playerLevelEl = document.getElementById("playerLevel");
const enemyNameEl = document.getElementById("enemyName");
const enemyLevelEl = document.getElementById("enemyLevel");

const playerHpValEl = document.getElementById("playerHpVal");
const playerMpValEl = document.getElementById("playerMpVal");
const playerExpValEl = document.getElementById("playerExpVal");
const enemyHpValEl = document.getElementById("enemyHpVal");
const playerHpBarEl = document.getElementById("playerHpBar");
const playerMpBarEl = document.getElementById("playerMpBar");
const playerExpBarEl = document.getElementById("playerExpBar");
const enemyHpBarEl = document.getElementById("enemyHpBar");

const battleLogEl = document.getElementById("battleLog");
const commandHintEl = document.getElementById("commandHint");
const mainMenuEl = document.getElementById("mainMenu");
const magicMenuEl = document.getElementById("magicMenu");
const itemMenuEl = document.getElementById("itemMenu");
const battleResultMenuEl = document.getElementById("battleResultMenu");

let previousState = null;
let mapRecoverOpen = false;
let pendingPlayerHitTimer = null;

document
  .getElementById("beginBtn")
  .addEventListener("click", () => safeCall("/begin"));
document
  .getElementById("openShopBtn")
  .addEventListener("click", () => safeCall("/shop/open"));
document
  .getElementById("closeShopBtn")
  .addEventListener("click", () => safeCall("/shop/close"));
recoverToggleBtnEl.addEventListener("click", () => {
  mapRecoverOpen = !mapRecoverOpen;
  recoverContentEl.classList.toggle("hidden", !mapRecoverOpen);
  recoverPanelEl.classList.toggle("expanded", mapRecoverOpen);
  recoverToggleBtnEl.textContent = mapRecoverOpen
    ? "💖 閉じる"
    : "💖 回復を開く";
});

async function fetchState(path) {
  const response = await fetch(`${API_BASE}${path}`);
  if (!response.ok) {
    throw new Error(`API ${response.status}`);
  }
  return response.json();
}

async function safeCall(path) {
  try {
    const state = await fetchState(path);
    render(state);
  } catch (error) {
    console.error(error);
  }
}

function render(state) {
  showScreen(state.screen);
  renderMap(state);
  renderShop(state);
  renderBattle(state);
  previousState = state;
}

function showScreen(screenName) {
  Object.entries(screens).forEach(([name, el]) => {
    if (name === screenName) {
      el.classList.add("active");
    } else {
      el.classList.remove("active");
    }
  });
}

function renderMap(state) {
  const player = state.player;
  mapStatsEl.innerHTML = `
    <span>Lv.${player.level}</span>
    <span>HP ${player.hp}/${player.maxHp}</span>
    <span class="gold">💰 ${player.gold}G</span>
  `;

  mapGridEl.innerHTML = "";
  state.map.nodes.forEach((node) => {
    const div = document.createElement("button");
    div.type = "button";
    div.className = `map-node${node.locked ? " locked" : ""}${node.completed ? " completed" : ""}${node.boss ? " boss" : ""}`;
    div.disabled = node.locked;
    div.innerHTML = `
      <span class="icon">${node.icon}</span>
      <span class="name">${node.name}</span>
      <span class="diff">${node.diff}</span>
    `;
    div.addEventListener("click", () => {
      safeCall(`/map/enter?node=${encodeURIComponent(node.id)}`);
    });
    mapGridEl.appendChild(div);
  });

  renderMapRecover(state);
}

function renderMapRecover(state) {
  const recoverable = [
    { id: "potion", label: "🧪 ポーション", desc: "HP +60回復" },
    { id: "hiPotion", label: "🧴 ハイポーション", desc: "HP +150回復" },
    { id: "ether", label: "💧 エーテル", desc: "MP +30回復" },
    { id: "elixir", label: "🌟 エリクサー", desc: "HP/MP全回復" },
    { id: "antidote", label: "🌿 どくけし草", desc: "毒を治す" },
  ];

  mapRecoverGridEl.innerHTML = "";
  recoverable.forEach((entry) => {
    const count = state.player.items[entry.id] || 0;
    const canRecover = canUseRecoveryItem(entry.id, state.player);
    const btn = createButton(
      `${entry.label} x${count} - ${entry.desc}`,
      "btn recover-item-btn",
      () => {
        safeCall(`/map/recover?item=${encodeURIComponent(entry.id)}`);
      },
    );
    btn.disabled = count <= 0 || !canRecover;
    mapRecoverGridEl.appendChild(btn);
  });
}

function canUseRecoveryItem(itemId, player) {
  if (itemId === "potion" || itemId === "hiPotion") {
    return player.hp < player.maxHp;
  }
  if (itemId === "ether") {
    return player.mp < player.maxMp;
  }
  if (itemId === "elixir") {
    return player.hp < player.maxHp || player.mp < player.maxMp;
  }
  if (itemId === "antidote") {
    return player.poisoned;
  }
  return false;
}

function renderShop(state) {
  shopGoldEl.textContent = `💰 所持金: ${state.player.gold}G`;
  shopGridEl.innerHTML = "";

  state.shop.items.forEach((item) => {
    const div = document.createElement("article");
    div.className = "shop-item";
    div.innerHTML = `
      <div class="name">${item.icon} ${item.name} (${item.owned}/${item.max})</div>
      <div class="desc">${item.desc}</div>
      <div class="cost">💰 ${item.cost}G</div>
    `;

    const button = document.createElement("button");
    button.className = "btn btn-action";
    button.textContent = "購入する";
    button.disabled = !item.canBuy;
    button.addEventListener("click", () => {
      safeCall(`/shop/buy?item=${encodeURIComponent(item.id)}`);
    });

    div.appendChild(button);
    shopGridEl.appendChild(div);
  });
}

function renderBattle(state) {
  const player = state.player;
  const battle = state.battle;
  const enemy = battle.enemy;

  playerNameEl.textContent = player.name;
  playerLevelEl.textContent = `Lv.${player.level}`;
  playerSpriteEl.textContent = player.icon;
  playerHpValEl.textContent = `${player.hp}/${player.maxHp}`;
  playerMpValEl.textContent = `${player.mp}/${player.maxMp}`;
  playerExpValEl.textContent = `${player.exp}/${player.expNext}`;
  playerHpBarEl.style.width = `${rate(player.hp, player.maxHp)}%`;
  playerMpBarEl.style.width = `${rate(player.mp, player.maxMp)}%`;
  playerExpBarEl.style.width = `${rate(player.exp, player.expNext)}%`;

  if (enemy) {
    enemyNameEl.textContent = enemy.name;
    enemyLevelEl.textContent = `Lv.${enemy.level}`;
    enemySpriteEl.textContent = enemy.icon;
    enemyHpValEl.textContent = `${Math.max(0, enemy.hp)}/${enemy.maxHp}`;
    enemyHpBarEl.style.width = `${rate(Math.max(0, enemy.hp), enemy.maxHp)}%`;
  } else {
    enemyNameEl.textContent = "-";
    enemyLevelEl.textContent = "Lv.-";
    enemySpriteEl.textContent = "👾";
    enemyHpValEl.textContent = "0/0";
    enemyHpBarEl.style.width = "0%";
  }

  renderLogs(battle.logs || []);
  renderCommandMenus(state);

  if (previousState && previousState.screen === "battle") {
    const prevEnemyHp = previousState.battle?.enemy?.hp ?? 0;
    const currEnemyHp = enemy?.hp ?? 0;
    const pDiff = player.hp - (previousState.player?.hp ?? player.hp);
    const eDiff = currEnemyHp - prevEnemyHp;
    runBattleEffects(pDiff, eDiff, !!enemy);
  }
}

function runBattleEffects(playerHpDiff, enemyHpDiff, hasEnemy) {
  // Cancel previous delayed enemy-hit animation before scheduling a new one.
  if (pendingPlayerHitTimer !== null) {
    clearTimeout(pendingPlayerHitTimer);
    pendingPlayerHitTimer = null;
  }

  if (enemyHpDiff < 0 && hasEnemy) {
    // Player's attack result: show enemy hit immediately.
    playEffect(
      enemyCardEl,
      enemySpriteEl,
      enemyFxEl,
      Math.abs(enemyHpDiff),
      "damage",
    );
  }

  if (playerHpDiff > 0) {
    playEffect(playerCardEl, playerSpriteEl, playerFxEl, playerHpDiff, "heal");
    return;
  }

  if (playerHpDiff < 0) {
    // Enemy's counter-attack: show only player's damaged effect after a short delay.
    pendingPlayerHitTimer = setTimeout(() => {
      playEffect(
        playerCardEl,
        playerSpriteEl,
        playerFxEl,
        Math.abs(playerHpDiff),
        "damage",
      );
      pendingPlayerHitTimer = null;
    }, 420);
  }
}

function renderLogs(lines) {
  battleLogEl.innerHTML = "";
  lines.forEach((line) => {
    const p = document.createElement("p");
    p.className = `log-line ${classifyLog(line)}`;
    p.textContent = line;
    battleLogEl.appendChild(p);
  });
  battleLogEl.scrollTop = battleLogEl.scrollHeight;
}

function classifyLog(text) {
  if (
    /回復|ヒール|ポーション|エーテル|エリクサー|heal|recovers|cure/i.test(text)
  )
    return "heal";
  if (/ダメージ|攻撃|毒|damage|attack|poison/i.test(text)) return "damage";
  if (
    /倒した|敗北|逃げ|レベルアップ|EXP|GOLD|魔王|victory|defeated|level up|escaped/i.test(
      text,
    )
  )
    return "end";
  return "system";
}

function renderCommandMenus(state) {
  const battle = state.battle;
  const isBattleScreen = state.screen === "battle";
  const ongoing = battle.status === "ONGOING";

  mainMenuEl.classList.add("hidden");
  magicMenuEl.classList.add("hidden");
  itemMenuEl.classList.add("hidden");
  battleResultMenuEl.classList.add("hidden");

  if (!isBattleScreen) return;

  if (!ongoing) {
    commandHintEl.textContent =
      battle.status === "LOST" ? "あなたは倒れた..." : "戦闘終了";
    battleResultMenuEl.classList.remove("hidden");
    battleResultMenuEl.innerHTML = "";

    const label = battle.status === "LOST" ? "タイトルへ戻る" : "地図へ戻る";
    const btn = createButton(label, "btn btn-primary", () =>
      safeCall("/battle/continue"),
    );
    battleResultMenuEl.appendChild(btn);
    return;
  }

  const menu = battle.menu || "main";

  if (menu === "magic") {
    commandHintEl.textContent = "まほうを選んでください";
    renderMagicMenu(battle.spells || []);
    return;
  }

  if (menu === "item") {
    commandHintEl.textContent = "アイテムを選んでください";
    renderItemMenu(battle.items || []);
    return;
  }

  commandHintEl.textContent = "行動を選択してください";
  renderMainMenu();
}

function renderMainMenu() {
  mainMenuEl.classList.remove("hidden");
  mainMenuEl.innerHTML = "";

  mainMenuEl.appendChild(
    createButton("⚔ たたかう", "btn", () => safeCall("/action?type=attack")),
  );
  mainMenuEl.appendChild(
    createButton("✨ まほう", "btn", () => safeCall("/menu?view=magic")),
  );
  mainMenuEl.appendChild(
    createButton("🎒 アイテム", "btn", () => safeCall("/menu?view=item")),
  );
  mainMenuEl.appendChild(
    createButton("💨 にげる", "btn btn-danger", () =>
      safeCall("/action?type=run"),
    ),
  );
}

function renderMagicMenu(spells) {
  magicMenuEl.classList.remove("hidden");
  magicMenuEl.innerHTML = "";

  spells.forEach((spell) => {
    const btn = createButton(
      `${spell.icon} ${spell.name} (MP ${spell.mp})`,
      "btn",
      () => {
        safeCall(`/action?type=magic&arg=${encodeURIComponent(spell.id)}`);
      },
    );
    btn.disabled = !spell.usable;
    magicMenuEl.appendChild(btn);
  });

  magicMenuEl.appendChild(
    createButton("← もどる", "btn btn-action", () =>
      safeCall("/menu?view=main"),
    ),
  );
}

function renderItemMenu(items) {
  itemMenuEl.classList.remove("hidden");
  itemMenuEl.innerHTML = "";

  items.forEach((item) => {
    const btn = createButton(
      `${item.icon} ${item.name} x${item.count}`,
      "btn",
      () => {
        safeCall(`/action?type=item&arg=${encodeURIComponent(item.id)}`);
      },
    );
    btn.disabled = item.count <= 0;
    itemMenuEl.appendChild(btn);
  });

  itemMenuEl.appendChild(
    createButton("← もどる", "btn btn-action", () =>
      safeCall("/menu?view=main"),
    ),
  );
}

function createButton(label, className, onClick) {
  const button = document.createElement("button");
  button.type = "button";
  button.className = className;
  button.textContent = label;
  button.addEventListener("click", onClick);
  return button;
}

function playEffect(cardEl, spriteEl, fxEl, value, type) {
  cardEl.classList.remove("hit");
  spriteEl.classList.remove("shake");
  cardEl.offsetWidth;
  spriteEl.offsetWidth;

  cardEl.classList.add("hit");
  if (type === "damage") {
    spriteEl.classList.add("shake");
  }

  const pop = document.createElement("span");
  pop.className = `damage-pop ${type}`;
  pop.textContent = type === "heal" ? `+${value}` : `-${value}`;
  fxEl.appendChild(pop);

  setTimeout(() => {
    cardEl.classList.remove("hit");
    spriteEl.classList.remove("shake");
  }, 320);

  setTimeout(() => pop.remove(), 700);
}

function rate(current, max) {
  if (!max || max <= 0) return 0;
  return Math.max(0, Math.min(100, (current / max) * 100));
}

safeCall("/state");




