import fs from 'node:fs';
import path from 'node:path';

const resDir = path.join(process.cwd(), 'app/src/main/res');
const sourceFile = path.join(resDir, 'values/roofy_strings.xml');
const source = fs.readFileSync(sourceFile, 'utf8');

const productUxRegex =
    /<string name="(product_ux_[^"]+)">([\s\S]*?)<\/string>/g;
const keys = new Map();
let match;
while ((match = productUxRegex.exec(source)) !== null) {
    keys.set(match[1], match[2]);
}

const localeDirs = fs
    .readdirSync(resDir)
    .filter((name) => name.startsWith('values-') && name !== 'values-night');

for (const localeDir of localeDirs) {
    const targetPath = path.join(resDir, localeDir, 'roofy_strings.xml');
    if (!fs.existsSync(targetPath)) continue;

    let xml = fs.readFileSync(targetPath, 'utf8');
    let added = 0;

    for (const [name, value] of keys) {
        if (xml.includes(`name="${name}"`)) continue;
        const escaped = value.replace(/\\/g, '\\\\').replace(/'/g, "\\'");
        xml = xml.replace(
            '</resources>',
            `    <string name="${name}">${escaped}</string>\n</resources>`,
        );
        added += 1;
    }

    if (added > 0) {
        fs.writeFileSync(targetPath, xml, 'utf8');
        console.log(`${localeDir}: added ${added} product_ux keys`);
    }
}

console.log('done');
