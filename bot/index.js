import { mkdirSync, readFileSync, renameSync, writeFileSync } from "node:fs";
import { dirname } from "node:path";
import { fileURLToPath } from "node:url";
import TelegramBot from "node-telegram-bot-api";

try {
  for (const line of readFileSync(new URL("./.env", import.meta.url), "utf8").split("\n")) {
    const m = line.match(/^([^#=]+)=(.*)$/);
    if (m && !process.env[m[1].trim()]) process.env[m[1].trim()] = m[2].trim();
  }
} catch { /* .env optional when vars are already set */ }

const token = process.env.TELEGRAM_BOT_TOKEN;
const webAppUrl = process.env.WEBAPP_URL || "https://daniel88801.github.io/axis-coach/";
const DATA_FILE = fileURLToPath(new URL("./data/leaderboard.json", import.meta.url));
const PUBLIC_FILE = fileURLToPath(new URL("../docs/leaders.json", import.meta.url));
const TOP_N = 10;
const MAX_REPS_PER_SET = 500;

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
      [{ text: "Таблица лидеров" }],
    ],
    resize_keyboard: true,
    is_persistent: true,
  };
}

function inline() {
  return {
    inline_keyboard: [
      [{ text: "Открыть AXIS", web_app: { url: appLink() } }],
      [{ text: "Таблица лидеров", callback_data: "leaders" }],
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
  return Object.values(board.users)
    .filter((u) => u.total > 0)
    .sort((a, b) => b.total - a.total || b.best - a.best || String(a.name).localeCompare(String(b.name), "ru"));
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

  const { entry } = recordPushups(msg.from, parsed.reps);
  const head = [
    `Засчитал ${parsed.reps} ${pushupWord(parsed.reps)}.`,
    `Всего: ${entry.total} · лучший сет: ${entry.best} · сетов: ${entry.sets}`,
    "",
    formatBoard(loadBoard(), msg.from.id),
  ].join("\n");
  await bot.sendMessage(msg.chat.id, head, { reply_markup: keyboard() });
}

function wantsLeaderboard(text) {
  return /^(таблица\s+лидеров|лидеры|рейтинг|топ)$/i.test(text.trim());
}

bot.setMyCommands([
  { command: "start", description: "Приветствие" },
  { command: "app", description: "Открыть AXIS" },
  { command: "leaders", description: "Таблица лидеров — отжимания" },
  { command: "help", description: "Как пользоваться" },
]).catch((err) => console.error("commands", err.message));

bot.onText(/\/start\b/, async (msg) => {
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
      "5. После отжиманий нажми «В таблицу лидеров»",
      "",
      "Команды:",
      "/start — приветствие",
      "/app — открыть мини-приложение",
      "/leaders — кто сделал больше отжиманий",
      "/help — эта подсказка",
    ].join("\n"),
    { reply_markup: keyboard() },
  );
});

bot.on("callback_query", async (q) => {
  try {
    if (q.data === "leaders") {
      await bot.answerCallbackQuery(q.id);
      await sendLeaderboard(q.message.chat.id, q.from);
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
  if (!msg.text || msg.text.startsWith("/")) return;
  const text = msg.text.toLowerCase();
  const name = msg.from?.first_name || "друг";
  if (wantsLeaderboard(text)) {
    await sendLeaderboard(msg.chat.id, msg.from);
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
