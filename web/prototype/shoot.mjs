import { chromium } from 'playwright';
import { fileURLToPath } from 'url';
import { dirname, resolve } from 'path';

const __dir = dirname(fileURLToPath(import.meta.url));
const url = 'file://' + resolve(__dir, 'index.html');

// 2K = 2560x1440, 4K = 3840x2160. scale 提升根字号以适配高分屏。
const shots = [
  { name:'01-总览-科技蓝-2K',      q:'?theme=tech&scale=1.15',           w:2560, h:1440 },
  { name:'02-总览-暗夜换肤-2K',    q:'?theme=midnight&scale=1.15',       w:2560, h:1440 },
  { name:'03-总览-商务金换肤-2K',  q:'?theme=gold&scale=1.15',           w:2560, h:1440 },
  { name:'04-开通租户与邮件-2K',   q:'?theme=tech&scale=1.1&modal=open', w:2560, h:1440 },
  { name:'05-总览-英文i18n-2K',    q:'?theme=tech&scale=1.15&lang=en',   w:2560, h:1440 },
  { name:'06-总览-4K自适应',       q:'?theme=tech&scale=1.7',            w:3840, h:2160 },
  { name:'07-开通邮件-英文-4K',    q:'?theme=midnight&scale=1.6&lang=en&modal=open', w:3840, h:2160 },
];

const browser = await chromium.launch();
for (const s of shots){
  const page = await browser.newPage({ viewport:{ width:s.w, height:s.h }, deviceScaleFactor:1 });
  await page.goto(url + s.q, { waitUntil:'networkidle' });
  await page.waitForTimeout(350);
  await page.screenshot({ path: resolve(__dir, 'shots', s.name + '.png'), fullPage:false });
  console.log('shot:', s.name, s.w+'x'+s.h);
  await page.close();
}
await browser.close();
console.log('done');
