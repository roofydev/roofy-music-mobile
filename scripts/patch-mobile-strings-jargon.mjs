/**
 * Overwrites jargon keys in all locale strings.xml files from values/strings.xml.
 */
import fs from 'node:fs';
import path from 'node:path';

const resDir = path.join(process.cwd(), 'app/src/main/res');
const sourcePath = path.join(resDir, 'values/strings.xml');
const sourceXml = fs.readFileSync(sourcePath, 'utf8');

const rawEntries = new Map();
const entryRegex = /<string name="([^"]+)">([\s\S]*?)<\/string>/g;
let match;
while ((match = entryRegex.exec(sourceXml)) !== null) {
    rawEntries.set(match[1], match[2].trim());
}

function resolveValue(name) {
    const raw = rawEntries.get(name);
    if (!raw) return null;
    const alias = raw.match(/^@string\/(.+)$/);
    if (alias) {
        const roofyPath = path.join(resDir, 'values/roofy_strings.xml');
        if (fs.existsSync(roofyPath)) {
            const roofy = fs.readFileSync(roofyPath, 'utf8');
            const m = roofy.match(
                new RegExp(`<string name="${alias[1]}">([\\s\\S]*?)</string>`),
            );
            if (m) return m[1].trim();
        }
        return resolveValue(alias[1]);
    }
    return raw;
}

const JARGON_KEYS = ['search_yt_music', 'enable_discord_rpc', 'library_tab'];

const canonical = new Map();
for (const key of JARGON_KEYS) {
    const value = resolveValue(key);
    if (value) canonical.set(key, value);
}

function escapeXml(text) {
    return text
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '\\"')
        .replace(/'/g, "\\'");
}

function patchFile(filePath) {
    let xml = fs.readFileSync(filePath, 'utf8');
    let updated = 0;

    for (const [name, value] of canonical) {
        const escaped = escapeXml(value);
        const existing = new RegExp(`<string name="${name}">[\\s\\S]*?</string>`, 'm');
        const replacement = `<string name="${name}">${escaped}</string>`;

        if (existing.test(xml)) {
            xml = xml.replace(existing, replacement);
            updated += 1;
        }
    }

    if (updated > 0) {
        fs.writeFileSync(filePath, xml, 'utf8');
    }
    return updated;
}

let total = 0;
for (const dir of fs.readdirSync(resDir)) {
    if (!dir.startsWith('values')) continue;
    const filePath = path.join(resDir, dir, 'strings.xml');
    if (!fs.existsSync(filePath)) continue;
    const count = patchFile(filePath);
    if (count > 0) {
        console.log(`${dir}: updated ${count} keys`);
        total += count;
    }
}

console.log(`done (${total} key updates)`);
