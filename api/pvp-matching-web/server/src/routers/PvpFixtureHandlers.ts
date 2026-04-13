import {Request, Response} from "express";
import {IDependencies} from "../Services";
import ServerError from "../consts/ServerError";
import PvpFixtureController, {IRegisterMatchGroupData} from "../services-impl/PvpFixtureController";

export default class PvpFixtureHandlers {
    readonly _controller: PvpFixtureController;

    constructor(
        private readonly _deps: IDependencies,
    ) {
        this._controller = new PvpFixtureController(_deps);
    }

    async getRegisteredMatches(req: Request, res: Response) {
        await this.tryCatch(res, async () => {
            const matches = await this._controller.getRegisteredMatches();
            res.sendSuccess(matches);
        });
    }

    async registerMatchGroup(req: Request, res: Response) {
        await this.tryCatch(res, async () => {
            const data = req.body as IRegisterMatchGroupData;
            const result = await this._controller.registerMatchGroup(data);
            res.sendSuccess(result.length > 0);
        });
    }

    async unregisterMatch(req: Request, res: Response) {
        interface IRequestData {
            id: number[];
        }

        await this.tryCatch(res, async () => {
            const data = req.body as IRequestData;
            await this._controller.unregisterMatches(data.id);
            res.sendSuccess(true);
        });
    }

    private async tryCatch(res: Response, call: () => Promise<void>) {
        try {
            await call();
        } catch (e) {
            this._deps.logger.error(e);
            if (e instanceof ServerError) {
                res.sendError(e.message);
            } else {
                res.sendGenericError();
            }
        }
    }
}