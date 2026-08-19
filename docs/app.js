import { PoseLandmarker, FilesetResolver } from "https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@0.10.14";

const tg = window.Telegram?.WebApp;
if (tg) {
  tg.ready();
  tg.expand();
  document.documentElement.style.setProperty("--tg-safe-top", (tg.safeAreaInset?.top || 0) + "px");
  document.documentElement.style.setProperty("--tg-safe-bottom", (tg.safeAreaInset?.bottom || 0) + "px");
}

const EX = {
  squat: { title: "Присед", metric: "повт", color: "#c6f135" },
  push_up: { title: "Отжимания", metric: "повт", color: "#5ee0b5" },
  plank: { title: "Планка", metric: "сек", color: "#ffc14a" },
};

const L = { LS: 11, RS: 12, LE: 13, RE: 14, LW: 15, RW: 16, LH: 23, RH: 24, LK: 25, RK: 26, LA: 27, RA: 28 };
const CONN = [[11,12],[11,13],[13,15],[12,14],[14,16],[11,23],[12,24],[23,24],[23,25],[25,27],[24,26],[26,28],[0,11],[0,12]];

function show(id) {
  document.querySelectorAll(".view").forEach((v) => v.classList.toggle("on", v.id === id));
  const dock = document.getElementById("dock");
  const tabs = ["home", "leaders", "history"];
  if (dock) {
    dock.hidden = !tabs.includes(id);
    dock.querySelectorAll("[data-tab]").forEach((b) => {
      b.classList.toggle("on", b.dataset.tab === id);
    });
  }
  if (id === "leaders") renderLeaders();
  if (id === "history") renderHistory();
}

function angle(a, b, c) {
  const abx = a.x - b.x, aby = a.y - b.y, cbx = c.x - b.x, cby = c.y - b.y;
  const magAb = Math.hypot(abx, aby), magCb = Math.hypot(cbx, cby);
  if (magAb < 1e-6 || magCb < 1e-6) return 180;
  return Math.acos(Math.min(1, Math.max(-1, (abx * cbx + aby * cby) / (magAb * magCb)))) * (180 / Math.PI);
}

function pickSide(lm) {
  const vis = (i) => lm[i]?.visibility ?? 0;
  const left = vis(L.LH) + vis(L.LK) + vis(L.LA);
  const useL = left >= vis(L.RH) + vis(L.RK) + vis(L.RA);
  const s = useL
    ? { s: lm[L.LS], e: lm[L.LE], w: lm[L.LW], h: lm[L.LH], k: lm[L.LK], a: lm[L.LA] }
    : { s: lm[L.RS], e: lm[L.RE], w: lm[L.RW], h: lm[L.RH], k: lm[L.RK], a: lm[L.RA] };
  if (!s.s || !s.h || !s.k || !s.a) return null;
  s.vis = (s.s.visibility + s.h.visibility + s.k.visibility + s.a.visibility) / 4;
  return s;
}

function visOf(p) { return p?.visibility ?? 0; }

function mid2(a, b) {
  if (!a && !b) return null;
  if (!a) return b;
  if (!b) return a;
  return { x: (a.x + b.x) / 2, y: (a.y + b.y) / 2, visibility: Math.min(visOf(a), visOf(b)) };
}

function ema(prev, next, a) {
  return prev == null ? next : prev + a * (next - prev);
}

class RepMachine {
  constructor(top, bottom) {
    this.top = top; this.bottom = bottom;
    this.phase = "idle"; this.reps = 0; this.reached = false; this.last = 0; this.lowest = 180;
  }
  onAngle(ang, now) {
    let counted = false;
    this.lowest = Math.min(this.lowest, ang);
    if (ang >= this.top) {
      if (this.reached && now - this.last > 450) { this.reps += 1; this.last = now; counted = true; }
      this.reached = false; this.lowest = 180; this.phase = "top";
    } else if (ang <= this.bottom) {
      this.reached = true; this.phase = "bottom";
    } else if (this.phase === "top" || this.phase === "idle") this.phase = "down";
    else if (this.phase === "bottom") this.phase = "up";
    return counted;
  }
  reset() { this.phase = "idle"; this.reps = 0; this.reached = false; this.last = 0; this.lowest = 180; }
}

/** Counts a push-up only after a real descent (elbows + shoulders) and a full lockout. */
class PushUpCounter {
  constructor() {
    this.reps = 0;
    this.phase = "idle";
    this.elbow = null;
    this.depth = null;
    this.topDepth = null;
    this.reached = false;
    this.last = 0;
    this.lowest = 180;
    this.highest = 0;
    this.maxDrop = 0;
    this.downAt = 0;
    this.seenTop = false;
    this.warm = 0;
    this.lastFeat = null;
  }

  reset() {
    this.reps = 0;
    this.phase = "idle";
    this.elbow = null;
    this.depth = null;
    this.topDepth = null;
    this.reached = false;
    this.last = 0;
    this.lowest = 180;
    this.highest = 0;
    this.maxDrop = 0;
    this.downAt = 0;
    this.seenTop = false;
    this.warm = 0;
    this.lastFeat = null;
  }

  measure(lm) {
    const ls = lm[L.LS], rs = lm[L.RS], le = lm[L.LE], re = lm[L.RE];
    const lw = lm[L.LW], rw = lm[L.RW], lh = lm[L.LH], rh = lm[L.RH];
    const la = lm[L.LA], ra = lm[L.RA];
    if (!ls || !rs || !lh || !rh) return null;

    const arms = [];
    const leftVis = (visOf(ls) + visOf(le) + visOf(lw)) / 3;
    const rightVis = (visOf(rs) + visOf(re) + visOf(rw)) / 3;
    if (ls && le && lw && leftVis >= 0.28) arms.push({ ang: angle(ls, le, lw), vis: leftVis });
    if (rs && re && rw && rightVis >= 0.28) arms.push({ ang: angle(rs, re, rw), vis: rightVis });
    if (!arms.length) return null;

    let elbow;
    if (arms.length === 2 && Math.abs(arms[0].ang - arms[1].ang) < 55) {
      const w = arms[0].vis + arms[1].vis;
      elbow = (arms[0].ang * arms[0].vis + arms[1].ang * arms[1].vis) / w;
    } else {
      elbow = arms.sort((a, b) => b.vis - a.vis)[0].ang;
    }

    const sh = mid2(ls, rs);
    const wr = mid2(lw, rw);
    const hp = mid2(lh, rh);
    const an = mid2(la, ra);
    if (!sh || !wr || !hp) return null;

    const torso = Math.max(0.08, Math.hypot(sh.x - hp.x, sh.y - hp.y));
    const front = Math.abs(ls.x - rs.x) > 0.14;
    const wristBelow = wr.y + 0.02 >= sh.y;
    const align = an ? 180 - angle(sh, hp, an) : 0;
    const bodyVis = (visOf(ls) + visOf(rs) + visOf(lh) + visOf(rh)) / 4;
    if (bodyVis < 0.35) return null;

    return { elbow, depth: sh.y, torso, front, wristBelow, align, ok: wristBelow };
  }

  onFrame(lm, now) {
    const f = this.measure(lm);
    this.lastFeat = f;
    if (!f || !f.ok) return false;

    this.elbow = ema(this.elbow, f.elbow, 0.30);
    this.depth = ema(this.depth, f.depth, 0.26);
    if (this.warm < 8) {
      this.warm += 1;
      if (this.elbow >= 148) this.topDepth = this.topDepth == null ? this.depth : Math.min(this.topDepth, this.depth);
      return false;
    }

    const elbow = this.elbow;
    const depth = this.depth;
    this.lowest = Math.min(this.lowest, elbow);
    this.highest = Math.max(this.highest, elbow);

    if (elbow >= 148) {
      this.topDepth = this.topDepth == null ? depth : Math.min(this.topDepth, depth);
      this.seenTop = true;
    }
    const drop = this.topDepth == null ? 0 : depth - this.topDepth;
    this.maxDrop = Math.max(this.maxDrop, drop);
    const minDrop = Math.max(0.042, Math.min(0.16, f.torso * 0.24));

    const atBottom = f.front
      ? (drop >= minDrop && elbow <= 150) || elbow <= 122
      : elbow <= 98 || (drop >= minDrop && elbow <= 118);
    const atTop = f.front
      ? elbow >= 140 && drop <= minDrop * 0.42
      : elbow >= 152;

    let counted = false;
    if (atTop) {
      const sinceLast = this.last ? now - this.last : 1e9;
      const cycle = this.downAt ? now - this.downAt : 0;
      const rom = this.highest - this.lowest;
      const deepEnough = this.maxDrop >= minDrop || rom >= (f.front ? 24 : 38);
      const timeOk = sinceLast >= 400 && (!this.downAt || (cycle >= 220 && cycle <= 4200));
      if (this.reached && this.seenTop && deepEnough && timeOk) {
        this.reps += 1;
        this.last = now;
        counted = true;
      }
      this.reached = false;
      this.lowest = elbow;
      this.highest = elbow;
      this.maxDrop = drop;
      this.downAt = 0;
      this.phase = "top";
    } else if (atBottom) {
      if (!this.reached) this.downAt = now;
      this.reached = true;
      this.phase = "bottom";
    } else if (this.phase === "top" || this.phase === "idle") {
      this.phase = "down";
    } else if (this.phase === "bottom") {
      this.phase = "up";
    }
    return counted;
  }
}

class Analyzer {
  constructor(kind) {
    this.kind = kind;
    this.machine = kind === "push_up" ? new PushUpCounter() : new RepMachine(158, 100);
    this.score = 80;
    this.hold = 0;
    this.lastT = null;
    this.cues = {};
  }
  analyze(lm, now) {
    const side = pickSide(lm);
    if (!side || side.vis < 0.45) {
      return { person: false, reps: this.machine.reps, hold: this.hold, score: Math.round(this.score), cue: "Нужно всё тело в кадре", color: "var(--amber)", newRep: false };
    }
    let cue = null, color = "var(--fog)", penalty = 0, counted = false;
    if (this.kind === "plank") {
      const align = 180 - angle(side.s, side.h, side.a);
      const dt = this.lastT == null ? 0 : Math.min(80, now - this.lastT);
      this.lastT = now;
      if (align <= 18) this.hold += dt;
      if (align > 18) { cue = side.h.y > (side.s.y + side.a.y) / 2 ? "Таз выше — одна линия" : "Таз ниже — не поднимай"; color = "var(--amber)"; penalty = 22; }
      else if (align > 10) { cue = "Напряги корпус"; color = "var(--amber)"; penalty = 8; }
    } else if (this.kind === "squat") {
      const knee = angle(side.h, side.k, side.a);
      const hip = angle(side.s, side.h, side.k);
      counted = this.machine.onAngle(knee, now);
      const shin = Math.max(0.05, Math.hypot(side.k.x - side.a.x, side.k.y - side.a.y));
      const fwd = Math.abs(side.k.x - side.a.x) / shin;
      if ((this.machine.phase === "bottom" || this.machine.phase === "down") && hip < 55) { cue = "Грудь вверх"; penalty = 18; color = "var(--amber)"; }
      else if (fwd > 0.85 && knee < 150) { cue = "Садись тазом назад"; penalty = 14; color = "var(--amber)"; }
      if (this.machine.phase === "up" && this.machine.lowest > 118) { cue = "Чуть глубже"; penalty = 16; color = "var(--amber)"; }
      if (counted && !cue) { cue = "Чисто"; color = "var(--lime)"; }
    } else {
      counted = this.machine.onFrame(lm, now);
      const feat = this.machine.lastFeat;
      if (!feat) {
        return { person: false, reps: this.machine.reps, hold: this.hold, score: Math.round(this.score), cue: "Руки и плечи в кадре", color: "var(--amber)", newRep: false };
      }
      if (!feat.ok) {
        cue = "Упор лёжа — ладони ниже плеч";
        color = "var(--amber)";
        penalty = 10;
      }
      const align = feat.align;
      if (align > 22) { cue = side.h.y < (side.s.y + side.a.y) / 2 ? "Таз ниже — не поднимай" : "Таз выше — одна линия"; penalty = 18; color = "var(--amber)"; }
      if (this.machine.phase === "up" && this.machine.lowest > 118) { cue = "Ниже грудь"; penalty = 14; color = "var(--amber)"; }
      if (counted && !cue) { cue = "Чисто"; color = "var(--lime)"; }
    }
    if (cue && color !== "var(--lime)") this.cues[cue] = (this.cues[cue] || 0) + 1;
    this.score = this.score * 0.82 + (100 - penalty) * 0.18;
    return {
      person: true,
      reps: this.machine.reps,
      hold: this.hold,
      score: Math.round(this.score),
      cue,
      color,
      newRep: counted,
    };
  }
  topCue() {
    const entries = Object.entries(this.cues);
    if (!entries.length) return "—";
    return entries.sort((a, b) => b[1] - a[1])[0][0];
  }
}

const state = {
  exercise: "squat",
  analyzer: null,
  started: 0,
  timer: null,
  facing: "user",
  stream: null,
  landmarker: null,
  running: false,
};

function loadHistory() {
  try { return JSON.parse(localStorage.getItem("axis.sessions") || "[]"); } catch { return []; }
}
function saveHistory(list) { localStorage.setItem("axis.sessions", JSON.stringify(list.slice(0, 40))); }

function esc(s) {
  return String(s ?? "").replace(/[&<>"']/g, (c) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
  }[c]));
}

function formatNick(username, fallback) {
  const raw = String(username || "").replace(/^@/, "").trim();
  if (raw) return `@${raw.slice(0, 32)}`;
  return String(fallback || "Атлет").slice(0, 40);
}

function me() {
  const u = tg?.initDataUnsafe?.user;
  if (!u?.id) return { id: "local", name: "Ты" };
  const fallback = [u.first_name, u.last_name].filter(Boolean).join(" ") || "Ты";
  return { id: u.id, name: formatNick(u.username, fallback) };
}

function emptyBoard() {
  return { v: 1, ts: 0, users: [] };
}

function loadLeaders() {
  try {
    const raw = JSON.parse(localStorage.getItem("axis.leaders") || "null");
    if (raw && Array.isArray(raw.users)) return raw;
  } catch { /* ignore */ }
  return emptyBoard();
}

function saveLeaders(board) {
  localStorage.setItem("axis.leaders", JSON.stringify(board));
}

function normalizeBoard(raw) {
  if (!raw || typeof raw !== "object") return null;
  const rows = raw.users || raw.u;
  if (!Array.isArray(rows)) return null;
  const users = rows.map((row) => {
    if (Array.isArray(row)) {
      const [id, name, total, best, sets] = row;
      return { id, name: formatNick("", name), total: Number(total) || 0, best: Number(best) || 0, sets: Number(sets) || 0 };
    }
    return {
      id: row.id ?? row.i,
      name: formatNick(row.username || row.nick, row.name || row.n || "Атлет"),
      total: Number(row.total ?? row.t) || 0,
      best: Number(row.best ?? row.b) || 0,
      sets: Number(row.sets ?? row.s) || 0,
    };
  }).filter((u) => u.id != null && u.total > 0);
  return { v: 1, ts: Number(raw.ts) || 0, users };
}

function ingestBoard(raw, { preferIncoming = false } = {}) {
  const incoming = normalizeBoard(raw);
  if (!incoming) return loadLeaders();
  const local = loadLeaders();
  const byId = new Map();
  for (const u of local.users) byId.set(String(u.id), { ...u });
  for (const u of incoming.users) {
    const id = String(u.id);
    const prev = byId.get(id);
    if (!prev) {
      byId.set(id, { ...u, id: u.id });
      continue;
    }
    const takeIncoming = preferIncoming || incoming.ts >= (local.ts || 0);
    byId.set(id, {
      id: u.id,
      name: u.name || prev.name,
      total: takeIncoming ? Math.max(prev.total, u.total) : Math.max(u.total, prev.total),
      best: Math.max(prev.best, u.best),
      sets: Math.max(prev.sets, u.sets),
    });
  }
  const board = {
    v: 1,
    ts: Math.max(local.ts || 0, incoming.ts || 0, Date.now()),
    users: [...byId.values()],
  };
  saveLeaders(board);
  return board;
}

function applyMyPushups(reps) {
  if (!Number.isFinite(reps) || reps < 1) return;
  const user = me();
  const board = loadLeaders();
  const id = String(user.id);
  const prev = board.users.find((u) => String(u.id) === id) || {
    id: user.id, name: user.name, total: 0, best: 0, sets: 0,
  };
  prev.name = user.name;
  prev.total += reps;
  prev.best = Math.max(prev.best, reps);
  prev.sets += 1;
  board.users = board.users.filter((u) => String(u.id) !== id).concat(prev);
  board.ts = Date.now();
  saveLeaders(board);
}

function seedLeadersFromHistory() {
  const user = me();
  const board = loadLeaders();
  if (board.users.some((u) => String(u.id) === String(user.id))) return;
  const sets = loadHistory().filter((s) => s.exercise === "push_up" && s.reps > 0);
  if (!sets.length) return;
  board.users.push({
    id: user.id,
    name: user.name,
    total: sets.reduce((sum, s) => sum + s.reps, 0),
    best: Math.max(...sets.map((s) => s.reps)),
    sets: sets.length,
  });
  board.ts = Date.now();
  saveLeaders(board);
}

function boardFromUrl() {
  const hash = new URLSearchParams(location.hash.replace(/^#/, ""));
  const query = new URLSearchParams(location.search);
  const raw = query.get("lb") || hash.get("lb");
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch {
    try {
      const pad = raw.replace(/-/g, "+").replace(/_/g, "/");
      const b64 = pad + "=".repeat((4 - (pad.length % 4)) % 4);
      return JSON.parse(atob(b64));
    } catch {
      return null;
    }
  }
}

async function fetchPublicBoard() {
  const url = new URL("leaders.json", document.baseURI);
  url.searchParams.set("t", String(Date.now()));
  const res = await fetch(url.href, { cache: "no-store" });
  if (!res.ok) throw new Error("leaders.json");
  return res.json();
}

function rankedUsers(board) {
  return [...(board?.users || [])]
    .filter((u) => u.total > 0)
    .sort((a, b) => b.total - a.total || b.best - a.best || String(a.name).localeCompare(String(b.name), "ru"));
}

function badgeHtml(i) {
  if (i === 0) return `<span class="badge gold" title="1 место">1</span>`;
  if (i === 1) return `<span class="badge silver" title="2 место">2</span>`;
  if (i === 2) return `<span class="badge bronze" title="3 место">3</span>`;
  return "";
}

function renderHomeLeaders() {
  const preview = document.getElementById("homeLeadersPreview");
  if (!preview) return;
  const top = rankedUsers(loadLeaders())[0];
  preview.textContent = top
    ? `${top.name} — ${top.total} отж.`
    : "Кто сделал больше — с ником и счётом";
}

function renderLeaders() {
  const board = loadLeaders();
  const list = rankedUsers(board);
  const mine = me();
  const myIdx = list.findIndex((u) => String(u.id) === String(mine.id));
  const meBox = document.getElementById("leadersMe");
  const box = document.getElementById("leadersList");
  if (myIdx === -1) {
    meBox.innerHTML = `<div class="tiny">ТЫ</div><b>Пока нет отжиманий</b><div class="muted">Закрой сет отжиманий — попадёшь в таблицу</div>`;
  } else {
    const row = list[myIdx];
    meBox.innerHTML = `<div class="tiny">ТВОЁ МЕСТО</div><div class="lead-nick"><b>${esc(row.name)}</b>${badgeHtml(myIdx)}</div><div class="muted">#${myIdx + 1} · ${row.total} отжиманий</div>`;
  }
  renderHomeLeaders();
  if (!list.length) {
    box.innerHTML = `<div class="muted" style="padding:12px 0">Таблица пуста. Сделай первый сет отжиманий.</div>`;
    return;
  }
  box.innerHTML = list.map((u, i) => {
    const mineRow = String(u.id) === String(mine.id);
    return `
      <div class="card lead${mineRow ? " mine" : ""}">
        <div class="lead-nick">
          <b>${esc(u.name)}${mineRow ? " · ты" : ""}</b>
          ${badgeHtml(i)}
        </div>
        <div class="total">${u.total}</div>
      </div>
    `;
  }).join("");
}

async function refreshLeaders() {
  const fromUrl = boardFromUrl();
  if (fromUrl) ingestBoard(fromUrl, { preferIncoming: true });
  seedLeadersFromHistory();
  try {
    ingestBoard(await fetchPublicBoard());
  } catch { /* static snapshot optional */ }
  renderLeaders();
}

function refreshHome() {
  const n = loadHistory().length;
  document.getElementById("pill").textContent = n ? `${n} СЕТОВ` : "ПЕРВЫЙ СЕТ";
  renderHomeLeaders();
}

function renderHistory() {
  const box = document.getElementById("histList");
  const items = loadHistory();
  if (!items.length) {
    box.innerHTML = `<div class="muted" style="padding:20px 0">Сетов пока нет. Сними первый.</div>`;
    return;
  }
  box.innerHTML = items.map((s) => `
    <div class="card" style="display:block">
      <div class="idx" style="color:${EX[s.exercise]?.color || "var(--lime)"}">${EX[s.exercise]?.title || s.exercise}</div>
      <b>${s.exercise === "plank" ? s.hold + " сек" : s.reps + " повт"} · техника ${s.score}</b>
      <div class="muted">${new Date(s.ended).toLocaleString()}</div>
    </div>
  `).join("");
}

async function initPose() {
  if (state.landmarker) return;
  const files = await FilesetResolver.forVisionTasks(
    "https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@0.10.14/wasm",
  );
  state.landmarker = await PoseLandmarker.createFromOptions(files, {
    baseOptions: {
      modelAssetPath: "https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/1/pose_landmarker_lite.task",
      delegate: "GPU",
    },
    runningMode: "VIDEO",
    numPoses: 1,
  });
}

async function startCamera() {
  if (state.stream) state.stream.getTracks().forEach((t) => t.stop());
  state.stream = await navigator.mediaDevices.getUserMedia({
    video: { facingMode: state.facing, width: { ideal: 720 }, height: { ideal: 1280 } },
    audio: false,
  });
  const video = document.getElementById("cam");
  video.srcObject = state.stream;
  await video.play();
}

function drawPose(lm, w, h) {
  const canvas = document.getElementById("overlay");
  const ctx = canvas.getContext("2d");
  canvas.width = w; canvas.height = h;
  ctx.clearRect(0, 0, w, h);
  if (!lm) return;
  ctx.lineWidth = 4;
  ctx.strokeStyle = "#c6f135";
  ctx.fillStyle = "#c6f135";
  CONN.forEach(([a, b]) => {
    if (!lm[a] || !lm[b] || (lm[a].visibility || 1) < 0.35) return;
    ctx.beginPath();
    ctx.moveTo(lm[a].x * w, lm[a].y * h);
    ctx.lineTo(lm[b].x * w, lm[b].y * h);
    ctx.stroke();
  });
  lm.forEach((p) => {
    if ((p.visibility || 1) < 0.35) return;
    ctx.beginPath();
    ctx.arc(p.x * w, p.y * h, 5, 0, Math.PI * 2);
    ctx.fill();
  });
}

let lastVideoTime = -1;
function loop() {
  if (!state.running) return;
  const video = document.getElementById("cam");
  if (state.landmarker && video.readyState >= 2 && video.currentTime !== lastVideoTime) {
    lastVideoTime = video.currentTime;
    const res = state.landmarker.detectForVideo(video, performance.now());
    const lm = res.landmarks?.[0];
    drawPose(lm, video.videoWidth || 720, video.videoHeight || 1280);
    if (lm && state.analyzer) {
      const v = state.analyzer.analyze(lm, performance.now());
      const isPlank = state.exercise === "plank";
      document.getElementById("metric").textContent = isPlank ? Math.floor(v.hold / 1000) : v.reps;
      document.getElementById("formScore").textContent = v.score;
      document.getElementById("formBar").style.width = `${v.score}%`;
      const cue = document.getElementById("cue");
      cue.textContent = v.cue || (v.person ? "Держи линию" : "Зайди в кадр");
      cue.style.color = v.color || "var(--fog)";
      if (v.newRep && navigator.vibrate) navigator.vibrate(30);
    }
  }
  requestAnimationFrame(loop);
}

function tickClock() {
  const s = Math.floor((Date.now() - state.started) / 1000);
  document.getElementById("clock").textContent = `${Math.floor(s / 60)}:${String(s % 60).padStart(2, "0")}`;
}

async function openSession(ex) {
  hideLeadersCta();
  state.exercise = ex;
  state.analyzer = new Analyzer(ex);
  state.started = Date.now();
  document.getElementById("exLabel").textContent = EX[ex].title.toUpperCase();
  document.getElementById("exLabel").style.color = EX[ex].color;
  document.getElementById("metricLabel").textContent = EX[ex].metric.toUpperCase();
  document.getElementById("metric").textContent = "0";
  document.getElementById("formScore").textContent = "100";
  show("session");
  try {
    await initPose();
    await startCamera();
    state.running = true;
    loop();
    clearInterval(state.timer);
    state.timer = setInterval(tickClock, 250);
  } catch (err) {
    document.getElementById("cue").textContent = "Нужен доступ к камере";
  }
}

function stopSession() {
  state.running = false;
  clearInterval(state.timer);
  if (state.stream) state.stream.getTracks().forEach((t) => t.stop());
  state.stream = null;
}

let pendingLeaders = null;

function hideLeadersCta() {
  pendingLeaders = null;
  const btn = document.getElementById("toLeaders");
  if (btn) {
    btn.hidden = true;
    btn.disabled = false;
    btn.textContent = "К таблице лидеров";
    btn.onclick = null;
  }
  if (tg?.MainButton) {
    try { tg.MainButton.offClick(onLeadersSubmit); } catch { /* optional */ }
    try { tg.MainButton.hide(); } catch { /* optional */ }
  }
}

function submitPushupsToBot(rec) {
  if (!tg?.sendData || rec.exercise !== "push_up" || rec.reps < 1) return false;
  try {
    tg.HapticFeedback?.notificationOccurred?.("success");
    tg.sendData(JSON.stringify({
      exercise: "push_up",
      reps: rec.reps,
      score: rec.score,
    }));
    return true;
  } catch {
    return false;
  }
}

function onLeadersSubmit() {
  const rec = pendingLeaders;
  const btn = document.getElementById("toLeaders");
  if (!rec) return;
  if (submitPushupsToBot(rec)) {
    if (btn) {
      btn.disabled = true;
      btn.textContent = "Отправлено боту";
    }
  }
}

function showLeadersCta(rec) {
  hideLeadersCta();
  if (rec.exercise !== "push_up" || rec.reps < 1) return;
  pendingLeaders = rec;
  const btn = document.getElementById("toLeaders");
  if (btn) {
    btn.hidden = false;
    btn.textContent = "К таблице лидеров";
    btn.onclick = () => show("leaders");
  }
  if (tg?.MainButton) {
    tg.MainButton.setText(`Отправить боту · ${rec.reps}`);
    tg.MainButton.show();
    tg.MainButton.onClick(onLeadersSubmit);
  }
}

function endSet() {
  stopSession();
  const a = state.analyzer;
  const ended = Date.now();
  const dur = ended - state.started;
  const rec = {
    exercise: state.exercise,
    reps: a.machine.reps,
    hold: Math.floor(a.hold / 1000),
    score: Math.round(a.score),
    cue: a.topCue(),
    ended,
    duration: dur,
  };
  const list = loadHistory();
  list.unshift(rec);
  saveHistory(list);
  document.getElementById("recapTitle").textContent = EX[state.exercise].title;
  document.getElementById("recapScore").textContent = rec.score;
  document.getElementById("scoreRing").style.background =
    `conic-gradient(${EX[state.exercise].color} 0 ${rec.score * 3.6}deg, #243028 ${rec.score * 3.6}deg)`;
  document.getElementById("recapMetricName").textContent = EX[state.exercise].metric.toUpperCase();
  document.getElementById("recapMetric").textContent = state.exercise === "plank" ? rec.hold + "s" : rec.reps;
  const m = Math.floor(dur / 60000), s = Math.floor((dur / 1000) % 60);
  document.getElementById("recapTime").textContent = `${m}:${String(s).padStart(2, "0")}`;
  document.getElementById("recapCue").textContent = rec.cue;
  refreshHome();
  if (rec.exercise === "push_up" && rec.reps > 0) applyMyPushups(rec.reps);
  showLeadersCta(rec);
  show("recap");
}

document.querySelectorAll("[data-ex]").forEach((el) => {
  el.addEventListener("click", () => openSession(el.dataset.ex));
});
document.getElementById("toHistory").onclick = () => show("history");
document.getElementById("homeLeaders").onclick = () => show("leaders");
document.getElementById("closeSession").onclick = () => { hideLeadersCta(); stopSession(); show("home"); };
document.getElementById("endSet").onclick = endSet;
document.getElementById("again").onclick = () => { hideLeadersCta(); openSession(state.exercise); };
document.getElementById("toHome").onclick = () => { hideLeadersCta(); show("home"); };
document.getElementById("flipCam").onclick = async () => {
  state.facing = state.facing === "user" ? "environment" : "user";
  if (state.running) await startCamera();
};
document.getElementById("dock").querySelectorAll("[data-tab]").forEach((btn) => {
  btn.addEventListener("click", () => {
    hideLeadersCta();
    show(btn.dataset.tab);
  });
});

refreshHome();
refreshLeaders();
setTimeout(() => show("home"), 1800);
