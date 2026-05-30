/**
 * Overwrites user-facing jargon keys in all locale roofy_strings.xml files
 * with canonical English from values/roofy_strings.xml (product UX rework).
 */
import fs from 'node:fs';
import path from 'node:path';

const resDir = path.join(process.cwd(), 'app/src/main/res');
const sourcePath = path.join(resDir, 'values/roofy_strings.xml');
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
    if (alias) return resolveValue(alias[1]);
    return raw;
}

/** Keys that must not mention YouTube Music, scrobble, or "cache" in normal UI. */
const JARGON_KEYS = [
    'allows_for_sync_witch_youtube',
    'sync_playlist_desc',
    'refetch_desc',
    'youtube_music_lyrics_note',
    'enable_song_cache',
    'enable_song_cache_desc',
    'edit_playlist_cover_note',
    'clear_song_cache_dialog',
    'clear_image_cache_dialog',
    'android_auto_youtube_playlists_desc',
    'cache_size_warning_message',
    'scrobbling_configuration',
    'scrobble_min_track_duration',
    'scrobble_delay_percent',
    'scrobble_delay_minutes',
    'cached_playlist',
    'remove_from_cache',
    'show_cached_playlist',
    'song_cache',
    'image_cache',
];

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
        const existing = new RegExp(
            `<string name="${name}">[\\s\\S]*?</string>`,
            'm',
        );
        const replacement = `<string name="${name}">${escaped}</string>`;

        if (existing.test(xml)) {
            xml = xml.replace(existing, replacement);
            updated += 1;
        } else {
            xml = xml.replace(
                '</resources>',
                `    <string name="${name}">${escaped}</string>\n</resources>`,
            );
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
    const filePath = path.join(resDir, dir, 'roofy_strings.xml');
    if (!fs.existsSync(filePath)) continue;
    const count = patchFile(filePath);
    if (count > 0) {
        console.log(`${dir}: updated ${count} keys`);
        total += count;
    }
}

console.log(`done (${total} key updates across locales)`);
