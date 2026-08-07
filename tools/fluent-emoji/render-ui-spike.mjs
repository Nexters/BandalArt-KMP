import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const generatedDirectory = path.join(scriptDirectory, "generated");
const outputDirectory = path.join(scriptDirectory, "ui-spike");
const catalog = JSON.parse(
  fs.readFileSync(path.join(generatedDirectory, "catalog.json"), "utf8"),
);

fs.mkdirSync(outputDirectory, { recursive: true });

const encodedImage = (style, resourceKey) => {
  const bytes = fs.readFileSync(
    path.join(generatedDirectory, style, `${resourceKey}.webp`),
  );
  return `data:image/webp;base64,${bytes.toString("base64")}`;
};

const escapeXml = (value) =>
  value.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");

function image(href, x, y, size) {
  return `<image href="${href}" x="${x}" y="${y}" width="${size}" height="${size}"/>`;
}

function comparisonSvg() {
  const width = 1600;
  const height = 1480;
  const columns = 5;
  const cardWidth = 296;
  const cardHeight = 310;
  const gap = 16;
  const startX = 28;
  const startY = 160;
  const cards = catalog.items.map((item, index) => {
    const column = index % columns;
    const row = Math.floor(index / columns);
    const x = startX + column * (cardWidth + gap);
    const y = startY + row * (cardHeight + gap);
    const color = encodedImage("color", item.resourceKey);
    const threeD = encodedImage("3d", item.resourceKey);
    const sizes = [22, 32, 48];
    const lightImages = sizes
      .map((size, sizeIndex) => image(color, x + 70 + sizeIndex * 72, y + 52 + (48 - size) / 2, size))
      .join("");
    const lightThreeD = sizes
      .map((size, sizeIndex) => image(threeD, x + 70 + sizeIndex * 72, y + 112 + (48 - size) / 2, size))
      .join("");
    const darkImages = sizes
      .map((size, sizeIndex) => image(color, x + 70 + sizeIndex * 72, y + 190 + (48 - size) / 2, size))
      .join("");
    const darkThreeD = sizes
      .map((size, sizeIndex) => image(threeD, x + 70 + sizeIndex * 72, y + 250 + (48 - size) / 2, size))
      .join("");
    return `
      <rect x="${x}" y="${y}" width="${cardWidth}" height="${cardHeight}" rx="20" fill="#F7F8FA" stroke="#DDE1E6"/>
      <path d="M ${x} ${y + 164} H ${x + cardWidth} V ${y + cardHeight - 20} Q ${x + cardWidth} ${y + cardHeight} ${x + cardWidth - 20} ${y + cardHeight} H ${x + 20} Q ${x} ${y + cardHeight} ${x} ${y + cardHeight - 20} Z" fill="#111827"/>
      <text x="${x + 18}" y="${y + 30}" class="name">${escapeXml(item.cldrName)}</text>
      <text x="${x + 18}" y="${y + 76}" class="label">COLOR</text>
      ${lightImages}
      <text x="${x + 18}" y="${y + 136}" class="label">3D</text>
      ${lightThreeD}
      <text x="${x + 18}" y="${y + 186}" class="darkLabel">COLOR</text>
      ${darkImages}
      <text x="${x + 18}" y="${y + 246}" class="darkLabel">3D</text>
      ${darkThreeD}
    `;
  });
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}">
    <style>
      .title { font: 700 34px system-ui, sans-serif; fill: #111827; }
      .subtitle { font: 400 18px system-ui, sans-serif; fill: #4B5563; }
      .name { font: 650 17px system-ui, sans-serif; fill: #111827; }
      .label { font: 700 10px system-ui, sans-serif; fill: #6B7280; letter-spacing: 1px; }
      .darkLabel { font: 700 10px system-ui, sans-serif; fill: #9CA3AF; letter-spacing: 1px; }
    </style>
    <rect width="100%" height="100%" fill="#FFFFFF"/>
    <text x="32" y="52" class="title">Fluent Emoji Color / 3D UI-size comparison</text>
    <text x="32" y="86" class="subtitle">Each row uses 22px · 32px · 48px. The lower surface is the Bandalart dark-mode approximation.</text>
    <text x="32" y="116" class="subtitle">These pixels represent a 1× density review target; platform dp scaling is validated later in Compose.</text>
    ${cards.join("")}
  </svg>`;
}

function pickerPanel(x, dark) {
  const panelColor = dark ? "#111827" : "#FFFFFF";
  const textColor = dark ? "#F9FAFB" : "#111827";
  const fieldColor = dark ? "#1F2937" : "#F3F4F6";
  const mutedColor = dark ? "#9CA3AF" : "#6B7280";
  const items = catalog.items.map((item, index) => {
    const column = index % 5;
    const row = Math.floor(index / 5);
    const cellX = x + 30 + column * 96;
    const cellY = 260 + row * 102;
    const selected = index === 0;
    return `
      <rect x="${cellX}" y="${cellY}" width="76" height="76" rx="18" fill="${fieldColor}" stroke="${selected ? "#18DFA1" : "transparent"}" stroke-width="${selected ? 3 : 0}"/>
      ${image(encodedImage("color", item.resourceKey), cellX + 22, cellY + 22, 32)}
      ${selected ? `<circle cx="${cellX + 66}" cy="${cellY + 10}" r="11" fill="#18DFA1"/><text x="${cellX + 61}" y="${cellY + 15}" font-size="14" font-weight="800" fill="#111827">✓</text>` : ""}
    `;
  });
  return `
    <rect x="${x}" y="40" width="540" height="720" rx="32" fill="${panelColor}" stroke="#D1D5DB"/>
    <rect x="${x + 230}" y="54" width="80" height="5" rx="3" fill="${mutedColor}"/>
    <text x="${x + 30}" y="108" font-size="28" font-weight="750" fill="${textColor}" font-family="system-ui, sans-serif">아이콘 선택</text>
    <text x="${x + 478}" y="108" font-size="26" fill="${textColor}" font-family="system-ui, sans-serif">×</text>
    <rect x="${x + 30}" y="132" width="480" height="52" rx="16" fill="${fieldColor}"/>
    <text x="${x + 50}" y="165" font-size="17" fill="${mutedColor}" font-family="system-ui, sans-serif">⌕  목표 아이콘 검색</text>
    <rect x="${x + 30}" y="202" width="66" height="34" rx="17" fill="#18DFA1"/>
    <text x="${x + 48}" y="225" font-size="14" font-weight="700" fill="#111827" font-family="system-ui, sans-serif">최근</text>
    <text x="${x + 116}" y="225" font-size="14" fill="${textColor}" font-family="system-ui, sans-serif">활동</text>
    <text x="${x + 172}" y="225" font-size="14" fill="${textColor}" font-family="system-ui, sans-serif">학습</text>
    <text x="${x + 228}" y="225" font-size="14" fill="${textColor}" font-family="system-ui, sans-serif">건강</text>
    <text x="${x + 284}" y="225" font-size="14" fill="${textColor}" font-family="system-ui, sans-serif">재정</text>
    ${items.join("")}
  `;
}

function pickerSvg() {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="1180" height="800" viewBox="0 0 1180 800">
    <rect width="100%" height="100%" fill="#E5E7EB"/>
    ${pickerPanel(30, false)}
    ${pickerPanel(610, true)}
  </svg>`;
}

const normalizedSvg = (value) => `${value.replace(/[ \t]+$/gm, "").trim()}\n`;

fs.writeFileSync(
  path.join(outputDirectory, "color-3d-ui-comparison.svg"),
  normalizedSvg(comparisonSvg()),
);
fs.writeFileSync(
  path.join(outputDirectory, "picker-wireframe.svg"),
  normalizedSvg(pickerSvg()),
);
