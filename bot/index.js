import { readFileSync } from "node:fs";
import TelegramBot from "node-telegram-bot-api";

try {
  for (const line of readFileSync(new URL("./.env", import.meta.url), "utf8").split("\n")) {
    const m = line.match(/^([^#=]+)=(.*)$/);
    if (m && !process.env[m[1].trim()]) process.env[m[1].trim()] = m[2].trim();
  }
} catch { /* .env optional when vars are already set */ }

const token = process.env.TELEGRAM_BOT_TOKEN;
const webAppUrl = process.env.WEBAPP_URL || "https://daniel88801.github.io/axis-coach/";

if (!token) {
  console.error("TELEGRAM_BOT_TOKEN is missing");
  process.exit(1);
}

const bot = new TelegramBot(token, { polling: true });

const keyboard = {
  keyboard: [[{ text: "Открыть AXIS", web_app: { url: webAppUrl } }]],
  resize_keyboard: true,
  is_persistent: true,
};

const inline = {
  inline_keyboard: [[{ text: "Открыть AXIS", web_app: { url: webAppUrl } }]],
};

function welcomeText(name) {
  return [
    `Привет, ${name}!`,
    "",
    "Я AXIS — твой тренер техники в Telegram.",
    "Смотрю присед, отжимания и планку через камеру. Считаю повторы, ловлю ошибки и подсказываю голосом.",
    "",
    "Камера остаётся на телефоне. В облако ничего не уходит.",
    "",
    "Нажми «Открыть AXIS», чтобы начать сет.",
  ].join("\n");
}

async function greet(chatId, name) {
  await bot.sendMessage(chatId, welcomeText(name), { reply_markup: keyboard });
  await bot.sendMessage(chatId, "Можно открыть мини-приложение прямо отсюда:", {
    reply_markup: inline,
  });
}

bot.onText(/\/start\b/, async (msg) => {
  await greet(msg.chat.id, msg.from?.first_name || "друг");
});

bot.onText(/\/app\b|\/open\b/, async (msg) => {
  await bot.sendMessage(msg.chat.id, "Открываю AXIS. Удачной тренировки!", {
    reply_markup: inline,
  });
});

bot.onText(/\/help\b|\/помощь\b/, async (msg) => {
  await bot.sendMessage(
    msg.chat.id,
    [
      "Как пользоваться AXIS:",
      "",
      "1. Нажми «Открыть AXIS»",
      "2. Выбери упражнение: присед, отжимания или планка",
      "3. Поставь телефон в 2–3 метрах, встань боком, всё тело в кадре",
      "4. Сделай сет — я посчитаю повторы и подскажу по технике",
      "",
      "Команды:",
      "/start — приветствие",
      "/app — открыть мини-приложение",
      "/help — эта подсказка",
    ].join("\n"),
    { reply_markup: keyboard },
  );
});

bot.on("message", async (msg) => {
  if (!msg.text || msg.text.startsWith("/")) return;
  if (msg.web_app_data) return;
  const text = msg.text.toLowerCase();
  const name = msg.from?.first_name || "друг";
  if (/(привет|здравств|добр|хай|hello|hi)/i.test(text)) {
    await greet(msg.chat.id, name);
    return;
  }
  await bot.sendMessage(
    msg.chat.id,
    `${name}, я рядом. Нажми «Открыть AXIS», и начнём тренировку.`,
    { reply_markup: inline },
  );
});

bot.on("polling_error", (err) => {
  console.error("polling", err.message);
});

console.log("AXIS бот слушает. Mini App:", webAppUrl);
