import {Request, Response} from "express";
import {IDependencies} from "../Services";
import ServerError from "../consts/ServerError";
import PvpTestController, {IRequestData} from "../services-impl/PvpTestController";

export default class PvpTestHandler {
    readonly _controller: PvpTestController;

    constructor(
        private readonly _deps: IDependencies,
    ) {
        this._controller = new PvpTestController(_deps);
    }

    async getRegisteredMatches(req: Request, res: Response) {
        await this.tryCatch(res, async () => {
            const players = await this._controller.getRegisteredMatch();
            const dataArray: IRequestData[] = Array.from(players.values());
            res.sendSuccess(dataArray);
        });
    }

    async registerMatch(req: Request, res: Response) {

        await this.tryCatch(res, async () => {
            const data = req.body as IRequestData;
            await this._controller.registerMatch(data);
            res.sendSuccess(true);
        });
    }


    async unregisterMatch(req: Request, res: Response) {
        interface IRequestData {
            id: string[];
        }

        await this.tryCatch(res, async () => {
            const data = req.body as IRequestData;
            await this._controller.unregisterMatch(data.id);
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