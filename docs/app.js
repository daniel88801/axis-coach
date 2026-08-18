import { PoseLandmarker, FilesetResolver } from "https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@0.10.14";

const tg = window.Telegram?.WebApp;
if (tg) {
  tg.ready();
  tg.expand();
  document.documentElement.style.setProperty("--tg-safe-top", (tg.safeAreaInset?.top || 0) + "px");
  document.documentElement.style.setProperty("--tg-safe-bottom", (tg.safeAreaInset?.bottom || 0) + "px");
}

const EX = {
  squat: { title: "Squat", metric: "reps", color: "#c6f135" },
  push_up: { title: "Push-up", metric: "reps", color: "#5ee0b5" },
  plank: { title: "Plank", metric: "sec", color: "#ffc14a" },
};

const L = { LS: 11, RS: 12, LE: 13, RE: 14, LW: 15, RW: 16, LH: 23, RH: 24, LK: 25, RK: 26, LA: 27, RA: 28 };
const CONN = [[11,12],[11,13],[13,15],[12,14],[14,16],[11,23],[12,24],[23,24],[23,25],[25,27],[24,26],[26,28],[0,11],[0,12]];

function show(id) {
  document.querySelectorAll(".view").forEach((v) => v.classList.toggle("on", v.id === id));
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

class Analyzer {
  constructor(kind) {
    this.kind = kind;
    this.machine = new RepMachine(kind === "push_up" ? 155 : 158, kind === "push_up" ? 95 : 100);
    this.score = 80;
    this.hold = 0;
    this.lastT = null;
    this.cues = {};
  }
  analyze(lm, now) {
    const side = pickSide(lm);
    if (!side || side.vis < 0.45) {
      return { person: false, reps: this.machine.reps, hold: this.hold, score: Math.round(this.score), cue: "I need your full body in frame", color: "var(--amber)", newRep: false };
    }
    let cue = null, color = "var(--fog)", penalty = 0, counted = false;
    if (this.kind === "plank") {
      const align = 180 - angle(side.s, side.h, side.a);
      const dt = this.lastT == null ? 0 : Math.min(80, now - this.lastT);
      this.lastT = now;
      if (align <= 18) this.hold += dt;
      if (align > 18) { cue = side.h.y > (side.s.y + side.a.y) / 2 ? "Hips up — one straight line" : "Hips down — don't pike"; color = "var(--amber)"; penalty = 22; }
      else if (align > 10) { cue = "Brace your core"; color = "var(--amber)"; penalty = 8; }
    } else if (this.kind === "squat") {
      const knee = angle(side.h, side.k, side.a);
      const hip = angle(side.s, side.h, side.k);
      counted = this.machine.onAngle(knee, now);
      const shin = Math.max(0.05, Math.hypot(side.k.x - side.a.x, side.k.y - side.a.y));
      const fwd = Math.abs(side.k.x - side.a.x) / shin;
      if ((this.machine.phase === "bottom" || this.machine.phase === "down") && hip < 55) { cue = "Chest up"; penalty = 18; color = "var(--amber)"; }
      else if (fwd > 0.85 && knee < 150) { cue = "Sit your hips back"; penalty = 14; color = "var(--amber)"; }
      if (this.machine.phase === "up" && this.machine.lowest > 118) { cue = "A little deeper"; penalty = 16; color = "var(--amber)"; }
      if (counted && !cue) { cue = "Clean"; color = "var(--lime)"; }
    } else {
      const elbow = angle(side.s, side.e, side.w);
      const align = 180 - angle(side.s, side.h, side.a);
      counted = this.machine.onAngle(elbow, now);
      if (align > 22) { cue = side.h.y < (side.s.y + side.a.y) / 2 ? "Hips down — don't pike" : "Hips up — one straight line"; penalty = 18; color = "var(--amber)"; }
      if (counted && !cue) { cue = "Clean"; color = "var(--lime)"; }
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

function refreshHome() {
  const n = loadHistory().length;
  document.getElementById("pill").textContent = n ? `${n} LOGGED` : "FIRST SESSION";
}

function renderHistory() {
  const box = document.getElementById("histList");
  const items = loadHistory();
  if (!items.length) {
    box.innerHTML = `<div class="muted" style="padding:20px 0">No sets yet. Film one.</div>`;
    return;
  }
  box.innerHTML = items.map((s) => `
    <div class="card" style="display:block">
      <div class="idx" style="color:${EX[s.exercise]?.color || "var(--lime)"}">${EX[s.exercise]?.title || s.exercise}</div>
      <b>${s.exercise === "plank" ? s.hold + "s" : s.reps + " reps"} · form ${s.score}</b>
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
      cue.textContent = v.cue || (v.person ? "Hold the line" : "Step into frame");
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
    document.getElementById("cue").textContent = "Camera permission needed";
  }
}

function stopSession() {
  state.running = false;
  clearInterval(state.timer);
  if (state.stream) state.stream.getTracks().forEach((t) => t.stop());
  state.stream = null;
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
  show("recap");
}

document.querySelectorAll("[data-ex]").forEach((el) => {
  el.addEventListener("click", () => openSession(el.dataset.ex));
});
document.getElementById("toHistory").onclick = () => { renderHistory(); show("history"); };
document.getElementById("backHome").onclick = () => show("home");
document.getElementById("closeSession").onclick = () => { stopSession(); show("home"); };
document.getElementById("endSet").onclick = endSet;
document.getElementById("again").onclick = () => openSession(state.exercise);
document.getElementById("toHome").onclick = () => show("home");
document.getElementById("flipCam").onclick = async () => {
  state.facing = state.facing === "user" ? "environment" : "user";
  if (state.running) await startCamera();
};

refreshHome();
setTimeout(() => show("home"), 1800);
