import {Express} from "express";
import {ILogger} from "../Services";

export function printRoutes(app: Express, logger: ILogger) {
    const routes: string[] = [];
    collectRoutes(app._router.stack, '', routes);
    logger.info(`Registered routes:\n${routes.join('\n')}`);
}

function extractPrefix(middleware: any): string {
    return middleware.regexp.source
        .replace('\\/?', '').replace('(?=\\/|$)', '').replace(/\\/g, '').replace('^', '').replace('?', '');
}

function collectRoutes(stack: any[], prefix: string, routes: string[]) {
    stack.forEach((layer: any) => {
        if (layer.route) {
            const methods = Object.keys(layer.route.methods).join(',').toUpperCase();
            routes.push(`${methods} ${prefix}${layer.route.path}`);
        } else if (layer.name === 'router' && layer.handle.stack) {
            const routerPrefix = extractPrefix(layer);
            collectRoutes(layer.handle.stack, prefix + routerPrefix, routes);
        }
    });
}
