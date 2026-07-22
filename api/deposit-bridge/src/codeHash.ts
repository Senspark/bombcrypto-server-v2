import {createHash} from "crypto";
import {readdirSync, readFileSync, statSync} from "fs";

// Fingerprint of the code ACTUALLY running: a sha256 over every source file (name + bytes) in the entry
// directory, computed at startup. Unlike a build-number/timestamp label, this changes IFF the compiled bytes
// change — so an unchanged hash after a "rebuild" proves the image still holds the OLD code (a cache/recreate
// miss). Covers .js (prod dist) and .ts (dev vite-node); skips type-decl and map files.
export function computeCodeHash(entryDir: string): string {
    const hash = createHash("sha256");
    const walk = (dir: string): void => {
        for (const name of readdirSync(dir).sort()) {
            const path = `${dir}/${name}`;
            if (statSync(path).isDirectory()) {
                walk(path);
            } else if (/\.(js|ts)$/.test(name) && !name.endsWith(".d.ts") && !name.endsWith(".map")) {
                hash.update(name);
                hash.update(readFileSync(path));
            }
        }
    };
    walk(entryDir);
    return hash.digest("hex").slice(0, 12);
}
