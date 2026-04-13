import {Request, Response} from "express";

function healthCheckHandler(req: Request, res: Response) {
    res.status(200).send('OK');
}

function tooManyRequest(req: Request, res: Response) {
    res.sendError("Too many requests, please try again later.", 429);
}

const simpleHandlers = {
    healthCheckHandler, tooManyRequest
};

export default simpleHandlers;