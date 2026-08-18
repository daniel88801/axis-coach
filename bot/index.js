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
  keyboard: [[{ text: "Open AXIS", web_app: { url: webAppUrl } }]],
  resize_keyboard: true,
};

const inline = {
  inline_keyboard: [[{ text: "Open AXIS", web_app: { url: webAppUrl } }]],
};

bot.onText(/\/start/, async (msg) => {
  const name = msg.from?.first_name || "athlete";
  await bot.sendMessage(
    msg.chat.id,
    `AXIS is ready, ${name}.\n\nLive form coach inside Telegram: squat, push-up, plank. Camera stays on your phone.\n\nTap Open AXIS below.`,
    { reply_markup: keyboard },
  );
  await bot.sendMessage(msg.chat.id, "Or open it as a Mini App:", { reply_markup: inline });
});

bot.onText(/\/app/, async (msg) => {
  await bot.sendMessage(msg.chat.id, "Open AXIS", { reply_markup: inline });
});

bot.on("polling_error", (err) => {
  console.error("polling", err.message);
});

console.log("AXIS bot polling. Mini App:", webAppUrl);
