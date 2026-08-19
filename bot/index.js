import { execFile } from "node:child_process";
import { mkdirSync, readFileSync, renameSync, writeFileSync } from "node:fs";
import { dirname } from "node:path";
import { fileURLToPath } from "node:url";
import { promisify } from "node:util";
import TelegramBot from "node-telegram-bot-api";

const execFileAsync = promisify(execFile);
const GH_REPO = "daniel88801/axis-coach";
const GH_LEADERS_PATH = "docs/leaders.json";

try {
  for (const line of readFileSync(new URL("./.env", import.meta.url), "utf8").split("\n")) {
    const m = line.match(/^([^#=]+)=(.*)$/);
    if (m && !process.env[m[1].trim()]) process.env[m[1].trim()] = m[2].trim();
  }
} catch { /* .env optional when vars are already set */ }

const token = process.env.TELEGRAM_BOT_TOKEN;
const webAppUrl = process.env.WEBAPP_URL || "https://daniel88801.github.io/axis-coach/";
const DATA_FILE = fileURLToPath(new URL("./data/leaderboard.json", import.meta.url));
const FIGHT_FILE = fileURLToPath(new URL("./data/fights.json", import.meta.url));
const PUBLIC_FILE = fileURLToPath(new URL("../docs/leaders.json", import.meta.url));
const TOP_N = 10;
const MAX_REPS_PER_SET = 500;
const FIGHT_TTL_MS = 24 * 60 * 60 * 1000;
let botUsername = "AXIStg01_bot";

if (!token) {
  console.error("TELEGRAM_BOT_TOKEN is missing");
  process.exit(1);
}

process.on("unhandledRejection", (err) => {
  console.error("unhandled", err?.message || err);
});

const bot = new TelegramBot(token, {
  polling: { autoStart: true, params: { timeout: 10 } },
  request: { timeout: 20000 },
});

function nickOf(u) {
  if (u.username) return `@${String(u.username).replace(/^@/, "")}`;
  return u.name || "Атлет";
}

function publicSnapshot(board = loadBoard()) {
  return {
    v: 1,
    ts: Date.now(),
    users: ranked(board).map((u) => ({
      id: u.id,
      name: nickOf(u),
      nick: nickOf(u),
      username: u.username || "",
      total: u.total,
      best: u.best,
      sets: u.sets,
    })),
  };
}

function appLink() {
  return String(webAppUrl).split("#")[0].split("?")[0].replace(/\/?$/, "/");
}

function keyboard() {
  return {
    keyboard: [
      [{ text: "Открыть AXIS", web_app: { url: appLink() } }],
      [{ text: "Таблица лидеров" }, { text: "Поединок", request_user: { request_id: 1, user_is_bot: false } }],
    ],
    resize_keyboard: true,
    is_persistent: true,
  };
}

function inline() {
  return {
    inline_keyboard: [
      [{ text: "Открыть AXIS", web_app: { url: appLink() } }],
      [{ text: "Таблица лидеров", callback_data: "leaders" }, { text: "Поединок", callback_data: "fight" }],
    ],
  };
}

function publishMenu() {
  bot.setChatMenuButton({
    menu_button: { type: "web_app", text: "AXIS", web_app: { url: appLink() } },
  }).catch((err) => console.error("menu", err.message));
}

function welcomeText(name) {
  return [
    `Привет, ${name}!`,
    "",
    "Я AXIS — твой тренер техники в Telegram.",
    "Смотрю присед, отжимания и планку через камеру. Считаю повторы, ловлю ошибки и подсказываю голосом.",
    "",
    "Камера остаётся на телефоне. В облако ничего не уходит.",
    "",
    "Закрой сет отжиманий — и попадёшь в таблицу лидеров.",
    "Хочешь сразиться с другом? Нажми «Поединок» и выбери соперника.",
    "Нажми «Открыть AXIS», чтобы начать сет.",
  ].join("\n");
}

async function greet(chatId, name) {
  await bot.sendMessage(chatId, welcomeText(name), { reply_markup: keyboard() });
  await bot.sendMessage(chatId, "Можно открыть мини-приложение прямо отсюда:", {
    reply_markup: inline(),
  });
}

function loadBoard() {
  try {
    const raw = JSON.parse(readFileSync(DATA_FILE, "utf8"));
    return raw && typeof raw === "object" && raw.users && typeof raw.users === "object"
      ? raw
      : { users: {} };
  } catch {
    return { users: {} };
  }
}

function saveBoard(board) {
  mkdirSync(dirname(DATA_FILE), { recursive: true });
  const tmp = `${DATA_FILE}.${process.pid}.tmp`;
  writeFileSync(tmp, JSON.stringify(board, null, 2));
  renameSync(tmp, DATA_FILE);
  try {
    mkdirSync(dirname(PUBLIC_FILE), { recursive: true });
    const pub = `${PUBLIC_FILE}.${process.pid}.tmp`;
    writeFileSync(pub, JSON.stringify(publicSnapshot(board)));
    renameSync(pub, PUBLIC_FILE);
  } catch (err) {
    console.error("leaders.json", err.message);
  }
  publishLeadersRemote(board);
}

async function publishLeadersRemote(board) {
  const snapshot = JSON.stringify(publicSnapshot(board));
  const payloadPath = fileURLToPath(new URL("./data/gh-leaders-put.json", import.meta.url));
  try {
    mkdirSync(dirname(payloadPath), { recursive: true });
    let sha;
    try {
      const { stdout } = await execFileAsync("gh", ["api", `repos/${GH_REPO}/contents/${GH_LEADERS_PATH}`], {
        timeout: 20000,
        windowsHide: true,
      });
      sha = JSON.parse(stdout).sha;
    } catch { /* file may be missing */ }
    const payload = {
      message: "Обновить таблицу лидеров",
      content: Buffer.from(snapshot).toString("base64"),
    };
    if (sha) payload.sha = sha;
    writeFileSync(payloadPath, JSON.stringify(payload));
    await execFileAsync(
      "gh",
      ["api", "-X", "PUT", `repos/${GH_REPO}/contents/${GH_LEADERS_PATH}`, "--input", payloadPath],
      { timeout: 25000, windowsHide: true },
    );
  } catch (err) {
    console.error("publish leaders", err.stderr || err.message);
  }
}

function displayName(user) {
  const name = [user.first_name, user.last_name].filter(Boolean).join(" ").replace(/\s+/g, " ").trim();
  return (name || "Атлет").slice(0, 40);
}

function recordPushups(user, reps) {
  const board = loadBoard();
  const id = String(user.id);
  const prev = board.users[id] || { id: user.id, total: 0, best: 0, sets: 0 };
  prev.id = user.id;
  prev.name = displayName(user);
  prev.username = user.username || "";
  prev.total += reps;
  prev.best = Math.max(prev.best, reps);
  prev.sets += 1;
  prev.updated = Date.now();
  board.users[id] = prev;
  saveBoard(board);
  publishMenu();
  return { entry: prev, board };
}

function ranked(board) {
  const byKey = new Map();
  for (const u of Object.values(board.users || {})) {
    if (!u || !(u.total > 0)) continue;
    const nick = String(u.username || u.name || "").replace(/^@/, "").toLowerCase().trim();
    const key = nick && nick !== "атлет" ? `n:${nick}` : `i:${u.id}`;
    const prev = byKey.get(key);
    if (!prev) {
      byKey.set(key, { ...u });
      continue;
    }
    prev.total = Math.max(prev.total, u.total);
    prev.best = Math.max(prev.best, u.best);
    prev.sets = Math.max(prev.sets || 0, u.sets || 0);
    if (u.username) prev.username = u.username;
  }
  return [...byKey.values()].sort(
    (a, b) => b.total - a.total || b.best - a.best || String(a.name).localeCompare(String(b.name), "ru"),
  );
}

function pushupWord(n) {
  const n10 = n % 10;
  const n100 = n % 100;
  if (n10 === 1 && n100 !== 11) return "отжимание";
  if (n10 >= 2 && n10 <= 4 && (n100 < 12 || n100 > 14)) return "отжимания";
  return "отжиманий";
}

function medal(i) {
  return ["🥇", "🥈", "🥉"][i] || `${i + 1}.`;
}

function formatBoard(board, userId) {
  const list = ranked(board);
  if (!list.length) {
    return [
      "Таблица лидеров — отжимания",
      "",
      "Пока пусто.",
      "Открой AXIS кнопкой внизу чата, закрой сет отжиманий и нажми «В таблицу лидеров».",
    ].join("\n");
  }

  const lines = ["Таблица лидеров — отжимания", ""];
  for (let i = 0; i < Math.min(TOP_N, list.length); i++) {
    const u = list[i];
    const mark = String(u.id) === String(userId) ? " ← ты" : "";
    lines.push(`${medal(i)} ${nickOf(u)} — ${u.total} (${pushupWord(u.total)})${mark}`);
  }

  const idx = list.findIndex((u) => String(u.id) === String(userId));
  lines.push("");
  if (idx === -1) {
    lines.push("Тебя ещё нет в таблице. Закрой сет отжиманий в AXIS.");
  } else {
    const me = list[idx];
    lines.push(`Твоё место: #${idx + 1} · ${me.total} ${pushupWord(me.total)} · лучший сет ${me.best}`);
  }
  return lines.join("\n");
}

async function sendLeaderboard(chatId, from) {
  await bot.sendMessage(chatId, formatBoard(loadBoard(), from?.id), { reply_markup: keyboard() });
}

function emptyFights() {
  return { items: {} };
}

function loadFights() {
  try {
    const raw = JSON.parse(readFileSync(FIGHT_FILE, "utf8"));
    return raw && raw.items && typeof raw.items === "object" ? raw : emptyFights();
  } catch {
    return emptyFights();
  }
}

function saveFights(store) {
  mkdirSync(dirname(FIGHT_FILE), { recursive: true });
  const tmp = `${FIGHT_FILE}.${process.pid}.tmp`;
  writeFileSync(tmp, JSON.stringify(store, null, 2));
  renameSync(tmp, FIGHT_FILE);
}

function newFightId() {
  return `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 6)}`;
}

function playerFromUser(user, extra = {}) {
  return {
    id: user.id,
    name: nickOf({ name: displayName(user), username: user.username }),
    username: user.username || "",
    chatId: extra.chatId || user.id,
    reps: extra.reps ?? null,
    score: extra.score ?? 0,
  };
}

function fightLink(id) {
  return `https://t.me/${botUsername}?start=fight_${id}`;
}

function findFightByUser(userId) {
  const store = loadFights();
  const now = Date.now();
  let found = null;
  for (const fight of Object.values(store.items)) {
    if (now - fight.created > FIGHT_TTL_MS) {
      fight.status = "expired";
      continue;
    }
    if (fight.status === "done" || fight.status === "expired" || fight.status === "declined") continue;
    if (String(fight.a.id) === String(userId) || String(fight.b.id) === String(userId)) {
      found = fight;
      break;
    }
  }
  saveFights(store);
  return found;
}

function getFight(id) {
  return loadFights().items[id] || null;
}

function upsertFight(fight) {
  const store = loadFights();
  store.items[fight.id] = fight;
  saveFights(store);
}

function sideOf(fight, userId) {
  if (String(fight.a.id) === String(userId)) return "a";
  if (String(fight.b.id) === String(userId)) return "b";
  return null;
}

function opponentOf(fight, userId) {
  const side = sideOf(fight, userId);
  if (side === "a") return fight.b;
  if (side === "b") return fight.a;
  return null;
}

function formatFight(fight) {
  const aReps = fight.a.reps == null ? "ждёт сет" : `${fight.a.reps}`;
  const bReps = fight.b.reps == null ? "ждёт сет" : `${fight.b.reps}`;
  return [
    `Поединок: ${fight.a.name} vs ${fight.b.name}`,
    `${fight.a.name} — ${aReps}`,
    `${fight.b.name} — ${bReps}`,
  ].join("\n");
}

function fightKeyboard(id) {
  return {
    inline_keyboard: [
      [{ text: "Открыть AXIS", web_app: { url: appLink() } }],
      [
        { text: "Принять", callback_data: `fight_ok:${id}` },
        { text: "Отклонить", callback_data: `fight_no:${id}` },
      ],
    ],
  };
}

async function promptFight(chatId) {
  await bot.sendMessage(
    chatId,
    [
      "Поединок 1 на 1 — кто сделает больше отжиманий за один сет.",
      "",
      "Нажми «Поединок» внизу и выбери друга.",
      "Или отправь: /fight @ник",
    ].join("\n"),
    { reply_markup: keyboard() },
  );
}

async function startFight(from, fromChatId, opponent) {
  if (String(from.id) === String(opponent.id)) {
    await bot.sendMessage(fromChatId, "С самим собой сражаться нельзя.", { reply_markup: keyboard() });
    return;
  }
  const existing = findFightByUser(from.id);
  if (existing) {
    await bot.sendMessage(
      fromChatId,
      `У тебя уже есть поединок.\n\n${formatFight(existing)}\n\nОтмена: /cancel`,
      { reply_markup: keyboard() },
    );
    return;
  }
  const other = findFightByUser(opponent.id);
  if (other) {
    await bot.sendMessage(fromChatId, `${opponent.name} уже в другом поединке.`, { reply_markup: keyboard() });
    return;
  }

  const fight = {
    id: newFightId(),
    status: "pending",
    created: Date.now(),
    a: playerFromUser(from, { chatId: fromChatId }),
    b: {
      id: opponent.id,
      name: opponent.name,
      username: opponent.username || "",
      chatId: opponent.chatId || opponent.id,
      reps: null,
      score: 0,
    },
  };
  upsertFight(fight);

  const link = fightLink(fight.id);
  await bot.sendMessage(
    fromChatId,
    [
      `Вызов ушёл: ${fight.b.name}`,
      "Кто сделает больше отжиманий за один сет — тот победил.",
      "",
      `Если соперник не получил сообщение, перешли ссылку:\n${link}`,
    ].join("\n"),
    { reply_markup: keyboard() },
  );

  try {
    await bot.sendMessage(
      fight.b.chatId,
      [
        `${fight.a.name} вызывает тебя на поединок по отжиманиям.`,
        "Один сет. Кто больше — тот выиграл.",
      ].join("\n"),
      { reply_markup: fightKeyboard(fight.id) },
    );
  } catch {
    await bot.sendMessage(
      fromChatId,
      `Не могу написать ${fight.b.name} — пусть сначала откроет бота по ссылке:\n${link}`,
      { reply_markup: keyboard() },
    );
  }
}

async function acceptFight(user, chatId, fightId) {
  const fight = getFight(fightId);
  if (!fight || fight.status === "expired" || Date.now() - fight.created > FIGHT_TTL_MS) {
    await bot.sendMessage(chatId, "Этот поединок уже недействителен. Начни новый: «Поединок».", { reply_markup: keyboard() });
    return;
  }
  if (fight.status === "done" || fight.status === "declined") {
    await bot.sendMessage(chatId, "Этот поединок уже закончен.", { reply_markup: keyboard() });
    return;
  }
  if (String(fight.a.id) === String(user.id)) {
    await bot.sendMessage(chatId, "Ждём соперника. Ссылку можно переслать ещё раз.", { reply_markup: keyboard() });
    return;
  }
  if (String(fight.b.id) !== String(user.id) && fight.status === "pending") {
    const taken = findFightByUser(user.id);
    if (taken && taken.id !== fight.id) {
      await bot.sendMessage(chatId, "Сначала закончи или отмени свой поединок: /cancel", { reply_markup: keyboard() });
      return;
    }
    fight.b = playerFromUser(user, { chatId });
  }
  if (String(fight.b.id) !== String(user.id)) {
    await bot.sendMessage(chatId, "Это не твой поединок.", { reply_markup: keyboard() });
    return;
  }

  fight.status = "active";
  fight.b.chatId = chatId;
  fight.b.name = nickOf({ name: displayName(user), username: user.username });
  fight.b.username = user.username || fight.b.username;
  upsertFight(fight);

  const go = [
    "Поединок начался!",
    formatFight(fight),
    "",
    "Открой AXIS, сделай сет отжиманий и отправь результат боту.",
  ].join("\n");
  await bot.sendMessage(chatId, go, { reply_markup: inline() });
  try {
    await bot.sendMessage(fight.a.chatId, go, { reply_markup: inline() });
  } catch { /* challenger chat may be unavailable */ }
}

async function declineFight(user, chatId, fightId) {
  const fight = getFight(fightId);
  if (!fight || fight.status === "done") {
    await bot.sendMessage(chatId, "Поединок уже не активен.", { reply_markup: keyboard() });
    return;
  }
  if (String(fight.b.id) !== String(user.id) && String(fight.a.id) !== String(user.id)) {
    await bot.sendMessage(chatId, "Это не твой поединок.", { reply_markup: keyboard() });
    return;
  }
  fight.status = "declined";
  upsertFight(fight);
  await bot.sendMessage(chatId, "Поединок отменён.", { reply_markup: keyboard() });
  const other = opponentOf(fight, user.id);
  if (other) {
    try {
      await bot.sendMessage(other.chatId, `${nickOf({ name: displayName(user), username: user.username })} отменил поединок.`, { reply_markup: keyboard() });
    } catch { /* ignore */ }
  }
}

async function applyFightReps(user, chatId, reps, score) {
  const fight = findFightByUser(user.id);
  if (!fight || (fight.status !== "active" && fight.status !== "pending")) return false;
  if (fight.status === "pending") {
    await bot.sendMessage(chatId, "Соперник ещё не принял вызов. Сначала пусть нажмёт «Принять».", { reply_markup: keyboard() });
    return true;
  }
  const side = sideOf(fight, user.id);
  if (!side) return false;
  if (fight[side].reps != null) {
    await bot.sendMessage(chatId, `Твой результат в этом поединке уже записан: ${fight[side].reps}.`, { reply_markup: keyboard() });
    return true;
  }
  fight[side].reps = reps;
  fight[side].score = score;
  fight[side].chatId = chatId;
  fight[side].name = nickOf({ name: displayName(user), username: user.username });

  if (fight.a.reps != null && fight.b.reps != null) {
    fight.status = "done";
    upsertFight(fight);
    let result;
    if (fight.a.reps === fight.b.reps) {
      result = `Ничья: по ${fight.a.reps} ${pushupWord(fight.a.reps)}.`;
    } else {
      const win = fight.a.reps > fight.b.reps ? fight.a : fight.b;
      const lose = fight.a.reps > fight.b.reps ? fight.b : fight.a;
      result = `${win.name} победил: ${win.reps} против ${lose.reps}.`;
    }
    const text = ["Поединок окончен.", formatFight(fight), "", result].join("\n");
    await bot.sendMessage(chatId, text, { reply_markup: keyboard() });
    const other = opponentOf(fight, user.id);
    if (other) {
      try { await bot.sendMessage(other.chatId, text, { reply_markup: keyboard() }); } catch { /* ignore */ }
    }
    return true;
  }

  upsertFight(fight);
  const other = opponentOf(fight, user.id);
  await bot.sendMessage(
    chatId,
    `Записал ${reps} ${pushupWord(reps)} в поединок. Ждём ${other?.name || "соперника"}.`,
    { reply_markup: keyboard() },
  );
  if (other) {
    try {
      await bot.sendMessage(
        other.chatId,
        `${fight[side].name} сделал ${reps} ${pushupWord(reps)}. Твой ход — открой AXIS.`,
        { reply_markup: inline() },
      );
    } catch { /* ignore */ }
  }
  return true;
}

async function resolveSharedOpponent(from, chatId, userId) {
  let name = "соперник";
  let username = "";
  try {
    const chat = await bot.getChat(userId);
    name = nickOf({
      name: [chat.first_name, chat.last_name].filter(Boolean).join(" ") || chat.title || "соперник",
      username: chat.username,
    });
    username = chat.username || "";
  } catch { /* only id known */ }
  await startFight(from, chatId, { id: userId, name, username, chatId: userId });
}

function parseWebAppPayload(raw) {
  let data;
  try {
    data = JSON.parse(raw);
  } catch {
    return null;
  }
  if (!data || data.exercise !== "push_up") return null;
  const reps = Number(data.reps);
  if (!Number.isInteger(reps) || reps < 1 || reps > MAX_REPS_PER_SET) return null;
  return { reps, score: Number(data.score) || 0 };
}

async function handleWebAppData(msg) {
  const parsed = parseWebAppPayload(msg.web_app_data?.data || "");
  if (!parsed) {
    await bot.sendMessage(
      msg.chat.id,
      "Не смог засчитать сет. В таблицу лидеров идут только отжимания — закрой сет ещё раз из кнопки «Открыть AXIS» внизу чата.",
      { reply_markup: keyboard() },
    );
    return;
  }

  const fight = findFightByUser(msg.from.id);
  const fightSide = fight ? sideOf(fight, msg.from.id) : null;
  const alreadyInFight = !!(fightSide && fight[fightSide].reps != null);
  const inFight = await applyFightReps(msg.from, msg.chat.id, parsed.reps, parsed.score);
  if (!alreadyInFight) recordPushups(msg.from, parsed.reps);
  if (inFight) return;
  const board = loadBoard();
  const entry = board.users[String(msg.from.id)] || { total: parsed.reps, best: parsed.reps, sets: 1 };
  const head = [
    `Засчитал ${parsed.reps} ${pushupWord(parsed.reps)}.`,
    `Всего: ${entry.total} · лучший сет: ${entry.best} · сетов: ${entry.sets}`,
    "",
    formatBoard(board, msg.from.id),
  ].join("\n");
  await bot.sendMessage(msg.chat.id, head, { reply_markup: keyboard() });
}

function wantsLeaderboard(text) {
  return /^(таблица\s+лидеров|лидеры|рейтинг|топ)$/i.test(text.trim());
}

function wantsFight(text) {
  return /^(поединок|соревнование|вызов|дуэль|спарринг)$/i.test(text.trim());
}

bot.setMyCommands([
  { command: "start", description: "Приветствие" },
  { command: "app", description: "Открыть AXIS" },
  { command: "leaders", description: "Таблица лидеров — отжимания" },
  { command: "fight", description: "Поединок 1 на 1 по отжиманиям" },
  { command: "cancel", description: "Отменить поединок" },
  { command: "help", description: "Как пользоваться" },
]).catch((err) => console.error("commands", err.message));

bot.getMe().then((me) => {
  if (me?.username) botUsername = me.username;
}).catch((err) => console.error("getMe", err.message));

bot.onText(/^\/start(?:@\w+)?(?:\s+(\S+))?/, async (msg, match) => {
  const payload = match?.[1] || "";
  if (payload.startsWith("fight_")) {
    await acceptFight(msg.from, msg.chat.id, payload.slice(6));
    return;
  }
  await greet(msg.chat.id, msg.from?.first_name || "друг");
});

bot.onText(/\/app\b|\/open\b/, async (msg) => {
  await bot.sendMessage(msg.chat.id, "Открываю AXIS. Удачной тренировки!", {
    reply_markup: inline(),
  });
});

bot.onText(/\/(leaders|лидеры|top|рейтинг)\b/i, async (msg) => {
  await sendLeaderboard(msg.chat.id, msg.from);
});

bot.onText(/^\/fight(?:@\w+)?(?:\s+@?(\S+))?/i, async (msg, match) => {
  const who = match?.[1];
  if (!who) {
    await promptFight(msg.chat.id);
    return;
  }
  try {
    const chat = await bot.getChat(who.startsWith("@") ? who : `@${who}`);
    await startFight(msg.from, msg.chat.id, {
      id: chat.id,
      name: nickOf({
        name: [chat.first_name, chat.last_name].filter(Boolean).join(" ") || chat.username || "соперник",
        username: chat.username,
      }),
      username: chat.username || "",
      chatId: chat.id,
    });
  } catch {
    await bot.sendMessage(
      msg.chat.id,
      `Не нашёл ${who}. Нажми «Поединок» и выбери человека из списка.`,
      { reply_markup: keyboard() },
    );
  }
});

bot.onText(/\/cancel\b/i, async (msg) => {
  const fight = findFightByUser(msg.from.id);
  if (!fight) {
    await bot.sendMessage(msg.chat.id, "Активного поединка нет.", { reply_markup: keyboard() });
    return;
  }
  await declineFight(msg.from, msg.chat.id, fight.id);
});

bot.onText(/\/help\b|\/помощь\b/, async (msg) => {
  await bot.sendMessage(
    msg.chat.id,
    [
      "Как пользоваться AXIS:",
      "",
      "1. Нажми «Открыть AXIS» внизу чата",
      "2. Выбери упражнение: присед, отжимания или планка",
      "3. Поставь телефон в 2–3 метрах, встань боком, всё тело в кадре",
      "4. Сделай сет — я посчитаю повторы и подскажу по технике",
      "5. После отжиманий нажми «Отправить боту»",
      "",
      "Поединок 1 на 1:",
      "Нажми «Поединок» и выбери друга. Оба делают один сет отжиманий — кто больше, тот победил.",
      "",
      "Команды:",
      "/start — приветствие",
      "/app — открыть мини-приложение",
      "/leaders — кто сделал больше отжиманий",
      "/fight — вызвать на поединок",
      "/cancel — отменить поединок",
      "/help — эта подсказка",
    ].join("\n"),
    { reply_markup: keyboard() },
  );
});

bot.on("callback_query", async (q) => {
  try {
    const data = q.data || "";
    if (data === "leaders") {
      await bot.answerCallbackQuery(q.id);
      await sendLeaderboard(q.message.chat.id, q.from);
      return;
    }
    if (data === "fight") {
      await bot.answerCallbackQuery(q.id);
      await promptFight(q.message.chat.id);
      return;
    }
    if (data.startsWith("fight_ok:")) {
      await bot.answerCallbackQuery(q.id);
      await acceptFight(q.from, q.message.chat.id, data.slice(9));
      return;
    }
    if (data.startsWith("fight_no:")) {
      await bot.answerCallbackQuery(q.id);
      await declineFight(q.from, q.message.chat.id, data.slice(9));
    }
  } catch (err) {
    console.error("callback", err.message);
  }
});

bot.on("message", async (msg) => {
  if (msg.web_app_data) {
    try {
      await handleWebAppData(msg);
    } catch (err) {
      console.error("web_app_data", err.message);
    }
    return;
  }
  const sharedId = msg.user_shared?.user_id || msg.users_shared?.users?.[0]?.user_id || msg.users_shared?.user_ids?.[0];
  if (sharedId) {
    try {
      await resolveSharedOpponent(msg.from, msg.chat.id, sharedId);
    } catch (err) {
      console.error("user_shared", err.message);
    }
    return;
  }
  if (!msg.text || msg.text.startsWith("/")) return;
  const text = msg.text.toLowerCase();
  const name = msg.from?.first_name || "друг";
  if (wantsLeaderboard(text)) {
    await sendLeaderboard(msg.chat.id, msg.from);
    return;
  }
  if (wantsFight(text)) {
    await promptFight(msg.chat.id);
    return;
  }
  if (/(привет|здравств|добр|хай|hello|hi)/i.test(text)) {
    await greet(msg.chat.id, name);
    return;
  }
  await bot.sendMessage(
    msg.chat.id,
    `${name}, я рядом. Нажми «Открыть AXIS», и начнём тренировку.`,
    { reply_markup: inline() },
  );
});

bot.on("polling_error", (err) => {
  console.error("polling", err.message);
});

publishMenu();
console.log("AXIS бот слушает. Mini App:", webAppUrl);
