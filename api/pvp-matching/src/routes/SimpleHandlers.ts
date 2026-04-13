import {Request, Response} from "express";

export function warmUp(req: Request, res: Response) {
    res.sendStatus(200);
}